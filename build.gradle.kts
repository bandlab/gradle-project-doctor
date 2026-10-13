import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `kotlin-dsl`
    alias(libs.plugins.mavenPublish)
}

group = "com.bandlab"
version = providers.gradleProperty("projectDoctorVersion").orElse("0.1.0-SNAPSHOT").get()

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

// Build with JDK 25 but target Java 21, so consumers can run Gradle on Java 21+.
// This also sets the published `org.gradle.jvm.version` attribute.
val targetJvm = 21

java {
    targetCompatibility = JavaVersion.toVersion(targetJvm)
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(targetJvm)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(targetJvm.toString()))
        // Fail on JDK APIs newer than the target, not just on bytecode level
        freeCompilerArgs.add("-Xjdk-release=$targetJvm")
    }
}

// Optionally run tests (and the TestKit builds they start) on another JDK, e.g. -PtestJavaVersion=21
prop("testJavaVersion")?.let { testJavaVersion ->
    tasks.withType<Test>().configureEach {
        javaLauncher.set(
            javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(testJavaVersion)) }
        )
    }
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
    configure(GradlePlugin(javadocJar = JavadocJar.Empty(), sourcesJar = SourcesJar.Sources()))
    coordinates(group.toString(), "project-doctor", version.toString())

    pom {
        name.set("Project Doctor")
        description.set("Gradle plugin to lint multi-module project structure")
        inceptionYear.set("2025")
        url.set("https://github.com/bandlab/gradle-project-doctor")
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
                email.set("support@bandlab.com")
                url.set("https://github.com/bandlab")
                organization.set("BandLab Technologies")
                organizationUrl.set("https://github.com/bandlab")
            }
        }
        scm {
            url.set("https://github.com/bandlab/gradle-project-doctor")
            connection.set("scm:git:git://github.com/bandlab/gradle-project-doctor.git")
            developerConnection.set("scm:git:ssh://git@github.com/bandlab/gradle-project-doctor.git")
        }
    }

    // Sonatype Central Portal. The plugin reads credentials from Gradle properties:
    // -P, gradle.properties, or ORG_GRADLE_PROJECT_ env vars (see gradle.properties).
    // Releases publish automatically once Central validates the deployment.
    publishToMavenCentral(automaticRelease = true)

    // Snapshots never need signing, even if local/CI signing credentials are configured.
    // Sign releases when a key is configured; Central rejects unsigned releases.
    if (!version.toString().endsWith("-SNAPSHOT") && prop("signingInMemoryKey") != null) {
        signAllPublications()
    }
}
