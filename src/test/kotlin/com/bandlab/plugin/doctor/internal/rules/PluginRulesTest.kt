// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.PluginRuleConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PluginRulesTest {

    // region HasPluginRule

    @Test
    fun `HasPluginRule reports error when plugin is present`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("com.android.library"),
                dependencies = emptySet(),
            )
        val config =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val rule = HasPluginRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).contains("com.android.library")
    }

    @Test
    fun `HasPluginRule does not report when plugin absent`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("kotlin"),
                dependencies = emptySet(),
            )
        val config =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val rule = HasPluginRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    // endregion

    // region NoPluginRule

    @Test
    fun `NoPluginRule reports error when plugin is absent`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = emptySet(),
            )
        val config =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val rule = NoPluginRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).contains("doesn't have plugin")
    }

    @Test
    fun `NoPluginRule does not report when plugin present`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("com.android.library"),
                dependencies = emptySet(),
            )
        val config =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val rule = NoPluginRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    // endregion

    // region NameRule

    @Test
    fun `NameRule reports when path matches all nameMatchers`() {
        // given
        val project =
            ProjectComposition(
                path = ":image:zoom:screen",
                plugins = emptySet(),
                dependencies = emptySet(),
            )
        val config =
            com.bandlab.plugin.doctor.rules.NameRuleConfig().apply {
                nameContains("screen")
            }
        val rule = NameRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
    }

    @Test
    fun `NameRule does not report when path does not match`() {
        // given
        val project =
            ProjectComposition(
                path = ":common:utils",
                plugins = emptySet(),
                dependencies = emptySet(),
            )
        val config =
            com.bandlab.plugin.doctor.rules.NameRuleConfig().apply {
                nameContains("screen")
            }
        val rule = NameRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    // endregion

    // region ErrorRule

    @Test
    fun `ErrorRule replaces placeholders and reports`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("plugin.a", "plugin.b"),
                dependencies = emptySet(),
            )
        val rule = ErrorRule("Project {{PATH}} has plugins {{PLUGINS}}")

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).isEqualTo("Project :feature:a has plugins plugin.a, plugin.b")
    }

    @Test
    fun `ErrorRule always reports regardless of project`() {
        // given
        val project = ProjectComposition(path = ":any", plugins = emptySet())
        val rule = ErrorRule("always")

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).isEqualTo("always")
    }

    // endregion
}
