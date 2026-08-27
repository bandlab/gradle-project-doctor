package com.bandlab.plugin.doctor.internal.models

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

/**
 * A snapshot of [org.gradle.api.Project] that will be used in analyzing the project structure.
 *
 * @param exposedDependencies Dependencies that this project exposes transitively to its consumers via `api` configuration. A subset of [dependencies].
 */
data class ProjectComposition(
    val path: String,
    val plugins: Set<String>,
    val dependencies: Set<String> = emptySet(),
    val exposedDependencies: Set<String> = emptySet(),
    val subProjects: Set<ProjectComposition> = emptySet(),
) : Serializable {
    companion object {
        fun toJson(composition: ProjectComposition): JSONObject {
            val json = JSONObject()
            json.put("path", composition.path)
            json.put("plugins", JSONArray(composition.plugins))
            json.put("dependencies", JSONArray(composition.dependencies))
            json.put("exposedDependencies", JSONArray(composition.exposedDependencies))
            return json
        }

        fun fromJson(json: JSONObject): ProjectComposition {
            val path = json.getString("path")
            val plugins = json.getJSONArray("plugins").toList().map { it.toString() }.toSet()
            val dependencies = json.optJSONArray("dependencies")?.toList()?.map { it.toString() }?.toSet() ?: emptySet()
            val exposedDependencies = json.optJSONArray("exposedDependencies")?.toList()?.map { it.toString() }?.toSet() ?: emptySet()
            return ProjectComposition(
                path = path,
                plugins = plugins,
                dependencies = dependencies,
                exposedDependencies = exposedDependencies,
                subProjects = emptySet() // Will be populated dynamically by DoctorCheckTask
            )
        }
    }
}
