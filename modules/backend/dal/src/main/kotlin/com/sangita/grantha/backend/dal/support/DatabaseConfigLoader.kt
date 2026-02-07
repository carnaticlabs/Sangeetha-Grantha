package com.sangita.grantha.backend.dal.support

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/** Configuration needed to establish a JDBC connection. */
data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val schema: String? = null
)

/**
 * Loads database connection settings from environment variables or a TOML configuration file.
 *
 * Precedence order (highest first):
 * 1. Explicit parameters passed to [load]
 * 2. Environment variables (`SG_DB_URL`, `SG_DB_USERNAME`, `SG_DB_PASSWORD`, etc.)
 * 3. Values under the `[database]` section of the TOML file (defaults to `config/application.local.toml`).
 */
object DatabaseConfigLoader {
    fun load(
        sysEnv: Map<String, String> = System.getenv()
    ): DatabaseConfig {
        // Merge order: development.env then local.env then system env (highest).
        val combinedEnv = HashMap<String, String>()
        
        // 1. Load development.env
        io.github.cdimascio.dotenv.dotenv {
            directory = "./config"
            filename = "development.env"
            ignoreIfMissing = true
        }.entries().forEach { combinedEnv[it.key] = it.value }

        // 2. Load local.env
        io.github.cdimascio.dotenv.dotenv {
            directory = "./config"
            filename = "local.env"
            ignoreIfMissing = true
        }.entries().forEach { combinedEnv[it.key] = it.value }

        // 3. System environment overrides
        sysEnv.forEach { (k, v) -> combinedEnv[k] = v }

        fun get(vararg keys: String): String? {
            return keys.firstNotNullOfOrNull { key ->
                combinedEnv[key]?.takeIf { it.isNotBlank() }
            }
        }

        // Build JDBC URL from components or explicit URL
        val url = get("SG_DB_URL", "DB_URL") ?: run {
            val host = get("DB_HOST", "SG_DB_HOST", "host") ?: "localhost"
            val port = get("DB_PORT", "SG_DB_PORT", "port") ?: "5432"
            val name = get("DB_NAME", "SG_DB_NAME", "name") ?: "sangita_grantha"
            "jdbc:postgresql://$host:$port/$name"
        }

        val username = get("DB_USER", "SG_DB_USERNAME", "SG_DB_USER", "DB_USERNAME", "user")
            ?: error("Database username not configured. Set DB_USER or SG_DB_USER.")
        
        val password = get("DB_PASSWORD", "SG_DB_PASSWORD", "password")
            ?: error("Database password not configured. Set DB_PASSWORD or SG_DB_PASSWORD.")
            
        val schema = get("DB_SCHEMA", "SG_DB_SCHEMA", "schema")

        return DatabaseConfig(
            jdbcUrl = url,
            username = username,
            password = password,
            schema = schema
        )
    }
}
