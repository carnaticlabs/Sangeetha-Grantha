package com.sangita.grantha.backend.api.config

import com.sangita.grantha.backend.dal.support.DatabaseConfig
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

data class StorageConfig(
    val uploadDirectory: String,
    val publicBaseUrl: String
)

data class ApiEnvironment(
    val environment: Environment = Environment.DEV,
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val adminToken: String = "dev-admin-token",
    val tokenTtlSeconds: Long = 86400,
    val jwtSecret: String = "dev-jwt-secret",
    val jwtIssuer: String = "sangita-grantha",
    val jwtAudience: String = "sangita-users",
    val jwtRealm: String = "Sangita Grantha API",
    val geminiApiKey: String? = null,
    val database: DatabaseConfig? = null,
    val storage: StorageConfig? = null,
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

    fun load(sysEnv: Map<String, String> = System.getenv()): ApiEnvironment {
        val environment = parseEnvironment(sysEnv)

        // Load .env using idiomatic Kotlin API
        val filename = when (environment) {
            Environment.DEV -> ".env.development"
            Environment.TEST -> ".env.test"
            Environment.PROD -> ".env.production"
        }

        val dotenv = io.github.cdimascio.dotenv.dotenv {
            directory = "./config"
            this.filename = filename
            ignoreIfMissing = true
        }

        // We will create a map merging strategies:
        // System Env > .env.development > TOML (legacy/fallback)
        val combinedEnv = HashMap<String, String>()

        // 2. Add from Dotenv (middle priority)
        // 2. Add from Dotenv (middle priority)
        dotenv.entries().forEach { combinedEnv[it.key] = it.value }

        // 3. Add System Env (highest priority)
        sysEnv.forEach { (k, v) -> combinedEnv[k] = v }
        
        // Helper to get value
        fun get(key: String, fallback: String? = null): String? {
             return combinedEnv[key]?.takeIf { it.isNotBlank() } ?: fallback
        }

        val host = get("API_HOST", "0.0.0.0")!!
        val port = get("API_PORT", "8080")?.toIntOrNull() ?: 8080
        val adminToken = get("ADMIN_TOKEN", "dev-admin-token")!!
        val tokenTtlSeconds = get("TOKEN_TTL_SECONDS", "86400")?.toLongOrNull() ?: 86400L
        val jwtSecret = get("JWT_SECRET", adminToken)!!
        val jwtIssuer = get("JWT_ISSUER", "sangita-grantha")!!
        val jwtAudience = get("JWT_AUDIENCE", "sangita-users")!!
        val jwtRealm = get("JWT_REALM", "Sangita Grantha API")!!
        
        // Gemini API Key lookup
        val geminiApiKey = get("SG_GEMINI_API_KEY") ?: get("GEMINI_API_KEY")

        val corsAllowedOrigins = parseCorsOrigins(get("CORS_ALLOWED_ORIGINS"))
        val frontendPort = get("FRONTEND_PORT", "5173")?.toIntOrNull() ?: 5173

        // Database Config
        val dbHost = get("DB_HOST", "localhost")!!
        val dbPort = get("DB_PORT", "5432")?.toIntOrNull() ?: 5432
        val dbName = get("DB_NAME", "sangita_grantha")!!
        val dbUser = get("DB_USER", "postgres")!!
        val dbPassword = get("DB_PASSWORD", "postgres")!!
        val dbSchema = get("DB_SCHEMA")

        val jdbcUrl = "jdbc:postgresql://$dbHost:$dbPort/$dbName"

        val databaseConfig = DatabaseConfig(
            jdbcUrl = jdbcUrl,
            username = dbUser,
            password = dbPassword,
            schema = dbSchema
        )

        // Storage Config
        val storageConfig = StorageConfig(
            uploadDirectory = get("STORAGE_UPLOAD_DIR", "uploads")!!,
            publicBaseUrl = get("STORAGE_PUBLIC_URL", "http://localhost:$port/uploads")!!
        )

        return ApiEnvironment(
            environment = environment,
            host = host,
            port = port,
            adminToken = adminToken,
            tokenTtlSeconds = tokenTtlSeconds,
            jwtSecret = jwtSecret,
            jwtIssuer = jwtIssuer,
            jwtAudience = jwtAudience,
            jwtRealm = jwtRealm,
            geminiApiKey = geminiApiKey,
            database = databaseConfig,
            storage = storageConfig,
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
    
    // Config path resolver removed as we strictly use .env now

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
