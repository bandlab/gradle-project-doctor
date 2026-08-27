package com.bandlab.plugin.doctor.internal.rules.exclusion

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import java.io.Serializable

interface ProjectExclusionMatcher : Serializable {
    fun isExcluded(project: ProjectComposition): Boolean
}