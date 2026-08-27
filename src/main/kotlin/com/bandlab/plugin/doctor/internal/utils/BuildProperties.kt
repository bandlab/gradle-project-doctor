package com.bandlab.plugin.doctor.internal.utils

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import java.util.Properties

internal fun Project.getBuildProperty(varName: String, defaultValue: String? = null): String? =
    getBuildPropertyInternal(varName, defaultValue)

@Suppress("UnstableApiUsage")
private fun Project.getBuildPropertyInternal(varName: String, defaultValue: String?): String? {
    val localPropsFile = isolated.rootProject.projectDirectory.file("local.properties")
    return providers.environmentVariable(varName)
        .orElse(providers.localPropertiesProvider(varName, localPropsFile))
        .orElse(providers.gradleProperty(varName))
        .orNull ?: defaultValue
}

private fun ProviderFactory.localPropertiesProvider(
    varName: String,
    localPropsFile: RegularFile,
) = of(LocalPropertiesValueSource::class.java) {
    parameters.localPropertiesFile.set(localPropsFile)
}.map { it[varName] }

internal abstract class LocalPropertiesValueSource : ValueSource<Map<String, String>, LocalPropertiesValueSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val localPropertiesFile: RegularFileProperty
    }

    override fun obtain(): Map<String, String>? {
        val file = parameters.localPropertiesFile.orNull?.asFile ?: return null
        if (!file.exists()) return null
        val properties = Properties()
        file.inputStream().use { properties.load(it) }
        return properties.entries.associate { it.key.toString() to it.value.toString() }
    }
}
