@file:Suppress("UnstableApiUsage")

package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.PluginAwareEntry
import com.bandlab.plugin.doctor.ProjectSelector
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.selector.ProjectSelectorConfig

internal class WithSelector(
    private val config: ProjectSelectorConfig,
) : ProjectSelector, PluginAwareEntry {

    private val pluginId = config.pluginId
    override val pluginIds: Set<String> = setOf(pluginId)

    override fun isApplicable(project: ProjectComposition): Boolean {
        val isNameMatch = config.name.isMatch(project.path)
        val isPluginMatch = if (pluginId.isEmpty()) true else project.plugins.contains(pluginId)

        return isNameMatch && isPluginMatch
    }

    override fun toString(): String {
        return "WithSelector(config=$config, pluginId='$pluginId')"
    }
}

internal class WithDependencySelector(
    private val config: ProjectSelectorConfig,
) : ProjectSelector, PluginAwareEntry {

    override val pluginIds: Set<String> get() = setOf(config.pluginId)

    override fun isApplicable(project: ProjectComposition): Boolean {
        return isDependencyMatch(project) && isPluginMatch(project)
    }

    private fun isDependencyMatch(project: ProjectComposition): Boolean {
        val dependency = config.name
        project.dependencies.forEach { dependencyItem ->
            if (dependency.isMatch(dependencyItem)) {
                return true
            }
        }
        return false
    }

    private fun isPluginMatch(project: ProjectComposition): Boolean {
        val pluginId = config.pluginId
        if (pluginId.isEmpty()) return true
        return project.subProjects.any { it.plugins.contains(pluginId) }
    }

    override fun toString(): String {
        return "WithDependencySelector(config=$config, pluginId='${config.pluginId}')"
    }
}