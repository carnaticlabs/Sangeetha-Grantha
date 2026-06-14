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
    implementation(libs.ktor.server.request.validation)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.call.id)
    implementation(libs.bundles.ktor.server.core.plugins)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)

    // Ktor Client
    implementation(libs.bundles.ktor.client)
    implementation(libs.ktor.client.cio)


    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    implementation(libs.dotenv.kotlin)
    implementation(libs.jwt.core)
    implementation(libs.micrometer.registry.prometheus)

    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.commons.csv)
    implementation(libs.jsoup)
    implementation(libs.caffeine)
    implementation(libs.password4j)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.mockk)

    // Shared integration-test substrate (TRACK-111, D11): IntegrationTestBase, SangitaPostgres,
    // TestDatabase, TestFixtures — plus Testcontainers + Flyway transitively.
    testImplementation(project(":modules:backend:test-support"))
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

// Run only @Tag("integration") tests (Testcontainers-backed). `make test` / check still run all.
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs only the @Tag(\"integration\") tests (self-provisioning Testcontainers DB)"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    workingDir = rootProject.projectDir
    shouldRunAfter(tasks.test)
}

// Fast, Docker-free unit slice (everything NOT @Tag("integration")) for the CI unit gate (D9: <30s).
// The default `test` task still runs the full suite, so `make test` / `check` are unchanged.
tasks.register<Test>("unitTest") {
    group = "verification"
    description = "Runs only the non-integration (unit) tests — no database required"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { excludeTags("integration") }
    workingDir = rootProject.projectDir
}

// Reference data ships via Flyway R__ repeatable migrations (make migrate / make db-reset)
// and dev sample data via `make seed-dev` per ADR-013, so a JVM seed entrypoint is redundant;
// the former `seedDatabase` JavaExec task (mainClass tools.SeedDatabaseKt, which never existed)
// was removed in TRACK-110.

// First-run admin provisioning (TRACK-110). Reads ADMIN_EMAIL / ADMIN_PASSWORD from the
// environment and upserts the admin user with an argon2id hash via PasswordHasher (TRACK-114).
tasks.register<JavaExec>("bootstrapAdmin") {
    group = "application"
    description = "Provision/update the admin user (argon2id) from ADMIN_EMAIL / ADMIN_PASSWORD"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sangita.grantha.backend.api.tools.BootstrapAdminKt")
    workingDir = rootProject.projectDir
}
