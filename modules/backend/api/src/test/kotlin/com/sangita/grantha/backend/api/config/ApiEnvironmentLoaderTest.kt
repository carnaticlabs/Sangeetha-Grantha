package com.sangita.grantha.backend.api.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createDirectories

class ApiEnvironmentLoaderTest {

    @Test
    fun `should load variables from dotenv file`() {
        // Setup config directory and .env.test file
        val configDir = File("config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
        val envFile = File(configDir, ".env.test")
        envFile.writeText("""
            SG_GEMINI_API_KEY=test-api-key
            API_PORT=9090
            DB_HOST=test-db-host
            DB_NAME=test-db
            DB_USER=test-user
            DB_PASSWORD=test-pass
            STORAGE_UPLOAD_DIR=test-uploads
        """.trimIndent())

        try {
            // Force environment to TEST
            val env = ApiEnvironmentLoader.load(mapOf("ENVIRONMENT" to "TEST"))

            assertEquals("test-api-key", env.geminiApiKey)
            assertEquals(9090, env.port)
            
            // Verify Database Config
            assertEquals("jdbc:postgresql://test-db-host:5432/test-db", env.database?.jdbcUrl)
            
            // Verify Storage Config
            assertEquals("test-uploads", env.storage?.uploadDirectory)
        } finally {
            // Cleanup
            envFile.delete()
        }
    }
}
