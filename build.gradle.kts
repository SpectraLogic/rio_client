import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.20"
//    application
    id("org.jmailen.kotlinter") version "3.2.0"
    id("java")
    id("maven-publish")
}

group = "com.spectralogic"
version = "1.0.0"
tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "15"
    }

//    val sourcesJar by creating(Jar::class) {
//        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
//        classifier = "sources"
//        from(java.sourceSets["main"].allSource)
//    }
//
//    val javadocJar by creating(Jar::class) {
//        dependsOn(JavaPlugin.JAVADOC_TASK_NAME)
//        classifier = "javadoc"
//        from(java.docsDir)
//    }
//
//    artifacts {
//        add("archives", sourcesJar)
//        add("archives", javadocJar)
//    }
}

publishing {
    publications {
        val mavenJava by creating(MavenPublication::class) {
            from(components["java"])
        }
    }
}

//task( "sourcesJar", Jar::class) {
//    classifier = "sources"
//    from(sourceSets.main.allSource)
//}
//
//task("javadocJar", Jar::class) {
//    classifier = "javadoc"
//    from(javadoc.destinationDir)
//}
//
//artifacts {
//    archives("sourcesJar")
//    archives("javadocJar")
//}

    repositories {
        mavenCentral()
    }

    dependencies {
        val jacksonVersion = "2.13.+"
        implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
        implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

        val guavaVersion = "30.1-jre"
        implementation("com.google.guava:guava:$guavaVersion")

        val kotlinVersion = "1.5.1"
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinVersion")

        val ktorVersion = "1.6.1"
        implementation("io.ktor:ktor-client-core:$ktorVersion")
        implementation("io.ktor:ktor-client-cio:$ktorVersion")
        implementation("io.ktor:ktor-client-jackson:$ktorVersion")
        implementation("io.ktor:ktor-client-logging:$ktorVersion")
        implementation("io.github.hakky54:sslcontext-kickstart:6.2.0")


        val retrofitVersion = "2.9.0"
        implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
        implementation("com.squareup.retrofit2:converter-simplexml:$retrofitVersion")
        implementation("com.squareup.retrofit2:adapter-rxjava2:$retrofitVersion")
        implementation("com.squareup.retrofit2:converter-jackson:$retrofitVersion")

//Test
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.1")
        testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
        testImplementation("org.assertj:assertj-core:3.16.1")
    }

//tasks.distZip {
//    into("${project.name}-${project.version}/bin") {
//        from(".")
//        include("log4j2.properties")
//    }
//}