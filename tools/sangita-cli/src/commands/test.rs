use crate::config::Config;
use crate::services::{
    ensure_database_running, ensure_process_alive, spawn_backend,
    spawn_frontend, wait_for_backend_health,
};
use crate::utils::{print_step, print_success, project_root};
use crate::{AppConfig, ConnectionString, DatabaseConfig, DatabaseManager};
use anyhow::{anyhow, Result};
use clap::{Args, Subcommand};
use reqwest::Client;
use std::path::PathBuf;
use std::time::Duration;
use tokio::time::sleep;

#[derive(Args)]
pub struct TestArgs {
    #[command(subcommand)]
    command: TestCommands,
}

#[derive(Subcommand)]
enum TestCommands {
    /// Run the Sangita Grantha steel thread smoke test
    SteelThread,
    /// Quick connectivity smoke test (health + search)
    Upload(UploadArgs),
}

#[derive(Args)]
pub struct UploadArgs {
    /// Override API base URL (default uses config/application.local.toml or env)
    #[arg(long)]
    base_url: Option<String>,
}

pub async fn run(args: TestArgs) -> Result<()> {
    match args.command {
        TestCommands::SteelThread => run_steel_thread().await,
        TestCommands::Upload(upload_args) => run_smoke_test(upload_args).await,
    }
}

async fn run_steel_thread() -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let client = Client::new();

    print_step("PHASE 1: Database & Migrations");
    let app_config = AppConfig::from_file(&root.join("config/application.local.toml"))?;
    ensure_database_running(&app_config).await?;
    apply_migrations(&app_config, &root).await?;

    print_step("PHASE 2: Backend Verification");
    let mut backend = spawn_backend(&config, &root, Some("test"))?;

    let health_url = format!("http://{}:{}/health", config.api_host, config.api_port);
    wait_for_backend_health(&client, &health_url, &mut backend, 180).await?;

    print_step("Running API Smoke Checks...");
    let search_url = format!(
        "http://{}:{}/v1/krithis/search",
        config.api_host, config.api_port
    );
    let search_resp = client.get(&search_url).send().await?;
    if !search_resp.status().is_success() {
        let _ = backend.kill();
        return Err(anyhow!("Search endpoint failed: {}", search_resp.status()));
    }
    print_success("Krithi search endpoint reachable");

    let audit_url = format!(
        "http://{}:{}/v1/audit/logs",
        config.api_host, config.api_port
    );
    let audit_resp = client
        .get(&audit_url)
        .bearer_auth(&config.admin_token)
        .send()
        .await?;
    if audit_resp.status().is_success() {
        print_success("Admin audit logs reachable with admin token");
    } else {
        eprintln!(
            "Warning: audit logs returned {} (check ADMIN_TOKEN)",
            audit_resp.status()
        );
    }

    ensure_process_alive(&mut backend, "Backend")?;

    print_step("PHASE 3: Frontend Launch");
    let mut frontend = spawn_frontend(&config, &root, true)?;
    sleep(Duration::from_secs(3)).await;
    print_success("Frontend started");

    print_step("PHASE 4: Manual Verification");
    println!("Admin Web: http://localhost:{}", config.frontend_port);
    println!("API: http://{}:{}", config.api_host, config.api_port);
    println!("Admin Token: {}", config.admin_token);
    println!("\nPress Ctrl+C to stop servers and exit.");

    tokio::signal::ctrl_c().await?;

    print_step("Shutting down...");
    let _ = backend.kill();
    let _ = frontend.kill();

    Ok(())
}

async fn apply_migrations(app_config: &AppConfig, root: &PathBuf) -> Result<()> {
    let migrations_path = root.join("database/migrations");
    let seed_data_path = root.join("database/seed_data");

    print_step("Applying migrations...");

    let connection_string = ConnectionString::new(app_config);
    let db_config = DatabaseConfig::new(connection_string.to_string())
        .with_admin_db(app_config.database.admin_db.clone())
        .with_admin_user(app_config.database.admin_user.clone())
        .with_admin_password(app_config.database.admin_password.clone());

    let seed_path = if seed_data_path.exists() {
        Some(seed_data_path)
    } else {
        None
    };

    let manager = DatabaseManager::new(db_config, migrations_path, seed_path);

    manager.ensure_database_exists().await?;
    manager.setup_connection_pool().await?;
    manager.run_migrations().await?;
    manager.run_seed_data().await?;

    print_success("Migrations complete");
    Ok(())
}

async fn run_smoke_test(args: UploadArgs) -> Result<()> {
    let config = Config::load()?;
    let root = project_root()?;
    let base_url = derive_api_base(&config, &root, args.base_url)?;
    let trimmed_base = base_url.trim_end_matches('/').to_string();

    print_step(&format!("API base: {trimmed_base}"));

    let client = Client::builder()
        .timeout(Duration::from_secs(30))
        .build()?;

    let health_url = format!("{trimmed_base}/health");
    print_step("Checking backend health...");
    let health_resp = client.get(&health_url).send().await?;
    if !health_resp.status().is_success() {
        return Err(anyhow!(
            "Health check failed with status {}",
            health_resp.status()
        ));
    }
    let body = health_resp.text().await.unwrap_or_default();
    print_success(&format!("Backend healthy ({})", body));

    let search_url = format!("{trimmed_base}/v1/krithis/search");
    print_step("Checking krithi search...");
    let search_resp = client.get(&search_url).send().await?;
    if !search_resp.status().is_success() {
        return Err(anyhow!(
            "Search check failed with status {}",
            search_resp.status()
        ));
    }
    print_success("Krithi search reachable");

    Ok(())
}

fn derive_api_base(config: &Config, root: &PathBuf, override_base: Option<String>) -> Result<String> {
    if let Some(base) = override_base {
        return Ok(base);
    }

    let config_path = root.join("config/application.local.toml");
    if config_path.exists() {
        let contents = std::fs::read_to_string(&config_path)?;
        if let Ok(val) = toml::from_str::<toml::Value>(&contents) {
            if let Some(url) = val
                .get("frontend")
                .and_then(|f| f.get("api_url"))
                .and_then(|v| v.as_str())
            {
                return Ok(url.to_string());
            }
        }
    }

    Ok(format!("http://{}:{}", config.api_host, config.api_port))
}
