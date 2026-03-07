use crate::app_config::AppConfig;
use std::fmt;
use urlencoding::encode;

#[derive(Debug, Clone)]
pub struct ConnectionString {
    pub user: String,
    pub password: String,
    pub host: String,
    pub port: u16,
    pub database: String,
}

impl ConnectionString {
    pub fn new(config: &AppConfig) -> Self {
        Self {
            user: config.database.user.clone(),
            password: config.database.password.clone(),
            host: config.database.host.clone(),
            port: config.database.port,
            database: config.database.name.clone(),
        }
    }
}

impl fmt::Display for ConnectionString {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        // URL-encode user, password, and database name to handle special characters
        write!(
            f,
            "postgres://{}:{}@{}:{}/{}",
            encode(&self.user),
            encode(&self.password),
            self.host,
            self.port,
            encode(&self.database)
        )
    }
}
