plugins {
    alias(libs.plugins.kotlinJvm)
}

kotlin {
    jvmToolchain(25)
}

// Shared integration-test infrastructure (TRACK-111, D11). Code lives in `src/main` — not `src/test`
// — so the test source sets of :modules:backend:api and :modules:backend:dal can consume it via
// `testImplementation(project(":modules:backend:test-support"))`. It is never shipped in production
// artifacts (only test classpaths depend on it).
dependencies {
    api(project(":modules:backend:dal"))

    // Substrate: Testcontainers + Flyway JVM API (originally TRACK-110 Sub-part B).
    api(libs.testcontainers.postgresql)
    api(libs.flyway.core)
    runtimeOnly(libs.flyway.database.postgresql)

    // JUnit Jupiter API for the @Tag / @BeforeAll / @AfterEach symbols used by IntegrationTestBase.
    api(kotlin("test-junit5"))

    api(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.logback.classic)
}
