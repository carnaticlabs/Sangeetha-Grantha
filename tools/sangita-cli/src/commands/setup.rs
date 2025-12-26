use crate::utils::{check_command_exists, print_error, print_step, print_success};
use anyhow::Result;

pub async fn run() -> Result<()> {
    print_step("Checking prerequisites...");

    let prerequisites = vec!["java", "bun", "docker", "cargo"];
    let mut missing = Vec::new();

    for tool in prerequisites {
        if check_command_exists(tool) {
            print_success(&format!("{} is installed", tool));
        } else {
            print_error(&format!("{} is missing", tool));
            missing.push(tool);
        }
    }

    if !missing.is_empty() {
        anyhow::bail!("Missing prerequisites: {:?}", missing);
    }

    print_step("Setting up environment files...");
    // TODO: Copy .env files if they don't exist

    print_success("Setup complete!");
    Ok(())
}
