use crate::app_config::AppConfig;
use std::fmt;

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
        write!(
            f,
            "postgres://{}:{}@{}:{}/{}",
            self.user, self.password, self.host, self.port, self.database
        )
    }
}
