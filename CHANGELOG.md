# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.0.2] - Unreleased

### Changed

- Publishing target moved from BandLab Artifactory to Maven Central via the Sonatype
  Central Portal (`com.vanniktech.maven.publish`). Consumers need only `mavenCentral()`;
  see [Add to your project and use](README.md#add-to-your-project-and-use).
- Reworked the `Publish` workflow: the version is resolved once from the release tag or
  the manual `version` input, credentials are verified up front, and unsigned releases
  are refused (Central rejects those). Snapshots keep publishing unsigned.
- POM now carries the developer email, organization, and repository URLs that Central
  requires. Removed the `TODO.md` and other obsolete Artifactory notes.
- The `sample/` consumes the published `0.1.0-SNAPSHOT` by default; uncomment
  `includeBuild("..")` in `sample/settings.gradle.kts` to develop against the working
  tree instead.
