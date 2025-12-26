package com.sangita.grantha.backend.api.config

import com.sangita.grantha.backend.dal.support.TomlConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class ApiEnvironment(
    val environment: Environment = Environment.DEV,
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val adminToken: String = "dev-admin-token",
    val tokenTtlSeconds: Long = 3600,
    val databaseConfigPath: Path? = null,
    val corsAllowedOrigins: List<String> = listOf(
        "http://localhost:5173",
        "http://localhost:5001"
    ),
    val frontendPort: Int = 5173,
    val backendPort: Int = 8080,
)

enum class Environment {
    DEV, TEST, PROD
}

object ApiEnvironmentLoader {
    private val defaultConfigPath = Paths.get("config/application.local.toml")

    fun load(env: Map<String, String> = System.getenv()): ApiEnvironment {
        val environment = parseEnvironment(env)
        val configPath = resolveConfigPath(env)

        val backendSettings = configPath?.let { TomlConfig.readSection(it, "backend") } ?: emptyMap()

        val host = env["API_HOST"]?.takeUnless { it.isBlank() }
            ?: backendSettings["host"]
            ?: "0.0.0.0"
        val port = env["API_PORT"]?.toIntOrNull()
            ?: backendSettings["port"]?.toIntOrNull()
            ?: 8080
        val adminToken = env["ADMIN_TOKEN"]?.takeUnless { it.isBlank() }
            ?: backendSettings["admin_token"]
            ?: "dev-admin-token"
        val tokenTtlSeconds = env["TOKEN_TTL_SECONDS"]?.toLongOrNull()
            ?: backendSettings["token_ttl_seconds"]?.toLongOrNull()
            ?: 3600L
        val corsAllowedOrigins = parseCorsOrigins(env["CORS_ALLOWED_ORIGINS"])
        val frontendPort = env["FRONTEND_PORT"]?.toIntOrNull() ?: 5001

        return ApiEnvironment(
            environment = environment,
            host = host,
            port = port,
            adminToken = adminToken,
            tokenTtlSeconds = tokenTtlSeconds,
            databaseConfigPath = configPath,
            corsAllowedOrigins = corsAllowedOrigins,
            frontendPort = frontendPort,
            backendPort = port
        )
    }

    private fun parseEnvironment(env: Map<String, String>): Environment {
        val envStr = env["ENVIRONMENT"]?.uppercase()
            ?: env["SG_ENV"]?.uppercase()
            ?: "DEV"
        return when (envStr) {
            "DEV", "DEVELOPMENT" -> Environment.DEV
            "TEST", "TESTING" -> Environment.TEST
            "PROD", "PRODUCTION" -> Environment.PROD
            else -> Environment.DEV
        }
    }

    private fun resolveConfigPath(env: Map<String, String>): Path? {
        val explicit = env["SG_APP_CONFIG_PATH"]
            ?.takeIf { it.isNotBlank() }
            ?.let { Paths.get(it) }
        if (explicit != null && Files.exists(explicit)) return explicit

        val fallback = TomlConfig.findConfigFile(defaultConfigPath)
        return fallback?.takeIf { Files.exists(it) }
    }

    private fun parseCorsOrigins(origins: String?): List<String> {
        if (origins.isNullOrBlank()) {
            return listOf(
                "http://localhost:5173",
                "http://localhost:5001"
            )
        }
        return origins.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }

}
