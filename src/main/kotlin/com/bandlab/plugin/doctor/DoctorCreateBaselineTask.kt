package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.ViolationMerger
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import com.bandlab.plugin.doctor.internal.utils.LoggerOutput
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

@org.gradle.api.tasks.UntrackedTask(because = "Creates baseline via logger; not safely cacheable")
abstract class DoctorCreateBaselineTask : DefaultTask() {

    /**
     * JSON report files from the sub-projects that created by [DoctorCheckTask]
     */
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val jsonReportFiles: ConfigurableFileCollection

    @get:OutputFile
    abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun run() {
        val processor = ViolationMerger()
        val mergedViolations = processor.createMergedViolations(jsonReportFiles.files.toList())

        val output = baselineFile.asFile.get()
        output.writeText(MessageGroup.toJsonArray(mergedViolations).toString())

        val loggerOutput = LoggerOutput(logger)
        loggerOutput.println("[Project Doctor] Baseline file created at ${output.absolutePath}")
    }
}