// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.sample.app

import com.sample.common.screen.Screen
import com.sample.feature.home.HomeImpl

fun main() {
    println(HomeImpl().title())
    println(Screen().render())
}
