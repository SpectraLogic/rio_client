/**
 * ***************************************************************************
 *    Copyright 2014-2024 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */

plugins {
    java
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    `maven-publish`
    alias(libs.plugins.kotlinter)
    alias(libs.plugins.owaspDepCheck)
    alias(libs.plugins.versions)
}

group = "com.spectralogic.rio"
version = "3.1.24"

dependencies {
    implementation(platform(libs.kotlinBom))
    implementation(platform(libs.ktorBom))

    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientJetty)
    implementation(libs.ktorClientJson)
    implementation(libs.ktorClientLogging)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientAuth)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.kotlinLogging)

    // Test
    testImplementation(libs.assertjCore)
    testImplementation(libs.junitJupiterApi)

    testRuntimeOnly(libs.junitJupiterEngine)
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

java {
    withSourcesJar()
}

tasks.named("compileKotlin", org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlinx.serialization.ExperimentalSerializationApi")
    }
}

tasks.test {
    useJUnitPlatform() {
        includeTags("RioClientTest")
    }
}

publishing {
    repositories {
        maven {
            name = "internal"
            val releasesRepoUrl = "https://artifacts.eng.sldomain.com/repository/spectra-releases/"
            val snapshotsRepoUrl = "https://artifacts.eng.sldomain.com/repository/spectra-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = extra.has("artifactsUsername").let {
                    if (it) extra.get("artifactsUsername") as String else null
                }
                password = extra.has("artifactsPassword").let {
                    if (it) extra.get("artifactsPassword") as String else null
                }
            }
        }
    }
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}

tasks.register("publishToInternalRepository") {
    group = "publishing"
    description = "Publishes all Maven publications to the internal Maven repository."
    dependsOn(tasks.withType<PublishToMavenRepository>().matching {
        it.repository == publishing.repositories["internal"]
    })
}

dependencyCheck {
    // fail the build if any vulnerable dependencies are identified (CVSS score > 0)
    failBuildOnCVSS = 0f
    suppressionFile = "project_files/owasp/dependency-check-suppression.xml"
}

tasks.wrapper {
    // to upgrade the gradle wrapper, bump the version below and run ./gradlew wrapper twice
    gradleVersion = "8.3"
}
