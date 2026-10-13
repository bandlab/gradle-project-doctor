# Releasing Project Doctor

This is maintainer guidance. Consumers only need Maven Central and the setup in
[README.md](../README.md).

## Preflight

- Run the verification commands in [CONTRIBUTING.md](../CONTRIBUTING.md).
- Check the release version, README requirements, changelog, and sample plugin version.
- The publishing workflow builds with **Java 25** but targets **Java 21** (`targetJvm` in
  `build.gradle.kts`). After publishing, check that `org.gradle.jvm.version` in the module
  metadata is still 21.

## Central Portal setup

Artifacts and plugin markers are published through `com.vanniktech.maven.publish` to the
[Sonatype Central Portal](https://central.sonatype.com/). Releases are immutable. Do not republish
an existing version. `1.0.0` is reserved because it was used for an internal release, so
don't publish it publicly either.

One-time setup:

1. Verify the `com.bandlab` namespace.
2. Generate a Portal user token, not an account login password.
3. Make the release signing public key available on a Central-supported keyserver, for example:
   `gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>`.
4. Configure deployment branch/tag restrictions and, if available, required reviewers on the
   GitHub `publish` environment. Naming the environment in a workflow does not protect secrets
   from workflows on arbitrary refs.

Environment secrets:

| Secret | Gradle property | Purpose |
| --- | --- | --- |
| `MAVEN_CENTRAL_USERNAME` | `mavenCentralUsername` | Portal token username |
| `MAVEN_CENTRAL_PASSWORD` | `mavenCentralPassword` | Portal token password |
| `SIGNING_IN_MEMORY_KEY` | `signingInMemoryKey` | ASCII-armored GPG private key |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | `signingInMemoryKeyPassword` | Optional key passphrase |

CI maps these properties through `ORG_GRADLE_PROJECT_` environment variables. Never commit
tokens or signing keys. For local publishing, use the same property names without that prefix
in `~/.gradle/gradle.properties`, outside the repository.

## Publish

The [Publish workflow](../.github/workflows/publish.yml) runs when a GitHub release is published
or via `workflow_dispatch`. It takes the version from a manual `version` input or the release tag,
stripping a leading `v`. Without a version, Gradle defaults to **0.1.0-SNAPSHOT**. Always provide
an explicit version for a release.

The workflow verifies credentials and the release signing key, runs `check`, then uploads to
Central. Releases must be signed. Snapshots skip signing and signing-key verification.
`automaticRelease = true` makes the deployment public once Central validates it.

Local commands, only when deliberately publishing:

```bash
# Local publication, not a dry run: writes artifacts into ~/.m2/repository.
# The default snapshot needs neither Central credentials nor signing.
./gradlew publishToMavenLocal --no-configuration-cache

# Replace <new-version> with an unpublished release or snapshot version.
./gradlew publishToMavenCentral -PprojectDoctorVersion="<new-version>" --no-configuration-cache
```

## Post-release verification

Confirm both the implementation artifact and plugin marker are available on Maven Central.
Inspect `project-doctor-<version>.module` for `org.gradle.jvm.version`, and run the sample without
an `includeBuild` or `--include-build` override after updating its version. Exercise module failure,
module summary, root aggregation failure, and baseline generation. Keep the sample's committed
baseline empty after verification.

This documentation review is not full-history secret clearance. Perform that separately before
making any such assurance.

## Suggested GitHub metadata

Apply manually if approved. No repository settings are changed by these docs.

- **Description:** Gradle plugin that enforces multi-module architecture rules, with baselines
  and actionable reports.
- **Topics:** `gradle`, `gradle-plugin`, `kotlin`, `android`, `lint`, `modularization`, `architecture`.
