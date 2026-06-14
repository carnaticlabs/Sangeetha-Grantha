package com.sangita.grantha.backend.testsupport

import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Lazily-started, JVM-lifetime PostgreSQL test container (TRACK-110 Sub-part B; extracted into the
 * shared test-support module in TRACK-111).
 *
 * Replaces the old hard-coded `localhost:5432` dependency: integration tests now self-provision
 * a throwaway, version-pinned Postgres via Testcontainers. The container starts once on first
 * access and is reaped at JVM exit by Testcontainers' Ryuk.
 *
 * The image is fully qualified (`docker.io/library/...`) for Podman-readiness — Podman does not
 * assume the Docker Hub registry for bare image names (Integration Tests Approach §3.5).
 */
object SangitaPostgres {
    private val image: DockerImageName =
        DockerImageName.parse("docker.io/library/postgres:18.3-alpine")
            .asCompatibleSubstituteFor("postgres")

    val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(image)
            .withDatabaseName("sangita_grantha_test")
            .withUsername("postgres")
            .withPassword("postgres")
            .also { it.start() }
    }
}
