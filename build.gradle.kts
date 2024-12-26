plugins {
    kotlin("jvm") version "2.1.0"
}

group = "com.thomas"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.0")
    implementation("net.java.dev.jna:jna:5.15.0")
    implementation("net.openhft:affinity:3.23.3")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}