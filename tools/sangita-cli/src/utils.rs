use anyhow::Result;
use console::style;
use get_if_addrs::get_if_addrs;
use std::collections::BTreeSet;
use std::fs;
use std::path::Path;
use std::process::Command;

pub fn print_step(msg: &str) {
    println!("{} {}", style("==>").bold().blue(), style(msg).bold());
}

pub fn print_success(msg: &str) {
    println!("{} {}", style("✓").bold().green(), msg);
}

pub fn print_error(msg: &str) {
    println!("{} {}", style("✗").bold().red(), msg);
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
