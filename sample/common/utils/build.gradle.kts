plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    // Intentionally expose Guava via api to trigger the transitivelyExpose rule
    api(libs.guava)
}

kotlin {
    jvmToolchain(17)
}
