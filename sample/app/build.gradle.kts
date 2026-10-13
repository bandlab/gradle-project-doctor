// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.bandlab.project.doctor")
}

dependencies {
    implementation(project(":common:screen"))
    implementation(project(":common:utils"))
    implementation(project(":feature:home"))
}

kotlin {
    jvmToolchain(17)
}
