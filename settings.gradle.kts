/**
 * ***************************************************************************
 *    Copyright 2014-2023 Spectra Logic Corporation. All Rights Reserved.
 * ***************************************************************************
 */

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention").version("0.5.0")
}

rootProject.name="rio-client"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("assertj", "3.23.1")
            version("junit", "5.8.2")
            version("kotlin", "1.9.10")
            version("kotlinLogging", "5.1.0")
            version("kotlinxCoroutines", "1.7.3")
            version("ktor", "2.3.3")

            library("kotlinBom", "org.jetbrains.kotlin", "kotlin-bom").versionRef("kotlin")
            library("kotlinLogging", "io.github.oshai", "kotlin-logging").versionRef("kotlinLogging")
            library("kotlinxCoroutinesCore", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("kotlinxCoroutines")
            library("ktorBom", "io.ktor", "ktor-bom").versionRef("ktor")
            library("ktorClientAuth", "io.ktor", "ktor-client-auth").withoutVersion()
            library("ktorClientCio", "io.ktor", "ktor-client-cio").withoutVersion()
            library("ktorClientContentNegotiation", "io.ktor", "ktor-client-content-negotiation").withoutVersion()
            library("ktorClientCore", "io.ktor", "ktor-client-core").withoutVersion()
            library("ktorClientJson", "io.ktor", "ktor-client-json").withoutVersion()
            library("ktorClientLogging", "io.ktor", "ktor-client-logging").withoutVersion()
            library("ktorSerializationKotlinxJson", "io.ktor", "ktor-serialization-kotlinx-json").withoutVersion()

            // Test-only dependencies
            library("assertjCore", "org.assertj", "assertj-core").versionRef("assertj")
            library("junitJupiterApi", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
            library("junitJupiterEngine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")

            plugin("kotlinJvm", "org.jetbrains.kotlin.jvm").versionRef("kotlin")
            plugin("kotlinPluginSerialization", "org.jetbrains.kotlin.plugin.serialization").versionRef("kotlin")
            plugin("kotlinter", "org.jmailen.kotlinter").version("3.13.0")
            plugin("owaspDepCheck","org.owasp.dependencycheck").version("8.1.2")
            plugin("versions", "com.github.ben-manes.versions").version("0.46.0")
        }
    }
}