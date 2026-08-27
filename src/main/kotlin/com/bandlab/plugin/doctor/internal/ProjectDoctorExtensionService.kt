package com.bandlab.plugin.doctor.internal

import com.bandlab.plugin.doctor.ProjectDoctorExtension
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import javax.inject.Inject

abstract class ProjectDoctorExtensionService @Inject constructor() : BuildService<BuildServiceParameters.None> {

    private var projectDoctorExtension: ProjectDoctorExtension? = null

    fun setProjectDoctorExtension(projectDoctorExtension: ProjectDoctorExtension) {
        this.projectDoctorExtension = projectDoctorExtension
    }

    fun getProjectDoctorExtension(): ProjectDoctorExtension {
        return projectDoctorExtension ?: error("ProjectDoctorExtension is not set in the root project.")
    }

    companion object {
        fun of(project: Project): Provider<ProjectDoctorExtensionService> = of(project.gradle)

        fun of(gradle: Gradle): Provider<ProjectDoctorExtensionService> {
            return gradle
                .sharedServices
                .registerIfAbsent("com.bandlab.doctorDslService", ProjectDoctorExtensionService::class.java) {}
        }
    }
}