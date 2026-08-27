import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `kotlin-dsl`
    alias(libs.plugins.mavenPublish)
}

group = "com.bandlab"
version = providers.gradleProperty("projectDoctorVersion").orElse("0.1.0-SNAPSHOT").get()

/** Environment variable, or null when unset *or* blank. */
fun env(name: String): String? =
    providers.environmentVariable(name).orNull?.takeIf(String::isNotBlank)

/** Gradle/`ORG_GRADLE_PROJECT_` property, or null when unset *or* blank. */
fun prop(name: String): String? =
    providers.gradleProperty(name).orNull?.takeIf(String::isNotBlank)

dependencies {
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.android.gradlePlugin)

    implementation(libs.json)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(gradleTestKit())
}

tasks.withType<ValidatePlugins>().configureEach {
    failOnWarning.set(true)
    enableStricterValidation.set(true)
}

gradlePlugin {
    plugins {
        register("com.bandlab.project.doctor") {
            implementationClass = "com.bandlab.plugin.doctor.ProjectDoctorPlugin"
        }
    }
}

mavenPublishing {
    configure(GradlePlugin(javadocJar = JavadocJar.Empty(), sourcesJar = true))
    coordinates(group.toString(), "project-doctor", version.toString())

    pom {
        name.set("Project Doctor")
        description.set("Gradle plugin to lint multi-module project structure")
        inceptionYear.set("2025")
        url.set("https://github.com/bandlab/project-doctor")
        licenses {
            license {
                name.set("Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("bandlab")
                name.set("BandLab Technologies")
                url.set("https://github.com/bandlab")
            }
        }
        scm {
            url.set("https://github.com/bandlab/project-doctor")
            connection.set("scm:git:git://github.com/bandlab/project-doctor.git")
            developerConnection.set("scm:git:ssh://git@github.com/bandlab/project-doctor.git")
        }
    }

    // Central Portal. Credentials come from ORG_GRADLE_PROJECT_mavenCentralUsername /
    // ORG_GRADLE_PROJECT_mavenCentralPassword, so only wire it up when they are present.
    if (prop("mavenCentralUsername") != null) {
        publishToMavenCentral(automaticRelease = false)
    }

    // Signing key comes from ORG_GRADLE_PROJECT_signingInMemoryKey /
    // ORG_GRADLE_PROJECT_signingInMemoryKeyPassword.
    if (prop("signingInMemoryKey") != null) {
        signAllPublications()
    }
}

publishing {
    repositories {
        val isSnapshot = project.version.toString().endsWith("-SNAPSHOT", ignoreCase = true)

        // Snapshot and release live in separate Artifactory repositories, picked by the
        // version suffix. ARTIFACTORY_URL is an escape hatch that forces a single repo
        // regardless of version (handy for `file:` smoke tests) -- prefer the
        // suffix-specific vars so a SNAPSHOT can never land in the release repo.
        //
        // GitHub Actions sets an undefined secret to "", which Gradle reports as *present*.
        // Treat blank as absent so defaults still apply and blank creds don't register an
        // unusable repository.
        val artifactoryUrl = env("ARTIFACTORY_URL") ?: if (isSnapshot) {
            env("ARTIFACTORY_SNAPSHOT_URL")
                ?: "https://artifactory.bandlab.cloud/artifactory/libs-snapshot-local"
        } else {
            env("ARTIFACTORY_RELEASE_URL")
                ?: "https://artifactory.bandlab.cloud/artifactory/libs-release-local"
        }
        val artifactoryUsername = env("ARTIFACTORY_USERNAME")
        val artifactoryPassword = env("ARTIFACTORY_PASSWORD")

        if (artifactoryUrl.startsWith("file:")) {
            maven {
                name = "BandLab"
                url = uri(artifactoryUrl)
            }
        } else if (artifactoryUsername != null && artifactoryPassword != null) {
            maven {
                name = "BandLab"
                url = uri(artifactoryUrl)
                credentials {
                    username = artifactoryUsername
                    password = artifactoryPassword
                }
            }
        }
    }
}
