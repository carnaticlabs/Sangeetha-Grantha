use crate::config::Config;
use crate::utils::{check_command_exists, is_windows, print_step, print_success};
use crate::{AppConfig, PostgresInstance};
use anyhow::{Context, Result};
use console;
use reqwest::Client;
use std::path::Path;
use std::process::{Child, Command, Stdio};
use std::time::Duration;
use tokio::time::sleep;

/// Ensure database is running using integrated database module
pub async fn ensure_database_running(config: &AppConfig) -> Result<()> {
    print_step("Ensuring database is running...");
    
    let instance = PostgresInstance::new(config);
    instance.ensure_running().await?;
    
    print_success("Database is running");
    Ok(())
}

/// Stop the database using integrated database module
pub async fn stop_database(config: &AppConfig) -> Result<()> {
    print_step("Stopping database...");
    
    let instance = PostgresInstance::new(config);
    
    match instance.stop().await {
        Ok(_) => {
            print_success("Database stopped");
            Ok(())
        }
        Err(err) => {
            eprintln!(
                "{}",
                console::style("Warning: failed to stop database cleanly")
                    .yellow()
            );
            Err(err).context("Database stop command failed")
        }
    }
}

pub fn spawn_backend(config: &Config, root: &Path, environment: Option<&str>) -> Result<Child> {
    println!("Starting backend with Gradle (this may take 2-3 minutes on first run)...");
    println!("Backend logs will appear below:");
    if let Some(env_name) = environment {
        println!("Using ENVIRONMENT={} for backend", env_name);
    }
    println!("{}", console::style("─".repeat(80)).dim());

    let gradlew_name = if is_windows() { "gradlew.bat" } else { "gradlew" };
    let gradlew_path = root.join(gradlew_name);
    let mut command = if gradlew_path.exists() {
        Command::new(gradlew_path)
    } else if check_command_exists("gradle") {
        eprintln!(
            "Gradle wrapper not found at {}, falling back to system 'gradle'",
            gradlew_path.display()
        );
        Command::new("gradle")
    } else {
        anyhow::bail!(
            "Gradle wrapper not found at {} and no 'gradle' on PATH. Restore ./gradlew or install Gradle.",
            gradlew_path.display()
        );
    };
    command
        .arg(":modules:backend:api:run")
        .current_dir(root)
        .env("API_HOST", &config.api_host)
        .env("API_PORT", &config.api_port);

    if let Some(env_name) = environment {
        command.env("ENVIRONMENT", env_name);
    }

    let backend = command
        .stdout(Stdio::inherit())
        .stderr(Stdio::inherit())
        .spawn()
        .context("Failed to start backend")?;

    Ok(backend)
}

pub async fn wait_for_backend_health(
    client: &Client,
    health_url: &str,
    backend: &mut Child,
    max_attempts: u32,
) -> Result<()> {
    println!("\nWaiting for backend health check at {}...", health_url);
    println!("Timeout: {} seconds", max_attempts);
    for attempt in 0..max_attempts {
        if let Some(status) = backend.try_wait()? {
            eprintln!(
                "\n{}",
                console::style("Backend process exited early!").red().bold()
            );
            eprintln!("Exit status: {}", status);
            eprintln!("\nTroubleshooting tips:");
            eprintln!("1. Check the Gradle logs above for compilation errors");
            eprintln!(
                "2. Verify database is running: cargo run -- db health (from tools/sangita-cli)"
            );
            eprintln!("3. Check if port 8080 is already in use: lsof -i :8080");
            eprintln!("4. Try running backend manually: ./gradlew :modules:backend:api:run (or gradle if wrapper is missing)");
            anyhow::bail!("Backend process exited early with status: {}", status);
        }

        match client.get(health_url).send().await {
            Ok(resp) if resp.status().is_success() => {
                println!("\n{}", console::style("─".repeat(80)).dim());
                print_success("Backend is ready!");
                return Ok(());
            }
            Ok(resp) => {
                if attempt % 10 == 0 {
                    println!(
                        "Attempt {}/{}: Health check returned {}",
                        attempt + 1,
                        max_attempts,
                        resp.status()
                    );
                }
            }
            Err(e) => {
                if attempt % 10 == 0 {
                    println!("Attempt {}/{}: {}", attempt + 1, max_attempts, e);
                }
            }
        }

        if attempt + 1 == max_attempts {
            let _ = backend.kill();
            eprintln!(
                "\n{}",
                console::style("Backend failed to become healthy!")
                    .red()
                    .bold()
            );
            eprintln!(
                "Waited {} seconds but backend did not respond to health checks.",
                max_attempts
            );
            eprintln!("\nTroubleshooting tips:");
            eprintln!("1. Review the Gradle/backend logs above");
            eprintln!("2. Check for compilation errors or missing dependencies");
            eprintln!("3. Verify database connection settings");
            eprintln!("4. Try increasing the timeout or starting backend manually");
            anyhow::bail!(
                "Backend failed health checks after {} attempts",
                max_attempts
            );
        }

        sleep(Duration::from_secs(1)).await;
    }

    anyhow::bail!("Backend health check loop exited unexpectedly")
}

pub fn ensure_process_alive(process: &mut Child, name: &str) -> Result<()> {
    if let Some(status) = process.try_wait()? {
        anyhow::bail!("{name} exited unexpectedly with status: {status}");
    }

    Ok(())
}

pub fn spawn_frontend(config: &Config, root: &Path, silent: bool) -> Result<Child> {
    let frontend_dir = root.join("modules/frontend/sangita-admin-web");
    let mut command = Command::new("bun");
    command
        .arg("run")
        .arg("dev")
        .current_dir(&frontend_dir)
        .env("PORT", &config.frontend_port);

    if silent {
        command.stdout(Stdio::null()).stderr(Stdio::null());
    }

    command.spawn().context("Failed to start frontend")
}

/// Kill any processes using the specified port.
/// Uses `lsof` to find processes and `kill` to terminate them.
pub fn kill_processes_on_port(port: &str, service_name: &str) -> Result<()> {
    // Use lsof to find PIDs using the port
    let output = Command::new("lsof")
        .arg("-ti")
        .arg(format!(":{}", port))
        .output();

    match output {
        Ok(output) if output.status.success() => {
            let pids_str = String::from_utf8_lossy(&output.stdout);
            let pids: Vec<&str> = pids_str.trim().split('\n').filter(|s| !s.is_empty()).collect();

            if pids.is_empty() {
                return Ok(());
            }

            print_step(&format!("Killing existing {} processes on port {}...", service_name, port));

            for pid in &pids {
                // Try graceful shutdown first (SIGTERM)
                let _ = Command::new("kill").arg("-15").arg(pid).output();
            }

            // Give processes a moment to cleanup
            std::thread::sleep(Duration::from_millis(1000));

            // Check if any are still running and force kill (SIGKILL)
            for pid in &pids {
                 // Check if process still exists by sending signal 0
                 if Command::new("kill").arg("-0").arg(pid).output().map(|o| o.status.success()).unwrap_or(false) {
                     eprintln!("{}", console::style(&format!("Process {} still running, force killing...", pid)).yellow());
                     if let Err(e) = Command::new("kill").arg("-9").arg(pid).output() {
                        eprintln!(
                            "{}",
                            console::style(&format!("Warning: failed to kill process {}: {}", pid, e))
                                .yellow()
                        );
                    } else {
                        println!("  Force killed process {} on port {}", pid, port);
                    }
                 }
            }

            // Give processes a moment to fully terminate
            std::thread::sleep(Duration::from_millis(500));
            print_success(&format!("Cleared port {} for {}", port, service_name));
        }
        Ok(_) => {
            // lsof returned non-zero (no processes found) - this is fine
        }
        Err(e) => {
            // lsof might not be available or failed - warn but don't fail
            eprintln!(
                "{}",
                console::style(&format!(
                    "Warning: could not check for processes on port {}: {}",
                    port, e
                ))
                .yellow()
            );
        }
    }

    Ok(())
}

/// Kill processes on all configured ports (database, backend, frontend).
/// This ensures a clean start by clearing ports before starting services.
pub fn cleanup_ports(config: &Config, start_db: bool) -> Result<()> {
    print_step("Cleaning up existing processes on configured ports...");

    if start_db {
        // Database port (PostgreSQL default)
        kill_processes_on_port("5432", "database")?;
    }

    // Backend port
    kill_processes_on_port(&config.api_port, "backend")?;

    // Frontend port
    kill_processes_on_port(&config.frontend_port, "frontend")?;

    Ok(())
}
