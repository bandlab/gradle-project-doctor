# Project Doctor Sample

Multi-module JVM project demonstrating `com.bandlab.project.doctor` from Maven Central.
The plugin version is set in [`settings.gradle.kts`](settings.gradle.kts).
It intentionally violates several rules.

## Prerequisites

- Run the repository's Gradle **9.8.0** wrapper. The plugin needs Java 21 or newer for Gradle,
  and the repository pins the daemon to Java 25 in `gradle/gradle-daemon-jvm.properties`.
- Kotlin JVM **2.1.20** is configured in the sample. Its `jvmToolchain(17)` controls source
  compilation, not the JVM running Gradle. Install JDK 17 if you also compile the sample.
- Internet access for artifact resolution. No Android SDK or publishing credentials are needed.

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
projectDoctor {
    baselineFile = file("doctor-baseline.json")
    errorOn("api modules must be Kotlin JVM") {
        projectWith { nameEndsWith("-api") }
            .thatDoesNotHavePluginWith { id("org.jetbrains.kotlin.jvm") }
    }
}
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

The default mode resolves the released artifact and plugin marker from Maven Central. It does
not test local plugin source changes, and no `mavenLocal` publication is needed.

For local development, uncomment `includeBuild("..")` in `pluginManagement`, or pass
`--include-build "$PWD"` when invoking the wrapper from the repository root. The composite build
then supplies the plugin from the working tree instead of Central. Note that a versionless
`id("com.bandlab.project.doctor")` cannot resolve from repositories alone without the
version declared in `pluginManagement`.

## Configuration Cache & Isolated Projects

`sample/gradle.properties` turns both on, so every command below exercises the plugin under them:

```properties
org.gradle.configuration-cache=true
org.gradle.isolated-projects=true
```

To check reuse, run the same successful command twice and inspect Gradle's output:

```bash
./gradlew -p sample :feature:home:doctorCheck --summary
./gradlew -p sample :feature:home:doctorCheck --summary
```

Look for `Configuration cache entry stored` followed by `Configuration cache entry reused`.
Reuse depends on unchanged inputs and Gradle's cache state, not just the enabled properties.
The check, aggregation, and baseline tasks are untracked, so configuration-cache reuse does
not mean their reports are restored from the build cache or that the checks are skipped.

## Run

From repo root:

```bash
# 1. With the initial empty baseline, a module check fails on an error-level violation.
./gradlew -p sample doctorCheck
# The failing module depends on execution order. Root aggregation may not run.

# 2. Module summary: succeeds despite violations, writes raw result.json.
#    It skips module console/XML reporting, but other execution failures are still possible.
./gradlew -p sample :feature:home:doctorCheck --summary

# 3. Task selector passes --summary to module checks. Root prints the merged report
#    and still fails on error-level violations not in the baseline.
./gradlew -p sample doctorCheck --summary
cat sample/build/doctor/merged.json

# 4. Replace the tracked baseline with all current violations, including warnings.
./gradlew -p sample doctorGenerateBaseline
cat sample/doctor-baseline.json
# Both checks now pass if no new errors are introduced.
./gradlew -p sample doctorCheck             # now passes
./gradlew -p sample doctorCheck --summary
```

Do not use `:doctorCheck --summary` to defer module failures. It applies the option only to the
root, which ignores it. Generating a baseline changes `sample/doctor-baseline.json`. Review and
restore it to `[]` after experiments unless intentionally adopting a baseline.

Outputs:
- `sample/build/doctor/common/utils/project_composition.json` (from `doctorPopulate`)
- `sample/build/doctor/<module-dir>/result.json` (module violations)
- `sample/build/doctor/<module-dir>/checkstyle.xml` (non-summary, non-baselined module violations)
- `sample/build/doctor/<module-dir>/baseline_result.json` (unfiltered baseline collection)
- `sample/build/doctor/merged.json` (non-empty filtered root results, JSON only)

There is no root Checkstyle XML. Summary mode does not generate XML, and a fully baselined run
does not necessarily clear old reports. In particular, `merged.json` may still contain the
previous failing run's results after a passing check. Use the current console report and task
outcome, not the presence of a report file, as the result of the current run.

## Baseline

`doctor-baseline.json` is `[]` initially. Generation captures all current violations. Checks
filter baseline entries by exact message and severity. New errors fail, and new warnings report
without failing. Module summary JSON is raw, while the root still applies the baseline.
Commit a reviewed baseline in your own project if you want to adopt rules incrementally.

## Notes

- This is a JVM sample, not Android compatibility coverage. The plugin's compile-only KGP/AGP
  dependency versions are not minimum supported versions. See [the root requirements](../README.md#requirements).
- Disable during IDE sync: `-PPROJECT_DOCTOR_DISABLE_ON_SYNC=true`.
- Wire into `KotlinCompile`: `-PPROJECT_DOCTOR_ENABLE_ON_COMPILE=true`.
