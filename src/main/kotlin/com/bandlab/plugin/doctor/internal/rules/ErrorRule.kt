package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.internal.models.ProjectComposition

/**
 * A [DoctorRule] that reports every selected project using a message template.
 *
 * Usage:
 *
 * ```kotlin
 * errorOn {
 *   projectWith { nameContains(":legacy:") }
 *     .reportAs("{{PATH}} is a legacy module. Migrate before adding features")
 * }
 * ```
 *
 * List of available placeholders:
 * - `{{PATH}}`: The path of the project that triggered the error
 * - `{{PLUGINS}}`: The observed plugin IDs recorded for the selected project, not all applied plugins
 */
class ErrorRule(private val messageTemplate: String) : DoctorRule {
    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        val message = messageTemplate
            .replace("{{PATH}}", project.path)
            .replace("{{PLUGINS}}", project.plugins.joinToString())
        onError(message)
    }
}
