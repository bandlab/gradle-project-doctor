// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import com.bandlab.plugin.doctor.selector.ProjectSelectorConfig
import com.google.common.truth.Truth.assertThat
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class SelectorsTest {

    private fun createConfig(block: ProjectSelectorConfig.() -> Unit): ProjectSelectorConfig {
        val factory = ProjectBuilder.builder().build().objects
        return ProjectSelectorConfig.create(factory).apply(block)
    }

    // region WithSelector

    @Test
    fun `WithSelector matches when name and plugin both match`() {
        // given
        val project =
            ProjectComposition(
                path = ":image:zoom:screen",
                plugins = setOf("com.android.library"),
                dependencies = emptySet(),
            )
        val config = createConfig {
            nameContains("screen")
            pluginId = "com.android.library"
        }
        val selector = WithSelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `WithSelector does not match when plugin mismatched`() {
        // given
        val project =
            ProjectComposition(
                path = ":image:zoom:screen",
                plugins = setOf("kotlin"),
                dependencies = emptySet(),
            )
        val config = createConfig {
            nameContains("screen")
            pluginId = "com.android.library"
        }
        val selector = WithSelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `WithSelector matches when pluginId empty regardless of plugins`() {
        // given
        val project =
            ProjectComposition(
                path = ":common:utils",
                plugins = emptySet(),
                dependencies = emptySet(),
            )
        val config = createConfig {
            name(":common:utils")
        }
        val selector = WithSelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `WithSelector uses name matcher correctly`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:api",
                plugins = emptySet(),
                dependencies = emptySet(),
            )
        val config = createConfig {
            nameEndsWith(":api")
        }
        val selector = WithSelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    // endregion

    // region WithDependencySelector

    @Test
    fun `WithDependencySelector matches when dependency and plugin both match`() {
        // given
        val subProject =
            ProjectComposition(
                path = ":common:di",
                plugins = setOf("com.android.library"),
                dependencies = emptySet(),
            )
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf(":common:di"),
                subProjects = setOf(subProject),
            )
        val config = createConfig {
            dependency(":common:di")
            pluginId = "com.android.library"
        }
        val selector = WithDependencySelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `WithDependencySelector does not match when dependency absent`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf(":common:utils"),
                subProjects = emptySet(),
            )
        val config = createConfig {
            nameContains("skiko")
        }
        val selector = WithDependencySelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `WithDependencySelector does not match when subProject plugin absent`() {
        // given
        val subProject =
            ProjectComposition(
                path = ":common:di",
                plugins = setOf("kotlin"),
                dependencies = emptySet(),
            )
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf(":common:di"),
                subProjects = setOf(subProject),
            )
        val config = createConfig {
            dependency(":common:di")
            pluginId = "com.android.library"
        }
        val selector = WithDependencySelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isFalse()
    }

    @Test
    fun `WithDependencySelector matches dependency substring`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf("org.jetbrains.skiko:skiko:0.9.4"),
                subProjects = emptySet(),
            )
        val config = createConfig {
            nameContains("skiko")
        }
        val selector = WithDependencySelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    @Test
    fun `WithDependencySelector with empty pluginId only checks dependency`() {
        // given
        val project =
            ProjectComposition(
                path = ":feature:a",
                plugins = emptySet(),
                dependencies = setOf(":common:utils"),
                subProjects = emptySet(),
            )
        val config = createConfig {
            dependency(":common:utils")
        }
        val selector = WithDependencySelector(config)

        // when
        val result = selector.isApplicable(project)

        // then
        assertThat(result).isTrue()
    }

    // endregion
}
