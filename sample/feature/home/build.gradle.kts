// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.bandlab.project.doctor")
}

dependencies {
    // Intentionally direct dependency on :common:screen to trigger indirection rule
    implementation(project(":common:screen"))
    implementation(project(":feature:home-api"))
    // Also triggers HasDependencyRule demo (feature should not directly depend on org.json)
    implementation(libs.json)
}

kotlin {
    jvmToolchain(17)
}
