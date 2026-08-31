plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.bandlab.project.doctor")
}

dependencies {
    // Intentionally expose Guava via api to trigger the transitivelyExpose rule
    api(libs.guava)
}

kotlin {
    jvmToolchain(17)
}
