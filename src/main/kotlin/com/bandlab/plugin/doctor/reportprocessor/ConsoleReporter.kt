package com.bandlab.plugin.doctor.reportprocessor

import com.bandlab.plugin.doctor.ErrorLevel
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import com.bandlab.plugin.doctor.internal.utils.LoggerOutput
import com.bandlab.plugin.doctor.internal.utils.PillBoxPrinter
import com.bandlab.plugin.doctor.internal.utils.TextStyler
import org.gradle.api.logging.Logger

internal class ConsoleReporter(
    private val logger: Logger,
    private val reportTitle: String? = null,
    private val useStyledOutput: Boolean = false,
) : MergedViolationProcessor {

    private val textStyler = TextStyler()
    private val output = LoggerOutput(logger)

    override fun process(mergedViolations: List<MessageGroup>): ProcessStatus {
        val messages = createOutputMessages(mergedViolations)
        val containsError = mergedViolations.find { it.errorLevel is ErrorLevel.Error } != null
        return reportViolations(violationMessage = messages, containsError = containsError)
    }

    private fun createOutputMessages(violations: List<MessageGroup>): String {
        return violations.filter { it.messages.isNotEmpty() }
            .joinToString(separator = System.lineSeparator()) {

                val additionalMessage = if (!it.errorLevel.msg.isNullOrEmpty()) {
                    "** ${it.errorLevel.msg} **\n"
                } else {
                    ""
                }
                val extraLineBreak = if (additionalMessage.isNotEmpty()) "\n" else ""

                val indicator = it.errorLevel.createIndicator()
                val items = it.messages
                    .filter { it.isNotEmpty() }
                    .joinToString("\n") { msg ->

                        if (useStyledOutput) {
                            val styledMessage = when (it.errorLevel) {
                                is ErrorLevel.Error -> textStyler.red(msg)
                                is ErrorLevel.Warning -> textStyler.yellow(msg)
                            }

                            "- $indicator $styledMessage"
                        } else {
                            "$indicator $msg"
                        }
                    }

                "$additionalMessage$items$extraLineBreak"
            }
    }

    private fun ErrorLevel.createIndicator(): String {
        return if (useStyledOutput) {
            when (this) {
                is ErrorLevel.Error -> textStyler.red("❌ ")
                is ErrorLevel.Warning -> textStyler.yellow("⚠️ ")
            }
        } else {
            when (this) {
                is ErrorLevel.Error -> "[DoctorError]"
                is ErrorLevel.Warning -> "[DoctorWarning]"
            }
        }
    }

    private fun reportViolations(
        violationMessage: String,
        containsError: Boolean,
    ): ProcessStatus {
        val disablePillbox = reportTitle.isNullOrEmpty()
        if (disablePillbox) {
            output.println(violationMessage)
            if (containsError) {
                return ProcessStatus.Failure(violationMessage)
            }
            return ProcessStatus.Success
        }

        // Print to console in pillbox format
        val printer = PillBoxPrinter(output, requireNotNull(reportTitle))

        if (violationMessage.isNotEmpty()) {
            printer.writePrescription(violationMessage)
            if (containsError) {
                return ProcessStatus.Failure("Project Doctor found error(s)")
            }
        } else {
            printer.writePrescription("Project Doctor found no issues")
        }
        return ProcessStatus.Success
    }
}