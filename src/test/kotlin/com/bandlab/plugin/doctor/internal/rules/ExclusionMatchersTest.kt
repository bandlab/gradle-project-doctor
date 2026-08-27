package com.bandlab.plugin.doctor.internal.rules

import com.bandlab.plugin.doctor.extension.MatchableValue
import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.internal.rules.exclusion.AllExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.DependencyExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.PluginExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectPathExclusionMatcher
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExclusionMatchersTest {

    @Test
    fun `PluginExclusionMatcher matches when plugin present`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = setOf("com.android.library"),
            dependencies = emptySet(),
        )
        val matcher = PluginExclusionMatcher(MatchableValue.Match("com.android.library"))

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `PluginExclusionMatcher does not match when plugin absent`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = setOf("kotlin"),
            dependencies = emptySet(),
        )
        val matcher = PluginExclusionMatcher(MatchableValue.Match("com.android.library"))

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `DependencyExclusionMatcher matches when dependency present`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf("org.jetbrains.skiko:skiko"),
        )
        val matcher = DependencyExclusionMatcher(MatchableValue.Contains("skiko"))

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `DependencyExclusionMatcher does not match when dependency absent`() {
        // given
        val project = ProjectComposition(
            path = ":feature:a",
            plugins = emptySet(),
            dependencies = setOf(":common:utils"),
        )
        val matcher = DependencyExclusionMatcher(MatchableValue.Contains("skiko"))

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `ProjectPathExclusionMatcher matches path`() {
        // given
        val project = ProjectComposition(
            path = ":image:zoom:screen",
            plugins = emptySet(),
            dependencies = emptySet(),
        )
        val matcher = ProjectPathExclusionMatcher(MatchableValue.Contains("zoom"))

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `AllExclusionMatcher requires all matchers to match`() {
        // given
        val project = ProjectComposition(
            path = ":feature:screen",
            plugins = setOf("com.android.library"),
            dependencies = setOf("org.jetbrains.skiko:skiko"),
        )
        val matcher = AllExclusionMatcher(
            listOf(
                PluginExclusionMatcher(MatchableValue.Match("com.android.library")),
                ProjectPathExclusionMatcher(MatchableValue.Contains("screen")),
            ),
        )

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `AllExclusionMatcher returns false when one matcher fails`() {
        // given
        val project = ProjectComposition(
            path = ":feature:screen",
            plugins = setOf("kotlin"),
            dependencies = emptySet(),
        )
        val matcher = AllExclusionMatcher(
            listOf(
                PluginExclusionMatcher(MatchableValue.Match("com.android.library")),
                ProjectPathExclusionMatcher(MatchableValue.Contains("screen")),
            ),
        )

        // when
        val result = matcher.isExcluded(project)

        // then
        assertThat(result).isFalse()
    }
}
