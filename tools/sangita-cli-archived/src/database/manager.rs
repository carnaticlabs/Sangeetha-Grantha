use crate::database::error::DatabaseError;
use crate::utils::{get_postgres_bin_path, run_command_with_env};
use anyhow::{Context, Result};
use log::{info, warn};
use sqlx::{postgres::PgPoolOptions, Executor, Pool, Postgres, Transaction};
use std::path::{Path, PathBuf};
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::time::sleep;
use urlencoding::encode;

pub struct DatabaseConfig {
    pub connection_string: String,
    pub pool_options: PgPoolOptions,
    pub timeouts: crate::database::instance::PostgresTimeouts,
    pub admin_db: String,
    pub admin_user: String,
    pub admin_password: String,
}

impl DatabaseConfig {
    pub fn new(connection_string: String) -> Self {
        Self {
            connection_string,
            pool_options: PgPoolOptions::new().max_connections(5),
            timeouts: crate::database::instance::PostgresTimeouts::default(),
            admin_db: "postgres".to_string(),
            admin_user: "postgres".to_string(),
            admin_password: "postgres".to_string(),
        }
    }

    pub fn with_admin_db(mut self, admin_db: impl Into<String>) -> Self {
        self.admin_db = admin_db.into();
        self
    }

    pub fn with_admin_user(mut self, admin_user: impl Into<String>) -> Self {
        self.admin_user = admin_user.into();
        self
    }

    pub fn with_admin_password(mut self, admin_password: impl Into<String>) -> Self {
        self.admin_password = admin_password.into();
        self
    }
}

pub struct DatabaseManager {
    config: DatabaseConfig,
    migrations_path: PathBuf,
    seed_data_path: Option<PathBuf>,
    pool: Arc<Mutex<Option<Pool<Postgres>>>>,
}

impl DatabaseManager {
    pub fn new(
        config: DatabaseConfig,
        migrations_path: impl Into<PathBuf>,
        seed_data_path: Option<PathBuf>,
    ) -> Self {
        Self {
            config,
            migrations_path: migrations_path.into(),
            seed_data_path,
            pool: Arc::new(Mutex::new(None)),
        }
    }

    pub async fn ensure_database_exists(&self) -> Result<()> {
        // Extract database name from target connection string
        let target_conn_str = &self.config.connection_string;
        let db_name = self.extract_database_name(target_conn_str)?;

        info!("Checking if database '{}' exists...", db_name);

        // Use manual methods directly for better reliability, especially with Docker
        // This explicitly connects to the admin database and checks/creates the target database
        if !self.manual_database_exists(&db_name).await? {
            info!("Creating database '{}'...", db_name);
            self.manual_create_database(&db_name)
                .await
                .with_context(|| format!("Failed to create database '{}'", db_name))?;
            info!("Database '{}' created successfully", db_name);
        } else {
            info!("Database '{}' already exists", db_name);
        }

        Ok(())
    }

    async fn manual_database_exists(&self, db_name: &str) -> Result<bool> {
        let admin_conn_str = self.build_admin_connection_string();
        let pool = self
            .config
            .pool_options
            .clone()
            .connect(&admin_conn_str)
            .await
            .with_context(|| {
                format!(
                    "Failed to connect to admin database '{}' at {}:{} for existence check. \
                 Verify that PostgreSQL is running and credentials are correct.",
                    self.config.admin_db,
                    self.extract_host_port(&self.config.connection_string).0,
                    self.extract_host_port(&self.config.connection_string).1
                )
            })?;

        let result: Option<(i32,)> = sqlx::query_as("SELECT 1 FROM pg_database WHERE datname = $1")
            .bind(db_name)
            .fetch_optional(&pool)
            .await
            .context("Failed to query database existence")?;

        // Pool will be dropped and connections closed automatically
        drop(pool);
        Ok(result.is_some())
    }

    async fn manual_create_database(&self, db_name: &str) -> Result<()> {
        let admin_conn_str = self.build_admin_connection_string();
        let pool = self
            .config
            .pool_options
            .clone()
            .connect(&admin_conn_str)
            .await
            .with_context(|| {
                format!(
                    "Failed to connect to admin database '{}' at {}:{} for database creation. \
                 Verify that PostgreSQL is running and credentials are correct.",
                    self.config.admin_db,
                    self.extract_host_port(&self.config.connection_string).0,
                    self.extract_host_port(&self.config.connection_string).1
                )
            })?;

        // Validate db_name to prevent SQL injection
        // PostgreSQL identifiers can contain letters, digits, and underscores
        // We also allow hyphens but need to quote them
        if !db_name
            .chars()
            .all(|c| c.is_alphanumeric() || c == '_' || c == '-')
        {
            anyhow::bail!("Invalid database name: {}", db_name);
        }

        // Quote the database name to handle special characters safely
        let query = format!("CREATE DATABASE \"{}\"", db_name.replace('"', "\"\""));
        sqlx::query(&query)
            .execute(&pool)
            .await
            .with_context(|| format!("Failed to create database '{}'", db_name))?;

        // Pool will be dropped and connections closed automatically
        drop(pool);
        Ok(())
    }

    fn build_admin_connection_string(&self) -> String {
        // Build connection string to admin database (postgres)
        let conn_str = &self.config.connection_string;
        let (host, port) = self.extract_host_port(conn_str);

        format!(
            "postgres://{}:{}@{}:{}/{}",
            encode(&self.config.admin_user),
            encode(&self.config.admin_password),
            host,
            port,
            self.config.admin_db
        )
    }

    fn extract_database_name(&self, conn_str: &str) -> Result<String> {
        if let Some(slash_pos) = conn_str.rfind('/') {
            let db_part = &conn_str[slash_pos + 1..];
            // Remove query parameters if any
            let db_name = if let Some(q_pos) = db_part.find('?') {
                &db_part[..q_pos]
            } else {
                db_part
            };
            // Decode URL-encoded database name (in case it was encoded)
            Ok(urlencoding::decode(db_name)
                .map(|s| s.to_string())
                .unwrap_or_else(|_| db_name.to_string()))
        } else {
            anyhow::bail!("Invalid connection string format: {}", conn_str)
        }
    }

    fn extract_host_port(&self, conn_str: &str) -> (String, u16) {
        // Extract host and port from postgres://user:password@host:port/database
        if let Some(at_pos) = conn_str.find('@') {
            let after_at = &conn_str[at_pos + 1..];
            if let Some(slash_pos) = after_at.find('/') {
                let host_port = &after_at[..slash_pos];
                if let Some(colon_pos) = host_port.find(':') {
                    let host = host_port[..colon_pos].to_string();
                    let port = host_port[colon_pos + 1..].parse().unwrap_or(5432);
                    (host, port)
                } else {
                    (host_port.to_string(), 5432)
                }
            } else {
                ("localhost".to_string(), 5432)
            }
        } else {
            ("localhost".to_string(), 5432)
        }
    }

    pub async fn setup_connection_pool(&self) -> Result<()> {
        let mut pool_guard = self.pool.lock().await;
        if pool_guard.is_some() {
            return Ok(());
        }

        let pool = self.create_connection_pool().await?;
        *pool_guard = Some(pool);
        Ok(())
    }

    async fn create_connection_pool(&self) -> Result<Pool<Postgres>> {
        let mut last_error = None;
        let conn_str = &self.config.connection_string;

        for attempt in 1..=self.config.timeouts.max_connection_attempts {
            match self.config.pool_options.clone().connect(conn_str).await {
                Ok(pool) => {
                    info!("Database connection pool created successfully");
                    return Ok(pool);
                }
                Err(e) if attempt < self.config.timeouts.max_connection_attempts => {
                    warn!("Connection attempt {} failed: {}", attempt, e);
                    last_error = Some(e);
                    sleep(self.config.timeouts.connection_retry_interval).await;
                }
                Err(e) => {
                    last_error = Some(e);
                    break;
                }
            }
        }

        let error_message = match last_error {
            Some(e) => e.to_string(),
            None => "no connection attempts were made; max_connection_attempts may be set to 0"
                .to_string(),
        };

        Err(DatabaseError::Connection(format!(
            "Failed to establish connection after {} attempts: {}",
            self.config.timeouts.max_connection_attempts, error_message
        ))
        .into())
    }

    pub async fn run_migrations(&self) -> Result<()> {
        let pool = self.get_pool().await?;
        info!("Running database migrations...");

        if !self.migrations_path.exists() {
            return Err(DatabaseError::Migration(format!(
                "Migrations directory not found: {:?}",
                self.migrations_path
            ))
            .into());
        }

        let migrator = sqlx::migrate::Migrator::new(&*self.migrations_path)
            .await
            .context("Failed to create migrator")?;

        migrator
            .run(&pool)
            .await
            .context("Failed to run migrations")?;

        info!("Migrations completed successfully");
        Ok(())
    }

    pub async fn run_seed_data(&self) -> Result<()> {
        let Some(seed_path) = &self.seed_data_path else {
            info!("Seed data directory not configured; skipping seed step");
            return Ok(());
        };

        if !seed_path.exists() {
            warn!("Seed data directory not found, skipping: {:?}", seed_path);
            return Ok(());
        }

        let pool = self.get_pool().await?;
        info!("Running seed data scripts from {:?}...", seed_path);

        let mut transaction = pool.begin().await?;

        let result = self.execute_seed_files(seed_path, &mut transaction).await;

        match result {
            Ok(_) => {
                transaction.commit().await?;
                info!("Seed data applied successfully");
                Ok(())
            }
            Err(e) => {
                transaction.rollback().await?;
                Err(e)
            }
        }
    }

    async fn execute_seed_files(
        &self,
        seed_dir: &Path,
        transaction: &mut Transaction<'_, Postgres>,
    ) -> Result<()> {
        let mut entries: Vec<_> = std::fs::read_dir(seed_dir)?
            .filter_map(Result::ok)
            .filter(|entry| {
                entry
                    .path()
                    .extension()
                    .and_then(|ext| Some(ext == "sql"))
                    .unwrap_or(false)
            })
            .collect();

        entries.sort_by_key(|entry| entry.path());

        for entry in entries {
            let path = entry.path();
            info!("Executing seed file: {:?}", path);

            let sql = std::fs::read_to_string(&path)
                .with_context(|| format!("Failed to read seed file: {:?}", path))?;

            transaction
                .execute(&*sql)
                .await
                .with_context(|| format!("Failed to execute seed file: {:?}", path))?;
        }

        Ok(())
    }

    pub async fn reset(&self, pg_home: &str) -> Result<()> {
        info!("Resetting database...");

        // Close existing pool if any
        self.shutdown().await?;

        // Extract connection details
        let conn_str = &self.config.connection_string;
        let parts: Vec<&str> = conn_str.split('/').collect();
        let db_name = parts.last().unwrap().to_string();

        // Extract host and port from connection string
        let host_port = parts[2].split('@').collect::<Vec<&str>>()[1];
        let host_port_parts: Vec<&str> = host_port.split(':').collect();
        let host = host_port_parts[0];
        let port = if host_port_parts.len() > 1 {
            host_port_parts[1]
        } else {
            "5432"
        };

        // Extract user from connection string
        let user_parts: Vec<&str> = parts[2].split('@').collect();
        let user_info: Vec<&str> = user_parts[0].split(':').collect();
        let username = user_info[0];

        // Get paths to PostgreSQL binaries
        let dropdb_path = get_postgres_bin_path(pg_home, "dropdb");
        let psql_path = get_postgres_bin_path(pg_home, "psql");
        let admin_user = self.config.admin_user.as_str();
        let admin_password = self.config.admin_password.as_str();
        let admin_db = self.config.admin_db.as_str();
        let admin_env = [("PGUSER", admin_user), ("PGPASSWORD", admin_password)];

        // Drop database if it exists
        info!("Dropping database {}...", db_name);
        let dropdb_args = [
            "-h",
            host,
            "-p",
            port,
            "-U",
            admin_user,
            "--if-exists",
            db_name.as_str(),
        ];
        let result = run_command_with_env(&dropdb_path, &dropdb_args, &admin_env);

        // Ignore error if database doesn't exist
        if let Err(e) = result {
            info!("Note: {}", e);
            info!("Database may not exist, continuing...");
        } else {
            info!("Database {} dropped successfully", db_name);
        }

        // Drop user if it exists
        if username == admin_user || username == "postgres" {
            info!(
                "Skipping drop for user {} (admin/system role cannot be dropped safely)",
                username
            );
            info!("Database reset completed successfully");
            return Ok(());
        }

        info!("Dropping user {}...", username);

        // Create SQL to drop user
        let drop_user_sql = format!(
            r#"
            DO $$
            BEGIN
                IF EXISTS (SELECT FROM pg_roles WHERE rolname = '{}') THEN
                    DROP OWNED BY "{}";
                    DROP USER "{}";
                END IF;
            END
            $$;
            "#,
            username, username, username
        );

        // Write SQL to a temporary file
        let temp_dir = std::env::temp_dir();
        let sql_file_path = temp_dir.join("drop_user.sql");
        std::fs::write(&sql_file_path, drop_user_sql)?;

        // Execute SQL file
        let sql_file_arg = sql_file_path.to_string_lossy().into_owned();
        let psql_args = [
            "-h",
            host,
            "-p",
            port,
            "-d",
            admin_db,
            "-U",
            admin_user,
            "-f",
            sql_file_arg.as_str(),
        ];
        let result = run_command_with_env(&psql_path, &psql_args, &admin_env);

        // Clean up temporary file
        std::fs::remove_file(sql_file_path)?;

        // Ignore error if user doesn't exist
        if let Err(e) = result {
            info!("Note: {}", e);
            info!("User may not exist, continuing...");
        } else {
            info!("User {} dropped successfully", username);
        }

        info!("Database reset completed successfully");
        Ok(())
    }

    pub async fn shutdown(&self) -> Result<()> {
        let mut pool_guard = self.pool.lock().await;
        if let Some(pool) = pool_guard.take() {
            info!("Closing database connection pool...");
            pool.close().await;
            info!("Database connection pool closed");
        }
        Ok(())
    }

    pub async fn check_health(&self) -> Result<bool> {
        if let Ok(pool) = self.get_pool().await {
            match sqlx::query("SELECT 1").execute(&pool).await {
                Ok(_) => Ok(true),
                Err(e) => {
                    warn!("Database health check failed: {}", e);
                    Ok(false)
                }
            }
        } else {
            Ok(false)
        }
    }

    pub async fn get_pool(&self) -> Result<Pool<Postgres>> {
        let pool_guard = self.pool.lock().await;
        pool_guard.as_ref().cloned().ok_or_else(|| {
            DatabaseError::Connection("Database connection pool not initialized".to_string()).into()
        })
    }
}
