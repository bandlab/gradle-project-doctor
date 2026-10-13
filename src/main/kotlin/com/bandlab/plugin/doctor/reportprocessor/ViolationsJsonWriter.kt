// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.reportprocessor

import com.bandlab.plugin.doctor.internal.models.MessageGroup
import java.io.File

internal class ViolationsJsonWriter(private val outputFile: File) : MergedViolationProcessor {

    override fun process(mergedViolations: List<MessageGroup>): ProcessStatus {
        if (mergedViolations.isNotEmpty()) {
            outputFile.delete()
            outputFile.writeText(MessageGroup.toJsonArray(mergedViolations).toString())
        }
        return ProcessStatus.Success
    }
}
