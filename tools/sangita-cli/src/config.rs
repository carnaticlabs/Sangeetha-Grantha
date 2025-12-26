use anyhow::Result;
use dotenvy::dotenv;
use std::env;

pub struct Config {
    pub api_host: String,
    pub api_port: String,
    pub frontend_port: String,
    pub admin_token: String,
}

impl Config {
    pub fn load() -> Result<Self> {
        // Load .env file if it exists
        dotenv().ok();

        Ok(Config {
            api_host: env::var("API_HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            api_port: env::var("API_PORT").unwrap_or_else(|_| "8080".to_string()),
            frontend_port: env::var("FRONTEND_PORT").unwrap_or_else(|_| "5001".to_string()),
            admin_token: env::var("ADMIN_TOKEN").unwrap_or_else(|_| "dev-admin-token".to_string()),
        })
    }
}
