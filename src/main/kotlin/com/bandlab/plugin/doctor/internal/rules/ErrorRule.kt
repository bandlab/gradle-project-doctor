package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.internal.models.ProjectComposition

/**
 * A [DoctorRule] that match all project to error.
 *
 * Usage:
 *
 * ```
 * errorOn {
 *   projectWith { plugin(bandlab.plugins.databinding) }
 *     .reportAs(""Data binding is no longer allowed in {{PATH}} }"
 * }
 * ```
 *
 * List of available placeholders:
 * - `{{PATH}}`: The path of the project that triggered the error
 * - `{{PLUGINS}}`: The list of plugins applied to the project that triggered the error
 */
class ErrorRule(private val messageTemplate: String) : DoctorRule {
    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        val message = messageTemplate
            .replace("{{PATH}}", project.path)
            .replace("{{PLUGINS}}", project.plugins.joinToString())
        onError(message)
    }
}