// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.rules

import java.io.Serializable
import org.gradle.api.internal.catalog.ExternalModuleDependencyFactory.PluginNotationSupplier
import org.gradle.api.provider.Provider
import org.gradle.plugin.use.PluginDependency

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
