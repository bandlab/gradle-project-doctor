package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.reportprocessor.MergedViolationProcessor
import com.bandlab.plugin.doctor.reportprocessor.ProcessStatus
import java.io.File

/**
 * An entry point for reporting violation messages from Project Doctor.
 *
 * It takes a list of JSON report files and a baseline file, merges the violations and processes
 * them using the provided [MergedViolationProcessor] implementations.
 *
 * @see ViolationMerger
 * @see MergedViolationProcessor
 */
internal class ViolationReporter(
    private val merger: ViolationMerger = ViolationMerger(),
    private val mergedViolationProcessors: List<MergedViolationProcessor>,
) {
    fun report(
        jsonReportFiles: List<File>,
        baselineFile: File?,
    ) {
        val mergedViolations = merger.createMergedViolations(
            jsonReportFiles = jsonReportFiles,
            baselineFile = baselineFile
        )

        val statuses = mergedViolationProcessors.map { it.process(mergedViolations) }
        val failure = statuses.find { it is ProcessStatus.Failure }
        if (failure != null) {
            if (failure is ProcessStatus.Failure) {
                error(failure.errorMessage)
            } else {
                error(failure)
            }
        }
    }
}