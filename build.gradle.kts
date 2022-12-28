import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    id("org.jmailen.kotlinter") version "3.3.0"
    id("java")
    id("maven-publish")
    id("org.owasp.dependencycheck") version "6.3.1"
}

group = "com.spectralogic"
version = "1.2.1"
tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        // since java 8 is the minimum version supported, make sure we always
        // produce java 8 bytecode
        if (JavaVersion.current() != org.gradle.api.JavaVersion.VERSION_1_8) {
            options.release.set(8)
        } else {
            // java 8 does not have a release option, so use source and target compatibility
            setSourceCompatibility(JavaVersion.VERSION_1_8.toString())
            setTargetCompatibility(JavaVersion.VERSION_1_8.toString())
        }
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
    val jacksonVersion = "2.13.4.20221013"
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    val ktorVersion = "1.6.8"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
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
