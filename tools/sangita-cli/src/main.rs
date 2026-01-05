mod app_config;
mod commands;
mod config;
mod database;
mod services;
mod utils;

use anyhow::Result;
use clap::{Parser, Subcommand};
use commands::{commit, db, dev, mobile, net, setup, test};

// Re-export for convenience
pub use app_config::AppConfig;
pub use database::{ConnectionString, DatabaseConfig, DatabaseManager, PostgresInstance};

#[derive(Parser)]
#[command(author, version, about, long_about = None)]
struct Cli {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Interactive setup of the development environment
    Setup,
    /// Database management commands
    Db(db::DbArgs),
    /// Start development servers
    Dev(dev::DevArgs),
    /// Network configuration and diagnostics
    Net(net::NetArgs),
    /// Run tests (including steel thread)
    Test(test::TestArgs),
    /// Mobile testing helpers and documentation
    Mobile(mobile::MobileArgs),
    /// Commit guardrails and workflow enforcement
    Commit(commit::CommitArgs),
}

#[tokio::main]
async fn main() -> Result<()> {
    env_logger::init();
    let cli = Cli::parse();

    match cli.command {
        Commands::Setup => setup::run().await,
        Commands::Db(args) => db::run(args).await,
        Commands::Dev(args) => dev::run(args).await,
        Commands::Net(args) => net::run(args).await,
        Commands::Test(args) => test::run(args).await,
        Commands::Mobile(args) => mobile::run(args).await,
        Commands::Commit(args) => commit::run(args).await,
    }
}
