package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.rules.HasDependencyConfig
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HasDependencyRuleTest {

    @Test
    fun `given project has matching dependency, when validated, error is reported`() {
        // given
        val project = ProjectComposition(
            path = ":image:zoom:screen",
            plugins = emptySet(),
            dependencies = setOf("org.jetbrains.skiko:skiko", ":common:utils"),
            subProjects = emptySet(),
        )
        val config = HasDependencyConfig(
            pluginId = "",
            dependency = MatchableValue.Contains("skiko"),
            excludedDependency = emptySet(),
            projectExclusion = ProjectExclusionConfig(emptyList()),
        )
        val rule = HasDependencyRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).contains("skiko")
    }

    @Test
    fun `given excludedDependency matches, when validated, excluded item is not reported`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf("org.jetbrains.skiko:skiko", "androidx.compose:compose-bom"),
            subProjects = emptySet(),
        )
        val config = HasDependencyConfig(
            pluginId = "",
            dependency = MatchableValue.Contains("skiko"),
            excludedDependency = setOf(MatchableValue.Match("org.jetbrains.skiko:skiko")),
            projectExclusion = ProjectExclusionConfig(emptyList()),
        )
        val rule = HasDependencyRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    @Test
    fun `given pluginId matches subProject plugin, when validated, error is reported for subProject`() {
        // given
        val subProject = ProjectComposition(
            path = ":common:di",
            plugins = setOf("com.android.library"),
            dependencies = emptySet(),
        )
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf(":common:di"),
            subProjects = setOf(subProject),
        )
        val config = HasDependencyConfig(
            pluginId = "com.android.library",
            dependency = MatchableValue.AlwaysInvalid(),
            excludedDependency = emptySet(),
            projectExclusion = ProjectExclusionConfig(emptyList()),
        )
        val rule = HasDependencyRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).contains("com.android.library")
        assertThat(errors.single()).contains(":common:di")
    }

    @Test
    fun `given dependency matches but subProject is excluded, when validated, no error`() {
        // given
        val subProject = ProjectComposition(
            path = ":common:di",
            plugins = setOf("plugin.a"),
            dependencies = emptySet(),
        )
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf(":common:di", "org.jetbrains.skiko:skiko"),
            subProjects = setOf(subProject),
        )
        val exclusion = com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectPathExclusionMatcher(
            MatchableValue.Match(":common:di"),
        )
        val config = HasDependencyConfig(
            pluginId = "",
            dependency = MatchableValue.Match(":common:di"),
            excludedDependency = emptySet(),
            projectExclusion = ProjectExclusionConfig(listOf(exclusion)),
        )
        val rule = HasDependencyRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    @Test
    fun `given EndsWith matcher, when dependency ends with value, error is reported`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf(":image:zoom:screen"),
            subProjects = emptySet(),
        )
        val config = HasDependencyConfig(
            pluginId = "",
            dependency = MatchableValue.EndsWith(":screen"),
            excludedDependency = emptySet(),
            projectExclusion = ProjectExclusionConfig(emptyList()),
        )
        val rule = HasDependencyRule(config)

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
    }
}
