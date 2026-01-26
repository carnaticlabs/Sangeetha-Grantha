import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
    id("application")
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("com.sangita.grantha.backend.api.AppKt")
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    systemProperty("io.ktor.development", "true")
}

dependencies {
    implementation(project(":modules:backend:dal"))
    implementation(project(":modules:shared:domain"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json.server)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.cors)
    implementation(libs.bundles.ktor.server.core.plugins)

    // Ktor Client
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)


    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.dotenv.kotlin)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.csv)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)
}

// Simple dev run task (no frontend coupling yet)
tasks.register<JavaExec>("runDev") {
    group = "application"
    description = "Run the Sangita Grantha backend in development mode"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sangita.grantha.backend.api.AppKt")
    systemProperty("io.ktor.development", "true")
    workingDir = rootProject.projectDir
}

// Configure Shadow plugin to create a fat JAR with all dependencies
tasks.withType<ShadowJar> {
    archiveBaseName.set("sangita-api")
    archiveClassifier.set("")
    archiveVersion.set("")

    mergeServiceFiles()

    manifest {
        attributes(
            "Main-Class" to application.mainClass.get()
        )
    }
}

// Make build depend on shadowJar
tasks.named("build") {
    dependsOn("shadowJar")
    dependsOn("copyConfig")
}

tasks.register<Copy>("copyConfig") {
    from(rootProject.file("config"))
    into(layout.buildDirectory.dir("libs/config"))
    exclude("**/*.toml")
}

// Disable default jar task
tasks.named<Jar>("jar") {
    enabled = false
}


tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
}

tasks.register<JavaExec>("seedDatabase") {
    group = "application"
    description = "Seed the database with initial reference data and sample content"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sangita.grantha.backend.api.tools.SeedDatabaseKt")
    systemProperty("io.ktor.development", "true")
    workingDir = rootProject.projectDir
}
