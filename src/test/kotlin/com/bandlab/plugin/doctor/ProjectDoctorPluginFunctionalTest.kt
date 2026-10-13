// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Instrumentation (functional) test that runs the plugin against a real Gradle build via Gradle
 * TestKit, instead of unit-testing internals with [org.gradle.testfixtures.ProjectBuilder].
 */
class ProjectDoctorPluginFunctionalTest {

    @get:Rule val testProjectDir = TemporaryFolder()

    private fun writeFixture(
        doctorRules: String,
        featurePlugins: String = "id 'java-library'",
    ) {
        testProjectDir
            .newFile("settings.gradle")
            .writeText(
                """
                rootProject.name = 'fixture'
                include ':feature:a'
                """
                    .trimIndent(),
            )
        testProjectDir
            .newFile("build.gradle")
            .writeText(
                """
            plugins {
                id 'com.bandlab.project.doctor'
            }

            subprojects {
                // Only projects with a build file have runtimeClasspath; skip empty containers
                if (buildFile.exists()) {
                    apply plugin: 'com.bandlab.project.doctor'
                }
            }

            projectDoctor {
                $doctorRules
            }
            """
                    .trimIndent(),
            )
        val featureDir = testProjectDir.newFolder("feature", "a")
        File(featureDir, "build.gradle")
            .writeText(
                """
            plugins {
                $featurePlugins
            }
            """
                    .trimIndent(),
            )
    }

    private fun runner(vararg arguments: String): GradleRunner =
        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(testProjectDir.root)
            .withArguments(*arguments)

    @Test
    fun `doctorCheck fails on error-level violation`() {
        writeFixture(
            doctorRules =
                """
                errorOn('feature modules must not use java-library') {
                    projectWith { nameContains(':feature:') }
                        .thatHasPluginWith { id('java-library') }
                }
                """
                    .trimIndent(),
        )

        val result = runner("doctorCheck").buildAndFail()

        assertThat(result.task(":feature:a:doctorCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains(":feature:a has plugin java-library")
    }

    @Test
    fun `doctorCheck succeeds when no rule is violated`() {
        writeFixture(
            doctorRules =
                """
                errorOn('feature modules must not use java-library') {
                    projectWith { nameContains(':feature:') }
                        .thatHasPluginWith { id('java-library') }
                }
                """
                    .trimIndent(),
            featurePlugins = "id 'java'",
        )

        val result = runner("doctorCheck").build()

        assertThat(result.task(":doctorCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(result.output).contains("no issues")
    }

    @Test
    fun `doctorCheck summary succeeds and writes merged report`() {
        writeFixture(
            doctorRules =
                """
                warnOn('feature modules should not use java-library') {
                    projectWith { nameContains(':feature:') }
                        .thatHasPluginWith { id('java-library') }
                }
                """
                    .trimIndent(),
        )

        val result = runner("doctorCheck", "--summary").build()

        assertThat(result.task(":doctorCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
        val mergedReport = File(testProjectDir.root, "build/doctor/merged.json")
        assertThat(mergedReport.exists()).isTrue()
        assertThat(mergedReport.readText()).contains(":feature:a has plugin java-library")
    }

    @Test
    fun `doctorCheck treats missing baseline file as empty`() {
        writeFixture(
            doctorRules =
                """
                baselineFile = file('doctor-baseline.json')
                errorOn('feature modules must not use java-library') {
                    projectWith { nameContains(':feature:') }
                        .thatHasPluginWith { id('java-library') }
                }
                """
                    .trimIndent(),
        )

        val result = runner("doctorCheck", "--summary").buildAndFail()

        assertThat(result.task(":doctorCheck")?.outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.output).contains(":feature:a has plugin java-library")
        assertThat(result.output).doesNotContain("does not exist")
    }

    @Test
    fun `doctorCheck succeeds when violating project is excluded`() {
        writeFixture(
            doctorRules =
                """
                errorOn('feature modules must not use java-library') {
                    projectWith { nameContains(':feature:') }
                        .withExclusion { path(':feature:a') }
                        .thatHasPluginWith { id('java-library') }
                }
                """
                    .trimIndent(),
        )

        val result = runner("doctorCheck").build()

        assertThat(result.task(":doctorCheck")?.outcome).isEqualTo(TaskOutcome.SUCCESS)
    }
}
