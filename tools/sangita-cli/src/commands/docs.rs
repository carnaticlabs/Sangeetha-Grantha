use crate::utils::{print_error, print_info, print_success, print_warning, project_root};
use anyhow::{Context, Result};
use clap::{Args, Subcommand};
use serde::Deserialize;
use std::collections::HashMap;
use std::fs;
use std::io::Write;

#[derive(Args)]
pub struct DocsArgs {
    #[command(subcommand)]
    command: DocsCommands,
}

#[derive(Subcommand)]
pub enum DocsCommands {
    /// Sync versions from source files to current-versions.md
    SyncVersions {
        /// Check if versions are in sync without updating
        #[arg(long)]
        check: bool,
    },
    /// Validate documentation links and references
    ValidateLinks,
}

pub async fn run(args: DocsArgs) -> Result<()> {
    match args.command {
        DocsCommands::SyncVersions { check } => sync_versions(check).await,
        DocsCommands::ValidateLinks => validate_links().await,
    }
}

// ============================================================================
// Version Sync Implementation
// ============================================================================

/// Represents the [versions] section of gradle/libs.versions.toml
#[derive(Deserialize, Debug)]
struct GradleVersions {
    kotlin: Option<String>,
    ktor: Option<String>,
    exposed: Option<String>,
    #[serde(rename = "kotlinxCoroutinesCore")]
    kotlinx_coroutines_core: Option<String>,
    #[serde(rename = "kotlinxDatetime")]
    kotlinx_datetime: Option<String>,
    #[serde(rename = "kotlinxSerializationJson")]
    kotlinx_serialization_json: Option<String>,
    compose: Option<String>,
    agp: Option<String>,
    postgresql: Option<String>,
    hikaricp: Option<String>,
    logback: Option<String>,
    #[serde(rename = "logstashLogbackEncoder")]
    logstash_logback_encoder: Option<String>,
    jwt: Option<String>,
    koin: Option<String>,
    shadow: Option<String>,
    #[serde(rename = "awsSdk")]
    aws_sdk: Option<String>,
    #[serde(rename = "googleAuth")]
    google_auth: Option<String>,
    micrometer: Option<String>,
    mockk: Option<String>,
    #[serde(rename = "commonsCsv")]
    commons_csv: Option<String>,
}

#[derive(Deserialize, Debug)]
struct GradleToml {
    versions: GradleVersions,
}

/// Represents package.json structure (partial)
#[derive(Deserialize, Debug)]
struct PackageJson {
    dependencies: Option<HashMap<String, String>>,
    #[serde(rename = "devDependencies")]
    dev_dependencies: Option<HashMap<String, String>>,
}

/// Represents .mise.toml structure (partial)
#[derive(Deserialize, Debug)]
struct MiseToml {
    tools: Option<MiseTools>,
}

#[derive(Deserialize, Debug)]
struct MiseTools {
    java: Option<String>,
    rust: Option<String>,
    bun: Option<String>,
    #[serde(rename = "docker-compose")]
    docker_compose: Option<String>,
}

/// Clean npm version string (remove ^, ~, etc.)
fn clean_npm_version(version: &str) -> String {
    version
        .trim_start_matches('^')
        .trim_start_matches('~')
        .trim_start_matches('=')
        .to_string()
}

/// Sync versions from source files to current-versions.md
async fn sync_versions(check_only: bool) -> Result<()> {
    let root = project_root()?;

    // Source files
    let gradle_path = root.join("gradle/libs.versions.toml");
    let package_json_path = root.join("modules/frontend/sangita-admin-web/package.json");
    let mise_path = root.join(".mise.toml");
    let output_path = root.join("application_documentation/00-meta/current-versions.md");

    // Verify source files exist
    for (path, name) in [
        (&gradle_path, "gradle/libs.versions.toml"),
        (&package_json_path, "package.json"),
        (&mise_path, ".mise.toml"),
    ] {
        if !path.exists() {
            print_error(&format!("Source file not found: {}", name));
            return Err(anyhow::anyhow!("Missing source file: {}", name));
        }
    }

    print_info("Extracting versions from source files...");

    // Parse gradle/libs.versions.toml
    let gradle_content = fs::read_to_string(&gradle_path)
        .context("Failed to read gradle/libs.versions.toml")?;
    let gradle: GradleToml = toml::from_str(&gradle_content)
        .context("Failed to parse gradle/libs.versions.toml")?;
    let v = &gradle.versions;

    // Parse package.json
    let package_content = fs::read_to_string(&package_json_path)
        .context("Failed to read package.json")?;
    let package: PackageJson = serde_json::from_str(&package_content)
        .context("Failed to parse package.json")?;

    // Parse .mise.toml
    let mise_content = fs::read_to_string(&mise_path)
        .context("Failed to read .mise.toml")?;
    let mise: MiseToml = toml::from_str(&mise_content)
        .context("Failed to parse .mise.toml")?;

    // Helper to get npm dependency version
    let get_npm = |name: &str| -> String {
        let deps = package.dependencies.as_ref();
        let dev_deps = package.dev_dependencies.as_ref();

        deps.and_then(|d| d.get(name))
            .or_else(|| dev_deps.and_then(|d| d.get(name)))
            .map(|v| clean_npm_version(v))
            .unwrap_or_else(|| "unknown".to_string())
    };

    // Helper to get mise tool version
    let tools = mise.tools.as_ref();
    let get_mise = |f: fn(&MiseTools) -> &Option<String>| -> String {
        tools
            .and_then(|t| f(t).as_ref())
            .cloned()
            .unwrap_or_else(|| "unknown".to_string())
    };

    // Generate timestamp
    let now = chrono::Utc::now();
    let date_only = now.format("%Y-%m-%d").to_string();
    let timestamp = now.format("%Y-%m-%dT%H:%M:%SZ").to_string();

    // Build the markdown content
    let content = format!(
        r#"| Metadata | Value |
|:---|:---|
| **Status** | Auto-Generated |
| **Version** | 1.0.0 |
| **Last Updated** | {date} |
| **Generated** | {timestamp} |
| **Tool** | sangita-cli docs sync-versions |

# Current Technology Versions

> **This file is auto-generated.** Do not edit manually.
>
> Run `sangita-cli docs sync-versions` to regenerate after updating dependencies.
>
> All documentation should reference this file instead of hardcoding version numbers.

---

## Development Toolchain

*Source: `.mise.toml`*

| Tool | Version | Notes |
|------|---------|-------|
| Java | `{java}` | Temurin distribution, JVM toolchain |
| Rust | `{rust}` | For sangita-cli tool |
| Bun | `{bun}` | Frontend package manager |
| Docker Compose | `{docker_compose}` | Database container management |

---

## Backend Stack (Kotlin/JVM)

*Source: `gradle/libs.versions.toml`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `{kotlin}` | Language version |
| Ktor | `{ktor}` | HTTP server & client framework |
| Exposed | `{exposed}` | SQL ORM (DSL-based) |
| Koin | `{koin}` | Dependency injection |

### Kotlinx Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| Coroutines | `{coroutines}` | Async programming |
| DateTime | `{datetime}` | Cross-platform date/time |
| Serialization JSON | `{serialization}` | JSON serialization |

### Database & Infrastructure

| Library | Version | Purpose |
|---------|---------|---------|
| PostgreSQL Driver | `{postgresql}` | JDBC driver |
| HikariCP | `{hikaricp}` | Connection pooling |
| Logback | `{logback}` | Logging framework |
| Logstash Encoder | `{logstash}` | JSON log formatting |
| Commons CSV | `{commons_csv}` | CSV parsing |

### Security & Auth

| Library | Version | Purpose |
|---------|---------|---------|
| JWT (Auth0) | `{jwt}` | JWT token handling |
| Google Auth | `{google_auth}` | OAuth2 (future SSO) |

### Build & Packaging

| Library | Version | Purpose |
|---------|---------|---------|
| Shadow Plugin | `{shadow}` | Fat JAR packaging |
| Micrometer | `{micrometer}` | Metrics & monitoring |

### Testing

| Library | Version | Purpose |
|---------|---------|---------|
| MockK | `{mockk}` | Kotlin mocking framework |

---

## Frontend Stack (React/TypeScript)

*Source: `modules/frontend/sangita-admin-web/package.json`*

### Core Framework

| Library | Version | Purpose |
|---------|---------|---------|
| React | `{react}` | UI framework |
| TypeScript | `{typescript}` | Type-safe JavaScript |
| Vite | `{vite}` | Build tool & dev server |

### Styling & UI

| Library | Version | Purpose |
|---------|---------|---------|
| Tailwind CSS | `{tailwind}` | Utility-first CSS |

### Routing & State

| Library | Version | Purpose |
|---------|---------|---------|
| React Router | `{react_router}` | Client-side routing |
| TanStack Query | `{tanstack}` | Data fetching & caching |

### Development & Testing

| Library | Version | Purpose |
|---------|---------|---------|
| ESLint | `{eslint}` | Code linting |
| Vitest | `{vitest}` | Unit testing |

---

## Mobile Stack (Kotlin Multiplatform)

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | `{kotlin}` | Shared with backend |
| Compose Multiplatform | `{compose}` | Cross-platform UI |
| Android Gradle Plugin | `{agp}` | Android build |
| Ktor Client | `{ktor}` | HTTP client |

---

## Cloud & External Services

*Source: `gradle/libs.versions.toml`*

| Library | Version | Purpose |
|---------|---------|---------|
| AWS SDK | `{aws_sdk}` | S3 storage (future) |
| Google Auth | `{google_auth}` | SSO integration (future) |

---

## Version History

| Date | Change |
|------|--------|
| {date} | Auto-generated from source files |

---

## How to Use This File

### In Documentation

Instead of hardcoding versions, reference this file:

```markdown
For current versions, see [Current Versions](./current-versions.md).

The backend uses Ktor (see [current version](./current-versions.md#core-framework))
with Exposed ORM for database access.
```

### Updating Versions

1. Update the source file (`gradle/libs.versions.toml`, `package.json`, or `.mise.toml`)
2. Run `sangita-cli docs sync-versions`
3. Commit both the source file and the regenerated `current-versions.md`

### CI Integration

Add to your CI pipeline to ensure versions stay in sync:

```yaml
- name: Verify version sync
  run: |
    sangita-cli docs sync-versions --check
```
"#,
        date = date_only,
        timestamp = timestamp,
        // Toolchain
        java = get_mise(|t| &t.java),
        rust = get_mise(|t| &t.rust),
        bun = get_mise(|t| &t.bun),
        docker_compose = get_mise(|t| &t.docker_compose),
        // Backend
        kotlin = v.kotlin.as_deref().unwrap_or("unknown"),
        ktor = v.ktor.as_deref().unwrap_or("unknown"),
        exposed = v.exposed.as_deref().unwrap_or("unknown"),
        koin = v.koin.as_deref().unwrap_or("unknown"),
        coroutines = v.kotlinx_coroutines_core.as_deref().unwrap_or("unknown"),
        datetime = v.kotlinx_datetime.as_deref().unwrap_or("unknown"),
        serialization = v.kotlinx_serialization_json.as_deref().unwrap_or("unknown"),
        postgresql = v.postgresql.as_deref().unwrap_or("unknown"),
        hikaricp = v.hikaricp.as_deref().unwrap_or("unknown"),
        logback = v.logback.as_deref().unwrap_or("unknown"),
        logstash = v.logstash_logback_encoder.as_deref().unwrap_or("unknown"),
        commons_csv = v.commons_csv.as_deref().unwrap_or("unknown"),
        jwt = v.jwt.as_deref().unwrap_or("unknown"),
        google_auth = v.google_auth.as_deref().unwrap_or("unknown"),
        shadow = v.shadow.as_deref().unwrap_or("unknown"),
        micrometer = v.micrometer.as_deref().unwrap_or("unknown"),
        mockk = v.mockk.as_deref().unwrap_or("unknown"),
        compose = v.compose.as_deref().unwrap_or("unknown"),
        agp = v.agp.as_deref().unwrap_or("unknown"),
        aws_sdk = v.aws_sdk.as_deref().unwrap_or("unknown"),
        // Frontend
        react = get_npm("react"),
        typescript = get_npm("typescript"),
        vite = get_npm("vite"),
        tailwind = get_npm("tailwindcss"),
        react_router = get_npm("react-router-dom"),
        tanstack = get_npm("@tanstack/react-query"),
        eslint = get_npm("eslint"),
        vitest = get_npm("vitest"),
    );

    if check_only {
        // Check mode - compare with existing file
        if output_path.exists() {
            let existing = fs::read_to_string(&output_path)?;

            // Compare content (ignoring timestamps)
            let normalize = |s: &str| -> String {
                s.lines()
                    .filter(|line| {
                        !line.contains("**Last Updated**")
                        && !line.contains("**Generated**")
                        && !line.contains("Auto-generated from source files")
                    })
                    .collect::<Vec<_>>()
                    .join("\n")
            };

            if normalize(&existing) != normalize(&content) {
                print_error("Version file is out of sync!");
                println!("\nRun 'sangita-cli docs sync-versions' to update.");
                std::process::exit(1);
            } else {
                print_success("Version file is in sync");
            }
        } else {
            print_error("current-versions.md does not exist");
            println!("\nRun 'sangita-cli docs sync-versions' to create it.");
            std::process::exit(1);
        }
    } else {
        // Write mode
        let mut file = fs::File::create(&output_path)
            .context("Failed to create current-versions.md")?;
        file.write_all(content.as_bytes())?;

        print_success(&format!("Generated {}", output_path.display()));
        println!();
        println!("Summary of extracted versions:");
        println!("  Backend:   Kotlin {}, Ktor {}, Exposed {}",
            v.kotlin.as_deref().unwrap_or("?"),
            v.ktor.as_deref().unwrap_or("?"),
            v.exposed.as_deref().unwrap_or("?")
        );
        println!("  Frontend:  React {}, Vite {}, Tailwind {}",
            get_npm("react"),
            get_npm("vite"),
            get_npm("tailwindcss")
        );
        println!("  Toolchain: Java {}, Rust {}, Bun {}",
            get_mise(|t| &t.java),
            get_mise(|t| &t.rust),
            get_mise(|t| &t.bun)
        );
    }

    Ok(())
}

// ============================================================================
// Link Validation (Future Enhancement)
// ============================================================================

async fn validate_links() -> Result<()> {
    print_warning("Link validation is not yet implemented");
    println!("\nPlanned features:");
    println!("  - Check all markdown links for broken references");
    println!("  - Validate cross-references between documents");
    println!("  - Report missing index entries");
    Ok(())
}
