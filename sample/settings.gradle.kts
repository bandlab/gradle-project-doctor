pluginManagement {
    includeBuild("..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
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
