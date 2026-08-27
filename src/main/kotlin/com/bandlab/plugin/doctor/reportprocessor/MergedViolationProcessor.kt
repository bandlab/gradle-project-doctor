package com.bandlab.plugin.doctor.reportprocessor

import com.bandlab.plugin.doctor.internal.models.MessageGroup

internal interface MergedViolationProcessor {
    fun process(mergedViolations: List<MessageGroup>): ProcessStatus
}

internal sealed interface ProcessStatus {
    object Success : ProcessStatus
    data class Failure(val errorMessage: String = "") : ProcessStatus
}