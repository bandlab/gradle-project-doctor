plugins {
    // Intentionally NOT kotlin-jvm — triggers "api modules must be Kotlin JVM" rule
    `java-library`
}

dependencies {
    api(project(":common:utils"))
}
