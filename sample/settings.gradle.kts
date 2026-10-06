pluginManagement {
    // Local development: uncomment to build the plugin from the working tree instead of
    // the published SNAPSHOT. The composite build takes precedence when present.
    // includeBuild("..")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://central.sonatype.com/repository/maven-snapshots/")
        google()
    }
    plugins {
        id("com.bandlab.project.doctor") version "0.1.0-SNAPSHOT"
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
