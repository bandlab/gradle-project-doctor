package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TransitivelyExposeRuleTest {

    @Test
    fun `given exposed dependency matches, when validated, error is reported`() {
        // given
        val project = ProjectComposition(
            path = ":common:utils",
            plugins = emptySet(),
            dependencies = setOf(":units", "org.jetbrains.kotlinx:kotlinx-coroutines-core"),
            exposedDependencies = setOf("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
        )
        val rule = TransitivelyExposeRule(MatchableValue.Contains("kotlinx-coroutines"))

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(1)
        assertThat(errors.single()).contains("transitively exposes")
        assertThat(errors.single()).contains(":common:utils")
    }

    @Test
    fun `given implementation dependency matches but not exposed, when validated, no error`() {
        // given
        val project = ProjectComposition(
            path = ":common:utils",
            plugins = emptySet(),
            dependencies = setOf("org.jetbrains.kotlinx:kotlinx-coroutines-core"),
            exposedDependencies = emptySet(),
        )
        val rule = TransitivelyExposeRule(MatchableValue.Contains("kotlinx-coroutines"))

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    @Test
    fun `given no exposed dependencies, when validated, no error`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf(":common:utils"),
            exposedDependencies = emptySet(),
        )
        val rule = TransitivelyExposeRule(MatchableValue.Match(":common:utils"))

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).isEmpty()
    }

    @Test
    fun `given multiple exposed dependencies, when validated, all matching are reported`() {
        // given
        val project = ProjectComposition(
            path = ":feature:b",
            plugins = emptySet(),
            dependencies = setOf(":a", ":b"),
            exposedDependencies = setOf(":a", ":b"),
        )
        val rule = TransitivelyExposeRule(MatchableValue.AlwaysValid())

        // when
        val errors = mutableListOf<String>()
        rule.validate(project) { errors.add(it) }

        // then
        assertThat(errors).hasSize(2)
    }
}
