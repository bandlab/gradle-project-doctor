package com.sample.app

import com.sample.common.screen.Screen
import com.sample.feature.home.HomeImpl

fun main() {
    println(HomeImpl().title())
    println(Screen().render())
}
