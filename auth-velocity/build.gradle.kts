plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0"
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(project(":auth-core"))
    compileOnly("com.velocitypowered:velocity-api:3.5.0-SNAPSHOT")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.mongodb:mongodb-driver-sync:5.5.1")

    implementation("org.flywaydb:flyway-core:10.10.0")
    implementation("org.flywaydb:flyway-mysql:10.10.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("SkarvexAuth-Proxy")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    runVelocity {
        velocityVersion("3.5.0-SNAPSHOT")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("velocity-plugin.json") {
            expand(props)
        }
    }
}
