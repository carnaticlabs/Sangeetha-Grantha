use crate::config::Config;
use crate::services::{
    cleanup_ports, ensure_database_running, spawn_backend, spawn_frontend,
    start_extraction_service, stop_database, stop_extraction_service, wait_for_backend_health,
};
use crate::utils::{print_step, print_success, print_warning, project_root};
use crate::AppConfig;
use anyhow::Result;
use clap::Args;
use reqwest::Client;
use tokio::signal;

#[cfg(unix)]
use tokio::signal::unix::{signal as unix_signal, SignalKind};

#[derive(Args)]
pub struct DevArgs {
    /// Start the database alongside backend and frontend servers
    #[arg(long, help = "Ensure DB is running for manual testing")]
    start_db: bool,

    /// Skip starting the extraction service (Docker container)
    #[arg(long, help = "Run without the PDF extraction worker")]
    no_extraction: bool,
}

pub async fn run(args: DevArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;

    let app_config = if args.start_db {
        Some(AppConfig::from_file(
            &root.join("config/application.local.toml"),
        )?)
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
        ensure_database_running(app_config).await?;
    }

    print_step("Starting development environment...");

    // ── Backend ──────────────────────────────────────────────────────────────
    print_step("Starting Backend (Gradle)...");
    let mut backend = spawn_backend(&config, &root, Some("dev"))?;

    let client = Client::new();
    print_step("Waiting for Backend to be ready...");
    let health_url = format!("http://{}:{}/health", config.api_host, config.api_port);
    wait_for_backend_health(&client, &health_url, &mut backend, 180).await?;

    // ── Frontend ─────────────────────────────────────────────────────────────
    print_step("Starting Frontend (Bun)...");
    let mut frontend = spawn_frontend(&config, &root, false)?;

    // ── Extraction service (Docker container) ────────────────────────────────
    let extraction_started = if !args.no_extraction {
        print_step("Starting extraction service (Docker)...");
        match start_extraction_service(&root) {
            Ok(()) => {
                print_success("Extraction service started (container: sangita_pdf_extractor)");
                true
            }
            Err(e) => {
                print_warning(&format!("Extraction service failed to start: {e}"));
                print_warning(
                    "Continuing without extraction. Start manually: sangita-cli extraction start",
                );
                false
            }
        }
    } else {
        false
    };

    // ── Ready banner ─────────────────────────────────────────────────────────
    println!();
    println!("╔══════════════════════════════════════════════════════════════╗");
    println!("║  Sangita Grantha — All services running                    ║");
    println!("╠══════════════════════════════════════════════════════════════╣");
    println!(
        "║  Admin Web:   http://localhost:{}                        ║",
        config.frontend_port
    );
    println!(
        "║  Backend API: http://{}:{}                          ║",
        config.api_host, config.api_port
    );
    if args.start_db {
        println!("║  Database:    localhost:5432                                ║");
    }
    if extraction_started {
        println!("║  Extraction:  sangita_pdf_extractor (Docker)               ║");
    }
    println!("╠══════════════════════════════════════════════════════════════╣");
    println!("║  Extraction logs: docker logs -f sangita_pdf_extractor     ║");
    println!("║  Press Ctrl+C to stop all services                         ║");
    println!("╚══════════════════════════════════════════════════════════════╝");
    println!();

    // ── Wait for SIGINT (Ctrl+C) or SIGTERM ──────────────────────────────────
    wait_for_shutdown_signal().await;
    println!("\nShutting down...");

    // ── Graceful shutdown (reverse order) ────────────────────────────────────

    // 1. Stop extraction container (by name — always works)
    if extraction_started {
        print_step("Stopping extraction service...");
        if let Err(e) = stop_extraction_service(&root) {
            print_warning(&format!("Failed to stop extraction service: {e}"));
        }
    }

    // 2. Kill frontend child process
    print_step("Stopping frontend...");
    let _ = frontend.kill();
    let _ = frontend.wait(); // reap zombie

    // 3. Kill backend child process (Gradle → Java)
    print_step("Stopping backend...");
    let _ = backend.kill();
    let _ = backend.wait(); // reap zombie

    // 4. Mop up any orphaned processes on our ports (handles Gradle→Java indirection)
    cleanup_ports(&config)?;

    // 5. Stop database if we started it
    if let Some(app_config) = &app_config {
        print_step("Stopping database...");
        if let Err(e) = stop_database(app_config).await {
            print_warning(&format!("Failed to stop database: {e}"));
        }
    }

    print_success("All services stopped.");
    Ok(())
}

/// Block until either SIGINT (Ctrl+C) or SIGTERM is received.
///
/// On Unix, both signals are handled.  On other platforms, only Ctrl+C is
/// supported (SIGTERM is a Unix concept).
async fn wait_for_shutdown_signal() {
    #[cfg(unix)]
    {
        let mut sigterm =
            unix_signal(SignalKind::terminate()).expect("Failed to register SIGTERM handler");

        tokio::select! {
            _ = signal::ctrl_c() => {
                println!("\nReceived SIGINT (Ctrl+C)");
            }
            _ = sigterm.recv() => {
                println!("\nReceived SIGTERM");
            }
        }
    }

    #[cfg(not(unix))]
    {
        signal::ctrl_c()
            .await
            .expect("Failed to register Ctrl+C handler");
        println!("\nReceived Ctrl+C");
    }
}
