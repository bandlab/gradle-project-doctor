// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class DoctorPopulateTask : DefaultTask() {

    @get:Input abstract val projectPath: Property<String>

    @get:Input abstract val observedPlugins: SetProperty<String>

    @get:Input abstract val dependencies: SetProperty<String>

    @get:Input abstract val exposedDependencies: SetProperty<String>

    @get:OutputFile abstract val projectCompositionFile: RegularFileProperty

    @TaskAction
    fun run() {
        val path = projectPath.get()
        val plugins = observedPlugins.get()
        val deps = dependencies.get()
        val exposedDeps = exposedDependencies.get()

        val composition =
            ProjectComposition(
                path = path,
                plugins = plugins,
                dependencies = deps,
                exposedDependencies = exposedDeps,
            )

        val json = ProjectComposition.toJson(composition)

        val file = projectCompositionFile.get().asFile
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        file.writeText(json.toString())
    }
}
