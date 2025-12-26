use crate::utils::{print_step, print_success, project_root};
use crate::{AppConfig, ConnectionString, DatabaseConfig, DatabaseManager, PostgresInstance};
use anyhow::{Context, Result};
use clap::{Args, Subcommand};

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
    Init,
    /// Reset the database (drop, recreate, migrate, seed)
    Reset,
    /// Run migrations
    Migrate,
    /// Seed the database
    Seed,
    /// Check database health
    Health {
        #[arg(short, long)]
        verbose: bool,
    },
    /// Start PostgreSQL instance
    Start,
    /// Stop PostgreSQL instance
    Stop,
    /// Shutdown database connections
    Shutdown,
}

pub async fn run(args: DbArgs) -> Result<()> {
    let root = project_root()?;
    
    // Load configuration
    let config_path = args.config
        .unwrap_or_else(|| root.join("config/application.local.toml"));
    
    let app_config = AppConfig::from_file(&config_path)
        .context("Failed to load application config")?;
    
    let migrations_path = root.join("database/migrations");
    let seed_data_path = root.join("database/seed_data");
    
    // Create connection string and database config
    let connection_string = ConnectionString::new(&app_config);
    let db_config = DatabaseConfig::new(connection_string.to_string())
        .with_admin_db(app_config.database.admin_db.clone())
        .with_admin_user(app_config.database.admin_user.clone())
        .with_admin_password(app_config.database.admin_password.clone());
    
    // Create database manager
    let manager = DatabaseManager::new(
        db_config,
        migrations_path.clone(),
        Some(seed_data_path.clone()),
    );
    
    match args.command {
        DbCommands::Init => {
            print_step("Initializing database...");
            let instance = PostgresInstance::new(&app_config);
            let is_local = matches!(
                app_config.database.host.as_str(),
                "localhost" | "127.0.0.1" | "0.0.0.0" | "::1"
            );
            if is_local && !instance.is_initialized() {
                print_step("Initializing PostgreSQL data directory...");
                instance.init_db()?;
            }
            if is_local {
                instance.ensure_running().await?;
            }
            // This is complex - for now call the init logic
            // We need to implement the init command properly
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            manager.run_seed_data().await?;
            print_success("Database initialized");
        }
        
        DbCommands::Reset => {
            print_step("Resetting database...");
            manager.reset(&app_config.paths.pg_home).await?;
            // After reset, reinitialize
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            manager.run_seed_data().await?;
            print_success("Database reset complete");
        }
        
        DbCommands::Migrate => {
            print_step("Running migrations...");
            manager.ensure_database_exists().await?;
            manager.setup_connection_pool().await?;
            manager.run_migrations().await?;
            print_success("Migrations complete");
        }
        
        DbCommands::Seed => {
            print_step("Seeding database...");
            manager.setup_connection_pool().await?;
            manager.run_seed_data().await?;
            print_success("Seeding complete");
        }
        
        DbCommands::Health { verbose } => {
            print_step("Checking database health...");
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
        
        DbCommands::Start => {
            print_step("Starting PostgreSQL instance...");
            let instance = PostgresInstance::new(&app_config);
            instance.ensure_running().await?;
            print_success("PostgreSQL started");
        }
        
        DbCommands::Stop => {
            print_step("Stopping PostgreSQL instance...");
            let instance = PostgresInstance::new(&app_config);
            instance.stop().await?;
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
