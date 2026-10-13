// Copyright 2026 BandLab Singapore Pte Ltd
// SPDX-License-Identifier: Apache-2.0
plugins {
    // Intentionally NOT kotlin-jvm; triggers "api modules must be Kotlin JVM" rule
    `java-library`
    id("com.bandlab.project.doctor")
}

dependencies {
    api(project(":common:utils"))
}
