use crate::config::Config;
use crate::services::{
    cleanup_ports, ensure_database_running, spawn_backend,
    spawn_frontend, stop_database, wait_for_backend_health,
};
use crate::utils::{print_step, project_root};
use crate::AppConfig;
use anyhow::Result;
use clap::Args;
use reqwest::Client;
use tokio::signal;

#[derive(Args)]
pub struct DevArgs {
    /// Start the database alongside backend and frontend servers
    #[arg(long, help = "Ensure DB is running for manual testing")]
    start_db: bool,
}

pub async fn run(args: DevArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;

    let app_config = if args.start_db {
        Some(AppConfig::from_file(&root.join("config/application.local.toml"))?)
    } else {
        None
    };

    if let Some(app_config) = &app_config {
        if let Err(e) = stop_database(app_config).await {
            eprintln!("Warning: failed to stop existing database: {e}");
        }
    }

    // Clean up any existing processes on configured ports
    cleanup_ports(&config)?;

    if let Some(app_config) = &app_config {
        // NOTE: DB defaults to Docker Compose (Postgres 15) if available (see `db start --mode auto`).
        ensure_database_running(app_config).await?;
    }

    print_step("Starting development environment...");

    // Start Backend
    print_step("Starting Backend (Gradle)...");
    let mut backend = spawn_backend(&config, &root, Some("dev"))?;

    // Always wait for backend health check
    let client = Client::new();
    print_step("Waiting for Backend to be ready...");
    let health_url = format!("http://{}:{}/health", config.api_host, config.api_port);
    wait_for_backend_health(&client, &health_url, &mut backend, 180).await?;

    // Start Frontend
    print_step("Starting Frontend (Bun)...");
    let mut frontend = spawn_frontend(&config, &root, false)?;

    println!("\nManual verification ready:");
    println!("- Admin Web: http://localhost:{}", config.frontend_port);
    println!("- API: http://{}:{}", config.api_host, config.api_port);
    print_step("Servers started. Press Ctrl+C to stop.");

    if args.start_db {
        println!("Database is running. Press Ctrl+C to stop all services.");
    }

    // Wait for Ctrl+C
    signal::ctrl_c().await?;
    println!("\nShutting down...");

    // Kill processes
    let _ = backend.kill();
    let _ = frontend.kill();

    // If we started the database for this session, attempt to stop it as well
    if let Some(app_config) = &app_config {
        if let Err(e) = stop_database(app_config).await {
            eprintln!("Failed to stop database: {e}");
        }
    }

    Ok(())
}
