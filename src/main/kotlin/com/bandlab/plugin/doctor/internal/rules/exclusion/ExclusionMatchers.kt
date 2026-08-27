package com.bandlab.plugin.doctor.internal.rules.exclusion

import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition

internal class PluginExclusionMatcher(internal val pluginId: MatchableValue) : ProjectExclusionMatcher {
    override fun isExcluded(project: ProjectComposition): Boolean {
        return project.plugins.any { pluginId.isMatch(it) }
    }
}

internal class DependencyExclusionMatcher(private val dependency: MatchableValue) : ProjectExclusionMatcher {
    override fun isExcluded(project: ProjectComposition): Boolean {
        return project.dependencies.any { dependency.isMatch(it) }
    }
}

internal class ProjectPathExclusionMatcher(private val path: MatchableValue) : ProjectExclusionMatcher {
    override fun isExcluded(project: ProjectComposition): Boolean {
        return path.isMatch(project.path)
    }
}

internal class AllExclusionMatcher(internal val matchers: List<ProjectExclusionMatcher>) : ProjectExclusionMatcher {
    override fun isExcluded(project: ProjectComposition): Boolean {
        return matchers.all { it.isExcluded(project) }
    }
}