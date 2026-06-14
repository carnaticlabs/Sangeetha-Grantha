plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    api(project(":modules:shared:domain"))

    api(libs.exposed.core)
    api(libs.exposed.dao)
    api(libs.exposed.jdbc)
    api(libs.exposed.java.time)

    implementation(libs.hikaricp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.postgresql)
    implementation(libs.micrometer.core)
    implementation(libs.dotenv.kotlin)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

    // Shared integration-test substrate (TRACK-111, D11): IntegrationTestBase, SangitaPostgres,
    // TestDatabase, TestFixtures — plus Testcontainers + Flyway transitively.
    testImplementation(project(":modules:backend:test-support"))
}

tasks.test {
    useJUnitPlatform()
    workingDir = rootProject.projectDir
}

// Run only @Tag("integration") tests (Testcontainers-backed). `make test` / check still run all.
tasks.register<Test>("integrationTest") {
    group = "verification"
    description = "Runs only the @Tag(\"integration\") DAL tests (self-provisioning Testcontainers DB)"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    workingDir = rootProject.projectDir
    shouldRunAfter(tasks.test)
}
