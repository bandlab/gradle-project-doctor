// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.ViolationReporter
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.internal.runDoctorCheck
import com.bandlab.plugin.doctor.reportprocessor.CheckstyleReportWriter
import com.bandlab.plugin.doctor.reportprocessor.ConsoleReporter
import com.bandlab.plugin.doctor.reportprocessor.ViolationsJsonWriter
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.json.JSONObject

/**
 * A task to check the project composition against the doctor entries
 *
 * It will generate a JSON report file that will be stored in [jsonReportFile] If the [summaryOnly]
 * is set to true, it will only generate the report without failing the task. In this case, the
 * responsibility will be passed to [DoctorCheckRootTask]
 */
@org.gradle.api.tasks.UntrackedTask(
    because = "Reports violations via logger/console and baseline filtering; not safely cacheable",
)
abstract class DoctorCheckTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val projectCompositionFile: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val subProjectCompositionFiles: ConfigurableFileCollection

    @get:Input abstract val entries: SetProperty<DoctorEntry>

    @get:Option(
        option = "summary",
        description = "Whether or not to enable the summary only report",
    )
    @get:Input
    abstract val summaryOnly: Property<Boolean>

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    // @InputFiles tolerates a missing file, which is treated as an empty baseline
    @get:InputFiles
    abstract val baselineFile: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val buildGradleFile: RegularFileProperty

    @get:OutputFile abstract val jsonReportFile: RegularFileProperty

    @get:OutputFile abstract val checkstyleReportFile: RegularFileProperty

    @TaskAction
    fun run() {
        val projectCompFile = projectCompositionFile.get().asFile
        val localComposition = ProjectComposition.fromJson(JSONObject(projectCompFile.readText()))

        val depsCompositions =
            subProjectCompositionFiles.files
                .map { file -> file.toProjectComposition() }
                .associateBy { it.path }

        val subProjects = localComposition.dependencies.mapNotNull { depsCompositions[it] }.toSet()
        val project = localComposition.copy(subProjects = subProjects)

        if (logger.isInfoEnabled) {
            logger.info("[Project Doctor] Project composition: $project")
        }

        val messageOutputs = runDoctorCheck(logger, project, entries.get())
        val containsMessage = messageOutputs.any { it.messages.isNotEmpty() }

        val logFile = jsonReportFile.get().asFile
        if (!containsMessage) {
            logFile.deleteIfExists()
            checkstyleReportFile.get().asFile.deleteIfExists()
            return
        }

        logFile.writeText(MessageGroup.toJsonArray(messageOutputs).toString())

        if (summaryOnly.get().not()) {
            val reporter =
                ViolationReporter(
                    mergedViolationProcessors =
                        listOf(
                            ViolationsJsonWriter(outputFile = logFile),
                            ConsoleReporter(logger = logger),
                            CheckstyleReportWriter(
                                buildGradleFile = buildGradleFile.asFile.get(),
                                outputFile = checkstyleReportFile.asFile.get(),
                            ),
                        ),
                )
            reporter.report(
                jsonReportFiles = listOf(logFile),
                baselineFile = baselineFile.orNull?.asFile,
            )
        }
    }

    private fun File.toProjectComposition(): ProjectComposition {
        val text = readText()
        return try {
            ProjectComposition.fromJson(JSONObject(text))
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse composition JSON from $path, content: $text", e)
        }
    }

    private fun File.deleteIfExists() {
        if (exists()) delete()
    }

    @Suppress("UnstableApiUsage")
    internal fun configure(
        extension: ProjectDoctorExtension,
        project: Project,
        reportFileName: String = "result.json",
        checkstyleFileName: String = "checkstyle.xml",
    ) {
        this.entries.set(extension.entries)

        val groovyGradle = project.layout.projectDirectory.file("build.gradle")
        if (groovyGradle.asFile.exists()) {
            this.buildGradleFile.set(groovyGradle)
        } else {
            this.buildGradleFile.set(project.layout.projectDirectory.file("build.gradle.kts"))
        }

        val rootDir = project.isolated.rootProject.projectDirectory
        val projectPaths = project.projectDir.relativeTo(rootDir.asFile).path
        val rootDoctorOutputDir = rootDir.dir("build").dir("doctor/${projectPaths}")

        this.jsonReportFile.set(rootDoctorOutputDir.file(reportFileName))
        this.checkstyleReportFile.set(rootDoctorOutputDir.file(checkstyleFileName))
    }
}
