use crate::utils::{docker_compose_available, print_step, print_success, project_root, run_docker_compose};
use crate::{AppConfig, ConnectionString, DatabaseConfig, DatabaseManager, PostgresInstance};
use anyhow::{Context, Result};
use clap::{Args, Subcommand, ValueEnum};
use std::path::Path;
use tokio::time::{sleep, Duration};

#[derive(Debug, Copy, Clone, ValueEnum)]
enum DbMode {
    /// Choose Docker Compose if available, otherwise fall back to local Postgres binaries.
    Auto,
    /// Use Docker Compose (compose.yaml at repo root).
    Docker,
    /// Use local Postgres binaries via pg_ctl/initdb using pg_home/pg_data.
    Local,
}

#[derive(Args)]
pub struct DbArgs {
    #[command(subcommand)]
    command: DbCommands,

    /// Path to config file (defaults to config/application.local.toml)
    #[arg(long)]
    config: Option<std::path::PathBuf>,
}

#[derive(Subcommand)]
enum DbCommands {
    /// Initialize database (create, migrate, seed)
    Init {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Reset the database (drop, recreate, migrate, seed)
    Reset {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Run migrations
    Migrate {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Seed the database
    Seed {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Check database health
    Health {
        #[arg(short, long)]
        verbose: bool,

        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Start PostgreSQL instance
    Start {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Stop PostgreSQL instance
    Stop {
        #[arg(long, value_enum, default_value_t = DbMode::Auto)]
        mode: DbMode,
    },
    /// Shutdown database connections
    Shutdown,
}

pub async fn run(args: DbArgs) -> Result<()> {
    let root = project_root()?;

    // Load configuration (TOML remains supported as a fallback, but .env is canonical)
    let config_path = args
        .config
        .unwrap_or_else(|| root.join("config/application.local.toml"));

    // Load `.env` first (best-effort) so env-based overrides work consistently.
    load_repo_dotenv(&root);

    // AppConfig comes from TOML, but we override DB fields from env if present.
    let mut app_config = AppConfig::from_file(&config_path).context("Failed to load application config")?;
    apply_env_overrides(&mut app_config);

    let migrations_path = root.join("database/migrations");
    let seed_data_path = root.join("database/seed_data");

    // Create connection string and database config
    let connection_string = ConnectionString::new(&app_config);
    let db_config = DatabaseConfig::new(connection_string.to_string())
        .with_admin_db(app_config.database.admin_db.clone())
        .with_admin_user(app_config.database.admin_user.clone())
        .with_admin_password(app_config.database.admin_password.clone());

    // Create database manager
    let manager = DatabaseManager::new(db_config, migrations_path.clone(), Some(seed_data_path.clone()));

    match args.command {
        DbCommands::Init { mode } => {
            print_step("Initializing database...");
            ensure_db_running(&root, &app_config, mode).await?;

            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            manager.run_seed_data().await?;

            print_success("Database initialized");
        }

        DbCommands::Reset { mode } => {
            print_step("Resetting database...");

            match resolve_mode(mode) {
                DbMode::Docker => {
                    reset_docker_postgres(&root).await?;
                }
                DbMode::Local => {
                    manager.reset(&app_config.paths.pg_home).await?;
                }
                DbMode::Auto => unreachable!("resolve_mode never returns Auto"),
            }

            // After reset, reinitialize
            ensure_db_running(&root, &app_config, mode).await?;
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            manager.run_seed_data().await?;

            print_success("Database reset complete");
        }

        DbCommands::Migrate { mode } => {
            print_step("Running migrations...");
            ensure_db_running(&root, &app_config, mode).await?;
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            print_success("Migrations complete");
        }

        DbCommands::Seed { mode } => {
            print_step("Seeding database...");
            ensure_db_running(&root, &app_config, mode).await?;
            manager.setup_connection_pool().await?;
            manager.run_seed_data().await?;
            print_success("Seeding complete");
        }

        DbCommands::Health { verbose, mode } => {
            print_step("Checking database health...");
            ensure_db_running(&root, &app_config, mode).await?;
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            let healthy = manager.check_health().await?;

            if healthy {
                print_success("Database is healthy");
                if verbose {
                    println!("  Connection: OK");
                    println!("  Pool: Active");
                }
            } else {
                anyhow::bail!("Database health check failed");
            }
        }

        DbCommands::Start { mode } => {
            print_step("Starting PostgreSQL...");
            ensure_db_running(&root, &app_config, mode).await?;
            print_success("PostgreSQL started");
        }

        DbCommands::Stop { mode } => {
            print_step("Stopping PostgreSQL...");
            stop_db(&root, &app_config, mode).await?;
            print_success("PostgreSQL stopped");
        }

        DbCommands::Shutdown => {
            print_step("Shutting down database connections...");
            manager.shutdown().await?;
            print_success("Database connections closed");
        }
    }

    Ok(())
}

fn resolve_mode(mode: DbMode) -> DbMode {
    match mode {
        DbMode::Auto => {
            if docker_compose_available() {
                DbMode::Docker
            } else {
                DbMode::Local
            }
        }
        DbMode::Docker => DbMode::Docker,
        DbMode::Local => DbMode::Local,
    }
}

async fn ensure_db_running(root: &Path, config: &AppConfig, mode: DbMode) -> Result<()> {
    match resolve_mode(mode) {
        DbMode::Docker => start_docker_postgres(root).await,
        DbMode::Local => {
            let instance = PostgresInstance::new(config);
            let is_localhost = matches!(
                config.database.host.as_str(),
                "localhost" | "127.0.0.1" | "0.0.0.0" | "::1"
            );

            // Keep existing behavior for local mode only.
            if is_localhost && !instance.is_initialized() {
                print_step("Initializing PostgreSQL data directory (local mode)...");
                instance.init_db()?;
            }
            if is_localhost {
                instance.ensure_running().await?;
            }
            Ok(())
        }
        DbMode::Auto => unreachable!(),
    }
}

async fn stop_db(root: &Path, config: &AppConfig, mode: DbMode) -> Result<()> {
    match resolve_mode(mode) {
        DbMode::Docker => stop_docker_postgres(root).await,
        DbMode::Local => {
            let instance = PostgresInstance::new(config);
            instance.stop().await
        }
        DbMode::Auto => unreachable!(),
    }
}

async fn start_docker_postgres(root: &Path) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!(
            "Docker Compose not available. Install Docker Desktop (macOS/Windows) or Docker Engine + Compose (Linux), or run with --mode local."
        );
    }

    // Start only the postgres service (dev default).
    run_docker_compose(root, &["up", "-d", "postgres"]).context("Failed to start postgres via Docker Compose")?;
    
    // Wait for PostgreSQL to be ready
    print_step("Waiting for PostgreSQL to be ready...");
    wait_for_docker_postgres_ready(root).await?;
    print_success("PostgreSQL is ready");
    
    Ok(())
}

async fn wait_for_docker_postgres_ready(root: &Path) -> Result<()> {
    use std::process::Command;
    
    let max_attempts = 30;
    let interval = Duration::from_secs(2);
    
    for attempt in 1..=max_attempts {
        // Check if container is healthy using docker compose ps
        let output = Command::new("docker")
            .args(["compose", "ps", "--format", "json", "postgres"])
            .current_dir(root)
            .output();
        
        match output {
            Ok(out) if out.status.success() => {
                let stdout = String::from_utf8_lossy(&out.stdout);
                // Check if health status indicates ready
                if stdout.contains("\"Health\":\"healthy\"") || stdout.contains("\"State\":\"running\"") {
                    // Also try to connect to verify
                    if check_postgres_connection().await.is_ok() {
                        return Ok(());
                    }
                }
            }
            _ => {}
        }
        
        // Fallback: try direct connection check
        if check_postgres_connection().await.is_ok() {
            return Ok(());
        }
        
        if attempt < max_attempts {
            if attempt % 5 == 0 {
                println!("  Still waiting... (attempt {}/{})", attempt, max_attempts);
            }
            sleep(interval).await;
        }
    }
    
    anyhow::bail!("PostgreSQL did not become ready after {} attempts", max_attempts);
}

async fn check_postgres_connection() -> Result<()> {
    use sqlx::postgres::PgConnectOptions;
    use sqlx::ConnectOptions;
    use std::str::FromStr;
    
    // Try to connect to postgres database with default Docker credentials
    // This is a lightweight check to see if the server is accepting connections
    // Using default credentials from compose.yaml: postgres/postgres
    // Note: This uses hardcoded defaults for the health check; actual operations use config values
    let conn_str = "postgres://postgres:postgres@localhost:5432/postgres";
    let mut options = PgConnectOptions::from_str(conn_str)?;
    
    // Set a short timeout for the health check
    options = options.log_statements(log::LevelFilter::Off);
    
    match sqlx::PgPool::connect_with(options).await {
        Ok(pool) => {
            // Try a simple query to ensure the connection is fully ready
            match sqlx::query("SELECT 1").execute(&pool).await {
                Ok(_) => {
                    pool.close().await;
                    Ok(())
                }
                Err(e) => {
                    pool.close().await;
                    Err(anyhow::anyhow!("Query failed: {}", e))
                }
            }
        }
        Err(e) => Err(anyhow::anyhow!("Connection failed: {}. Make sure PostgreSQL is running and accessible at localhost:5432", e)),
    }
}

async fn stop_docker_postgres(root: &Path) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!("Docker Compose not available; cannot stop docker postgres.");
    }

    // Stop only the postgres service.
    run_docker_compose(root, &["stop", "postgres"]).context("Failed to stop postgres via Docker Compose")?;
    Ok(())
}

async fn reset_docker_postgres(root: &Path) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!("Docker Compose not available; cannot reset docker postgres.");
    }

    // This wipes the named volume and recreates it.
    // NOTE: Since compose.yaml currently defines only postgres, `down -v` is safe.
    run_docker_compose(root, &["down", "-v"]).context("Failed to docker compose down -v")?;
    run_docker_compose(root, &["up", "-d", "postgres"]).context("Failed to docker compose up -d postgres")?;
    Ok(())
}

fn load_repo_dotenv(root: &Path) {
    // Resolve environment to match backend logic: ENVIRONMENT/SG_ENV -> DEV/TEST/PROD.
    let env = std::env::var("ENVIRONMENT")
        .or_else(|_| std::env::var("SG_ENV"))
        .unwrap_or_else(|_| "DEV".to_string())
        .to_uppercase();

    let filename = match env.as_str() {
        "TEST" | "TESTING" => ".env.test",
        "PROD" | "PRODUCTION" => ".env.production",
        _ => "development.env",
    };

    let path = root.join("config").join(filename);

    // Best-effort: missing file is fine.
    let _ = dotenvy::from_path(&path);
}

fn apply_env_overrides(config: &mut AppConfig) {
    // Canonical `.env` keys (aligns with backend ApiEnvironmentLoader)
    if let Ok(host) = std::env::var("DB_HOST") {
        if !host.trim().is_empty() {
            config.database.host = host;
        }
    }
    if let Ok(port) = std::env::var("DB_PORT") {
        if let Ok(p) = port.trim().parse::<u16>() {
            config.database.port = p;
        }
    }
    if let Ok(name) = std::env::var("DB_NAME") {
        if !name.trim().is_empty() {
            config.database.name = name;
        }
    }
    if let Ok(user) = std::env::var("DB_USER") {
        if !user.trim().is_empty() {
            config.database.user = user;
            // If admin user wasn't explicitly set, keep it aligned.
            if config.database.admin_user.trim().is_empty() {
                config.database.admin_user = config.database.user.clone();
            }
        }
    }
    if let Ok(password) = std::env::var("DB_PASSWORD") {
        // Allow empty password if someone wants it, but trim whitespace.
        config.database.password = password.trim().to_string();
        if config.database.admin_password.trim().is_empty() {
            config.database.admin_password = config.database.password.clone();
        }
    }
}
