// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.selector

import com.bandlab.plugin.doctor.extension.MatchableValue
import java.io.Serializable
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.internal.catalog.DelegatingProjectDependency
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.PluginNotationSupplier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import org.gradle.plugin.use.PluginDependency

abstract class ProjectSelectorConfig : Serializable {
    internal var pluginId: String = ""
    internal var name: MatchableValue = MatchableValue.AlwaysValid()

    /** Exact match project by name (ex: :image:cropper:screen) */
    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) {
        this.name = MatchableValue.Match(name, ignoreCase)
    }

    /** Match project name by containing substring (ex: screen or :screen) */
    @JvmOverloads
    fun nameContains(name: String, ignoreCase: Boolean = false) {
        this.name = MatchableValue.Contains(name, ignoreCase)
    }

    @JvmOverloads
    fun nameEndsWith(suffix: String, ignoreCase: Boolean = false) {
        this.name = MatchableValue.EndsWith(suffix, ignoreCase)
    }

    @JvmOverloads
    fun nameDoesNotContains(name: String, ignoreCase: Boolean = false) {
        this.name = MatchableValue.DoesNotContain(name, ignoreCase)
    }

    fun dependency(dependency: DelegatingProjectDependency) {
        this.name = MatchableValue.Match(dependency.path)
    }

    fun dependency(path: String) {
        this.name = MatchableValue.Match(path)
    }

    /**
     * Match a library dependency from a version catalog accessor (ex: `libs.metro.runtime`). The
     * dependency is matched by its `group:name` coordinates, ignoring the version.
     */
    fun library(library: Provider<MinimalExternalModuleDependency>) {
        val module = library.get().module
        this.name = MatchableValue.Match("${module.group}:${module.name}")
    }

    fun plugin(plugin: Provider<PluginDependency>) {
        pluginId = plugin.get().pluginId
    }

    fun plugin(pluginSupplier: PluginNotationSupplier) {
        pluginId = pluginSupplier.asProvider().get().pluginId
    }

    override fun toString(): String {
        return "ProjectSelectorConfig(pluginId='$pluginId', name=$name)"
    }

    companion object {
        fun create(objectFactory: ObjectFactory): ProjectSelectorConfig {
            return objectFactory.newInstance<ProjectSelectorConfig>()
        }
    }
}
