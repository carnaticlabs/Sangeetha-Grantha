use crate::utils::{
    check_command_exists, docker_compose_available, print_error, print_info, print_step,
    print_success, print_warning,
};
use anyhow::Result;
use std::env;

/// Check if running in a mise-managed environment
fn is_mise_environment() -> bool {
    // Check for MISE_ENV variable (set by mise)
    env::var("MISE_ENV").is_ok() || env::var("MISE_DATA_DIR").is_ok()
}

/// Check if mise is available on the system
fn mise_available() -> bool {
    check_command_exists("mise")
}

pub async fn run() -> Result<()> {
    print_step("Checking sangita-cli environment...");

    // Trust mise for toolchain - just verify we're in a mise-managed environment
    if is_mise_environment() {
        print_success("Running in mise-managed environment");
    } else if mise_available() {
        print_warning("Not running via mise. Recommended: 'mise exec cargo run -- setup'");
        print_info(
            "This ensures correct tool versions (Rust 1.92.0, Java 25, Bun 1.3.0, Docker Compose)",
        );
        print_info("See .mise.toml for version requirements");
    } else {
        print_warning(
            "mise is not installed. Consider installing for better toolchain management.",
        );
        print_info("Install: curl https://mise.run | sh");
        print_info("Then run: mise install");
    }

    // Check sangita-cli specific prerequisites
    print_step("Checking prerequisites...");

    // Docker service (system requirement, not managed by mise)
    // Note: mise manages docker-compose CLI, but Docker Desktop/Engine must be installed separately
    if check_command_exists("docker") {
        print_success("Docker service is available");
    } else {
        print_error("Docker is required but not found");
        print_info("Install Docker Desktop (macOS/Windows) or Docker Engine (Linux)");
        print_info("Note: mise manages docker-compose CLI version, but Docker service must be installed separately");
        anyhow::bail!("Docker not found");
    }

    // Docker Compose (managed by mise, but check if available)
    if docker_compose_available() {
        print_success("Docker Compose is available (via mise or system)");
    } else {
        print_warning("Docker Compose not found. Install via mise: 'mise install'");
    }

    // Note: We trust mise for Rust, Java, Bun, and docker-compose
    // No need to check these tools - mise ensures correct versions when active

    print_step("Setting up environment files...");
    // TODO: Copy .env files if they don't exist

    print_success("Setup complete!");
    print_info("For complete environment setup, see: application_documentation/01-requirements/features/cross-platform-development-environment-standardisation.md");
    Ok(())
}
