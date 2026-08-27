plugins {
    id("com.bandlab.project.doctor")
    alias(libs.plugins.kotlin.jvm) apply false
}

// Apply doctor to leaf subprojects only (skip intermediate :common and :feature containers)
subprojects {
    // Only projects with a build file have runtimeClasspath; skip empty containers
    if (buildFile.exists()) {
        apply(plugin = "com.bandlab.project.doctor")
    }
}

projectDoctor {
    baselineFile = file("doctor-baseline.json")

    // 1. Enforce convention: :api modules must be Kotlin JVM (not java-library)
    //    :feature:home-api intentionally uses java-library to trigger this.
    errorOn("api modules must be Kotlin JVM") {
        projectWith { nameEndsWith("-api") }
            .thatDoesNotHavePluginWith { id("org.jetbrains.kotlin.jvm") }
    }

    // 2. Enforce indirection: :common:screen should not be depended on directly
    //    :feature:home intentionally does, :app is excluded.
    errorOn("Use convention instead of direct :common:screen dependency") {
        projectWithDependency { dependency(":common:screen") }
            .withExclusion { path(":app") }
            .thatDoesNotHavePluginWith { id("com.sample.convention.screen") }
    }

    // 3. Enforce API hygiene: Guava must not be exposed via api()
    //    :common:utils exposes guava via api() to trigger this.
    errorOn("Guava must not be exposed via api") {
        allProjects().thatTransitivelyExpose { library(libs.guava) }
    }

    // 4. Demo custom message template ({{PATH}} placeholder)
    warnOn {
        projectWith { nameContains(":feature:") }
            .reportAs("{{PATH}} is a feature module — review naming")
    }

    // 4b. Demo name rule: all feature paths contain "feature" -> warn (shows NameRule)
    warnOn("feature naming convention") {
        projectWith { nameContains(":feature:") }
            .thatNamedWith { nameContains("feature") }
    }

    // 5. Demo thatHasDependencyWith: forbid direct json usage in feature modules
    //    (to showcase HasDependencyRule — note this uses nameContains, not library)
    warnOn("feature should not directly depend on org.json — use wrapper") {
        projectWith { nameContains(":feature:") }
            .thatHasDependencyWith { nameContains("json") }
    }

    // 6. Demo thatHasPluginWith: java-library should not appear outside :api
    //    (shows HasPluginRule as forbidden-plugin check)
    warnOn("java-library plugin should only be used in :api modules") {
        projectWith { nameContains(":feature:") }
            .thatHasPluginWith { id("java-library") }
    }
}
