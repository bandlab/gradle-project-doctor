# Project Doctor Sample

Sample multi-module Gradle project that dogfoods `com.bandlab.project.doctor`.

## Structure

```
sample/
  settings.gradle.kts          # plugin version + repos (uncomment includeBuild for local dev)
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

See [`settings.gradle.kts`](settings.gradle.kts) for the plugin version and repositories
(`includeBuild("..")` is there, commented out, for local development).

`sample/build.gradle.kts` (root), which registers the aggregation task and holds the rules:

```kotlin
plugins { id("com.bandlab.project.doctor") }
projectDoctor { baselineFile = file("doctor-baseline.json"); errorOn { ... } }
```

Each module's own build file registers `doctorPopulate` / `doctorCheck`:

```kotlin
plugins {
  alias(libs.plugins.kotlin.jvm)
  id("com.bandlab.project.doctor")
}
```

The plugin is applied **per module**, not through `subprojects {}`. A cross-project
configuration block breaks Isolated Projects on its own, regardless of which plugin it applies,
so the sample avoids one. In a real build you would put the `id(...)` in a convention plugin and
write it once. Empty container projects (`:common`, `:feature`) simply don't apply it.

The sample consumes the published SNAPSHOT from Central snapshots (see `pluginManagement`
in [`settings.gradle.kts`](settings.gradle.kts)), so no `mavenLocal` publish is needed.
For local development against the working tree, uncomment `includeBuild("..")` there; the
composite build then takes precedence over the published artifact. Note that a versionless
`id("com.bandlab.project.doctor")` cannot resolve from repositories alone without the
version declared in `pluginManagement`.

## Configuration Cache & Isolated Projects

`sample/gradle.properties` turns both on, so every command below exercises the plugin under them:

```properties
org.gradle.configuration-cache=true
org.gradle.isolated-projects=true
```

Runs report `Configuration cache entry stored` / `reused` with no problems.

## Run

From repo root:

```bash
# 1. Check: fails fast on first error-level violation (subproject doctorCheck stops)
./gradlew -p sample doctorCheck
# Subproject failure example: :common:utils:doctorCheck → Guava api violation

# 2. Summary at leaf: never fails, writes result.json (no console throw)
./gradlew -p sample :feature:home:doctorCheck --summary

# 3. Aggregation with violations: fails at :doctorCheck (root) but prints merged report
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
- Runs with the configuration cache and Isolated Projects enabled (see `gradle.properties`).
- No Android SDK required (pure JVM). For Android variant-aware demo, add `com.android.library` module and note `stageDebugRuntimeClasspath` vs `debugRuntimeClasspath` handling.
- Disable on sync: `-PPROJECT_DOCTOR_DISABLE_ON_SYNC=true`
- Wire into `KotlinCompile`: `-PPROJECT_DOCTOR_ENABLE_ON_COMPILE=true`
