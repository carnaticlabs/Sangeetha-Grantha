use crate::utils::{print_error, print_success, project_root};
use anyhow::{Context, Result};
use clap::{Args, Subcommand};
use regex::Regex;
use std::fs;
use std::io::{self, Read};
use std::process::Command;

#[derive(Args)]
pub struct CommitArgs {
    #[command(subcommand)]
    command: CommitCommands,
}

#[derive(Subcommand)]
pub enum CommitCommands {
    /// Validate commit message format and reference
    Check {
        /// Commit message to validate (if not provided, reads from stdin)
        #[arg(long)]
        message: Option<String>,
    },
    /// Scan staged files for sensitive data (used by pre-commit hook)
    ScanSensitive,
    /// Install Git hooks for commit validation
    InstallHooks,
    /// Remove installed Git hooks
    UninstallHooks,
}

pub async fn run(args: CommitArgs) -> Result<()> {
    match args.command {
        CommitCommands::Check { message } => check_commit_message(message).await,
        CommitCommands::ScanSensitive => scan_sensitive_data().await,
        CommitCommands::InstallHooks => install_hooks().await,
        CommitCommands::UninstallHooks => uninstall_hooks().await,
    }
}

/// Validates a commit message according to the guardrails requirements
async fn check_commit_message(message: Option<String>) -> Result<()> {
    let commit_msg = match message {
        Some(msg) => msg,
        None => {
            // Read from stdin
            let mut buffer = String::new();
            io::stdin().read_to_string(&mut buffer)?;
            buffer
        }
    };

    let root = project_root()?;
    let doc_root = root.join("application_documentation");

    // Extract all Ref: references from the commit message
    let ref_pattern = Regex::new(r"(?i)ref:\s*(.+?)(?:\n|$)").unwrap();
    let mut references = Vec::new();

    for cap in ref_pattern.captures_iter(&commit_msg) {
        if let Some(ref_match) = cap.get(1) {
            references.push(ref_match.as_str().trim().to_string());
        }
    }

    // Validate that at least one reference exists
    if references.is_empty() {
        print_error("Commit message must include a reference to application_documentation");
        println!("\nExpected format:");
        println!("  <subject line>");
        println!();
        println!("  Ref: application_documentation/01-requirements/features/my-feature.md");
        println!();
        println!("  <optional body>");
        std::process::exit(1);
    }

    // Validate that only one reference exists (enforce 1:1 mapping)
    if references.len() > 1 {
        print_error("Multiple references found in commit message. Only one reference per commit is allowed.");
        println!("\nFound references:");
        for (i, ref_path) in references.iter().enumerate() {
            println!("  {}. {}", i + 1, ref_path);
        }
        std::process::exit(1);
    }

    // Validate that the referenced file exists
    let ref_path = &references[0];
    let full_path = if ref_path.starts_with("application_documentation/") {
        root.join(ref_path)
    } else if ref_path.starts_with("./") || !ref_path.contains('/') {
        // Relative path or just filename - try to resolve
        doc_root.join(ref_path.strip_prefix("./").unwrap_or(ref_path))
    } else {
        // Assume it's relative to application_documentation
        doc_root.join(ref_path)
    };

    // Normalize the path
    let normalized_path = full_path.canonicalize().or_else(|_| {
        // If canonicalize fails, try to construct the path manually
        if full_path.exists() {
            Ok(full_path)
        } else {
            Err(std::io::Error::new(
                std::io::ErrorKind::NotFound,
                "Path not found",
            ))
        }
    })?;

    // Ensure the path is within application_documentation
    let doc_root_normalized = doc_root
        .canonicalize()
        .context("application_documentation directory not found. Are you in the project root?")?;

    if !normalized_path.starts_with(&doc_root_normalized) {
        print_error(&format!(
            "Reference path '{}' is outside application_documentation directory",
            ref_path
        ));
        std::process::exit(1);
    }

    // Check if file exists
    if !normalized_path.exists() {
        print_error(&format!(
            "Referenced documentation file does not exist: {}",
            normalized_path.display()
        ));
        println!("\nExpected path: {}", normalized_path.display());
        std::process::exit(1);
    }

    // Success
    print_success(&format!(
        "Commit message validated. Reference: {}",
        normalized_path.display()
    ));
    Ok(())
}

/// Installs Git hooks for commit validation
async fn install_hooks() -> Result<()> {
    let root = project_root()?;
    let git_dir = root.join(".git");
    let hooks_dir = git_dir.join("hooks");

    // Check if we're in a Git repository
    if !git_dir.exists() {
        anyhow::bail!("Not a Git repository. Please run this command from the project root.");
    }

    // Create hooks directory if it doesn't exist
    if !hooks_dir.exists() {
        fs::create_dir_all(&hooks_dir)?;
    }

    // Get the path to the sangita-cli binary
    // In development, we use cargo run, but for hooks we need the actual binary
    let binary_name = if cfg!(target_os = "windows") {
        "sangita-cli.exe"
    } else {
        "sangita-cli"
    };

    // Try to find the binary in target/release or target/debug
    let binary_path = root
        .join("tools/sangita-cli/target/release")
        .join(binary_name)
        .canonicalize()
        .or_else(|_| {
            root.join("tools/sangita-cli/target/debug")
                .join(binary_name)
                .canonicalize()
        })
        .ok();

    // Install commit-msg hook
    let commit_msg_hook = hooks_dir.join("commit-msg");
    let hook_content = if let Some(ref bin_path) = binary_path {
        format!(
            r#"#!/bin/sh
# Sangita Grantha Commit Guardrails Hook
# This hook validates commit messages to ensure they reference documentation

exec "{}" commit check --message "$(cat "$1")"
"#,
            bin_path.display()
        )
    } else {
        // Fallback to cargo run (for development)
        let cli_dir = root.join("tools/sangita-cli");
        format!(
            r#"#!/bin/sh
# Sangita Grantha Commit Guardrails Hook
# This hook validates commit messages to ensure they reference documentation

cd "{}" && cargo run --quiet -- commit check --message "$(cat "$1")"
"#,
            cli_dir.display()
        )
    };

    fs::write(&commit_msg_hook, hook_content)?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(&commit_msg_hook)?.permissions();
        perms.set_mode(0o755);
        fs::set_permissions(&commit_msg_hook, perms)?;
    }

    // Install pre-commit hook for sensitive data scanning
    let pre_commit_hook = hooks_dir.join("pre-commit");
    let pre_commit_content = if let Some(ref bin_path) = binary_path {
        format!(
            r#"#!/bin/sh
# Sangita Grantha Pre-commit Hook
# This hook scans staged files for sensitive data

exec "{}" commit scan-sensitive
"#,
            bin_path.display()
        )
    } else {
        let cli_dir = root.join("tools/sangita-cli");
        format!(
            r#"#!/bin/sh
# Sangita Grantha Pre-commit Hook
# This hook scans staged files for sensitive data

cd "{}" && cargo run --quiet -- commit scan-sensitive
"#,
            cli_dir.display()
        )
    };

    fs::write(&pre_commit_hook, pre_commit_content)?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        let mut perms = fs::metadata(&pre_commit_hook)?.permissions();
        perms.set_mode(0o755);
        fs::set_permissions(&pre_commit_hook, perms)?;
    }

    print_success("Git hooks installed successfully!");
    println!("  - commit-msg: Validates commit message format and references");
    println!("  - pre-commit: Scans for sensitive data (API keys, secrets)");
    Ok(())
}

/// Removes installed Git hooks
async fn uninstall_hooks() -> Result<()> {
    let root = project_root()?;
    let hooks_dir = root.join(".git/hooks");

    let commit_msg_hook = hooks_dir.join("commit-msg");
    let pre_commit_hook = hooks_dir.join("pre-commit");

    let mut removed = 0;

    if commit_msg_hook.exists() {
        // Only remove if it's our hook (check for our marker)
        let content = fs::read_to_string(&commit_msg_hook)?;
        if content.contains("Sangita Grantha Commit Guardrails Hook") {
            fs::remove_file(&commit_msg_hook)?;
            removed += 1;
        }
    }

    if pre_commit_hook.exists() {
        let content = fs::read_to_string(&pre_commit_hook)?;
        if content.contains("Sangita Grantha Pre-commit Hook") {
            fs::remove_file(&pre_commit_hook)?;
            removed += 1;
        }
    }

    if removed > 0 {
        print_success(&format!("Removed {} Git hook(s)", removed));
    } else {
        println!("No Sangita Grantha hooks found to remove");
    }

    Ok(())
}

/// Scans staged files for sensitive data (API keys, secrets)
/// This is called by the pre-commit hook
pub async fn scan_sensitive_data() -> Result<()> {
    let root = project_root()?;

    // Get list of staged files
    let output = Command::new("git")
        .args(&["diff", "--cached", "--name-only", "--diff-filter=ACM"])
        .current_dir(&root)
        .output()
        .context("Failed to run git diff. Are you in a Git repository?")?;

    if !output.status.success() {
        anyhow::bail!("Failed to get staged files");
    }

    let staged_files = String::from_utf8(output.stdout)?;
    let files: Vec<&str> = staged_files.lines().filter(|s| !s.is_empty()).collect();

    if files.is_empty() {
        // No files staged, nothing to check
        return Ok(());
    }

    // Patterns to detect sensitive data
    // Note: Using separate patterns for single and double quotes to avoid escaping issues
    let sensitive_patterns = vec![
        // API keys - match with optional quotes
        (Regex::new(r#"(?i)(?:api[_-]?key|apikey)\s*[=:]\s*["']?([a-zA-Z0-9_-]{20,})["']?"#).unwrap(), "API key"),
        (Regex::new(r#"(?i)SG_GEMINI_API_KEY\s*[=:]\s*["']?([a-zA-Z0-9_-]+)["']?"#).unwrap(), "SG_GEMINI_API_KEY"),
        // Secrets
        (Regex::new(r#"(?i)(?:secret|password|token)\s*[=:]\s*["']?([a-zA-Z0-9_-]{16,})["']?"#).unwrap(), "Secret/Password/Token"),
        // AWS keys
        (Regex::new(r#"(?i)AWS[_-]?(?:ACCESS[_-]?KEY[_-]?ID|SECRET[_-]?ACCESS[_-]?KEY)\s*[=:]\s*["']?([a-zA-Z0-9_+/=]{20,})["']?"#).unwrap(), "AWS credentials"),
    ];

    let mut found_issues = Vec::new();

    for file_path in files {
        let full_path = root.join(file_path);

        // Skip binary files
        if let Ok(metadata) = fs::metadata(&full_path) {
            if metadata.len() > 1_000_000 {
                // Skip files larger than 1MB (likely binary)
                continue;
            }
        }

        // Read file content
        let content = match fs::read_to_string(&full_path) {
            Ok(c) => c,
            Err(_) => {
                // Skip binary files or files that can't be read as text
                continue;
            }
        };

        // Check each pattern
        for (pattern, pattern_name) in &sensitive_patterns {
            for (line_num, line) in content.lines().enumerate() {
                if let Some(cap) = pattern.captures(line) {
                    // Check if this is in a comment or string that might be a placeholder
                    let matched_value = cap.get(1).map(|m| m.as_str()).unwrap_or("");

                    // Skip common placeholder patterns
                    if matched_value.contains("your-")
                        || matched_value.contains("YOUR_")
                        || matched_value == "xxx"
                        || matched_value == "***"
                        || matched_value.len() < 10
                    {
                        continue;
                    }

                    found_issues.push((
                        file_path.to_string(),
                        line_num + 1,
                        pattern_name.to_string(),
                        line.trim().to_string(),
                    ));
                }
            }
        }
    }

    if !found_issues.is_empty() {
        print_error("Sensitive data detected in staged files!");
        println!("\nPlease remove or mask the following before committing:\n");

        for (file, line, pattern, content) in &found_issues {
            println!("  File: {}:{}", file, line);
            println!("  Pattern: {}", pattern);
            println!(
                "  Content: {}",
                if content.len() > 80 {
                    format!("{}...", &content[..80])
                } else {
                    content.clone()
                }
            );
            println!();
        }

        println!("Tip: Use environment variables or a secrets manager instead of hardcoding credentials.");
        std::process::exit(1);
    }

    print_success("No sensitive data detected in staged files");
    Ok(())
}
