package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.PluginAwareEntry
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.PluginRuleConfig

internal class NoPluginRule(config: PluginRuleConfig) : DoctorRule, PluginAwareEntry {

    private val pluginId = config.pluginId
    override val pluginIds: Set<String> = setOf(config.pluginId)

    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        if (!project.plugins.contains(pluginId)) {
            onError("${project.path} doesn't have plugin $pluginId")
        }
    }

    override fun toString(): String {
        return "NoPluginRule(pluginId='$pluginId', pluginIds=$pluginIds)"
    }
}