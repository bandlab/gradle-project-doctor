package com.bandlab.plugin.doctor.extension

import com.bandlab.plugin.doctor.apply
import com.bandlab.plugin.doctor.internal.rules.exclusion.AllExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.DependencyExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.PluginExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectExclusionMatcher
import com.bandlab.plugin.doctor.internal.rules.exclusion.ProjectPathExclusionMatcher
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import org.gradle.api.Action
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.PluginNotationSupplier
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.newInstance
import org.gradle.plugin.use.PluginDependency
import javax.inject.Inject

abstract class ProjectExclusionConfigExtension @Inject constructor(private val factory: ObjectFactory) {

    internal val matchers = mutableListOf<ProjectExclusionMatcher>()

    fun path(path: String) {
        matchers += ProjectPathExclusionMatcher(MatchableValue.Match(path))
    }

    fun paths(vararg paths: String) {
        paths.forEach {
            matchers += ProjectPathExclusionMatcher(MatchableValue.Match(it))
        }
    }

    @JvmOverloads
    fun nameContains(name: String, ignoreCase: Boolean = false) {
        matchers += ProjectPathExclusionMatcher(MatchableValue.Contains(name, ignoreCase))
    }

    @JvmOverloads
    fun nameEndsWith(suffix: String, ignoreCase: Boolean = false) {
        matchers += ProjectPathExclusionMatcher(MatchableValue.EndsWith(suffix, ignoreCase))
    }

    @JvmOverloads
    fun dependency(path: String, ignoreCase: Boolean = false) {
        matchers += DependencyExclusionMatcher(MatchableValue.Match(path, ignoreCase))
    }

    @JvmOverloads
    fun dependencyContains(path: String, ignoreCase: Boolean = false) {
        matchers += DependencyExclusionMatcher(MatchableValue.Contains(path, ignoreCase))
    }

    fun plugin(plugin: Provider<PluginDependency>) {
        matchers += PluginExclusionMatcher(MatchableValue.Match(plugin.get().pluginId))
    }

    fun plugin(pluginSupplier: PluginNotationSupplier) {
        matchers += PluginExclusionMatcher(MatchableValue.Match(pluginSupplier.asProvider().get().pluginId))
    }

    fun matchAll(action: Action<ProjectExclusionConfigExtension>) {
        val config = factory.newInstance<ProjectExclusionConfigExtension>().apply(action)
        matchers += AllExclusionMatcher(config.matchers)
    }

    internal fun toConfig(): ProjectExclusionConfig = ProjectExclusionConfig(matchers = matchers)

    companion object {
        fun create(objectFactory: ObjectFactory): ProjectExclusionConfigExtension {
            return objectFactory.newInstance<ProjectExclusionConfigExtension>()
        }
    }
}