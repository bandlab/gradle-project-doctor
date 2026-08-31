<div align="center">

# 🩺 Project Doctor

**A Gradle plugin that lints your project structure.**

Module conventions, forbidden dependencies, leaky `api` exposure — checked as part of the build,
so structural rules are enforced instead of remembered.

[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE.txt)

Configuration Cache · Isolated Projects · **Battle-tested** on a 1600+ modules project.

</div>

## Why Project Doctor?

Most linters check your code. **Project Doctor checks the shape of your build.**

As a modular codebase grows, architecture rules become increasingly important and increasingly easy to forget.

Rules like:

- `:api` modules must stay Kotlin/JVM-only
- Nothing should depend directly on `:common:screen`
- DI runtime dependencies must never leak through `api()`

Today, these rules often live in tribal knowledge, documentation, or review comments. Over time, they drift.

**Project Doctor turns those architectural rules into executable build checks.**

Write down the rules that define how your project should be structured, and let the build enforce them automatically.

```kotlin
errorOn("Metro runtime must not be exposed via api") {
    allProjects().thatTransitivelyExpose { library(libs.metro.runtime) }
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

## Installation

**`settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
```

Then apply it to the root project **and** each module you want checked:

```kotlin
// root build.gradle.kts, registers the aggregation task + the rules DSL
plugins {
    id("com.bandlab.project.doctor")
}

// each module's build.gradle.kts, registers doctorPopulate / doctorCheck
plugins {
    id("com.bandlab.project.doctor")
}
```

**Requirements:** Gradle with Kotlin DSL. Developed and tested against **Gradle 9.5.1**, Kotlin
Gradle Plugin `2.4.10`, and Android Gradle Plugin `9.3.1`.

## Quickstart

Declare your architecture rules once, in the root project:

```kotlin
// root build.gradle.kts
projectDoctor {
    baselineFile = file("doctor-baseline.json")

    // 1. Convention: :api modules must be Kotlin JVM only
    errorOn("api modules must be Kotlin JVM") {
        projectWith { nameEndsWith(":api") }
            .thatHasPluginWith { alias(libs.plugins.android.library) }
    }

    // 2. Indirection: go through the convention plugin, not a direct dependency
    errorOn("Use plugins.screen instead of :common:android:screen dep") {
        projectWithDependency { dependency(":common:android:screen") }
            .withExclusion { path(":app") }
            .thatDoesNotHavePluginWith { alias(bandlab.plugins.screen) }
    }

    // 3. API hygiene: the DI runtime must not leak transitively
    errorOn("Metro runtime must not be exposed via api") {
        allProjects()
            .thatTransitivelyExpose { library(libs.metro.runtime) }
    }
}
```

Run it:

```bash
./gradlew doctorCheck                 # fail the build on error-level violations
./gradlew doctorGenerateBaseline      # (re)generate doctor-baseline.json
```

### What you get

```
========================= Project Doctor Report =========================
| ** feature should not directly depend on org.json — use wrapper **
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

| Task                     | Scope          | Description                                                         | Output                                              |
| ------------------------ | -------------- | ------------------------------------------------------------------- | --------------------------------------------------- |
| `doctorPopulate`         | per subproject | Snapshots `ProjectComposition` for the module                       | `build/doctor/<path>/project_composition.json`      |
| `doctorCheck`            | per subproject | Evaluates rules for that module                                     | `build/doctor/<path>/result.json`, `checkstyle.xml` |
| `doctorCheck`            | root           | Merges every module's JSON, filters the baseline, prints the report | `build/doctor/merged.json`                          |
| `doctorGenerateBaseline` | root           | Regenerates the baseline from current violations                    | `doctor-baseline.json`                              |
| `doctorValidate`         | per subproject | Sanity-checks that composition data resolved correctly              | —                                                   |

### Gradle Properties

| Property                            | Default | Description                                                                                                           |
| ----------------------------------- | ------- | --------------------------------------------------------------------------------------------------------------------- |
| `PROJECT_DOCTOR_ENABLE_ON_COMPILE`  | `false` | Make `KotlinCompile` tasks depend on `doctorCheck`, so structure is validated on every compile                        |
| `PROJECT_DOCTOR_DISABLE_ON_SYNC`    | `false` | Skip the plugin entirely during IDE sync, for faster syncs                                                            |
| `PROJECT_DOCTOR_AGGR_ALWAYS_ENABLE` | `false` | Always configure the root aggregation task, instead of only when `doctorCheck`/`doctorGenerateBaseline` was requested |

Each is enabled by setting it to `true`. Values resolve in this order: **environment variable** →
**`local.properties`** (at the root project) → **Gradle property** — so any of these work:

```bash
./gradlew doctorCheck -PPROJECT_DOCTOR_DISABLE_ON_SYNC=true    # gradle property
echo "PROJECT_DOCTOR_DISABLE_ON_SYNC=true" >> local.properties # local.properties
export PROJECT_DOCTOR_DISABLE_ON_SYNC=true                     # environment variable
```

### Task Arguments

| Argument  | Default | Effect                                                                                                 |
| --------- | ------- | ------------------------------------------------------------------------------------------------------ |
| `summary` | off     | On a **module** task, report violations without failing that module, it will be summarized at the end. |

## Rules DSL

A rule reads as one sentence: **select** projects → optionally **exclude** some → assert what is
**forbidden**.

### Selectors — which projects to check

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
| `dependency("...")`          | `projectWithDependency { dependency(":common:android:screen") }` |
| `library(...)`               | `projectWithDependency { library(libs.metro.runtime) }`          |

> Selector `plugin(...)` takes a **version catalog accessor** (`libs.plugins.…`), not a plain
> string. To match a plugin by its string id, use a rule-side `id("...")` instead.

### Exclusions — carve out the exceptions

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

### Rules — what is forbidden

| Rule                                | Fails when…                                                  | Accepts                                                                                                |
| ----------------------------------- | ------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------ |
| `thatHasPluginWith { ... }`         | the project applies a matching plugin                        | `alias(libs.plugins.x)`, `id("...")`                                                                   |
| `thatDoesNotHavePluginWith { ... }` | the project is **missing** an expected plugin                | `alias(libs.plugins.x)`, `id("...")`                                                                   |
| `thatHasDependencyWith { ... }`     | the project declares a matching dependency                   | `name`, `nameContains`, `nameEndsWith`, `plugin`, `excludes`, `excludeContains`, `excludeProjects { }` |
| `thatTransitivelyExpose { ... }`    | the module leaks a dependency through `api()`                | `library(libs.x)`, `name`, `nameContains`, `nameEndsWith`                                              |
| `thatNamedWith { ... }`             | the project name matches the condition                       | `nameContains`, `nameDoesNotContain`                                                                   |
| `reportAs("{{PATH}} …")`            | always — reports the selected projects with a custom message | `{{PATH}}` placeholder                                                                                 |

For example, forbidding a dependency everywhere except a few grandfathered modules:

```kotlin
errorOn("features must not use org.json directly") {
    projectWith { nameContains(":feature:") }
        .thatHasDependencyWith {
            nameContains("json")
            excludeContains("moshi")            // ignore these dependencies
            excludeProjects { path(":feature:legacy") }
        }
}
```

### Severity

`errorOn { }` fails the build. `warnOn { }` reports without failing — ideal for staging a new rule
before you enforce it.

---

## 🩹 Baselines

Introducing a structural rule to an existing codebase usually surfaces a lot of violations at once.
A baseline records the current ones so you can enforce the rule from today onward:

```bash
./gradlew doctorGenerateBaseline   # snapshot the current violations
./gradlew doctorCheck              # passes — new violations still fail
```

Recorded violations are filtered out of subsequent runs, anything new fails the build.

---

## How It Works

```
subprojects                             root
──────────                              ────
doctorPopulate ──► project_composition.json ──┐
                                              ├─► DoctorChecker ─┬─► merged.json
doctorCheck ─────► result.json ───────────────┘                  ├─► checkstyle.xml
                   (aggregated via DoctorCheckAttr                └─► console report
                    variant-aware configurations)
```

Aggregation rides Gradle's dependency resolution: each module publishes its report on a
`consumable` configuration tagged with the `DoctorCheckAttr` attribute, and the root resolves them
through a matching `resolvable` configuration. That is what makes the plugin parallel-friendly,
incremental, and safe under Isolated Projects.

---

## Sample

A runnable multi-module sample lives in [`sample/`](sample/).

```bash
./gradlew -p sample doctorCheck             # fails on the intentional violations
./gradlew -p sample doctorGenerateBaseline  # (re)generate sample/doctor-baseline.json
./gradlew -p sample doctorCheck             # now passes
```

See [`sample/README.md`](sample/README.md) for the module graph and the rules it exercises.

### Publishing

Releases go out via the [`Publish` workflow](.github/workflows/publish.yml) — triggered on a GitHub
release, or manually through `workflow_dispatch` with a version.

The version comes from `-PprojectDoctorVersion` (default `0.1.0-SNAPSHOT`); a `-SNAPSHOT` suffix
routes to the snapshot repository automatically. Remote repositories are only registered when their
credentials are present, so local builds work unconfigured.

## Contributing

Issues and pull requests are welcome. The fastest way to demonstrate a bug is a failing case in
[`sample/`](sample/) or a unit test under `src/test/kotlin`.

## License

**Apache 2.0** Copyright BandLab Technologies. See [`LICENSE.txt`](LICENSE.txt) for the full text.
</content>
</invoke>
