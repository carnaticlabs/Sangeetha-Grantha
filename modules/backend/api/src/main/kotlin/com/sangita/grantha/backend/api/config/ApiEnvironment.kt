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
    val geminiModelUrl: String = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent",
    /** Minimum milliseconds between Gemini API calls to avoid 429 (e.g. 10000 = ~6 RPM). */
    val geminiMinIntervalMs: Long = 10_000L,
    val geminiQpsLimit: Double = 0.1,
    val geminiMaxConcurrent: Int = 1,
    val geminiMaxRetries: Int = 5,
    val geminiMaxRetryWindowMs: Long = 120_000L,
    val geminiRequestTimeoutMs: Long = 90_000L,
    val geminiFallbackModelUrl: String? = null,
    val geminiUseSchemaMode: Boolean = false,
    /** When true, skip Gemini API calls in WebScrapingService and use deterministic parsing only. */
    val geminiStubMode: Boolean = false,
    val scrapeCacheTtlHours: Long = 24,
    val scrapeCacheMaxEntries: Long = 500,
    val geoProvider: String = "osm",
    val geoApiKey: String? = null,
    val templeAutoCreateConfidence: Double = 0.9,
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
            Environment.DEV -> "development.env"
            Environment.TEST -> ".env.test"
            Environment.PROD -> ".env.production"
        }

        val dotenv = io.github.cdimascio.dotenv.dotenv {
            directory = "./config"
            this.filename = filename
            ignoreIfMissing = true
        }

        // Merge order: <env>.env then local.env (if present) then system env (highest).
        // Use a single gitignored config/local.env for all local overrides; variable names in repo-root tools.yaml.
        val combinedEnv = HashMap<String, String>()
        dotenv.entries().forEach { combinedEnv[it.key] = it.value }

        val dotenvLocal = io.github.cdimascio.dotenv.dotenv {
            directory = "./config"
            this.filename = "local.env"
            ignoreIfMissing = true
        }
        dotenvLocal.entries().forEach { combinedEnv[it.key] = it.value }

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
        
        // Gemini API Config
        val geminiApiKey = get("SG_GEMINI_API_KEY") ?: get("GEMINI_API_KEY")
        val geminiModelUrl = get("SG_GEMINI_MODEL_URL", "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent")!!
        val geminiMinIntervalMs = get("SG_GEMINI_MIN_INTERVAL_MS", "10000")?.toLongOrNull()?.coerceIn(1000L, 60_000L) ?: 10_000L
        val geminiQpsLimit = get("SG_GEMINI_QPS_LIMIT", "0.1")?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.1
        val geminiMaxConcurrent = get("SG_GEMINI_MAX_CONCURRENT", "1")?.toIntOrNull()?.coerceIn(1, 8) ?: 1
        val geminiMaxRetries = get("SG_GEMINI_MAX_RETRIES", "5")?.toIntOrNull()?.coerceIn(1, 10) ?: 5
        val geminiMaxRetryWindowMs = get("SG_GEMINI_MAX_RETRY_WINDOW_MS", "120000")?.toLongOrNull()?.coerceIn(30_000L, 300_000L) ?: 120_000L
        val geminiRequestTimeoutMs = get("SG_GEMINI_REQUEST_TIMEOUT_MS", "90000")?.toLongOrNull()?.coerceIn(30_000L, 180_000L) ?: 90_000L
        val geminiFallbackModelUrl = get("SG_GEMINI_FALLBACK_MODEL_URL")
        val geminiUseSchemaMode = get("SG_GEMINI_USE_SCHEMA_MODE", "false")?.equals("true", ignoreCase = true) ?: false
        val geminiStubMode = get("SG_GEMINI_STUB_MODE", "false")?.equals("true", ignoreCase = true) ?: false
        val scrapeCacheTtlHours = get("SG_SCRAPE_CACHE_TTL_HOURS", "24")?.toLongOrNull()?.coerceIn(1L, 168L) ?: 24
        val scrapeCacheMaxEntries = get("SG_SCRAPE_CACHE_MAX_ENTRIES", "500")?.toLongOrNull()?.coerceAtLeast(0L) ?: 500

        // Geocoding Config
        val geoProvider = get("SG_GEO_PROVIDER", "osm")!!
        val geoApiKey = get("SG_GEO_API_KEY")
        val templeAutoCreateConfidence = get("SG_TEMPLE_AUTO_CREATE_CONFIDENCE", "0.9")?.toDoubleOrNull() ?: 0.9

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
            geminiModelUrl = geminiModelUrl,
            geminiMinIntervalMs = geminiMinIntervalMs,
            geminiQpsLimit = geminiQpsLimit,
            geminiMaxConcurrent = geminiMaxConcurrent,
            geminiMaxRetries = geminiMaxRetries,
            geminiMaxRetryWindowMs = geminiMaxRetryWindowMs,
            geminiRequestTimeoutMs = geminiRequestTimeoutMs,
            geminiFallbackModelUrl = geminiFallbackModelUrl,
            geminiUseSchemaMode = geminiUseSchemaMode,
            geminiStubMode = geminiStubMode,
            scrapeCacheTtlHours = scrapeCacheTtlHours,
            scrapeCacheMaxEntries = scrapeCacheMaxEntries,
            geoProvider = geoProvider,
            geoApiKey = geoApiKey,
            templeAutoCreateConfidence = templeAutoCreateConfidence,
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
