// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.sample.common.screen

import com.sample.common.utils.Utils

class Screen {
    fun render(): String = Utils.sample().joinToString()
}
