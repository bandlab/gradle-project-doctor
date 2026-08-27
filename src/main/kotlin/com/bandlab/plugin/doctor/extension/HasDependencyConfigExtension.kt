package com.bandlab.plugin.doctor.extension

import com.bandlab.plugin.doctor.rules.HasDependencyConfig
import org.gradle.api.Action
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.PluginNotationSupplier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import javax.inject.Inject

abstract class HasDependencyConfigExtension @Inject constructor(factory: ObjectFactory) {

    private var pluginId: String = ""
    private var dependency: MatchableValue = MatchableValue.AlwaysInvalid()
    private var excludedDependency = mutableSetOf<MatchableValue>()
    private val exclusionExtension = ProjectExclusionConfigExtension.create(factory)

    fun plugin(plugin: String) {
        this.pluginId = plugin
    }

    fun plugin(plugin: Provider<PluginDependency>) {
        pluginId = plugin.get().pluginId
    }

    fun plugin(pluginSupplier: PluginNotationSupplier) {
        pluginId = pluginSupplier.asProvider().get().pluginId
    }

    @JvmOverloads
    fun name(name: String, ignoreCase: Boolean = false) {
        this.dependency = MatchableValue.Match(name, ignoreCase)
    }

    @JvmOverloads
    fun nameContains(name: String, ignoreCase: Boolean = false) {
        this.dependency = MatchableValue.Contains(name, ignoreCase)
    }

    @JvmOverloads
    fun nameEndsWith(suffix: String, ignoreCase: Boolean = false) {
        this.dependency = MatchableValue.EndsWith(suffix, ignoreCase)
    }

    /**
     * Exclude dependencies from rule check.
     *
     * @param excludes list of complete dependency names to exclude (ex: bandlab.android.common:screen)
     */
    fun excludes(vararg excludes: String) {
        this.excludedDependency.addAll(excludes.map { MatchableValue.Match(it) })
    }

    /**
     * Exclude dependencies from rule check.
     *
     * @param excludes list of substring contained in dependency name to exclude (ex: screen)
     */
    fun excludeContains(vararg excludes: String) {
        this.excludedDependency.addAll(excludes.map { MatchableValue.Contains(it) })
    }

    fun excludeProjects(action: Action<ProjectExclusionConfigExtension>) {
        action.execute(exclusionExtension)
    }

    fun toConfig(): HasDependencyConfig = HasDependencyConfig(
        pluginId = pluginId,
        dependency = dependency,
        excludedDependency = excludedDependency,
        projectExclusion = exclusionExtension.toConfig(),
    )
}