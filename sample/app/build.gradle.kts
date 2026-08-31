plugins {
    alias(libs.plugins.kotlin.jvm)
    id("com.bandlab.project.doctor")
}

dependencies {
    implementation(project(":feature:home"))
    implementation(project(":common:screen"))
    implementation(project(":common:utils"))
}

kotlin {
    jvmToolchain(17)
}
