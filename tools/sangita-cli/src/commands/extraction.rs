use crate::utils::{docker_compose_available, print_step, print_success, project_root, run_docker_compose};
use anyhow::{Context, Result};
use clap::{Args, Subcommand};
use std::path::Path;
use std::process::Command;

#[derive(Args)]
pub struct ExtractionArgs {
    #[command(subcommand)]
    command: ExtractionCommands,
}

#[derive(Subcommand)]
enum ExtractionCommands {
    /// Build the PDF extractor Docker image
    Build {
        /// Don't use cache when building
        #[arg(long)]
        no_cache: bool,
    },
    /// Start the extraction service (requires postgres to be running)
    Start {
        /// Also start postgres if not running
        #[arg(long)]
        with_db: bool,
        /// Follow logs after starting
        #[arg(short, long)]
        follow: bool,
    },
    /// Stop the extraction service
    Stop,
    /// View extraction service logs
    Logs {
        /// Follow log output
        #[arg(short, long)]
        follow: bool,
        /// Number of lines to show
        #[arg(short = 'n', long, default_value = "100")]
        tail: String,
    },
    /// Check extraction queue status
    Status,
    /// Restart the extraction service
    Restart,
}

pub async fn run(args: ExtractionArgs) -> Result<()> {
    let root = project_root()?;

    match args.command {
        ExtractionCommands::Build { no_cache } => {
            build_extractor(&root, no_cache)?;
        }
        ExtractionCommands::Start { with_db, follow } => {
            start_extractor(&root, with_db, follow).await?;
        }
        ExtractionCommands::Stop => {
            stop_extractor(&root)?;
        }
        ExtractionCommands::Logs { follow, tail } => {
            show_logs(&root, follow, &tail)?;
        }
        ExtractionCommands::Status => {
            show_status(&root).await?;
        }
        ExtractionCommands::Restart => {
            stop_extractor(&root)?;
            start_extractor(&root, false, false).await?;
        }
    }

    Ok(())
}

fn build_extractor(root: &Path, no_cache: bool) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!(
            "Docker Compose not available. Install Docker Desktop (macOS/Windows) or Docker Engine + Compose (Linux)."
        );
    }

    print_step("Building PDF extractor Docker image...");

    let mut args = vec!["build"];
    if no_cache {
        args.push("--no-cache");
    }
    args.push("pdf-extractor");

    run_docker_compose(root, &args).context("Failed to build pdf-extractor image")?;

    print_success("PDF extractor image built successfully");
    Ok(())
}

async fn start_extractor(root: &Path, with_db: bool, follow: bool) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!(
            "Docker Compose not available. Install Docker Desktop (macOS/Windows) or Docker Engine + Compose (Linux)."
        );
    }

    // Check if postgres is running
    let postgres_running = check_postgres_running(root);

    if !postgres_running && !with_db {
        anyhow::bail!(
            "PostgreSQL is not running. Either:\n  \
             1. Start it first: sangita-cli db start\n  \
             2. Use --with-db flag: sangita-cli extraction start --with-db"
        );
    }

    if with_db && !postgres_running {
        print_step("Starting PostgreSQL...");
        run_docker_compose(root, &["up", "-d", "postgres"])
            .context("Failed to start postgres")?;

        // Wait for postgres to be healthy
        print_step("Waiting for PostgreSQL to be ready...");
        wait_for_postgres_healthy(root).await?;
    }

    print_step("Starting PDF extractor service...");

    // Use --profile extraction to include the pdf-extractor service
    run_docker_compose(root, &["--profile", "extraction", "up", "-d", "pdf-extractor"])
        .context("Failed to start pdf-extractor")?;

    print_success("PDF extractor service started");
    println!("\n  Container: sangita_pdf_extractor");
    println!("  Logs: sangita-cli extraction logs -f");
    println!("  Status: sangita-cli extraction status");

    if follow {
        println!("\nFollowing logs (Ctrl+C to stop)...\n");
        show_logs(root, true, "50")?;
    }

    Ok(())
}

fn stop_extractor(root: &Path) -> Result<()> {
    if !docker_compose_available() {
        anyhow::bail!("Docker Compose not available.");
    }

    print_step("Stopping PDF extractor service...");

    // Stop only the pdf-extractor service
    run_docker_compose(root, &["--profile", "extraction", "stop", "pdf-extractor"])
        .context("Failed to stop pdf-extractor")?;

    print_success("PDF extractor service stopped");
    Ok(())
}

fn show_logs(root: &Path, follow: bool, tail: &str) -> Result<()> {
    let mut args = vec!["--profile", "extraction", "logs"];

    if follow {
        args.push("-f");
    }

    args.push("--tail");
    args.push(tail);
    args.push("pdf-extractor");

    // For logs, we want to stream output directly to terminal
    let status = Command::new("docker")
        .args(["compose"])
        .args(&args)
        .current_dir(root)
        .status()
        .context("Failed to show logs")?;

    if !status.success() {
        // Try legacy docker-compose
        Command::new("docker-compose")
            .args(&args)
            .current_dir(root)
            .status()
            .context("Failed to show logs")?;
    }

    Ok(())
}

async fn show_status(root: &Path) -> Result<()> {
    print_step("Extraction service status:");

    // Check container status
    let output = Command::new("docker")
        .args(["compose", "--profile", "extraction", "ps", "--format", "table", "pdf-extractor"])
        .current_dir(root)
        .output();

    match output {
        Ok(out) if out.status.success() => {
            let stdout = String::from_utf8_lossy(&out.stdout);
            if stdout.trim().is_empty() || stdout.contains("no such service") {
                println!("\n  Container: Not running");
                println!("  Start with: sangita-cli extraction start --with-db");
            } else {
                println!("\n{}", stdout);
            }
        }
        _ => {
            println!("\n  Container: Not running or Docker not available");
        }
    }

    // Try to get queue stats from database
    println!("\nExtraction queue status:");
    if let Err(e) = show_queue_stats().await {
        println!("  Unable to fetch queue stats: {}", e);
        println!("  (Database may not be running)");
    }

    Ok(())
}

async fn show_queue_stats() -> Result<()> {
    use sqlx::postgres::PgConnectOptions;
    use sqlx::ConnectOptions;
    use sqlx::Row;
    use std::str::FromStr;

    let conn_str = "postgres://postgres:postgres@localhost:5432/sangita_grantha";
    let options = PgConnectOptions::from_str(conn_str)?
        .log_statements(log::LevelFilter::Off);

    let pool = sqlx::PgPool::connect_with(options).await?;

    let rows = sqlx::query(
        "SELECT status::text, COUNT(*) as count FROM extraction_queue GROUP BY status ORDER BY status"
    )
    .fetch_all(&pool)
    .await?;

    if rows.is_empty() {
        println!("  Queue is empty (no extraction tasks)");
    } else {
        for row in rows {
            let status: String = row.get("status");
            let count: i64 = row.get("count");
            println!("  {}: {}", status, count);
        }
    }

    pool.close().await;
    Ok(())
}

fn check_postgres_running(root: &Path) -> bool {
    let output = Command::new("docker")
        .args(["compose", "ps", "--format", "json", "postgres"])
        .current_dir(root)
        .output();

    match output {
        Ok(out) if out.status.success() => {
            let stdout = String::from_utf8_lossy(&out.stdout);
            stdout.contains("running") || stdout.contains("healthy")
        }
        _ => false,
    }
}

async fn wait_for_postgres_healthy(root: &Path) -> Result<()> {
    use tokio::time::{sleep, Duration};

    let max_attempts = 30;
    let interval = Duration::from_secs(2);

    for attempt in 1..=max_attempts {
        let output = Command::new("docker")
            .args(["compose", "ps", "--format", "json", "postgres"])
            .current_dir(root)
            .output();

        if let Ok(out) = output {
            let stdout = String::from_utf8_lossy(&out.stdout);
            if stdout.contains("healthy") {
                return Ok(());
            }
        }

        if attempt % 5 == 0 {
            println!("  Still waiting... (attempt {}/{})", attempt, max_attempts);
        }
        sleep(interval).await;
    }

    anyhow::bail!("PostgreSQL did not become healthy after {} attempts", max_attempts);
}
