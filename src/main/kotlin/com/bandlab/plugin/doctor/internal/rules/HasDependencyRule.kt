// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.PluginAwareEntry
import com.bandlab.plugin.doctor.extension.MatchableValue.AlwaysInvalid
import com.bandlab.plugin.doctor.extension.MatchableValue.AlwaysValid
import com.bandlab.plugin.doctor.extension.MatchableValue.Contains
import com.bandlab.plugin.doctor.extension.MatchableValue.DoesNotContain
import com.bandlab.plugin.doctor.extension.MatchableValue.EndsWith
import com.bandlab.plugin.doctor.extension.MatchableValue.Match
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.HasDependencyConfig
import com.bandlab.plugin.doctor.rules.exclusion.observedPlugins

internal class HasDependencyRule(private val config: HasDependencyConfig) :
    DoctorRule, PluginAwareEntry {

    override val pluginIds: Set<String>
        get() = config.projectExclusion.observedPlugins()

    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        val dependency = config.dependency
        val excludes = config.excludedDependency

        project.dependencies
            .filter { excludes.none { exclude -> exclude.isMatch(it) } }
            .filter { !isDependencyExcluded(it, project.subProjects) }
            .forEach {
                val isContain = dependency.isMatch(it)
                if (isContain) {
                    val errMessage =
                        when (dependency) {
                            is EndsWith ->
                                "${project.path} has dependency with name ends with \"${dependency.value}\" ($it)"
                            is Contains ->
                                "${project.path} has dependency with name contains \"${dependency.value}\" ($it)"
                            is DoesNotContain ->
                                "${project.path} has dependency with name does not contain \"${dependency.value}\" ($it)"
                            is Match -> "${project.path} has dependency with name $it"
                            is AlwaysInvalid -> error("${project.path} match with AlwaysInvalid")
                            is AlwaysValid -> error("${project.path} match with AlwaysValid")
                        }
                    onError(errMessage)
                }
            }

        val pluginId = config.pluginId
        project.subProjects.forEach {
            it.plugins.forEach { plugin ->
                if (plugin == pluginId) {
                    onError("${project.path} has dependency with plugin $pluginId (${it.path})")
                }
            }
        }
    }

    private fun isDependencyExcluded(
        dependency: String,
        subprojects: Set<ProjectComposition>,
    ): Boolean {
        val matchers = config.projectExclusion.matchers
        val matchedProject =
            subprojects.find { project -> project.path == dependency } ?: return false
        return matchers.any { it.isExcluded(matchedProject) }
    }

    override fun toString(): String {
        return "HasDependencyWithNameRule(dependency=$config)"
    }
}
