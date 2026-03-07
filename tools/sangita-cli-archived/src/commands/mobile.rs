use crate::utils::{print_error, print_step, print_success, project_root};
use anyhow::Result;
use clap::{Args, Subcommand};
use std::path::PathBuf;

#[derive(Args)]
pub struct MobileArgs {
    #[command(subcommand)]
    command: MobileCommands,
}

#[derive(Subcommand)]
pub enum MobileCommands {
    /// Show where the mobile testing guides and checklists live
    Guide,
}

pub async fn run(args: MobileArgs) -> Result<()> {
    match args.command {
        MobileCommands::Guide => show_guide(),
    }
}

fn show_guide() -> Result<()> {
    print_step("Mobile testing resources");
    let root = project_root()?;
    let guide = root.join("application_documentation/07-quality/qa/testing-readme.md");
    let checklist =
        root.join("application_documentation/07-quality/qa/mobile-upload-test-checklist.md");

    emit_doc("Comprehensive guide", guide);
    emit_doc("Checklist", checklist);

    Ok(())
}

fn emit_doc(label: &str, path: PathBuf) {
    if path.exists() {
        print_success(&format!("{label}: {}", path.display()));
    } else {
        print_error(&format!(
            "{label}: not found (expected at {})",
            path.display()
        ));
    }
}
