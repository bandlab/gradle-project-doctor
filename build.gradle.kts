plugins {
    `kotlin-dsl`
    `maven-publish`
}

group = "com.bandlab"
version = providers.gradleProperty("projectDoctorVersion").orElse("0.1.0-SNAPSHOT").get()

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
    compileOnly("com.android.tools.build:gradle:9.3.1")

    implementation("org.json:json:20260814")

    testImplementation("junit:junit:4.13.2")
    testImplementation("com.google.truth:truth:1.4.5")
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

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Project Doctor")
                description.set("Gradle plugin to lint multi-module project structure")
                url.set("https://github.com/bandlab/project-doctor")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("bandlab")
                        name.set("BandLab Technologies")
                    }
                }
                scm {
                    url.set("https://github.com/bandlab/project-doctor")
                    connection.set("scm:git:git@github.com:bandlab/project-doctor.git")
                }
            }
        }
    }
}