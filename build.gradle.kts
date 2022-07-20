import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    id("org.jmailen.kotlinter") version "3.3.0"
    id("java")
    id("maven-publish")
    id("org.owasp.dependencycheck") version "6.3.1"
}

group = "com.spectralogic"
version = "1.2.1"
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "16"
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
    val jacksonVersion = "2.13.2.20220324"
    implementation(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.6.10"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")

    val ktorVersion = "1.6.8"
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-auth:$ktorVersion")
    implementation("io.github.hakky54:sslcontext-kickstart:7.0.3")

    implementation("org.apache.logging.log4j:log4j-api-kotlin:1.2.0")


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
