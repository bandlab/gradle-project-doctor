package com.bandlab.plugin.doctor.internal.utils

import com.android.build.api.variant.AndroidComponentsExtension
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.UnknownConfigurationException
import org.gradle.api.artifacts.component.ComponentSelector
import org.gradle.api.artifacts.component.ModuleComponentSelector
import org.gradle.api.artifacts.component.ProjectComponentSelector
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.provider.Provider

internal fun Project.runtimeConfiguration(): Provider<Configuration> = provider {
    val classPathName = preferredRuntimeClasspathName()
    val classPathNames = listOf(
        preferredRuntimeClasspathName(),
        "runtimeClasspath",
        "debugRuntimeClasspath",
        "releaseRuntimeClasspath"
    )
    findConfiguration(classPathNames)?.get()
        ?: throw UnknownConfigurationException(
            "Configuration '$classPathName' not found in project '${this.path}'." +
                " Available: ${configurations.names.filter { it.contains("runtime", ignoreCase = true) }}"
        )
}

internal fun Project.runtimeResolvedComponents(): Provider<Set<String>> {
    val configuration = runtimeConfiguration()
    return configuration.map { it.incoming.resolutionResult.rootComponent.get().toDependenciesIdentifiers() }
}

/**
 * The dependencies this project exposes transitively to its consumers via `api` configuration.
 */
internal fun Project.exposedDeclaredComponents(): Provider<Set<String>> = provider {
    findConfiguration(listOf("api"))?.get()?.dependencies
        .orEmpty()
        .map { dependency ->
            when (dependency) {
                is ProjectDependency -> dependency.path
                else -> "${dependency.group}:${dependency.name}"
            }
        }
        .toSet()
}

/**
 * Wait until the runtime configuration is available and call [onAvailable].
 * This avoids [afterEvaluate] by using plugin-specific callbacks.
 */
internal fun Project.onRuntimeConfigurationAvailable(onAvailable: (Configuration) -> Unit) {
    plugins.withId("com.android.base") {
        val extension = extensions.findByType(AndroidComponentsExtension::class.java)
        requireNotNull(extension).onVariants {
            onAvailable(it.runtimeConfiguration)
        }
    }
    plugins.withId("org.jetbrains.kotlin.jvm") {
        onAvailable(configurations.getByName("runtimeClasspath"))
    }
}

private fun Project.preferredRuntimeClasspathName(): String {
    return when {
        plugins.hasPlugin("com.android.application") -> "stageDebugRuntimeClasspath"
        plugins.hasPlugin("com.android.library") -> "debugRuntimeClasspath"
        plugins.hasPlugin("androidx.baselineprofile") -> "prodBenchmarkMinificationRuntimeClasspath"
        plugins.hasPlugin("com.android.dynamic-feature") -> "stageDebugRuntimeClasspath"
        else -> "runtimeClasspath"
    }
}

private fun Project.findConfiguration(names: List<String>): NamedDomainObjectProvider<Configuration>? {
    return names.firstNotNullOfOrNull { name ->
        try {
            configurations.named(name)
        } catch (_: UnknownConfigurationException) {
            null
        }
    }
}

internal fun ComponentSelector.identifier(): String {
    return when (this) {
        is ProjectComponentSelector -> projectPath
        is ModuleComponentSelector -> "${group}:${module}"
        else -> error("Unsupported ComponentSelector type: ${this::class.java.name}")
    }
}


internal fun ResolvedComponentResult.toDependenciesIdentifiers(): Set<String> =
    dependencies.map { it.requested.identifier() }.toSet()