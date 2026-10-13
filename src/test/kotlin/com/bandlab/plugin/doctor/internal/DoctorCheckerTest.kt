// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.DoctorEntry
import com.bandlab.plugin.doctor.ErrorLevel
import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.internal.rules.HasDependencyRule
import com.bandlab.plugin.doctor.internal.rules.HasPluginRule
import com.bandlab.plugin.doctor.rules.HasDependencyConfig
import com.bandlab.plugin.doctor.rules.PluginRuleConfig
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import com.bandlab.plugin.doctor.selector.ProjectSelectorConfig
import com.google.common.truth.Truth.assertThat
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class DoctorCheckerTest {

    private val logger: Logger = Logging.getLogger(DoctorCheckerTest::class.java)

    private fun createSelector(block: ProjectSelectorConfig.() -> Unit): ProjectSelectorConfig {
        val factory = ProjectBuilder.builder().build().objects
        return ProjectSelectorConfig.create(factory).apply(block)
    }

    @Test
    fun `when selector not applicable, rule is not evaluated`() {
        // given
        val project =
            ProjectComposition(
                path = ":common:utils",
                plugins = emptySet(),
                dependencies = setOf(":common:di"),
            )
        val selectorConfig = createSelector { nameContains("screen") }
        val selector = WithSelector(selectorConfig)
        val ruleConfig =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val entry =
            DoctorEntry(
                rule = HasPluginRule(ruleConfig),
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entry))

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `when selector applicable and rule reports error, message group is created`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("com.android.library"),
                dependencies = emptySet(),
            )
        val selectorConfig = createSelector { nameContains("feature") }
        val selector = WithSelector(selectorConfig)
        val ruleConfig =
            object : PluginRuleConfig() {
                init {
                    id("com.android.library")
                }
            }
        val entry =
            DoctorEntry(
                rule = HasPluginRule(ruleConfig),
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entry))

        // then
        assertThat(result).hasSize(1)
        assertThat(result.single().errorLevel).isEqualTo(ErrorLevel.Error(null))
        assertThat(result.single().messages).hasSize(1)
    }

    @Test
    fun `when project is excluded, rule is not evaluated`() {
        // given
        val project =
            ProjectComposition(
                path = ":image:zoom:screen",
                plugins = setOf("com.android.library"),
                dependencies = setOf("org.jetbrains.skiko:skiko"),
            )
        val selectorConfig = createSelector { nameContains("screen") }
        val selector = WithSelector(selectorConfig)
        val ruleConfig =
            HasDependencyConfig(
                pluginId = "",
                dependency = MatchableValue.Contains("skiko"),
                excludedDependency = emptySet(),
                projectExclusion = ProjectExclusionConfig(emptyList()),
            )
        val rule = HasDependencyRule(ruleConfig)
        val exclusionMatcher =
            com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectPathExclusionMatcher(
                MatchableValue.Contains("zoom")
            )
        val entry =
            DoctorEntry(
                rule = rule,
                selector = selector,
                errorLevel = ErrorLevel.Warning(null),
                projectExclusion = ProjectExclusionConfig(listOf(exclusionMatcher)),
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entry))

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `when multiple entries with same error level, messages are merged`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("plugin.a"),
                dependencies = setOf("dep.a", "dep.b"),
            )
        val selectorConfig = createSelector { nameContains("feature") }
        val selector = WithSelector(selectorConfig)
        val entry1 =
            DoctorEntry(
                rule =
                    HasDependencyRule(
                        HasDependencyConfig(
                            pluginId = "",
                            dependency = MatchableValue.Match("dep.a"),
                            excludedDependency = emptySet(),
                            projectExclusion = ProjectExclusionConfig(emptyList()),
                        )
                    ),
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )
        val entry2 =
            DoctorEntry(
                rule =
                    HasDependencyRule(
                        HasDependencyConfig(
                            pluginId = "",
                            dependency = MatchableValue.Match("dep.b"),
                            excludedDependency = emptySet(),
                            projectExclusion = ProjectExclusionConfig(emptyList()),
                        )
                    ),
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entry1, entry2))

        // then
        assertThat(result).hasSize(1)
        assertThat(result.single().messages).hasSize(2)
    }

    @Test
    fun `when entries have different error levels, groups are separated`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = setOf("plugin.a"),
                dependencies = setOf("dep.a", "dep.b"),
            )
        val selectorConfig = createSelector { nameContains("feature") }
        val selector = WithSelector(selectorConfig)
        val entryError =
            DoctorEntry(
                rule =
                    HasDependencyRule(
                        HasDependencyConfig(
                            pluginId = "",
                            dependency = MatchableValue.Match("dep.a"),
                            excludedDependency = emptySet(),
                            projectExclusion = ProjectExclusionConfig(emptyList()),
                        )
                    ),
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )
        val entryWarn =
            DoctorEntry(
                rule =
                    HasDependencyRule(
                        HasDependencyConfig(
                            pluginId = "",
                            dependency = MatchableValue.Match("dep.b"),
                            excludedDependency = emptySet(),
                            projectExclusion = ProjectExclusionConfig(emptyList()),
                        )
                    ),
                selector = selector,
                errorLevel = ErrorLevel.Warning(null),
                projectExclusion = null,
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entryError, entryWarn))

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.errorLevel })
            .containsExactly(ErrorLevel.Error(null), ErrorLevel.Warning(null))
    }

    @Test
    fun `when rule reports no errors, no group is created`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf(":common:utils"),
            )
        val selectorConfig = createSelector { nameContains("feature") }
        val selector = WithSelector(selectorConfig)
        val rule =
            HasDependencyRule(
                HasDependencyConfig(
                    pluginId = "",
                    dependency = MatchableValue.Contains("skiko"),
                    excludedDependency = emptySet(),
                    projectExclusion = ProjectExclusionConfig(emptyList()),
                )
            )
        val entry =
            DoctorEntry(
                rule = rule,
                selector = selector,
                errorLevel = ErrorLevel.Error(null),
                projectExclusion = null,
            )

        // when
        val result = runDoctorCheck(logger, project, setOf(entry))

        // then
        assertThat(result).isEmpty()
    }
}
