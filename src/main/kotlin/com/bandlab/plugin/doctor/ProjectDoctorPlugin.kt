package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.DoctorCheckAttr
import com.bandlab.plugin.doctor.internal.utils.exposedDeclaredComponents
import com.bandlab.plugin.doctor.internal.utils.getBuildProperty
import com.bandlab.plugin.doctor.internal.utils.runtimeClasspathConfigurations
import com.bandlab.plugin.doctor.internal.utils.runtimeResolvedComponents
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationPublications
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Plugin that applies [DoctorCheckRootTask] to the root project and [DoctorCheckTask] to the subprojects.
 *
 * ## High level overview
 *
 * 1. In configuration phase, it will configure all the tasks.
 * 2. Sub-projects will run [DoctorPopulateTask] to generate composition JSON.
 * 3. Sub-projects will run [DoctorCheckTask], analyze the project based on the [DoctorEntry], generate a JSON file.
 * 4. Root project will run [DoctorCheckRootTask] to merge all the JSON files from the sub-projects.
 *
 * ## Variant-Aware Configuration Resolution
 *
 * This plugin uses Gradle's variant-aware dependency resolution to aggregate JSON reports from subprojects:
 *
 * - **Variant name**: A logical identifier (e.g., "doctorCheck") used for matching producer and consumer configurations
 * - **Configuration names**: The actual Gradle configuration objects (e.g., "doctorCheckResolvable", "doctorCheckConsumable")
 *
 * Matching happens via **attributes**, not configuration names:
 *
 * 1. Producer (subproject) creates `doctorCheckConsumable` configuration with attribute `DoctorCheckAttr.ATTR = "doctorCheck"`
 * 2. Consumer (root) creates `doctorCheckResolvable` configuration with attribute `DoctorCheckAttr.ATTR = "doctorCheck"`
 * 3. When root depends on `:subproject:doctorCheckConsumable`, Gradle matches configurations by their variant attributes
 *
 * This approach decouples producer and consumer configurations and allows multiple configurations to provide the same variant type.
 */
@Suppress("UnstableApiUsage")
class ProjectDoctorPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        val isIdeaSync = target.providers.systemProperty("idea.sync.active").getOrElse("false").toBoolean()
        if (isIdeaSync && target.disableOnSync) {
            // Create extension to avoid extension not found error
            if (target == target.rootProject) {
                ProjectDoctorExtension.create(target.rootProject)
            }
            return
        }

        if (target == target.rootProject) {
            target.applyRootProject()
        } else {
            target.applyProject()
        }
    }

    /**
     * Create an aggregating task that only active when the exact path (`:doctorCheck`)
     * or relative path (`doctorCheck`) task is requested.
     */
    private fun Project.applyRootProject() {
        if (rootProject != this) return
        val extension = ProjectDoctorExtension.create(rootProject)
        val aggregateTaskNames = setOf(
            TASK_DOCTOR_CHECK, ":$TASK_DOCTOR_CHECK",
            TASK_BASELINE_GENERATE, ":$TASK_BASELINE_GENERATE",
        )
        val isAggregateRequested = alwaysEnableAggregation || gradle.startParameter.taskRequests.any { request ->
            request.args.any { taskPath -> taskPath in aggregateTaskNames }
        }

        // Register schema for standalone usage (when CiCheckPlugin is not applied)
        rootProject.dependencies.attributesSchema {
            attribute(DoctorCheckAttr.ATTR)
        }

        if (!isAggregateRequested) return
        logger.info("[Project Doctor] Configuring aggregation task…")

        val rootDoctorConf = configurations.dependencyScope(DepsScope.CHECK)
        val doctorCheckDependency = rootProject.resolvable(
            name = ConfigurationSpec.doctorCheck,
            parentConf = provider { rootDoctorConf.get() },
        )

        // baseline: separate non-failing variant
        val rootBaselineConf = configurations.dependencyScope(DepsScope.BASELINE)
        val doctorBaselineDependency = rootProject.resolvable(
            name = ConfigurationSpec.doctorBaseline,
            parentConf = provider { rootBaselineConf.get() },
        )

        subprojects.forEach { sub ->
            dependencies.add(rootDoctorConf.name, consumableDeps(sub.path, ConfigurationSpec.doctorCheck))
            dependencies.add(rootBaselineConf.name, consumableDeps(sub.path, ConfigurationSpec.doctorBaseline))
        }

        tasks.register<DoctorCheckRootTask>(TASK_DOCTOR_CHECK) {
            baselineFile.set(extension.baselineFile)
            mergedReportFile.set(layout.buildDirectory.file("doctor/merged.json"))
            jsonReportFiles.from(doctorCheckDependency.map { it.incoming.artifactView { lenient(true) }.files })
        }

        tasks.register<DoctorCreateBaselineTask>(TASK_BASELINE_GENERATE) {
            baselineFile.set(extension.baselineFile)
            jsonReportFiles.from(doctorBaselineDependency.map { it.incoming.artifactView { lenient(true) }.files })
        }
    }

    private fun Project.applyProject() {
        val project = this
        if (rootProject == project) return

        val populateTask = tasks.register<DoctorPopulateTask>(TASK_DOCTOR_POPULATE) {
            val extension = ProjectDoctorExtension.from(project)
            projectPath.set(project.path)
            observedPlugins.set(
                extension.observedPlugins.map { observed ->
                    observed.filter { project.pluginManager.hasPlugin(it) }.toSet()
                },
            )

            dependencies.set(project.runtimeResolvedComponents())
            exposedDependencies.set(project.exposedDeclaredComponents())

            val rootDir = project.isolated.rootProject.projectDirectory
            val projectPaths = project.projectDir.relativeTo(rootDir.asFile).path
            val rootDoctorOutputDir = rootDir.dir("build").dir("doctor/${projectPaths}")
            projectCompositionFile.set(rootDoctorOutputDir.file(PROJECT_COMPOSITION_FILE))
        }

        project.consumable(ConfigurationSpec.doctorComposition) {
            artifact(populateTask.flatMap { it.projectCompositionFile })
        }

        // only direct project deps; avoids unwanted deps like desktop-variant KMP libs
        val doctorCompositionDeps = configurations.dependencyScope(DepsScope.COMPOSITION)
        val compositionResolvable = project.resolvable(
            name = ConfigurationSpec.doctorComposition,
            parentConf = provider { doctorCompositionDeps.get() },
        )
        compositionResolvable.configure { isTransitive = false }

        doctorCompositionDeps.get().withDependencies {
            val seen = mutableSetOf<String>()
            fun addDep(path: String) {
                if (seen.add(path)) add(consumableDeps(path, ConfigurationSpec.doctorComposition))
            }
            fun collect(conf: Configuration, visited: MutableSet<Configuration> = mutableSetOf()) {
                if (!visited.add(conf)) return
                conf.dependencies.filterIsInstance<ProjectDependency>().forEach { addDep(it.path) }
                conf.extendsFrom.forEach { collect(it, visited) }
            }
            runtimeClasspathConfigurations().forEach { collect(it) }
        }
        val compositionFiles = compositionResolvable.map {
            // lenient: missing consumable -> size check in DoctorValidate
            it.incoming.artifactView { lenient(true) }.files
        }

        tasks.register<DoctorValidateTask>(TASK_DOCTOR_VALIDATE) {
            projectCompositionFile.set(populateTask.flatMap { it.projectCompositionFile })
            subProjectCompositionFiles.from(compositionFiles)
        }

        val checkTask = tasks.register<DoctorCheckTask>(TASK_DOCTOR_CHECK) {
            val extension = ProjectDoctorExtension.from(project)
            summaryOnly.set(false)
            baselineFile.set(extension.baselineFile)

            projectCompositionFile.set(populateTask.flatMap { it.projectCompositionFile })
            subProjectCompositionFiles.from(compositionFiles)
            configure(extension, project)
        }

        // summaryOnly baseline; same check, distinct outputs
        val baselineTask = tasks.register<DoctorCheckTask>(TASK_BASELINE_GENERATE) {
            val extension = ProjectDoctorExtension.from(project)
            summaryOnly.set(true)

            projectCompositionFile.set(populateTask.flatMap { it.projectCompositionFile })
            subProjectCompositionFiles.from(compositionFiles)
            configure(
                extension = extension,
                project = project,
                reportFileName = "baseline_result.json",
                checkstyleFileName = "baseline_checkstyle.xml",
            )
        }

        project.consumable(ConfigurationSpec.doctorCheck) {
            artifact(checkTask.flatMap { it.jsonReportFile })
        }

        project.consumable(ConfigurationSpec.doctorBaseline) {
            artifact(baselineTask.flatMap { it.jsonReportFile })
        }

        if (isEnableCompileTaskIntegration) {
            tasks.withType<KotlinCompile>().configureEach {
                dependsOn(checkTask)
            }
        }
    }

    private fun Project.resolvable(name: ConfigurationName, parentConf: Provider<Configuration>? = null) =
        project.configurations.resolvable(name.resolvable) {
            if (parentConf != null) {
                extendsFrom(parentConf)
            }
            attributes { attribute(DoctorCheckAttr.ATTR, name.variantName) }
        }

    private fun Project.consumable(
        name: ConfigurationName,
        outgoingAction: Action<ConfigurationPublications>,
    ) = project.configurations.consumable(name.consumable) {
        attributes { attribute(DoctorCheckAttr.ATTR, name.variantName) }
        outgoing(outgoingAction)
    }

    private fun Project.consumableDeps(
        path: String,
        name: ConfigurationName,
    ) = dependencies.project(
        mapOf(
            "path" to path,
            "configuration" to name.consumable,
        ),
    )

    private val Project.isEnableCompileTaskIntegration: Boolean
        get() = getBuildProperty("PROJECT_DOCTOR_ENABLE_ON_COMPILE") == "true"

    private val Project.disableOnSync: Boolean
        get() = getBuildProperty("PROJECT_DOCTOR_DISABLE_ON_SYNC") == "true"

    private val Project.alwaysEnableAggregation: Boolean
        get() = getBuildProperty("PROJECT_DOCTOR_AGGR_ALWAYS_ENABLE") == "true"

    companion object {
        private const val TASK_DOCTOR_POPULATE = "doctorPopulate"
        private const val TASK_DOCTOR_CHECK = "doctorCheck"
        private const val TASK_BASELINE_GENERATE = "doctorGenerateBaseline"
        private const val TASK_DOCTOR_VALIDATE = "doctorValidate"

        private const val PROJECT_COMPOSITION_FILE = "project_composition.json"
    }

    private object DepsScope {
        const val CHECK = "doctorCheckDeps"
        const val BASELINE = "doctorBaselineDeps"
        const val COMPOSITION = "doctorCompositionDeps"
    }

    /**
     * Specification for project doctor plugin configurations.
     *
     * Each configuration has three forms:
     * - [ConfigurationName.variantName]: Used as the attribute value for variant selection
     * - [ConfigurationName.resolvable]: Resolvable configuration for resolving dependencies
     * - [ConfigurationName.consumable]: Consumable configuration for publishing artifacts
     */
    private object ConfigurationSpec {
        val doctorCheck = ConfigurationName("doctorCheck")
        val doctorBaseline = ConfigurationName("doctorBaseline")
        val doctorComposition = ConfigurationName("doctorComposition")
    }

    /**
     * Represents a configuration family for the project doctor plugin.
     *
     * @param variantName The variant identifier used as the attribute value for variant selection.
     *                    This is the common name shared by resolvable and consumable configurations.
     */
    data class ConfigurationName(val variantName: String) {
        val resolvable: String = "${variantName}Resolvable"
        val consumable: String = "${variantName}Consumable"
    }
}
