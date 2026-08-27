package com.bandlab.plugin.doctor.rules.exclusion

import com.bandlab.plugin.doctor.internal.rules.exclusion.AllExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.PluginExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectExclusionMatcher
import java.io.Serializable

/**
 * A class that contains [ProjectExclusionMatcher]s to exclude projects from a rule check.
 */
data class ProjectExclusionConfig(
    val matchers: List<ProjectExclusionMatcher>,
) : Serializable

internal fun ProjectExclusionConfig.observedPlugins(): Set<String> {
    return matchers.flatMap {
        when (it) {
            is AllExclusionMatcher -> it.matchers.mapNotNull { (it as? PluginExclusionMatcher)?.pluginId?.value }
            is PluginExclusionMatcher -> listOf(it.pluginId.value)
            else -> emptyList()
        }
    }.toSet()
}