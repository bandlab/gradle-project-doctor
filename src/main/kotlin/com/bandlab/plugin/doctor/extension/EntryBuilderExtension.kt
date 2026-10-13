// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.extension

import com.bandlab.plugin.doctor.DoctorEntry
import com.bandlab.plugin.doctor.DoctorRule
import com.bandlab.plugin.doctor.EntryRegistry
import com.bandlab.plugin.doctor.ProjectSelector
import com.bandlab.plugin.doctor.apply
import com.bandlab.plugin.doctor.internal.rules.ErrorRule
import com.bandlab.plugin.doctor.internal.rules.HasDependencyRule
import com.bandlab.plugin.doctor.internal.rules.HasPluginRule
import com.bandlab.plugin.doctor.internal.rules.NameRule
import com.bandlab.plugin.doctor.internal.rules.NoPluginRule
import com.bandlab.plugin.doctor.internal.rules.TransitivelyExposeRule
import com.bandlab.plugin.doctor.rules.NameRuleConfig
import com.bandlab.plugin.doctor.rules.PluginRuleConfig
import com.bandlab.plugin.doctor.rules.exclusion.ProjectExclusionConfig
import com.bandlab.plugin.doctor.selector.ProjectSelectorConfig
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class EntryBuilderExtension @Inject constructor(private val objectFactory: ObjectFactory) {

    private lateinit var selector: ProjectSelector
    private lateinit var errorLevelExtension: ErrorLevelExtension
    private lateinit var entryRegistry: EntryRegistry

    private var exclusionConfig: ProjectExclusionConfig? = null

    private fun initialize(
        selector: ProjectSelector,
        errorLevelExtension: ErrorLevelExtension,
    ) {
        this.selector = selector
        this.errorLevelExtension = errorLevelExtension
        this.entryRegistry = errorLevelExtension
    }

    infix fun withExclusion(
        action: Action<ProjectExclusionConfigExtension>,
    ): EntryBuilderExtension {
        val extension = objectFactory.newInstance<ProjectExclusionConfigExtension>().apply(action)
        this.exclusionConfig = extension.toConfig()
        return this
    }

    infix fun thatHasPluginWith(action: Action<PluginRuleConfig>) {
        val config = objectFactory.newInstance<PluginRuleConfig>().apply(action)
        val rule = HasPluginRule(config)
        entryRegistry.addEntry(toEntry(rule = rule))
    }

    infix fun thatDoesNotHavePluginWith(action: Action<PluginRuleConfig>) {
        val config = objectFactory.newInstance<PluginRuleConfig>().apply(action)
        val rule = NoPluginRule(config)
        entryRegistry.addEntry(toEntry(rule = rule))
    }

    infix fun thatHasDependencyWith(action: Action<HasDependencyConfigExtension>) {
        val builder = objectFactory.newInstance<HasDependencyConfigExtension>().apply(action)
        val rule = HasDependencyRule(builder.toConfig())
        entryRegistry.addEntry(toEntry(rule = rule))
    }

    infix fun thatTransitivelyExpose(action: Action<ProjectSelectorConfig>) {
        val config = ProjectSelectorConfig.create(objectFactory).apply(action)
        val rule = TransitivelyExposeRule(config.name)
        entryRegistry.addEntry(toEntry(rule = rule))
    }

    infix fun thatNamedWith(action: Action<NameRuleConfig>) {
        val config = objectFactory.newInstance<NameRuleConfig>().apply(action)
        val rule = NameRule(config)
        entryRegistry.addEntry(toEntry(rule = rule))
    }

    infix fun reportAs(errorTemplate: String) {
        entryRegistry.addEntry(toEntry(rule = ErrorRule(errorTemplate)))
    }

    private fun toEntry(rule: DoctorRule): DoctorEntry {
        val selector = selector
        val errorLevel = errorLevelExtension.toErrorLevel()

        return DoctorEntry(
            selector = selector,
            errorLevel = errorLevel,
            rule = rule,
            projectExclusion = exclusionConfig,
        )
    }

    companion object {
        fun create(
            objectFactory: ObjectFactory,
            selector: ProjectSelector,
            errorLevelExtension: ErrorLevelExtension,
        ): EntryBuilderExtension {
            return objectFactory.newInstance<EntryBuilderExtension>().apply {
                initialize(
                    selector = selector,
                    errorLevelExtension = errorLevelExtension,
                )
            }
        }
    }
}
