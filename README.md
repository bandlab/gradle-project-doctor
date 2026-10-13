<div align="center">

# 🩺 Project Doctor

**A Gradle plugin that lints your project structure.**

Module conventions, forbidden dependencies, leaky `api` exposure. All checked as part of the build,
so structural rules are enforced instead of remembered.

[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE.txt)
[![Maven Central](https://img.shields.io/maven-central/v/com.bandlab/project-doctor.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/com.bandlab/project-doctor)
[![Latest release](https://img.shields.io/github/v/release/bandlab/gradle-project-doctor?label=latest)](https://github.com/bandlab/gradle-project-doctor/releases/latest)

Configuration Cache · Isolated Projects · **Battle-tested** on a 1600+ modules project.

</div>

## Why Project Doctor?

Most linters check your code. **Project Doctor checks the shape of your build.**

As a modular codebase grows, architecture rules become increasingly important and increasingly easy to forget.

Rules like:

- `:api` modules must stay Kotlin/JVM-only
- API and UI modules must not depend on screen modules
- DI runtime dependencies must never leak through `api()`

Today, these rules often live in tribal knowledge, documentation, or review comments. Over time, they drift.

**Project Doctor turns those architectural rules into executable build checks.**

Write down the rules that define how your project should be structured, and let the build enforce them automatically.

```kotlin
errorOn("Metro runtime must not be exposed via api") {
    allProjects().thatTransitivelyExpose {
        library(libs.metro.runtime)                // version catalog entry
        // or: name("dev.zacsweers.metro:runtime") // group:name coordinates
        // or: nameContains("metro")               // substring match
    }
}
```

If a codebase already breaks a rule you want to introduce,
[baseline it](#-baselines) and enforce it from that point forward.

Originally built for BandLab Android (1600+ modules), now open source under Apache 2.0.

## ✨ Highlights

|                                       |                                                                    |
| ------------------------------------- | ------------------------------------------------------------------ |
| 🧬 **Isolated Projects ready**        | No cross-project configuration access. Faster IDE Sync             |
| ⚡ **Configuration Cache compatible** | Works with the configuration cache enabled. Check the `sample/`.   |
| 🩹 **Baselines**                      | Adopt on an existing codebase without fixing every violation first |
| 📊 **Actionable Reports**             | Console, JSON, and **Checkstyle XML**                              |

---

## Requirements

- **Java 21 or newer for the JVM running Gradle.** This is the JVM shown as **Daemon JVM** in
  `./gradlew --version`, not your modules' `jvmToolchain`. `0.2.0` still requires Java 25.
- A compatible Gradle version. The repository wrapper is **9.8.0**. No lower Gradle minimum
  has been established by a compatibility matrix.
- Kotlin DSL or Groovy DSL. Examples below use Kotlin DSL, and the functional tests use Groovy.

## Add to your project and use

The simplest approach is to add the following:

**`settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.bandlab.project.doctor") version "<latest_version>"
    }
}
```

Replace `<latest_version>` with the version shown above or check the
[latest release](https://github.com/bandlab/gradle-project-doctor/releases/latest).
`0.2.0` is available on Maven Central, including the plugin marker.
Declare the version once here, then apply the plugin versionless anywhere.

Then apply it to the root project **and** each module you want checked:

**Root `build.gradle.kts`**

```kotlin
// Registers aggregation tasks + the rules DSL
plugins {
    id("com.bandlab.project.doctor")
}
```

**Each checked module's `build.gradle.kts`**

```kotlin
// Registers doctorPopulate / doctorCheck alongside your usual JVM or Android plugins
plugins {
    id("com.bandlab.project.doctor")
}
```

Keep the module's existing JVM or Android plugins, which provide its runtime classpath.
Skip empty container projects. Apply Project Doctor per module or through a convention plugin,
not a cross-project `subprojects { }` block when using Isolated Projects.

(A version catalog works too, e.g. `alias(libs.plugins.projectDoctor)`.)

## Quickstart

These rules are adapted from BandLab Android: keep most API modules Kotlin/JVM-only,
prevent API/UI modules from depending on screens, and keep the DI runtime out of public APIs.
The examples use `libs` aliases from an existing version catalog. Adapt the aliases and module
suffixes to your build.

Declare the rules once, in the **root `build.gradle.kts`**:

```kotlin
// root build.gradle.kts
projectDoctor {
    // 1. Android-specific APIs are explicitly named :android:api
    errorOn("api modules must be Kotlin JVM unless named :android:api") {
        projectWith { nameEndsWith(":api") }
            .withExclusion { nameEndsWith(":android:api") }
            .thatDoesNotHavePluginWith { id("org.jetbrains.kotlin.jvm") }
    }

    // 2. Screen implementations must not leak into API or shared UI modules
    errorOn("api and ui modules must not depend on screen modules") {
        projectWith { nameEndsWith(":api") }
            .thatHasDependencyWith { nameEndsWith(":screen") }
        projectWith { nameEndsWith(":ui") }
            .thatHasDependencyWith { nameEndsWith(":screen") }
    }

    // 3. Keep the DI runtime as an implementation detail
    errorOn("Metro runtime must not be exposed via api") {
        allProjects()
            .thatTransitivelyExpose { library(libs.metro.runtime) }
    }
}
```

Run it:

```bash
./gradlew doctorCheck                 # fail the build on error-level violations
./gradlew doctorCheck --summary       # let module checks finish, then fail at the root if needed
```

Already have violations you can't fix yet? See [Baselines](#-baselines).

### What you get

```
========================= Project Doctor Report =========================
| ** feature should not directly depend on org.json; use wrapper **
| - ⚠️  :feature:home has dependency with name contains "json" (org.json:json)
|
| ** Guava must not be exposed via api **
| - ❌  :common:utils transitively exposes "com.google.guava:guava" via api(...)
|
| ** Use convention instead of direct :common:screen dependency **
| - ❌  :feature:home doesn't have plugin com.sample.convention.screen
|
| ** api modules must be Kotlin JVM **
| - ❌  :feature:home-api doesn't have plugin org.jetbrains.kotlin.jvm
=========================================================================
```

_(Real output from [`sample/`](sample/), abridged to fit.)_

## Tasks & CLI Options

### Tasks

| Task                     | Scope          | Description                                                          | Output                                     |
| ------------------------ | -------------- | -------------------------------------------------------------------- | ------------------------------------------ |
| `doctorPopulate`         | per subproject | Snapshots `ProjectComposition` for the module                        | `project_composition.json`                 |
| `doctorCheck`            | per subproject | Evaluates rules for that module                                      | `result.json`, `checkstyle.xml`            |
| `doctorCheck`            | root           | Merges every module's JSON, filters the baseline, prints the report  | `build/doctor/merged.json`                 |
| `doctorGenerateBaseline` | root           | Regenerates the configured baseline from current violations          | configured `baselineFile`                  |
| `doctorValidate`         | per subproject | Sanity-checks that composition data resolved correctly               | none                                       |

Module reports are written to `<root>/build/doctor/<module-dir>/`. Treat the console output and
task outcome as the result of a run. See [docs/reports.md](docs/reports.md) for where each report
is written and when it is generated.

### Gradle Properties

| Property                            | Default | Description                                                                                                           |
| ----------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------- |
| `PROJECT_DOCTOR_ENABLE_ON_COMPILE`  | `false` | Make `KotlinCompile` tasks depend on `doctorCheck`, so structure is validated on every compile                        |
| `PROJECT_DOCTOR_DISABLE_ON_SYNC`    | `false` | Skip the plugin entirely during IDE sync, for faster syncs                                                            |
| `PROJECT_DOCTOR_AGGR_ALWAYS_ENABLE` | `false` | Always configure the root aggregation task, instead of only when `doctorCheck`/`doctorGenerateBaseline` was requested |

Each is enabled by setting it to `true`. Values resolve in this order: **environment variable** →
**`local.properties`** (at the root project) → **Gradle property**. Any of these work:

```bash
./gradlew doctorCheck -PPROJECT_DOCTOR_DISABLE_ON_SYNC=true    # gradle property
echo "PROJECT_DOCTOR_DISABLE_ON_SYNC=true" >> local.properties # local.properties
export PROJECT_DOCTOR_DISABLE_ON_SYNC=true                     # environment variable
```

### Task Arguments

| Argument    | Default | Effect                                                                                   |
| ----------- | ------- | ---------------------------------------------------------------------------------------- |
| `--summary` | off     | Module checks don't fail, so every module reports. The root still fails on non-baselined errors. |

Use `doctorCheck --summary`, not `:doctorCheck --summary`, which only reaches the root task.

## Rules DSL

A rule reads as one sentence: **select** projects → optionally **exclude** some → define a
violation (including a missing required plugin).

### Selectors: which projects to check

```kotlin
projectWith { /* match by name or applied plugin */ }
projectWithDependency { /* match by declared dependency */ }
allProjects()
```

| Helper                       | Example                                                          |
| ---------------------------- | ---------------------------------------------------------------- |
| `name("...")`                | `projectWith { name(":feature:home") }` - exact path             |
| `nameContains("...")`        | `projectWith { nameContains(":feature:") }`                      |
| `nameEndsWith("...")`        | `projectWith { nameEndsWith(":api") }`                           |
| `nameDoesNotContains("...")` | `projectWith { nameDoesNotContains(":test") }`                   |
| `plugin(...)`                | `projectWith { plugin(libs.plugins.kotlin.jvm) }`                |
| `dependency("...")`          | `projectWithDependency { dependency(":common:screen") }` |
| `library(...)`               | `projectWithDependency { library(libs.guava) }`          |

> Selector `plugin(...)` takes a **version catalog accessor** (`libs.plugins.…`), not a plain
> string. To match a plugin by its string id, use a rule-side `id("...")` instead.

Catalog examples require entries in your own `gradle/libs.versions.toml`. For example:

```toml
[libraries]
guava = { module = "com.google.guava:guava", version = "32.1.2-jre" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version = "2.1.20" }
android-application = { id = "com.android.application", version = "9.3.1" }
```

These are example catalog versions, not minimum supported versions. String dependency matching
uses project paths (`:common:screen`) or external `group:name` coordinates, without versions.

### Exclusions: carve out the exceptions

```kotlin
.withExclusion {
    path(":app")                          // exact project path
    paths(":legacy:a", ":legacy:b")
    nameContains("sample")
    nameEndsWith(":test")
    dependency(":common:legacy")          // exact dependency
    dependencyContains("legacy")
    plugin(libs.plugins.android.application)

    matchAll {                            // AND: excluded only if every matcher hits
        nameContains(":feature:")
        plugin(libs.plugins.kotlin.jvm)
    }
}
```

Matchers are **OR** by default. A project is excluded if any one matches. Wrap them in
`matchAll { }` when you need all of them to match.

### Rules: what is forbidden

| Rule                                | Fails when…                                                  | Accepts                                                                                                |
| ----------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| `thatHasPluginWith { ... }`         | the project applies a matching plugin                        | `alias(libs.plugins.x)`, `id("...")`                                                                   |
| `thatDoesNotHavePluginWith { ... }` | the project is **missing** an expected plugin                | `alias(libs.plugins.x)`, `id("...")`                                                                   |
| `thatHasDependencyWith { ... }`     | the project declares a matching dependency                   | `name`, `nameContains`, `nameEndsWith`, `plugin`, `excludes`, `excludeContains`, `excludeProjects { }` |
| `thatTransitivelyExpose { ... }`    | the module leaks a dependency through `api()`                | `library(libs.x)`, `name`, `nameContains`, `nameEndsWith`                                              |
| `thatNamedWith { ... }`             | the project name matches the condition                       | `nameContains`, `nameDoesNotContain`                                                                   |
| `reportAs("{{PATH}} …")`            | always: reports the selected projects with a custom message  | `{{PATH}}`, `{{PLUGINS}}` (observed plugin IDs) placeholders                                           |

For example, forbidding a dependency in feature modules except a grandfathered module:

```kotlin
errorOn("features must not use org.json directly") {
    projectWith { nameContains(":feature:") }
        .withExclusion { path(":feature:legacy") }  // grandfathered module
        .thatHasDependencyWith {
            nameContains("json")
            excludeContains("moshi")                // ignore these dependencies
        }
}
```

### Severity

`errorOn { }` fails the build. `warnOn { }` reports without failing, ideal for staging a new rule
before you enforce it.

---

## 🩹 Baselines

Introducing a structural rule to an existing codebase usually surfaces a lot of violations at once.
A baseline records the current ones so you can enforce the rule from today onward:

First configure `baselineFile = file("doctor-baseline.json")` in the root `projectDoctor` block.
There is no default baseline path. Generation replaces the file with all current violations,
including warnings. Review and commit the result. A missing baseline file is treated as empty
(in `0.2.0`, create it with `[]` first).

```bash
./gradlew doctorGenerateBaseline   # snapshot the current violations
./gradlew doctorCheck              # passes; new violations still fail
```

Recorded violations are filtered by exact message and severity. New error-level violations fail
the build, while new warnings are reported without failure. Changing a rule's message can make
an existing violation count as new. `--summary` module JSON remains unfiltered, but root checks
still apply the baseline.

---

## How It Works

```
module
──────
doctorPopulate ──► project_composition.json
                          │ (plus direct dependencies' compositions)
                          ▼
doctorCheck ──► rule evaluation ─┬─► result.json ──► root via DoctorCheckAttr
                               └─► console + checkstyle.xml (non-summary only)

root                 doctorCheck ◄── module result.json files
────                      │
                          └─► merge + baseline filter
                                      ├─► merged.json (non-empty results)
                                      └─► console + fail on remaining errors
```

Aggregation rides Gradle's dependency resolution: each module publishes its report on a
`consumable` configuration tagged with the `DoctorCheckAttr` attribute, and the root resolves them
through a matching `resolvable` configuration. Normal module checks apply the baseline before
reporting and can fail before aggregation. Summary checks allow aggregation to finish, but do
not turn root errors into warnings.

The sample enables Configuration Cache and Isolated Projects. Configuration-cache reuse is
separate from build-cache reuse: check, aggregation, and baseline tasks are explicitly untracked
and run again rather than being restored from the build cache.

---

## Sample

A runnable multi-module sample lives in [`sample/`](sample/). It consumes released **0.2.0**
by default.

```bash
./gradlew -p sample doctorCheck             # fails on the intentional violations
./gradlew -p sample doctorGenerateBaseline  # (re)generate sample/doctor-baseline.json
./gradlew -p sample doctorCheck             # now passes
```

See [`sample/README.md`](sample/README.md) for the module graph and the rules it exercises.

## Contributing

Issues and pull requests are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for prerequisites,
tests, and local sample development. Maintainers can find publishing instructions in
[docs/releasing.md](docs/releasing.md).

## License

**Apache 2.0** Copyright BandLab Technologies. See [`LICENSE.txt`](LICENSE.txt) for the full text.
