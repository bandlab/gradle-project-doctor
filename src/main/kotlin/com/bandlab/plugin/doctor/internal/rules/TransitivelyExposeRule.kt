// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition

/**
 * Fails when a project transitively exposes (via `api(...)`) any dependency matching [dependency].
 *
 * Unlike [HasDependencyRule], which inspects the full runtime classpath, this rule only looks at
 * [ProjectComposition.exposedDependencies]: the dependencies the module author declared with the
 * `api` configuration, so `implementation(...)` never triggers it.
 */
internal class TransitivelyExposeRule(private val dependency: MatchableValue) : DoctorRule {

    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        project.exposedDependencies
            .filter { dependency.isMatch(it) }
            .forEach { onError("${project.path} transitively exposes \"$it\" via api(...)") }
    }

    override fun toString(): String {
        return "TransitivelyExposeRule(dependency=$dependency)"
    }
}
