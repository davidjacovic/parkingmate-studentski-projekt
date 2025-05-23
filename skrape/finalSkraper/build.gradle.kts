plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "1.9.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib"))
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("it.skrape:skrapeit:1.2.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.json:json:20240303")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}