import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jmailen.kotlinter") version "3.13.0"
    id("java")
    id("maven-publish")
    id("org.owasp.dependencycheck") version "7.4.4"
}

group = "com.spectralogic.rio"
version = "2.0.4"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AMAZON)
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

java {
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "internal"
            val releasesRepoUrl = "http://artifacts.eng.sldomain.com/repository/spectra-releases/"
            val snapshotsRepoUrl = "http://artifacts.eng.sldomain.com/repository/spectra-snapshots/"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            isAllowInsecureProtocol = true
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
        val mavenJava by creating(MavenPublication::class) {
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

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(platform(libs.kotlinBom))
    implementation(platform(libs.ktorBom))

    implementation(libs.kotlinxCoroutinesCore)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientJson)
    implementation(libs.ktorClientLogging)
    implementation(libs.ktorClientContentNegotiation)
    implementation(libs.ktorClientAuth)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.sslcontextKickstart)
    implementation(libs.kotlinLoggingJvm)

    // Test
    testImplementation(libs.assertjCore)
    testImplementation(libs.junitJupiterApi)

    testRuntimeOnly(libs.junitJupiterEngine)
}

dependencyCheck {
    // fail the build if any vulnerable dependencies are identified (CVSS score > 0)
    failBuildOnCVSS = 0f
    suppressionFile = "project_files/owasp/dependency-check-suppression.xml"
}

tasks.wrapper {
    // to upgrade the gradle wrapper, bump the version below and run ./gradlew wrapper twice
    gradleVersion = "8.1.1"
}
