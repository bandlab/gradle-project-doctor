package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.extension.ErrorLevelExtension
import com.bandlab.plugin.doctor.internal.ProjectDoctorExtensionService
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import com.bandlab.plugin.doctor.rules.exclusion.observedPlugins
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.create
import java.io.Serializable

/**
 * The extension for the [ProjectDoctorPlugin].
 *
 * Example:
 * ```kotlin
 * projectDoctor {
 *   baselineFile = file("doctor-baseline.json")
 *
 *   errorOn("api modules must apply Kotlin/JVM") {
 *     projectWith { nameEndsWith(":api") }
 *       .thatDoesNotHavePluginWith { id("org.jetbrains.kotlin.jvm") }
 *   }
 *
 *   warnOn("feature modules should not depend directly on org.json") {
 *     projectWith { nameContains(":feature:") }
 *       .thatHasDependencyWith { name("org.json:json") }
 *   }
 * }
 * ```
 */
open class ProjectDoctorExtension(factory: ObjectFactory) : ErrorLevelBuilderExtension {

    override val objectFactory: ObjectFactory = factory

    /**
     * Optional baseline for filtering violations by exact message and severity during checks.
     * Must be configured before running `doctorGenerateBaseline`, which replaces its contents.
     * A missing file is treated as an empty baseline.
     */
    val baselineFile = factory.fileProperty()

    /**
     * A set of doctor entries that will be checked. This is being added in [ErrorLevelExtension]
     * but the configuration builder is in all builder extension class like [com.bandlab.plugin.doctor.extension.EntryBuilderExtension]
     */
    internal val entries: SetProperty<DoctorEntry> = factory.setProperty(DoctorEntry::class.java)

    internal val observedPlugins: Provider<Set<String>> = entries.map { it.observedPlugins() }

    override fun commitEntries(newEntries: Set<DoctorEntry>) {
        entries.addAll(newEntries)
    }

    companion object {
        fun from(project: Project): ProjectDoctorExtension {
            return ProjectDoctorExtensionService.of(project).get().getProjectDoctorExtension()
        }

        fun from(gradle: Gradle): ProjectDoctorExtension {
            return ProjectDoctorExtensionService.of(gradle).get().getProjectDoctorExtension()
        }

        fun create(root: Project): ProjectDoctorExtension {
            check(root == root.rootProject) {
                "ProjectDoctorExtension can only be created in the root project"
            }
            val rootExtension = root.rootProject.extensions.create<ProjectDoctorExtension>("projectDoctor")
            ProjectDoctorExtensionService.of(root).get().setProjectDoctorExtension(rootExtension)
            return rootExtension
        }
    }
}

/**
 * Entry point from the [ProjectDoctorExtension] to create a new [DoctorEntry] with error level.
 */
interface ErrorLevelBuilderExtension : EntryCommiter {

    val objectFactory: ObjectFactory

    fun warnOn(
        additionalMessage: String? = null,
        scope: Action<ErrorLevelExtension>,
    ) {
        val errorLevelExtension = ErrorLevelExtension.create(
            objectFactory = objectFactory,
            errCode = LEVEL_WARN,
            additionalMessage = additionalMessage
        )
        scope.execute(errorLevelExtension)
        commitEntries(errorLevelExtension.entries)
    }

    fun errorOn(
        additionalMessage: String? = null,
        scope: Action<ErrorLevelExtension>,
    ) {
        val errorLevelExtension = ErrorLevelExtension.create(
            objectFactory = objectFactory,
            errCode = LEVEL_ERR,
            additionalMessage = additionalMessage
        )
        scope.execute(errorLevelExtension)
        commitEntries(errorLevelExtension.entries)
    }

    companion object {
        const val LEVEL_ERR = "error"
        const val LEVEL_WARN = "warn"
    }
}

/**
 * An interface that holds a set of [DoctorEntry]s. This is used in all extension builders to easily
 * make it interoperable with Groovy DSL.
 */
interface EntryRegistry {
    val entries: Set<DoctorEntry>
    fun addEntry(entry: DoctorEntry)
}

interface EntryCommiter {
    fun commitEntries(newEntries: Set<DoctorEntry>)
}

/**
 * Return a set of plugin ids that are registered in the doctor extension.
 *
 * This is useful to determine which plugins are being observed by the doctor since we cannot get
 * the list of plugin IDs in the execution phase.
 */
private fun Set<DoctorEntry>.observedPlugins(): Set<String> = flatMap {
    val ruleList = if (it.rule is PluginAwareEntry) it.rule.pluginIds else null
    val selectorList = if (it.selector is PluginAwareEntry) it.selector.pluginIds else null
    val exclusionList = it.projectExclusion?.observedPlugins()
    ruleList.orEmpty() + selectorList.orEmpty() + exclusionList.orEmpty()
}.toSet()

internal interface PluginAwareEntry {
    val pluginIds: Set<String>
}

sealed interface ErrorLevel : Comparable<ErrorLevel>, Serializable {
    val msg: String?

    data class Error(override val msg: String?) : ErrorLevel {
        override fun compareTo(other: ErrorLevel): Int {
            return when (other) {
                is Error -> 0
                is Warning -> 1
            }
        }
    }

    data class Warning(override val msg: String?) : ErrorLevel {
        override fun compareTo(other: ErrorLevel): Int {
            return when (other) {
                is Error -> -1
                is Warning -> 0
            }
        }
    }

    companion object
}

interface DoctorRule : Serializable {
    fun validate(project: ProjectComposition, onError: (String) -> Unit)
}

interface ProjectSelector : Serializable {
    fun isApplicable(project: ProjectComposition): Boolean
}


class DoctorEntry(
    val rule: DoctorRule,
    val selector: ProjectSelector,
    val errorLevel: ErrorLevel,
    val projectExclusion: ProjectExclusionConfig?,
) : Serializable

/** Interop for Kotlin lambda to Gradle Action */
internal fun <T : Any> T.apply(action: Action<T>): T {
    action.execute(this)
    return this
}
