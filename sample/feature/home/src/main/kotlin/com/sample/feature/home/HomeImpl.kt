// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
package com.sample.feature.home

import com.sample.common.screen.Screen
import com.sample.feature.homeapi.HomeApi
import org.json.JSONObject

class HomeImpl : HomeApi {
    override fun title(): String {
        val screen = Screen()
        val json = JSONObject().apply { put("title", "home") }
        return "${screen.render()} - ${json.getString("title")}"
    }
}
