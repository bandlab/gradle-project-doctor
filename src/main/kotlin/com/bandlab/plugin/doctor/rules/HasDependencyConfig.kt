package com.bandlab.plugin.doctor.rules

import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import java.io.Serializable

data class HasDependencyConfig(
    val pluginId: String,
    val dependency: MatchableValue,
    val excludedDependency: Set<MatchableValue>,
    val projectExclusion: ProjectExclusionConfig,
) : Serializable