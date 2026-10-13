// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.extension

import com.bandlab.plugin.doctor.DoctorEntry
import com.bandlab.plugin.doctor.EntryRegistry
import com.bandlab.plugin.doctor.ErrorLevel
import com.bandlab.plugin.doctor.ErrorLevelBuilderExtension
import com.bandlab.plugin.doctor.apply
import com.bandlab.plugin.doctor.internal.WithDependencySelector
import com.bandlab.plugin.doctor.internal.WithSelector
import com.bandlab.plugin.doctor.selector.ProjectSelectorConfig
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.newInstance

abstract class ErrorLevelExtension @Inject constructor(private val objectFactory: ObjectFactory) :
    EntryRegistry {

    private lateinit var errCode: String
    private var additionalMessage: String? = null

    private val _entries = mutableSetOf<DoctorEntry>()
    override val entries: Set<DoctorEntry> = _entries

    override fun addEntry(entry: DoctorEntry) {
        _entries.add(entry)
    }

    private fun initialize(errCode: String, additionalMessage: String?) {
        this.errCode = errCode
        this.additionalMessage = additionalMessage
    }

    fun allProjects(): EntryBuilderExtension {
        return EntryBuilderExtension.create(
            objectFactory = objectFactory,
            selector = WithSelector(ProjectSelectorConfig.create(objectFactory)),
            errorLevelExtension = this,
        )
    }

    fun projectWith(configBuilder: Action<ProjectSelectorConfig>): EntryBuilderExtension {
        val config = ProjectSelectorConfig.create(objectFactory).apply(configBuilder)
        return EntryBuilderExtension.create(
            objectFactory = objectFactory,
            selector = WithSelector(config),
            errorLevelExtension = this,
        )
    }

    fun projectWithDependency(configBuilder: Action<ProjectSelectorConfig>): EntryBuilderExtension {
        val config = ProjectSelectorConfig.create(objectFactory).apply(configBuilder)
        return EntryBuilderExtension.create(
            objectFactory = objectFactory,
            selector = WithDependencySelector(config),
            errorLevelExtension = this,
        )
    }

    fun toErrorLevel(): ErrorLevel {
        return when (errCode) {
            ErrorLevelBuilderExtension.Companion.LEVEL_ERR -> ErrorLevel.Error(additionalMessage)
            ErrorLevelBuilderExtension.Companion.LEVEL_WARN -> ErrorLevel.Warning(additionalMessage)
            else -> error("Unknown error level: $errCode")
        }
    }

    companion object {
        fun create(
            objectFactory: ObjectFactory,
            errCode: String,
            additionalMessage: String? = null,
        ): ErrorLevelExtension {
            return objectFactory.newInstance<ErrorLevelExtension>().apply {
                initialize(errCode, additionalMessage)
            }
        }
    }
}
