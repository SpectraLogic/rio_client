import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    kotlin("plugin.serialization") version "1.8.0"
    id("org.jmailen.kotlinter") version "3.13.0"
    id("java")
    id("maven-publish")
    id("org.owasp.dependencycheck") version "6.3.1"
}

group = "com.spectralogic.rioclient"
version = "2.0.1"
tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(17)
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

publishing {
    publications {
        val mavenJava by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnitPlatform()
}


repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    val ktorVersion = "2.2.1"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.github.hakky54:sslcontext-kickstart:7.4.3")
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.20")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

dependencyCheck {
    // fail the build if any vulnerable dependencies are identified (CVSS score > 0)
    failBuildOnCVSS = 0f
    suppressionFile = "project_files/owasp/dependency-check-suppression.xml"
}
