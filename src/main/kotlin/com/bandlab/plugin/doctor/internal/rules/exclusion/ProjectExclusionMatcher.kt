// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.rules.exclusion

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import java.io.Serializable

interface ProjectExclusionMatcher : Serializable {
    fun isExcluded(project: ProjectComposition): Boolean
}
