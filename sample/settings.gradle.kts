pluginManagement {
    // Local development: uncomment to build the plugin from the working tree instead of
    // the released artifact. The composite build takes precedence when present.
    // includeBuild("..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
    plugins {
        id("com.bandlab.project.doctor") version "0.2.0"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

rootProject.name = "project-doctor-sample"

include(":app")
include(":common:utils")
include(":common:screen")
include(":feature:home")
include(":feature:home-api")
