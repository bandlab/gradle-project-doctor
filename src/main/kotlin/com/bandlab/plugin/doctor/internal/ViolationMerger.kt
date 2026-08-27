package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.internal.models.MessageGroup
import org.gradle.internal.cc.base.logger
import org.json.JSONArray
import java.io.File

internal class ViolationMerger {

    fun createMergedViolations(
        jsonReportFiles: List<File>,
        baselineFile: File? = null,
    ): List<MessageGroup> {
        val messageGroups = jsonReportFiles
            .filter { it.exists() }
            .flatMap { it.createMessageGroups() }
            .sortedBy { it.errorLevel }
            .groupBy { it.errorLevel }

        val baselineViolations = baselineFile
            ?.createMessageGroups()
            .orEmpty()
            .groupBy { it.errorLevel }

        // Merge messages with the same ErrorLevel
        val mergedViolations = messageGroups.keys.map { key ->
            MessageGroup(
                errorLevel = key,
                messages = messageGroups[key].orEmpty().flatMap { it.messages }
            )
        }

        // Remove merged violations that exists in baseline
        return mergedViolations.map { group ->
            val baselineGroup = baselineViolations[group.errorLevel].orEmpty()
            val messages = group.messages.filter { message ->
                !baselineGroup.any { it.messages.contains(message) }
            }
            group.copy(messages = messages)
        }.filter {
            it.messages.isNotEmpty()
        }
    }

    private fun File.createMessageGroups(): List<MessageGroup> {
        val text = readText()
        return try {
            val array = JSONArray(text)
            MessageGroup.fromJsonArray(array)
        } catch (e: Exception) {
            logger.error("[Project Doctor] Error parsing file at $path, content: $text")
            throw e
        }
    }
}