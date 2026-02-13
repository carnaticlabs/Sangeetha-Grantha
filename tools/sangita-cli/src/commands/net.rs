use crate::utils::{
    check_command_exists, local_ipv4_addresses, primary_ipv4_address, print_error, print_step,
    print_success, project_root, write_with_backup,
};
use anyhow::{anyhow, Context, Result};
use clap::{Args, Subcommand, ValueEnum};
use console::style;
use reqwest::Url;
use std::fs;
use std::net::TcpListener;
use std::path::Path;
use std::process::Command;
use tokio::net::lookup_host;

#[derive(Args)]
pub struct NetArgs {
    #[command(subcommand)]
    command: NetCommands,
}

#[derive(Subcommand)]
pub enum NetCommands {
    /// Show local network details (IPs, port availability, firewall)
    Info,
    /// Write local network config for IP / mDNS / Pi-hole DNS
    Configure(NetConfigureArgs),
    /// Verify local network setup for mobile testing
    Verify,
}

#[derive(Args)]
pub struct NetConfigureArgs {
    /// Mode: direct IP, mDNS hostname, or Pi-hole DNS hostname
    #[arg(long, value_enum, default_value = "ip")]
    mode: NetMode,
    /// Override the detected IP/hostname
    #[arg(long)]
    target: Option<String>,
    /// Skip Android/iOS client config generation
    #[arg(long)]
    skip_mobile: bool,
}

#[derive(Clone, ValueEnum)]
pub enum NetMode {
    Ip,
    Mdns,
    Pihole,
}

pub async fn run(args: NetArgs) -> Result<()> {
    match args.command {
        NetCommands::Info => show_info().await,
        NetCommands::Configure(cfg) => configure(cfg).await,
        NetCommands::Verify => verify().await,
    }
}

async fn show_info() -> Result<()> {
    print_step("Local network information");
    let ips = local_ipv4_addresses()?;
    if ips.is_empty() {
        print_error("No non-loopback IPv4 addresses detected");
    } else {
        println!("{} {}", style("IPs:").bold(), ips.join(", "));
    }

    let ports = [8080u16, 5001u16];
    for port in ports {
        let status = if is_port_available(port) {
            style("free").green()
        } else {
            style("in use").red()
        };
        println!("Port {}: {}", port, status);
    }

    if let Some(fw) = firewall_status() {
        println!("Firewall: {}", fw);
    } else {
        println!(
            "Firewall: {}",
            style("unknown (socketfilterfw not found)").yellow()
        );
    }

    Ok(())
}

async fn configure(args: NetConfigureArgs) -> Result<()> {
    let root = project_root()?;
    let host = match args.mode {
        NetMode::Ip => args
            .target
            .or_else(|| primary_ipv4_address().ok().flatten())
            .context("No IP detected. Provide one with --target")?,
        NetMode::Mdns => args
            .target
            .unwrap_or_else(|| "sangita-api.local".to_string()),
        NetMode::Pihole => args
            .target
            .unwrap_or_else(|| "api.sangita.home".to_string()),
    };

    let mode_label = match args.mode {
        NetMode::Ip => "Direct IP",
        NetMode::Mdns => "mDNS",
        NetMode::Pihole => "Pi-hole DNS",
    };

    print_step(&format!(
        "Writing network configuration ({mode_label}) using host: {host}"
    ));

    write_application_local(&root, &host)?;
    write_env_development(&root, &host)?;

    if !args.skip_mobile {
        write_android_network_config(&root, &host)?;
        write_shared_api_config(&root, &host)?;
    }

    let uploads_dir = root.join("uploads");
    fs::create_dir_all(&uploads_dir)?;

    print_success("Configuration written");
    println!(
        "  - config/application.local.toml\n  - config/development.env\n  - androidApp/src/main/res/xml/network_security_config.xml (if androidApp exists)\n  - modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/config/ApiConfig.kt"
    );
    println!("Uploads directory ready at {}", uploads_dir.display());

    Ok(())
}

async fn verify() -> Result<()> {
    let root = project_root()?;
    let mut errors = 0usize;

    print_step("Verifying local network setup");

    let ips = local_ipv4_addresses()?;
    if ips.is_empty() {
        fail("Local IP addresses", "No non-loopback IPv4 addresses found");
        errors += 1;
    } else {
        pass("Local IP addresses", &format!("Found: {}", ips.join(", ")));
    }

    let app_config = root.join("config/application.local.toml");
    let config_value = if app_config.exists() {
        match fs::read_to_string(&app_config) {
            Ok(s) => match toml::from_str::<toml::Value>(&s) {
                Ok(val) => Some(val),
                Err(err) => {
                    fail(
                        "config/application.local.toml",
                        &format!("Could not parse TOML: {err}"),
                    );
                    errors += 1;
                    None
                }
            },
            Err(err) => {
                fail(
                    "config/application.local.toml",
                    &format!("Could not read file: {err}"),
                );
                errors += 1;
                None
            }
        }
    } else {
        warn(
            "config/application.local.toml",
            "Not found. Run `sangita net configure`.",
        );
        None
    };

    if let Some(val) = config_value.as_ref() {
        let backend_host = val
            .get("backend")
            .and_then(|v| v.get("host"))
            .and_then(|v| v.as_str())
            .unwrap_or_default();
        if backend_host == "0.0.0.0" {
            pass("Backend host", "Listening on 0.0.0.0 (mobile reachable)");
        } else {
            warn(
                "Backend host",
                &format!(
                    "Configured as `{}` (expected 0.0.0.0 for LAN access)",
                    backend_host
                ),
            );
        }

        if let Some(api_url) = val
            .get("frontend")
            .and_then(|f| f.get("api_url"))
            .and_then(|s| s.as_str())
        {
            match Url::parse(api_url) {
                Ok(url) => {
                    let host = url
                        .host_str()
                        .ok_or_else(|| anyhow!("Missing host in api_url"))?;
                    let port = url.port_or_known_default().unwrap_or(8080);
                    match lookup_host((host, port)).await {
                        Ok(_) => pass("API URL DNS", &format!("{host}:{port} resolves")),
                        Err(_) => warn(
                            "API URL DNS",
                            &format!("{host}:{port} did not resolve (check Pi-hole/mDNS)"),
                        ),
                    }
                }
                Err(err) => {
                    warn("API URL", &format!("Invalid api_url value: {err}"));
                }
            }
        }
    }

    if check_command_exists("pg_isready") {
        let mut cmd = Command::new("pg_isready");
        cmd.arg("-q");

        if let Some(val) = config_value.as_ref() {
            if let Some(db_table) = val.get("database").and_then(|v| v.as_table()) {
                if let Some(host) = db_table.get("host").and_then(|v| v.as_str()) {
                    cmd.arg("-h").arg(host);
                }
                if let Some(port) = db_table.get("port").and_then(|v| v.as_integer()) {
                    cmd.arg("-p").arg(port.to_string());
                }
            }
        }

        match cmd.output() {
            Ok(output) if output.status.success() => {
                pass("PostgreSQL", "pg_isready reports healthy");
            }
            Ok(_) => {
                warn("PostgreSQL", "pg_isready could not reach server");
            }
            Err(err) => {
                warn("PostgreSQL", &format!("Failed to run pg_isready: {err}"));
            }
        }
    } else {
        warn("PostgreSQL", "pg_isready not found");
    }

    let uploads_dir = root.join("uploads");
    if uploads_dir.exists() {
        pass(
            "Uploads directory",
            &format!("Present at {}", uploads_dir.display()),
        );
    } else {
        warn(
            "Uploads directory",
            &format!(
                "Not found at {} (will create on demand)",
                uploads_dir.display()
            ),
        );
    }

    let android_config = root.join("androidApp/src/main/res/xml/network_security_config.xml");
    if android_config.exists() {
        let content = fs::read_to_string(&android_config).unwrap_or_default();
        if content.contains("cleartextTrafficPermitted=\"true\"") {
            pass(
                "Android network security",
                "network_security_config.xml allows HTTP",
            );
        } else {
            warn(
                "Android network security",
                "File exists but does not allow cleartext HTTP",
            );
        }
    } else {
        warn(
            "Android network security",
            "network_security_config.xml not found (run sangita net configure)",
        );
    }

    if root.join("gradlew").exists() {
        pass("Gradle wrapper", "gradlew found");
    } else {
        fail("Gradle wrapper", "gradlew not found");
        errors += 1;
    }

    if check_command_exists("adb") {
        pass("ADB", "adb found (Android deploy ready)");
    } else {
        warn("ADB", "adb not found in PATH");
    }

    if check_command_exists("xcodebuild") {
        pass("Xcode", "xcodebuild found");
    } else {
        warn("Xcode", "xcodebuild not found");
    }

    if is_port_available(8080) {
        pass("Port 8080", "Available");
    } else {
        warn("Port 8080", "In use; stop the process or change API port");
    }

    if let Some(fw) = firewall_status() {
        println!("Firewall: {}", fw);
    }

    println!();
    if errors == 0 {
        print_success("Verification complete: no critical blockers.");
    } else {
        print_error(&format!(
            "Verification complete: {errors} critical issue(s)."
        ));
    }

    Ok(())
}

fn write_application_local(root: &Path, host: &str) -> Result<()> {
    let pg_data = root.join("database/db-data");
    let tablespace_data = root.join("database/tablespaces/data");
    let tablespace_index = root.join("database/tablespaces/index");
    let content = format!(
        r#"# Generated by sangita net configure

[database]
host = "localhost"
port = 5432
name = "sangita_grantha"
user = "sangita_admin_app"
password = "change-me"
admin_db = "postgres"
admin_user = "postgres"
admin_password = "postgres"

[paths]
pg_home = "/opt/homebrew/opt/postgresql@15"
pg_data = "{}"
tablespace_data = "{}"
tablespace_index = "{}"

[migrations]
directory = "migrations"
seed_directory = "seed_data"

[roles]
admin_group = "grp_sangita_admin"
groups = [
  {{ name = "grp_sangita_admin", attributes = "NOLOGIN" }},
  {{ name = "grp_sangita_editor", attributes = "NOLOGIN" }},
  {{ name = "grp_sangita_reviewer", attributes = "NOLOGIN" }}
]

[backend]
host = "0.0.0.0"
port = 8080
admin_token = "dev-admin-token"
token_ttl_seconds = 3600

[frontend]
api_url = "http://{host}:8080"

[storage]
upload_directory = "uploads"
public_base_url = "http://{host}:8080/uploads"
"#,
        pg_data.display(),
        tablespace_data.display(),
        tablespace_index.display()
    );

    write_with_backup(&root.join("config/application.local.toml"), &content)
}

fn write_env_development(root: &Path, host: &str) -> Result<()> {
    let content = format!(
        r#"# Generated by sangita net configure
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost:8080,http://{host}:8080,http://{host}:5173
"#
    );
    write_with_backup(&root.join("config/development.env"), &content)
}

fn write_android_network_config(root: &Path, host: &str) -> Result<()> {
    let android_root = root.join("androidApp");
    if !android_root.exists() {
        return Ok(());
    }

    let content = format!(
        r##"<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by sangita net configure -->
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">{host}</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
"##
    );
    write_with_backup(
        &root.join("androidApp/src/main/res/xml/network_security_config.xml"),
        &content,
    )
}

fn write_shared_api_config(root: &Path, host: &str) -> Result<()> {
    let content = format!(
        r#"package com.sangita.grantha.shared.config

/**
 * Generated by sangita net configure
 */
object ApiConfig {{
    const val BASE_URL = "http://{host}:8080"
}}
"#
    );
    write_with_backup(
        &root.join("modules/shared/domain/src/commonMain/kotlin/com/sangita/grantha/shared/config/ApiConfig.kt"),
        &content,
    )
}

fn is_port_available(port: u16) -> bool {
    TcpListener::bind(("0.0.0.0", port)).is_ok()
}

fn firewall_status() -> Option<String> {
    if !check_command_exists("socketfilterfw") {
        return None;
    }

    let output = Command::new("/usr/libexec/ApplicationFirewall/socketfilterfw")
        .arg("--getglobalstate")
        .output()
        .ok()?;

    if !output.status.success() {
        return None;
    }

    let stdout = String::from_utf8_lossy(&output.stdout);
    if stdout.to_ascii_lowercase().contains("enabled") {
        Some(style("enabled").yellow().to_string())
    } else if stdout.to_ascii_lowercase().contains("disabled") {
        Some(style("disabled").green().to_string())
    } else {
        None
    }
}

fn pass(title: &str, msg: &str) {
    println!("{} {} - {}", style("✓").green(), title, msg);
}

fn warn(title: &str, msg: &str) {
    println!("{} {} - {}", style("⚠").yellow(), title, msg);
}

fn fail(title: &str, msg: &str) {
    println!("{} {} - {}", style("✗").red(), title, msg);
}
