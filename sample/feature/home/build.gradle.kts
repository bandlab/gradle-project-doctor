plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.bandlab.project.doctor")
}

dependencies {
    implementation(project(":feature:home-api"))
    // Intentionally direct dependency on :common:screen to trigger indirection rule
    implementation(project(":common:screen"))
    // Also triggers HasDependencyRule demo (feature should not directly depend on org.json)
    implementation(libs.json)
}

kotlin {
    jvmToolchain(17)
}
