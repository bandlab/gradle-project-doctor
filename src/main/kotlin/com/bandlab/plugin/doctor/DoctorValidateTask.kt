package com.bandlab.plugin.doctor

import com.bandlab.plugin.doctor.internal.models.ProjectComposition
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject
import java.io.File

/**
 * Check if Project Composition is properly configured. It's useful to have this as a test
 * for [DoctorPopulateTask]
 *
 * Unlike actual check that will ignore project that doesn't have Project doctor plugin,
 * this task will verify evey dependencies.
 */
@org.gradle.api.tasks.UntrackedTask(because = "Validates composition via logger/error; not safely cacheable")
abstract class DoctorValidateTask : DefaultTask() {

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val projectCompositionFile: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFiles
    abstract val subProjectCompositionFiles: ConfigurableFileCollection

    @TaskAction
    fun run() {
        val projectCompFile = projectCompositionFile.get().asFile
        val localComposition = ProjectComposition.fromJson(JSONObject(projectCompFile.readText()))

        val depsCompositions = subProjectCompositionFiles.files
            .map { file -> file.toProjectComposition() }
            .associateBy { it.path }

        val subProjects = localComposition.dependencies.mapNotNull { depsCompositions[it] }.toSet()
        val project = localComposition.copy(subProjects = subProjects)

        validateProjectComposition(project = project)
    }

    private fun File.toProjectComposition(): ProjectComposition {
        val text = readText()
        return try {
            ProjectComposition.fromJson(JSONObject(text))
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse composition JSON from $path, content: $text", e)
        }
    }

    private fun validateProjectComposition(project: ProjectComposition) {
        val subDeps = project.dependencies.filter { it.startsWith(":") }
        val subProjects = project.subProjects

        val isSizeValid = subProjects.size == subDeps.size
        val statusCaption = if (isSizeValid) "🟢" else "🛑"
        val compositionMessage =
            """
                |Project composition status $statusCaption
                |--–––––––––––––––––––––––––––––––––
                |==> Deps:
                |- ${subDeps.sortedBy { it }.joinToString(separator = "\n- ")} 
                |
                |==> Subs:
                |- ${subProjects.sortedBy { it.path }.joinToString(separator = "\n- ") { it.path }}
            """.trimMargin()

        if (isSizeValid) {
            logger.info(compositionMessage)
        } else {
            error(compositionMessage)
        }
    }
}