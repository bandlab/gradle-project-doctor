// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.ViolationReporter
import com.bandlab.plugin.doctor.reportprocessor.ConsoleReporter
import com.bandlab.plugin.doctor.reportprocessor.ViolationsJsonWriter
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@org.gradle.api.tasks.UntrackedTask(
    because = "Aggregates doctor reports via logger/console; not safely cacheable",
)
abstract class DoctorCheckRootTask : DefaultTask() {

    /** JSON report files from the sub-projects that created by [DoctorCheckTask] */
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val jsonReportFiles: ConfigurableFileCollection

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    // @InputFiles tolerates a missing file, which is treated as an empty baseline
    @get:InputFiles
    abstract val baselineFile: RegularFileProperty

    /** Merged JSON report file as the output */
    @get:OutputFile abstract val mergedReportFile: RegularFileProperty

    @Suppress("unused", "PropertyName")
    @get:Option(option = "summary", description = "This is ignored and unused in root task")
    @get:Optional
    @get:Input
    abstract val _ignored: Property<Boolean>

    @TaskAction
    fun run() {
        val reporter =
            ViolationReporter(
                mergedViolationProcessors =
                    listOf(
                        ConsoleReporter(
                            logger = logger,
                            useStyledOutput = true,
                            reportTitle = "Project Doctor Report",
                        ),
                        ViolationsJsonWriter(mergedReportFile.get().asFile),
                    ),
            )

        reporter.report(
            jsonReportFiles = jsonReportFiles.files.toList(),
            baselineFile = baselineFile.orNull?.asFile,
        )
    }
}
