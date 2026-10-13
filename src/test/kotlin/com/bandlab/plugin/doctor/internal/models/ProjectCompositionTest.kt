// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.models

import com.google.common.truth.Truth.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test

class ProjectCompositionTest {

    @Test
    fun `toJson and fromJson roundtrip preserves path plugins and dependencies`() {
        // given
        val original =
            ProjectComposition(
                path = ":image:zoom:screen",
                plugins = setOf("com.android.library", "org.jetbrains.kotlin.jvm"),
                dependencies =
                    setOf(
                        ":common:utils",
                        "org.jetbrains.skiko:skiko:0.9.4",
                        "androidx.compose:compose-bom",
                    ),
                exposedDependencies = setOf(":common:utils"),
            )

        // when
        val json = ProjectComposition.toJson(original)
        val restored = ProjectComposition.fromJson(json)

        // then
        assertThat(restored.path).isEqualTo(original.path)
        assertThat(restored.plugins).isEqualTo(original.plugins)
        assertThat(restored.dependencies).isEqualTo(original.dependencies)
        assertThat(restored.exposedDependencies).isEqualTo(original.exposedDependencies)
        assertThat(restored.subProjects).isEmpty()
    }

    @Test
    fun `toJson writes expected keys`() {
        // given
        val composition =
            ProjectComposition(
                path = ":common:utils",
                plugins = setOf("kotlin"),
                dependencies = setOf(":annotations"),
                exposedDependencies = setOf(":annotations"),
            )

        // when
        val json = ProjectComposition.toJson(composition)

        // then
        assertThat(json.getString("path")).isEqualTo(":common:utils")
        assertThat(json.getJSONArray("plugins").toString())
            .isEqualTo(JSONArray(setOf("kotlin")).toString())
        assertThat(json.getJSONArray("dependencies").toString())
            .isEqualTo(JSONArray(setOf(":annotations")).toString())
        assertThat(json.getJSONArray("exposedDependencies").toString())
            .isEqualTo(JSONArray(setOf(":annotations")).toString())
    }

    @Test
    fun `fromJson handles missing optional arrays as empty`() {
        // given
        val json =
            JSONObject().apply {
                put("path", ":feature:a")
                put("plugins", JSONArray())
            }

        // when
        val composition = ProjectComposition.fromJson(json)

        // then
        assertThat(composition.dependencies).isEmpty()
        assertThat(composition.exposedDependencies).isEmpty()
        assertThat(composition.subProjects).isEmpty()
    }

    @Test
    fun `fromJson always resets subProjects to empty`() {
        // given
        val json =
            JSONObject().apply {
                put("path", ":feature:a")
                put("plugins", JSONArray(listOf("kotlin")))
                put("dependencies", JSONArray(listOf(":core")))
                put("exposedDependencies", JSONArray())
            }

        // when
        val composition = ProjectComposition.fromJson(json)

        // then
        assertThat(composition.subProjects).isEmpty()
    }

    @Test
    fun `toJson and fromJson preserve empty sets`() {
        // given
        val original =
            ProjectComposition(
                path = ":empty",
                plugins = emptySet(),
            )

        // when
        val json = ProjectComposition.toJson(original)
        val restored = ProjectComposition.fromJson(json)

        // then
        assertThat(restored.path).isEqualTo(":empty")
        assertThat(restored.plugins).isEmpty()
        assertThat(restored.dependencies).isEmpty()
        assertThat(restored.exposedDependencies).isEmpty()
    }
}
