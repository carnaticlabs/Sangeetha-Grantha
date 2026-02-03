| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangita Grantha Architect |

# IDE Setup Guide

This guide provides detailed instructions for configuring your IDE for optimal development experience with the Sangita Grantha project.

## Overview

The project uses multiple technologies requiring specific IDE configurations:

| Component | Language/Framework | Recommended IDE |
|-----------|-------------------|-----------------|
| Backend | Kotlin + Ktor | IntelliJ IDEA |
| Frontend | React + TypeScript | VS Code or WebStorm |
| Mobile | Kotlin Multiplatform | Android Studio / Fleet |
| CLI Tools | Rust | VS Code with rust-analyzer |

## IntelliJ IDEA Setup (Backend & KMP)

### Installation

1. **Download**: [IntelliJ IDEA](https://www.jetbrains.com/idea/download/) (Ultimate recommended, Community works)
2. **Version**: 2024.3 or later (for Kotlin 2.3.0 support)

### Required Plugins

Install these plugins via `Settings > Plugins > Marketplace`:

| Plugin | Purpose |
|--------|---------|
| Kotlin | Core Kotlin support (bundled) |
| Ktor | Ktor framework support |
| Database Tools | PostgreSQL browser |
| EnvFile | Environment variable management |
| Compose Multiplatform | KMP UI development |

### Project Import

```bash
# 1. Clone the repository
git clone <repository-url>
cd sangeetha-grantha

# 2. Install toolchain
mise install

# 3. Open in IntelliJ
# File > Open > Select the project root directory
```

### Gradle Configuration

1. **Open Settings**: `File > Settings` (or `Cmd + ,` on macOS)
2. **Navigate to**: `Build, Execution, Deployment > Build Tools > Gradle`
3. **Configure**:
   - **Gradle JVM**: `temurin-25` (from mise)
   - **Build and run using**: `Gradle`
   - **Run tests using**: `Gradle`

### JDK Configuration

1. **Open Project Structure**: `File > Project Structure` (or `Cmd + ;`)
2. **Configure SDK**:
   - **Project SDK**: Add `~/.local/share/mise/installs/java/temurin-25/`
   - **Language Level**: 21 (or auto-detect)

### Code Style

Import the project code style:

1. **Open Settings**: `File > Settings`
2. **Navigate to**: `Editor > Code Style > Kotlin`
3. **Set from**: `Kotlin style guide`
4. **Additional settings**:
   - Line length: 120
   - Continuation indent: 4
   - Use single name import

### Run Configurations

Create these run configurations for development:

#### Backend API

```
Name: Backend API
Type: Gradle
Tasks: :modules:backend:api:run
Arguments: --console=plain
Environment: SANGITA_ENV=local
```

#### Backend Dev Mode

```
Name: Backend Dev
Type: Gradle
Tasks: :modules:backend:api:runDev
Arguments: --console=plain
Environment: SANGITA_ENV=local
```

#### Backend Tests

```
Name: Backend Tests
Type: Gradle
Tasks: :modules:backend:api:test
Arguments: --console=plain
```

### Database Tool Configuration

1. **Open Database Panel**: `View > Tool Windows > Database`
2. **Add Data Source**: Click `+` > `Data Source` > `PostgreSQL`
3. **Configure**:
   - **Host**: `localhost`
   - **Port**: `5432`
   - **Database**: `sangita_grantha`
   - **User**: `sangita`
   - **Password**: `sangita_dev_password`
4. **Test Connection** and **Apply**

### File Templates

Add these file templates for consistency:

#### Kotlin Repository

```kotlin
package ${PACKAGE_NAME}

import org.jetbrains.exposed.sql.*
import com.sangitagrantha.backend.dal.DatabaseFactory.dbQuery

class ${NAME}Repository {

    suspend fun findById(id: Int): ${NAME}? = dbQuery {
        // TODO: Implement
    }

    suspend fun findAll(): List<${NAME}> = dbQuery {
        // TODO: Implement
    }
}
```

#### Kotlin Ktor Route

```kotlin
package ${PACKAGE_NAME}

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.${NAME}Routes() {
    route("/${NAME.toLowerCase()}") {
        get {
            // TODO: Implement
        }

        get("/{id}") {
            // TODO: Implement
        }
    }
}
```

### Live Templates

Add these live templates for common patterns:

#### `dbq` - Database Query

```kotlin
dbQuery {
    $END$
}
```

#### `kget` - Ktor GET Route

```kotlin
get("$PATH$") {
    $END$
}
```

#### `kpost` - Ktor POST Route

```kotlin
post("$PATH$") {
    val request = call.receive<$TYPE$>()
    $END$
}
```

---

## VS Code Setup (Frontend & CLI)

### Installation

1. **Download**: [VS Code](https://code.visualstudio.com/)
2. **Version**: Latest stable

### Required Extensions

Install these extensions:

```bash
# Core Development
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension bradlc.vscode-tailwindcss

# TypeScript/React
code --install-extension dsznajder.es7-react-js-snippets
code --install-extension formulahendry.auto-rename-tag
code --install-extension styled-components.vscode-styled-components

# Rust
code --install-extension rust-lang.rust-analyzer
code --install-extension tamasfe.even-better-toml
code --install-extension serayuzgur.crates

# General
code --install-extension eamodio.gitlens
code --install-extension yzhang.markdown-all-in-one
code --install-extension redhat.vscode-yaml
```

### Workspace Settings

Create `.vscode/settings.json` in the project root (if not present):

```json
{
  "editor.formatOnSave": true,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "editor.codeActionsOnSave": {
    "source.fixAll.eslint": "explicit",
    "source.organizeImports": "explicit"
  },
  "typescript.preferences.importModuleSpecifier": "relative",
  "typescript.updateImportsOnFileMove.enabled": "always",
  "tailwindCSS.includeLanguages": {
    "typescript": "javascript",
    "typescriptreact": "javascript"
  },
  "tailwindCSS.experimental.classRegex": [
    ["cva\(([^)]*)\)", "["'`]([^"'`]*).*?["'`]"]
  ],
  "[typescript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[typescriptreact]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  "[rust]": {
    "editor.defaultFormatter": "rust-lang.rust-analyzer"
  },
  "rust-analyzer.linkedProjects": [
    "tools/sangita-cli/Cargo.toml"
  ],
  "files.exclude": {
    "**/node_modules": true,
    "**/target": true,
    "**/.gradle": true,
    "**/build": true
  }
}
```

### Launch Configurations

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "name": "Frontend: Dev Server",
      "type": "node",
      "request": "launch",
      "runtimeExecutable": "bun",
      "runtimeArgs": ["run", "dev"],
      "cwd": "${workspaceFolder}/modules/frontend/sangita-admin-web",
      "console": "integratedTerminal"
    },
    {
      "name": "Frontend: Debug Chrome",
      "type": "chrome",
      "request": "launch",
      "url": "http://localhost:5001",
      "webRoot": "${workspaceFolder}/modules/frontend/sangita-admin-web/src"
    },
    {
      "name": "Rust CLI: Debug",
      "type": "lldb",
      "request": "launch",
      "program": "${workspaceFolder}/tools/sangita-cli/target/debug/sangita-cli",
      "args": ["db", "status"],
      "cwd": "${workspaceFolder}",
      "sourceLanguages": ["rust"]
    }
  ]
}
```

### Tasks Configuration

Create `.vscode/tasks.json`:

```json
{
  "version": "2.0.0",
  "tasks": [
    {
      "label": "Frontend: Install",
      "type": "shell",
      "command": "bun install",
      "options": {
        "cwd": "${workspaceFolder}/modules/frontend/sangita-admin-web"
      },
      "group": "build"
    },
    {
      "label": "Frontend: Build",
      "type": "shell",
      "command": "bun run build",
      "options": {
        "cwd": "${workspaceFolder}/modules/frontend/sangita-admin-web"
      },
      "group": "build",
      "problemMatcher": ["$tsc"]
    },
    {
      "label": "Frontend: Lint",
      "type": "shell",
      "command": "bun run lint",
      "options": {
        "cwd": "${workspaceFolder}/modules/frontend/sangita-admin-web"
      },
      "group": "test"
    },
    {
      "label": "CLI: Build",
      "type": "shell",
      "command": "cargo build",
      "options": {
        "cwd": "${workspaceFolder}/tools/sangita-cli"
      },
      "group": "build",
      "problemMatcher": ["$rustc"]
    },
    {
      "label": "CLI: Test",
      "type": "shell",
      "command": "cargo test",
      "options": {
        "cwd": "${workspaceFolder}/tools/sangita-cli"
      },
      "group": "test",
      "problemMatcher": ["$rustc"]
    },
    {
      "label": "DB: Reset",
      "type": "shell",
      "command": "mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- db reset",
      "group": "none"
    },
    {
      "label": "Dev: Full Stack",
      "type": "shell",
      "command": "mise exec -- cargo run --manifest-path tools/sangita-cli/Cargo.toml -- dev --start-db",
      "group": "none",
      "isBackground": true
    }
  ]
}
```

---

## Android Studio Setup (Mobile)

### Installation

1. **Download**: [Android Studio](https://developer.android.com/studio) (Ladybug or later)
2. **Install**: Follow the setup wizard

### Required Plugins

Install via `Settings > Plugins`:

| Plugin | Purpose |
|--------|---------|
| Kotlin Multiplatform | KMP support |
| Compose Multiplatform | Compose UI for KMP |

### SDK Configuration

1. **Open SDK Manager**: `Tools > SDK Manager`
2. **Install**:
   - Android SDK Platform 35
   - Android SDK Build-Tools 35.0.0
   - Android Emulator
   - Android SDK Platform-Tools

### Project Import

1. **Open Android Studio**
2. **Select**: `Open an existing project`
3. **Navigate to**: `<project-root>/modules/mobile`
4. **Wait for**: Gradle sync to complete

### Run Configurations

#### Android App

```
Name: Sangita Android
Type: Android App
Module: modules.mobile.composeApp.main
Deploy: Default APK
```

#### iOS App (macOS only)

Requires Xcode installed:

```
Name: Sangita iOS
Type: iOS Application
Configuration: Debug
Destination: iPhone 15 Pro (simulator)
```

---

## Cursor IDE Setup (AI-Assisted)

For AI-assisted development with Cursor:

### Installation

1. **Download**: [Cursor](https://cursor.sh/)
2. **Version**: Latest

### Configuration

The project includes `.cursorrules` for AI context. Ensure it's loaded:

1. **Open Settings**: `Cmd + ,`
2. **Navigate to**: `Features > Cursor`
3. **Enable**: `Use .cursorrules file`

### Recommended Settings

```json
{
  "cursor.chat.defaultModel": "claude-3.5-sonnet",
  "cursor.composer.enabled": true,
  "cursor.ai.contextLength": "long"
}
```

---

## Common Configuration

### Git Hooks

Install pre-commit hooks:

```bash
# Install hooks (if using husky or similar)
bun install

# Or manually configure
git config core.hooksPath .githooks
```

### Environment Variables

Create local environment files:

```bash
# Database config (auto-loaded by mise)
# Already configured in config/postgres-local.env

# Backend local overrides (optional)
cp config/application.sample.toml config/application.local.toml
```

### EditorConfig

The project includes `.editorconfig` for consistent formatting across IDEs:

```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
indent_style = space
indent_size = 4
insert_final_newline = true
trim_trailing_whitespace = true

[*.{ts,tsx,js,jsx,json,yaml,yml}]
indent_size = 2

[*.md]
trim_trailing_whitespace = false
```

---

## Troubleshooting

### IntelliJ IDEA

| Issue | Solution |
|-------|----------|
| Gradle sync fails | `File > Invalidate Caches > Restart` |
| JDK not found | Re-run `mise install`, re-import project |
| Kotlin plugin outdated | Update via `Settings > Plugins > Updates` |
| Unresolved references | `Build > Rebuild Project` |

### VS Code

| Issue | Solution |
|-------|----------|
| ESLint not working | Run `bun install` in frontend directory |
| Rust analyzer fails | Check `rust-analyzer.linkedProjects` setting |
| TypeScript errors | Restart TS server: `Cmd+Shift+P > Restart TS Server` |
| Tailwind not suggesting | Check `tailwindCSS.includeLanguages` setting |

### Android Studio

| Issue | Solution |
|-------|----------|
| Gradle sync fails | `File > Sync Project with Gradle Files` |
| SDK missing | Install via SDK Manager |
| Emulator slow | Enable hardware acceleration |
| KMP issues | Update Kotlin Multiplatform plugin |

---

## Keyboard Shortcuts Reference

### IntelliJ IDEA (macOS)

| Action | Shortcut |
|--------|----------|
| Find file | `Cmd + Shift + O` |
| Find in files | `Cmd + Shift + F` |
| Go to definition | `Cmd + B` |
| Refactor rename | `Shift + F6` |
| Run | `Ctrl + R` |
| Debug | `Ctrl + D` |
| Build | `Cmd + F9` |
| Format code | `Cmd + Alt + L` |

### VS Code (macOS)

| Action | Shortcut |
|--------|----------|
| Find file | `Cmd + P` |
| Find in files | `Cmd + Shift + F` |
| Go to definition | `F12` |
| Rename symbol | `F2` |
| Run task | `Cmd + Shift + B` |
| Open terminal | `Ctrl + `` |
| Format document | `Shift + Alt + F` |
| Quick fix | `Cmd + .` |

---

## Next Steps

After IDE setup:

1. Follow [Getting Started](./getting-started.md) for project setup
2. Review [Troubleshooting](./troubleshooting.md) for common issues
3. Check [Conductor Tracks](../../conductor/tracks.md) for current work
4. Read [Architecture Overview](../02-architecture/README.md)

---

## See Also

- [Getting Started Guide](./getting-started.md)
- [Troubleshooting Guide](./troubleshooting.md)
- [Development Workflow](./getting-started.md#4-development-workflow)
- [Tech Stack Reference](../02-architecture/tech-stack.md)