package com.bandlab.plugin.doctor.rules

import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.PluginNotationSupplier
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency
import java.io.Serializable

abstract class PluginRuleConfig : Serializable {
    internal var pluginId: String = ""

    fun alias(plugin: Provider<PluginDependency>) {
        pluginId = plugin.get().pluginId
    }

    fun alias(pluginSupplier: PluginNotationSupplier) {
        pluginId = pluginSupplier.asProvider().get().pluginId
    }

    fun id(plugin: String) {
        this.pluginId = plugin
    }
}