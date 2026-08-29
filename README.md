# Project Doctor

> Gradle plugin to lint multi-module project structure — built for scale.

Originally developed for **BandLab Android (1600+ modules)**, now open source under **Apache 2.0** — see [`LICENSE.txt`](LICENSE.txt).

| Coordinates   | Value                                  |
| ------------- | -------------------------------------- |
| **Plugin ID** | `com.bandlab.project.doctor`           |
| **Artifact**  | `com.bandlab:project-doctor:<version>` |

---

## Table of Contents

- [Installation](#installation)
- [Quickstart](#quickstart)
- [Tasks & CLI Options](#tasks--cli-options)
- [Rules DSL](#rules-dsl)
- [How It Works](#how-it-works)
- [Sample](#sample)
- [Development](#development)
- [Migration](#migration)
- [License](#license)

---

## Installation

Add the plugin to your Gradle build.

**`settings.gradle.kts`**

```kotlin
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
}
```

**Root `build.gradle.kts`**

```kotlin
plugins {
  id("com.bandlab.project.doctor")
}
```

> **Requires:** Gradle with Kotlin DSL. Tested with Kotlin Gradle Plugin `2.4.10` and Android Gradle Plugin `9.3.1` (both `compileOnly`).

---

## Quickstart

Define structural rules in the root project:

```kotlin
// root build.gradle.kts
projectDoctor {
  baselineFile = file("doctor-baseline.json")

  // 1. Enforce convention: :api modules must be Kotlin JVM only
  errorOn("api modules must be Kotlin JVM") {
    projectWith { nameEndsWith(":api") }
      .thatHasPluginWith { alias(libs.plugins.android.library) } // or plugin("com.android.library")
  }

  // 2. Enforce indirection: use plugins.screen instead of direct dependency
  errorOn("Use plugins.screen instead of :common:android:screen dep") {
    projectWithDependency { dependency(":common:android:screen") }
      .withExclusion { path(":app") }
      .thatDoesNotHavePluginWith { alias(bandlab.plugins.screen) }
  }

  // 3. Enforce API hygiene: Metro runtime must not leak transitively
  errorOn("Metro runtime must not be exposed via api") {
    allProjects()
      .thatTransitivelyExpose { library(libs.metro.runtime) }
  }
}
```

### Run

```bash
./gradlew doctorCheck                 # fail on violations
./gradlew doctorCheck --summary       # always succeeds, writes JSON summary
./gradlew doctorGenerateBaseline      # (re)generate doctor-baseline.json
```

---

## Tasks & CLI Options

### Tasks

| Task                                            | Scope                          | Description                                   | Output                                                       |
| ----------------------------------------------- | ------------------------------ | --------------------------------------------- | ------------------------------------------------------------ |
| `doctorPopulate`                                | per subproject                 | Snapshots `ProjectComposition` for the module | `build/doctor/<path>/project_composition.json`               |
| `doctorCheck`                                   | per project + root aggregation | Checks rules, fails on violations             | `build/doctor/<path>/result.json`, `checkstyle.xml`, console |
| `doctorGenerateBaseline`                        | per project + root             | Regenerates baseline from current violations  | `doctor-baseline.json`                                       |
| `doctorCheck` / `doctorGenerateBaseline` (root) | root                           | Merges JSON, filters baseline, reports        | `build/doctor/merged.json` via `ViolationMerger`             |

### Gradle Properties

| Property                                   | Description                                |
| ------------------------------------------ | ------------------------------------------ |
| `-PPROJECT_DOCTOR_ENABLE_ON_COMPILE=true`  | Hook check into `KotlinCompile` tasks      |
| `-PPROJECT_DOCTOR_DISABLE_ON_SYNC=true`    | Disable plugin during Gradle sync          |
| `-PPROJECT_DOCTOR_AGGR_ALWAYS_ENABLE=true` | Always enable root aggregation task        |

### Task Arguments

| Argument    | Effect                                                                 |
| ----------- | ---------------------------------------------------------------------- |
| `--summary` | Do not fail build; always write JSON summary (`doctorCheck --summary`) |

### Wiring into your own CI aggregation task

`doctorCheck` is not attached to any existing `check`/CI task automatically. Wire it in from
your own build logic, e.g.:

```kotlin
tasks.matching { it.name == "check" }.configureEach {
    dependsOn(tasks.matching { it.name == "doctorCheck" })
}
```

---

## Rules DSL

### Selectors — choose which projects to check

```kotlin
projectWith { /* match project by name/plugin */ }
projectWithDependency { /* match project by declared dependency */ }
allProjects() // match every project
```

| Selector helper       | Example                                                          |
| --------------------- | ---------------------------------------------------------------- |
| `nameContains("...")` | `projectWith { nameContains(":feature:") }`                      |
| `nameEndsWith("...")` | `projectWith { nameEndsWith(":api") }`                           |
| `nameMatches(regex)`  | `projectWith { nameMatches(Regex(".*:impl")) }`                  |
| `plugin("...")`       | `projectWith { plugin("com.android.library") }`                  |
| `alias(...)`          | `projectWith { alias(libs.plugins.kotlin.jvm) }`                 |
| `dependency("...")`   | `projectWithDependency { dependency(":common:android:screen") }` |
| `library(...)`        | `projectWithDependency { library(libs.metro.runtime) }`          |

### Exclusions — carve out exceptions

```kotlin
.withExclusion {
  path(":app")
  paths(":legacy:a", ":legacy:b")
  nameContains("sample")
  nameEndsWith(":test")
  plugin("com.android.application")
  library(libs.some.lib)
}
```

### Rules — define what is forbidden

| Rule                                | Description                                        |
| ----------------------------------- | -------------------------------------------------- |
| `thatHasDependencyWith { ... }`     | Project has a specific (direct) dependency         |
| `thatHasPluginWith { ... }`         | Project applies a specific plugin                  |
| `thatDoesNotHavePluginWith { ... }` | Project is _missing_ an expected plugin            |
| `thatTransitivelyExpose { ... }`    | Module transitively exposes a dependency via `api` |
| `thatNamedWith { ... }`             | Project name matches condition                     |
| `reportAs("{{PATH}} template")`     | Custom violation message (`{{PATH}}` placeholder)  |

---

## How It Works

```
subprojects                    root
──────────                     ────
doctorPopulate ──► project_composition.json ──┐
                                              ├─► DoctorChecker ├──► result.json
doctorCheck ─────► runtimeClasspath ──────────┘                 ├─► checkstyle.xml
                   (+ stageDebugRuntimeClasspath,               └─► console
                    via DoctorCheckAttr variant-aware configurations)
```

---

## Sample

A runnable multi-module sample lives in [`sample/`](sample/) that dogfoods the plugin via `includeBuild("..")` — no publish step required.

```bash
./gradlew -p sample doctorCheck --summary   # never fails, writes build/doctor/merged.json
./gradlew -p sample doctorCheck             # fails on error-level violations
./gradlew -p sample doctorGenerateBaseline  # (re)generate sample/doctor-baseline.json
```

See [`sample/README.md`](sample/README.md) for module graph and rules covered (`thatHasPluginWith`, `thatDoesNotHavePluginWith`, `thatHasDependencyWith`, `thatTransitivelyExpose`, `thatNamedWith`, `reportAs`, `withExclusion`).

## Development

```bash
# Run tests (68 tests)
./gradlew check

# Publish SNAPSHOT to local Maven for dogfooding
./gradlew publishToMavenLocal -PprojectDoctorVersion=0.1.0-SNAPSHOT
```

### Publishing

Releases are published by the [`Publish` workflow](.github/workflows/publish.yml) —
triggered on GitHub release, or manually via `workflow_dispatch` with a version.

The version comes from `-PprojectDoctorVersion` (default `0.1.0-SNAPSHOT`); a
`-SNAPSHOT` suffix routes to the snapshot repository automatically. Remote
repositories are only registered when their credentials are present, so local
builds work unconfigured.

---

## License

**Apache 2.0** — same as [`github.com/bandlab/metro-station/LICENSE.txt`](https://github.com/bandlab/metro-station/blob/main/LICENSE.txt).

Copyright BandLab Technologies. See [`LICENSE.txt`](LICENSE.txt) for full text.
