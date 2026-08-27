package com.bandlab.plugin.doctor.internal.rules.exclusion

import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig

internal interface ProjectExclusionOwner {
    val exclusionConfig: ProjectExclusionConfig
}