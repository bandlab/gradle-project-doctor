# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-13

This release skips `1.0.0`, which was already used for an internal release.

### Changed

- The plugin now targets Java 21, so Gradle can run on Java 21 or newer. `0.2.0` required
  Java 25.
- A configured `baselineFile` that does not exist is now treated as an empty baseline instead
  of failing the check, so you no longer need to create it with `[]` first.

### Documentation

- Added [CONTRIBUTING.md](CONTRIBUTING.md), [docs/releasing.md](docs/releasing.md), and
  [docs/reports.md](docs/reports.md).
- README now lists requirements and has a simpler quickstart. Maintainer publishing
  instructions moved out of the README.
- The sample now uses the released `0.2.0` from Maven Central instead of a snapshot.

## [0.2.0] - 2026-10-06

### Changed

- Artifacts are now published to Maven Central instead of BandLab Artifactory, so
  consumers need only `mavenCentral()`. See
  [Add to your project and use](README.md#add-to-your-project-and-use).
