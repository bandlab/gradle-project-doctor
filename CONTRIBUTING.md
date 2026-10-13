# Contributing

Issues and pull requests are welcome. Include a minimal build or failing test for a bug, plus
the plugin, Gradle, and Gradle JVM versions. Keep changes focused and avoid committing credentials.

## Prerequisites

- JDK **25** to run Gradle and compile the plugin, matching the publishing environment.
  The plugin itself targets Java 21.
- Use the repository's `./gradlew` wrapper (**9.8.0**), not a system Gradle installation.
- Internet access to resolve Gradle and dependencies. No publishing credentials are needed.
- Python 3 for publishing-workflow tests.
- JDK **17** for the sample's Kotlin compilation tasks. Doctor checks do not compile sources.
  An Android SDK is not needed for the JVM sample or the existing test suite.

Set `JAVA_HOME` to your JDK 25 installation and check `./gradlew --version`.
If Gradle uses a separately configured daemon JVM, ensure that is Java 25 too.

## Verification

From the repository root:

```bash
./gradlew check --no-configuration-cache
python3 -B .github/tests/test_publish_workflow.py
```

`check` runs the JUnit unit tests, Gradle TestKit functional tests (Groovy fixtures), and Gradle
plugin validation. CI uses `--no-configuration-cache` for this verification. For a focused run:

```bash
./gradlew test --tests 'com.bandlab.plugin.doctor.ProjectDoctorPluginFunctionalTest' --no-configuration-cache
```

Add tests under `src/test/kotlin`. The sample is useful for manual reproduction but is not a
substitute for regression tests. Kotlin DSL and Groovy DSL are both consumer entry points.

## Code style

This repository uses [Kempt](https://github.com/ZacSweers/kempt) to enforce code style. Kempt runs
[ktfmt](https://github.com/facebook/ktfmt) (`kotlinlang` style) on Kotlin sources, sorts Gradle
dependency blocks, normalizes trailing whitespace, and inserts Apache 2.0 license headers.
Configuration lives in [`.kempt.toml`](.kempt.toml).

CI runs `kempt check` on every push and pull request via the `format-check` job in
[`.github/workflows/build.yml`](.github/workflows/build.yml). A pull request fails this check if
any file is not formatted.

### Install Kempt

A working Git 2.25+ and a JDK 17+ on your `PATH` are required.

```bash
# Homebrew (macOS, Linux)
brew install ZacSweers/tap/kempt-fmt

# or the shell installer
curl --proto '=https' --tlsv1.2 -LsSf \
  https://github.com/ZacSweers/kempt/releases/latest/download/kempt-fmt-installer.sh | sh

# or via Cargo
cargo install kempt-fmt
```

### Set up the commit hook

Install the pre-commit hook so staged files are formatted automatically before each commit:

```bash
kempt install-hook
```

This writes `.git/hooks/pre-commit`, which calls `kempt hook`. In `format` mode the hook formats
matching staged files and re-stages only the files it changed. Because `.git/hooks/` is local Git
metadata, every contributor must run `kempt install-hook` once after cloning.

### Formatting manually

```bash
kempt format            # format all tracked files in place
kempt format --staged   # format only staged files
kempt check             # read-only, exits non-zero if anything needs formatting (what CI runs)
```

## Local sample development

The sample resolves the released plugin from Maven Central by default, so running it normally
does **not** verify changes to the plugin source. To test the working tree without publishing,
run this from the repository root:

```bash
./gradlew -p sample --include-build "$PWD" doctorCheck --summary
```

Alternatively, uncomment `includeBuild("..")` inside `pluginManagement` in
`sample/settings.gradle.kts`. Avoid committing that local override unless intentionally changing
how the sample resolves the plugin.

The initial sample baseline is empty, so error-level violations deliberately fail the build.
A module-only summary check should succeed despite its violations:

```bash
./gradlew -p sample --include-build "$PWD" :feature:home:doctorCheck --summary
```

See [sample/README.md](sample/README.md) for reports, baseline generation, and cache checks.
Baseline generation modifies a tracked file. Review it and restore the original empty baseline
after experiments unless that change is intentional.

## Maintainers

Publishing instructions and suggested repository metadata are in
[docs/releasing.md](docs/releasing.md). Do not publish artifacts or change repository settings
as part of routine contribution verification.
