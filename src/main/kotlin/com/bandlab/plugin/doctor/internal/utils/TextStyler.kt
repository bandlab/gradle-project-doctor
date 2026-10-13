// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.bandlab.plugin.doctor.internal.utils

internal class TextStyler {
    fun red(text: String): String {
        return "\u001B[31m$text\u001B[0m"
    }

    fun yellow(text: String): String {
        return "\u001B[33m$text\u001B[0m"
    }
}
