// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.utils

import org.gradle.api.logging.Logger

/** Handle printing output */
internal interface TextOutput {
    fun println(message: String)
}

internal class LoggerOutput(private val logger: Logger) : TextOutput {
    override fun println(message: String) {
        if (message.isNotEmpty()) {
            logger.lifecycle(message)
        }
    }
}
