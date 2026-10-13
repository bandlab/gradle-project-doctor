// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.NameRuleConfig

internal class NameRule(private val config: NameRuleConfig) : DoctorRule {

    override fun validate(project: ProjectComposition, onError: (String) -> Unit) {
        val isNameInvalid =
            config.nameMatchers.all { matcher ->
                matcher.isMatch(project.path)
            }

        if (isNameInvalid) {
            onError("${project.path} does not match the name convention")
        }
    }

    override fun toString(): String {
        return "NameRule(matchers='${config.nameMatchers}')"
    }
}
