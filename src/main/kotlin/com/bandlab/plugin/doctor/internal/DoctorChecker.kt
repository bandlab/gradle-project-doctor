package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.DoctorEntry
import com.bandlab.plugin.doctor.internal.models.MessageGroup
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import org.gradle.api.logging.Logger

internal fun runDoctorCheck(
    logger: Logger,
    project: ProjectComposition,
    entries: Set<DoctorEntry>,
): List<MessageGroup> {
    val filteredEntries = entries.filter { entry -> entry.selector.isApplicable(project) }
    val entriesByErrorLevel = filteredEntries.groupBy { it.errorLevel }

    return entriesByErrorLevel.mapNotNull { (errorLevel, entriesWithLevel) ->

        // Collect all messages for the current error level
        val messages = entriesWithLevel.flatMap { entry ->

            val isExcluded = entry.isProjectExcluded(project)
            if (!isExcluded) {
                buildList {
                    entry.rule.validate(project) { errorMsg ->
                        add(errorMsg)

                        logger.info(
                            """
                            [Project Doctor] $errorLevel - $errorMsg
                                Selector: ${entry.selector}
                                Rule: ${entry.rule}
                            """.trimIndent()
                        )
                    }
                }
            } else {
                emptyList()
            }
        }

        // Only create message groups for levels that have messages
        if (messages.isNotEmpty()) {
            MessageGroup(errorLevel = errorLevel, messages = messages)
        } else null
    }
}

private fun DoctorEntry.isProjectExcluded(project: ProjectComposition): Boolean {
    val exclusionMatchers = projectExclusion?.matchers ?: return false
    return exclusionMatchers.any { it.isExcluded(project) }
}