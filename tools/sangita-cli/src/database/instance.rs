use crate::app_config::AppConfig;
use crate::database::error::DatabaseError;
use crate::utils::{get_postgres_bin_path, run_command};
use anyhow::{Context, Result};
use log::info;
use std::path::PathBuf;
use std::time::{Duration, Instant};
use tokio::time::sleep;

#[derive(Debug, Clone, Copy)]
pub struct PostgresTimeouts {
    pub startup: Duration,
    pub shutdown: Duration,
    pub connection_retry_interval: Duration,
    pub max_connection_attempts: u32,
}

impl Default for PostgresTimeouts {
    fn default() -> Self {
        Self {
            startup: Duration::from_secs(30),
            shutdown: Duration::from_secs(10),
            connection_retry_interval: Duration::from_secs(1),
            max_connection_attempts: 3,
        }
    }
}

#[derive(Debug)]
pub struct PostgresInstance {
    pg_home: PathBuf,
    pg_data: PathBuf,
    timeouts: PostgresTimeouts,
    host: String,
    port: u16,
    admin_user: String,
    admin_password: String,
}

impl PostgresInstance {
    pub fn new(config: &AppConfig) -> Self {
        Self {
            pg_home: PathBuf::from(&config.paths.pg_home),
            pg_data: PathBuf::from(&config.paths.pg_data),
            timeouts: PostgresTimeouts::default(),
            host: config.database.host.clone(),
            port: config.database.port,
            admin_user: config.database.admin_user.clone(),
            admin_password: config.database.admin_password.clone(),
        }
    }

    pub fn is_initialized(&self) -> bool {
        self.pg_data.join("PG_VERSION").exists()
    }

    pub fn init_db(&self) -> Result<()> {
        info!("Initializing PostgreSQL data directory...");
        if !self.pg_data.exists() {
            std::fs::create_dir_all(&self.pg_data)?;
        }

        let initdb = self.get_binary_path("initdb");
        let mut args = vec![
            "-D".to_string(),
            self.pg_data.to_string_lossy().into_owned(),
        ];

        if !self.admin_user.is_empty() {
            args.push("-U".to_string());
            args.push(self.admin_user.clone());
        }

        let mut pwfile_path = None;
        if !self.admin_password.is_empty() {
            let temp_dir = std::env::temp_dir();
            let pwfile = temp_dir.join(format!("sangita-initdb-{}.txt", std::process::id()));
            std::fs::write(&pwfile, &self.admin_password)?;
            args.push("--pwfile".to_string());
            args.push(pwfile.to_string_lossy().into_owned());
            pwfile_path = Some(pwfile);
        }

        let args_ref: Vec<&str> = args.iter().map(String::as_str).collect();
        let result = run_command(&initdb.to_string_lossy(), &args_ref);

        if let Some(pwfile) = pwfile_path {
            let _ = std::fs::remove_file(pwfile);
        }

        result.context("Failed to initialize PostgreSQL data directory")?;
        info!("PostgreSQL data directory initialized");
        Ok(())
    }

    pub async fn start(&self) -> Result<()> {
        info!("Starting PostgreSQL instance...");
        let pg_ctl = self.get_binary_path("pg_ctl");
        let log_file = self.pg_data.join("logfile");

        run_command(
            &pg_ctl.to_string_lossy(),
            &[
                "-D",
                &self.pg_data.to_string_lossy(),
                "-l",
                &log_file.to_string_lossy(),
                "start",
            ],
        )
        .context("Failed to start PostgreSQL")?;

        self.wait_for_status(true, self.timeouts.startup, "start")
            .await
    }

    pub async fn ensure_running(&self) -> Result<()> {
        if self.is_running().await? {
            info!(
                "PostgreSQL instance already running on {}:{}",
                self.host, self.port
            );
            return Ok(());
        }

        self.start().await
    }

    pub async fn stop(&self) -> Result<()> {
        info!("Stopping PostgreSQL instance...");
        let pg_ctl = self.get_binary_path("pg_ctl");

        run_command(
            &pg_ctl.to_string_lossy(),
            &["-D", &self.pg_data.to_string_lossy(), "stop", "-m", "fast"],
        )
        .context("Failed to stop PostgreSQL")?;

        self.wait_for_status(false, self.timeouts.shutdown, "stop")
            .await
    }

    fn get_binary_path(&self, binary_name: &str) -> PathBuf {
        get_postgres_bin_path(&self.pg_home.to_string_lossy(), binary_name).into()
    }

    async fn is_running(&self) -> Result<bool> {
        let pg_isready = self.get_binary_path("pg_isready");
        let output = tokio::process::Command::new(pg_isready)
            .args(["-h", &self.host, "-p", &self.port.to_string()])
            .output()
            .await
            .context("Failed to execute pg_isready")?;

        Ok(output.status.success())
    }

    async fn wait_for_status(
        &self,
        target_state: bool,
        timeout: Duration,
        operation: &str,
    ) -> Result<()> {
        let start = Instant::now();
        while start.elapsed() < timeout {
            if self.is_running().await? == target_state {
                info!("PostgreSQL instance {} operation completed", operation);
                return Ok(());
            }
            sleep(self.timeouts.connection_retry_interval).await;
        }

        Err(DatabaseError::Timeout {
            operation: operation.to_string(),
            duration: timeout,
        }
        .into())
    }
}
