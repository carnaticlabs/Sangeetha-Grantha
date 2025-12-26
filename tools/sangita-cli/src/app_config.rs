use anyhow::Result;
use config::{Config, ConfigError, File};
use serde::Deserialize;
use std::path::{Path, PathBuf};

#[derive(Debug, Deserialize, Clone)]
pub struct DatabaseConfig {
    pub host: String,
    pub port: u16,
    pub name: String,
    pub user: String,
    pub password: String,
    #[serde(default = "default_admin_db")]
    pub admin_db: String,
    #[serde(default = "default_admin_user")]
    pub admin_user: String,
    #[serde(default = "default_admin_password")]
    pub admin_password: String,
}

fn default_admin_db() -> String {
    "postgres".to_string()
}

fn default_admin_user() -> String {
    "postgres".to_string()
}

fn default_admin_password() -> String {
    "postgres".to_string()
}

#[derive(Debug, Deserialize, Clone)]
pub struct PathsConfig {
    pub pg_home: String,
    pub pg_data: String,
    #[serde(default)]
    pub tablespace_data: Option<String>,
    #[serde(default)]
    pub tablespace_index: Option<String>,
}

#[derive(Debug, Deserialize, Clone)]
pub struct MigrationsConfig {
    #[serde(default = "default_migrations_dir")]
    pub directory: PathBuf,
    #[serde(default)]
    pub seed_directory: Option<PathBuf>,
}

fn default_migrations_dir() -> PathBuf {
    PathBuf::from("migrations")
}

#[derive(Debug, Deserialize, Clone)]
pub struct RoleDefinition {
    pub name: String,
    #[serde(default = "default_role_attributes")]
    pub attributes: String,
}

fn default_role_attributes() -> String {
    "NOLOGIN".to_string()
}

#[derive(Debug, Deserialize, Clone)]
pub struct RolesConfig {
    #[serde(default = "default_group_roles")]
    pub groups: Vec<RoleDefinition>,
    #[serde(default = "default_admin_group")]
    pub admin_group: String,
}

fn default_group_roles() -> Vec<RoleDefinition> {
    Vec::new()
}

fn default_admin_group() -> String {
    String::new()
}

impl Default for RolesConfig {
    fn default() -> Self {
        Self {
            groups: default_group_roles(),
            admin_group: default_admin_group(),
        }
    }
}

#[derive(Debug, Deserialize, Clone)]
pub struct AppConfig {
    pub database: DatabaseConfig,
    pub paths: PathsConfig,
    #[serde(default = "default_migrations_config")]
    pub migrations: MigrationsConfig,
    #[serde(default)]
    pub roles: RolesConfig,
}

fn default_migrations_config() -> MigrationsConfig {
    MigrationsConfig {
        directory: default_migrations_dir(),
        seed_directory: None,
    }
}

impl AppConfig {
    // Renamed function to clearly reflect intent and source
    pub fn from_file<P: AsRef<Path>>(config_path: P) -> Result<Self, ConfigError> {
        load_config_from_path(config_path.as_ref())
    }
}

// Extracted common loading logic into a private helper function
fn load_config_from_path<T: for<'de> Deserialize<'de>>(path: &Path) -> Result<T, ConfigError> {
    let config = Config::builder().add_source(File::from(path)).build()?;
    config.try_deserialize()
}
