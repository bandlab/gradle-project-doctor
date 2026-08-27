# Project Doctor — Sample

Sample multi-module Gradle project that dogfoods `com.bandlab.project.doctor` via composite build.

## Structure

```
sample/
  settings.gradle.kts          # pluginManagement { includeBuild("..") }
  build.gradle.kts             # root: projectDoctor { ... } rules
  gradle/libs.versions.toml
  doctor-baseline.json         # baseline for suppressed violations
  app/                         # excluded from :common:screen rule via withExclusion { path(":app") }
  common/utils                 # api(libs.guava) → triggers transitivelyExpose
  common/screen                # consumed directly by :feature:home (violation)
  feature/home-api             # java-library only → triggers api-must-be-kotlin-jvm
  feature/home                 # has :common:screen + org.json → triggers several rules
```

## Rules Demonstrated

| # | Selector | Rule | Intentionally Violated By |
|---|----------|------|---------------------------|
| 1 | `projectWith { nameEndsWith("-api") }` | `thatDoesNotHavePluginWith { id("org.jetbrains.kotlin.jvm") }` | `:feature:home-api` (uses `java-library`, not `kotlin-jvm`) |
| 2 | `projectWithDependency { dependency(":common:screen") }` + `withExclusion { path(":app") }` | `thatDoesNotHavePluginWith { id("com.sample.convention.screen") }` | `:feature:home` (direct dep, `:app` excluded) |
| 3 | `allProjects()` | `thatTransitivelyExpose { library(libs.guava) }` | `:common:utils` (`api` guava) |
| 4 | `projectWith { nameContains(":feature:") }` | `reportAs("{{PATH}} template")` | warn, all feature modules (custom message) |
| 4b | `projectWith { nameContains(":feature:") }` | `thatNamedWith { nameContains("feature") }` | warn, all feature modules (NameRule) |
| 5 | `projectWith { nameContains(":feature:") }` | `thatHasDependencyWith { nameContains("json") }` | `:feature:home` (`org.json:json`) |
| 6 | `projectWith { nameContains(":feature:") }` | `thatHasPluginWith { id("java-library") }` | `:feature:home-api` |

## How the Plugin Is Applied

`sample/settings.gradle.kts`:

```kotlin
pluginManagement {
  includeBuild("..") // publishes com.bandlab.project.doctor from root project
  repositories { gradlePluginPortal(); mavenCentral(); google() }
}
```

`sample/build.gradle.kts` (root):

```kotlin
plugins { id("com.bandlab.project.doctor") }
subprojects { if (buildFile.exists()) apply(plugin = "com.bandlab.project.doctor") }
projectDoctor { baselineFile = file("doctor-baseline.json"); errorOn { ... } }
```

Root applies the plugin for aggregation, `subprojects {}` applies it to leaf modules for `doctorPopulate`/`doctorCheck` (this matches `ProjectDoctorPlugin`'s `root vs subproject` branching — skip empty `:common`/`:feature` containers). Client projects typically apply the plugin once in the root; subproject tasks are registered automatically via the plugin's configuration if applied via convention.

This uses **composite build** — no `mavenLocal` publish needed. For published artifact usage, replace `includeBuild("..")` with `mavenCentral()` + version.

## Run

From repo root:

```bash
# 1. Check — fails fast on first error-level violation (subproject doctorCheck stops)
./gradlew -p sample doctorCheck
# Subproject failure example: :common:utils:doctorCheck → Guava api violation

# 2. Summary at leaf — never fails, writes result.json (no console throw)
./gradlew -p sample :feature:home:doctorCheck --summary

# 3. Aggregation with violations — fails at :doctorCheck (root) but prints merged report
#    (--summary on leaf prevents leaf failure, root still reports errors)
./gradlew -p sample doctorCheck --summary
cat sample/build/doctor/merged.json

# 4. (Re)generate baseline from current violations (uses doctorGenerateBaseline variant)
./gradlew -p sample doctorGenerateBaseline
cat sample/doctor-baseline.json
# subsequent doctorCheck will filter baseline entries and pass
./gradlew -p sample doctorCheck             # now passes
./gradlew -p sample doctorCheck --summary   # merged.json contains filtered-empty or baseline-equivalent
```

Outputs:
- `sample/build/doctor/common/utils/project_composition.json` (from `doctorPopulate`)
- `sample/build/doctor/<path>/result.json` and `checkstyle.xml` (from `doctorCheck`)
- `sample/build/doctor/merged.json` (from root `doctorCheck`)

## Baseline

`doctor-baseline.json` is `[]` initially. After `doctorGenerateBaseline`, violations are captured and future `doctorCheck` ignores them (via `ViolationMerger`). Commit the file if you want to suppress existing violations incrementally.

## Notes

- Uses Kotlin JVM `2.1.20` (compatible with plugin `compileOnly` `2.4.10`), `jvmToolchain(17)`, Gradle `9.5.1`.
- No Android SDK required (pure JVM). For Android variant-aware demo, add `com.android.library` module and note `stageDebugRuntimeClasspath` vs `debugRuntimeClasspath` handling.
- Disable on sync: `-PPROJECT_DOCTOR_DISABLE_ON_SYNC=true`
- Wire into `KotlinCompile`: `-PPROJECT_DOCTOR_ENABLE_ON_COMPILE=true`
