use anyhow::{Context, Result};
use console::style;
use get_if_addrs::get_if_addrs;
use std::collections::BTreeSet;
use std::fs;
use std::path::Path;
use std::process::Command;

// -----------------------------
// Docker Compose helpers
// -----------------------------

pub fn docker_compose_available() -> bool {
    // Prefer: `docker compose` (v2)
    let docker_compose = Command::new("docker")
        .args(["compose", "version"])
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false);

    if docker_compose {
        return true;
    }

    // Fallback: legacy `docker-compose`
    Command::new("docker-compose")
        .arg("version")
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

/// Run docker compose from a specific working directory.
///
/// Tries `docker compose ...` first, then falls back to `docker-compose ...`.
pub fn run_docker_compose(cwd: &Path, args: &[&str]) -> Result<()> {
    // Prefer: `docker compose ...` (v2)
    let output = Command::new("docker")
        .args(["compose"])
        .args(args)
        .current_dir(cwd)
        .output();

    match output {
        Ok(out) if out.status.success() => return Ok(()),
        Ok(out) => {
            // Detect common "daemon not running" errors and give a friendly hint.
            let stderr = String::from_utf8_lossy(&out.stderr);
            let stdout = String::from_utf8_lossy(&out.stdout);
            let combined = format!("{}\n{}", stdout, stderr);

            if combined.contains("Cannot connect to the Docker daemon")
                || combined.contains("is the docker daemon running")
                || combined.contains("error during connect")
            {
                anyhow::bail!(
                    "Docker is installed but the daemon is not running. Start Docker Desktop (macOS/Windows) or start the Docker service (Linux), then retry.\n\nOriginal error:\n{}",
                    combined.trim()
                );
            }

            // Otherwise, fall through to docker-compose fallback.
        }
        Err(_) => {
            // docker binary missing or failed; try legacy docker-compose
        }
    }

    // Fallback: legacy `docker-compose ...`
    let output = Command::new("docker-compose")
        .args(args)
        .current_dir(cwd)
        .output()
        .context("Failed to run docker-compose")?;

    if !output.status.success() {
        anyhow::bail!(
            "docker compose command failed.\n\nstdout:\n{}\n\nstderr:\n{}",
            String::from_utf8_lossy(&output.stdout),
            String::from_utf8_lossy(&output.stderr)
        );
    }

    Ok(())
}

pub fn print_step(msg: &str) {
    println!("{} {}", style("==>").bold().blue(), style(msg).bold());
}

pub fn print_success(msg: &str) {
    println!("{} {}", style("✓").bold().green(), msg);
}

pub fn print_error(msg: &str) {
    println!("{} {}", style("✗").bold().red(), msg);
}

pub fn print_warning(msg: &str) {
    println!("{} {}", style("⚠").bold().yellow(), msg);
}

pub fn print_info(msg: &str) {
    println!("{} {}", style("ℹ").bold().cyan(), msg);
}

pub fn check_command_exists(cmd: &str) -> bool {
    Command::new("which")
        .arg(cmd)
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

pub fn project_root() -> Result<std::path::PathBuf> {
    let mut path = std::env::current_dir()?;
    // If we are in tools/sangita-cli, go up two levels
    if path.ends_with("tools/sangita-cli") {
        path.pop();
        path.pop();
    }
    Ok(path)
}

pub fn local_ipv4_addresses() -> Result<Vec<String>> {
    let addrs = get_if_addrs()?;
    let mut uniq = BTreeSet::new();
    for iface in addrs {
        if let std::net::IpAddr::V4(ip) = iface.ip() {
            if !ip.is_loopback() {
                uniq.insert(ip.to_string());
            }
        }
    }
    Ok(uniq.into_iter().collect())
}

pub fn primary_ipv4_address() -> Result<Option<String>> {
    let list = local_ipv4_addresses()?;
    Ok(list.into_iter().next())
}

pub fn write_with_backup(path: &Path, contents: &str) -> Result<()> {
    if path.exists() {
        if let Some(name) = path.file_name().and_then(|n| n.to_str()) {
            let backup = path.with_file_name(format!("{name}.bak"));
            fs::copy(path, &backup)?;
        }
    } else if let Some(parent) = path.parent() {
        fs::create_dir_all(parent)?;
    }

    fs::write(path, contents)?;
    Ok(())
}

// OS utilities (integrated from database/rust/src/utils/os.rs)
pub fn is_windows() -> bool {
    cfg!(target_os = "windows")
}

pub fn get_postgres_bin_path(pg_home: &str, bin_name: &str) -> String {
    let bin_dir = if is_windows() {
        format!("{}\\bin", pg_home)
    } else {
        format!("{}/bin", pg_home)
    };

    let bin_name = if is_windows() {
        format!("{}.exe", bin_name)
    } else {
        bin_name.to_string()
    };

    if is_windows() {
        format!("{}\\{}", bin_dir, bin_name)
    } else {
        format!("{}/{}", bin_dir, bin_name)
    }
}

pub fn run_command(command: &str, args: &[&str]) -> Result<()> {
    run_command_with_env(command, args, &[])
}

pub fn run_command_with_env(command: &str, args: &[&str], env_vars: &[(&str, &str)]) -> Result<()> {
    let mut cmd = Command::new(command);
    cmd.args(args);

    for (key, value) in env_vars {
        cmd.env(key, value);
    }

    let status = cmd.status()?;

    if !status.success() {
        anyhow::bail!("Command failed: {} {:?}", command, args);
    }

    Ok(())
}
