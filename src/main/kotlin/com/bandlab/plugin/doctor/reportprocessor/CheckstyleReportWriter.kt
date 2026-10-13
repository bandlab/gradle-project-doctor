// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.reportprocessor

import com.bandlab.plugin.doctor.ErrorLevel
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import java.io.File

internal class CheckstyleReportWriter(
    private val buildGradleFile: File,
    private val outputFile: File,
) : MergedViolationProcessor {

    override fun process(mergedViolations: List<MessageGroup>): ProcessStatus {
        val items = mergedViolations.map { it.toCheckstyleItems() }.flatten()

        // Do not create file if there is no violation
        if (items.isEmpty()) return ProcessStatus.Success

        val violationItems = items.joinToString(separator = System.lineSeparator())
        val reportContent =
            """
            <checkstyle version="4.3">
                <file name="${buildGradleFile.absolutePath}">
                $violationItems
                </file>
            </checkstyle>
        """
                .trimIndent()

        outputFile.delete()
        outputFile.writeText(reportContent)

        return ProcessStatus.Success
    }

    private fun MessageGroup.toCheckstyleItems(): List<String> {
        if (messages.isEmpty()) return emptyList()

        val severity =
            when (errorLevel) {
                is ErrorLevel.Error -> "error"
                is ErrorLevel.Warning -> "warning"
            }
        val prefix = errorLevel.msg.orEmpty()
        return messages.map { msg ->
            val escapedMessage = escapeXml("$prefix\n$msg")
            """<error line="0" severity="$severity" message="$escapedMessage" source="ProjectDoctor"/>"""
        }
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("\n", "&#10;")
            .replace("\r", "&#13;")
    }
}
