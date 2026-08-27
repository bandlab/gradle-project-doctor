plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":common:utils"))
}

kotlin {
    jvmToolchain(17)
}
