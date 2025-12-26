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
    private val defaultConfigPath: Path = Paths.get("config/application.local.toml")

    fun load(
        configPath: Path? = null,
        env: Map<String, String> = System.getenv()
    ): DatabaseConfig {
        val resolvedPath = resolveConfigPath(configPath, env)
        val fileSettings = resolvedPath?.let { path ->
            if (isEnvFile(path)) {
                DotenvConfig.read(path)
            } else {
                TomlConfig.readSection(path, "database")
            }
        } ?: emptyMap()

        val url = firstPresent(env, fileSettings, "SG_DB_URL", "DB_URL", "url")
            ?: buildJdbcUrl(env, fileSettings)
        val username = firstPresent(env, fileSettings, "SG_DB_USERNAME", "SG_DB_USER", "DB_USERNAME", "DB_USER", "user")
            ?: error(
                "Database username not configured. Set SG_DB_USERNAME or add 'user' under [database] in ${resolvedPath ?: defaultConfigPath}."
            )
        val password = firstPresent(env, fileSettings, "SG_DB_PASSWORD", "DB_PASSWORD", "password")
            ?: error(
                "Database password not configured. Set SG_DB_PASSWORD or add 'password' under [database] in ${resolvedPath ?: defaultConfigPath}."
            )
        val schema = firstPresent(env, fileSettings, "SG_DB_SCHEMA", "DB_SCHEMA", "schema")

        return DatabaseConfig(
            jdbcUrl = url,
            username = username,
            password = password,
            schema = schema
        )
    }

    private fun resolveConfigPath(configPath: Path?, env: Map<String, String>): Path? {
        configPath?.let { if (Files.exists(it)) return it }

        val candidate = sequenceOf(
            env["SG_DB_ENV_PATH"],
            env["SG_DB_CONFIG_PATH"],
            env["SG_APP_CONFIG_PATH"],
            System.getProperty("sg.db.env"),
            System.getProperty("sg.db.config")
        ).filterNotNull()
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            ?.let { Paths.get(it) }

        if (candidate != null && Files.exists(candidate)) {
            return candidate
        }

        return TomlConfig.findConfigFile(defaultConfigPath)?.takeIf { Files.exists(it) }
    }

    private fun isEnvFile(path: Path): Boolean {
        val name = path.fileName.toString().lowercase()
        return name.endsWith(".env") || name.endsWith(".env.local") || name.endsWith(".env.test")
    }

    private fun buildJdbcUrl(env: Map<String, String>, fileSettings: Map<String, String>): String {
        val host = firstPresent(env, fileSettings, "SG_DB_HOST", "DB_HOST", "host")
            ?: error("Database host not configured. Set SG_DB_HOST or add 'host' under [database].")
        val port = firstPresent(env, fileSettings, "SG_DB_PORT", "DB_PORT", "port")
            ?: error("Database port not configured. Set SG_DB_PORT or add 'port' under [database].")
        val name = firstPresent(env, fileSettings, "SG_DB_NAME", "DB_NAME", "name")
            ?: error("Database name not configured. Set SG_DB_NAME or add 'name' under [database].")

        return "jdbc:postgresql://$host:${port.trim()}/$name"
    }

    private fun firstPresent(env: Map<String, String>, fileSettings: Map<String, String>, vararg keys: String): String? =
        keys.firstNotNullOfOrNull { key ->
            env[key]?.takeIf { it.isNotBlank() } ?: fileSettings[key]?.takeIf { it.isNotBlank() }
        }
}
