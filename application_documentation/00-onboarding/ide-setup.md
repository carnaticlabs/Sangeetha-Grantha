| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.5.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# IDE and editor setup

---

First complete [local setup](./getting-started.md). IDE configuration should use the repository's toolchain and module boundaries rather than a parallel set of language versions or migration commands.

## Kotlin backend and shared mobile code

Open the repository as a Gradle project in IntelliJ IDEA or Android Studio. Use the JDK required by the [Gradle version/toolchain configuration](../../gradle/libs.versions.toml) and [Current Versions](../00-meta/current-versions.md). Let Gradle import the backend, shared, and native Android modules.

Useful tasks are `:modules:backend:api:run`, API/DAL tests, and the shared `mobile-data`/`presentation` JVM tests. Run configurations should start from the repository root so backend relative configuration paths resolve correctly.

## Android and iOS

Android Studio supplies the Android SDK and emulator environment for `modules/mobile/androidApp`. Use the existing app configuration and journey harness rather than inventing a new host.

On macOS, use Xcode for `modules/mobile/iosApp`. `make mobile-ios` runs the repository build wrapper; device journey tests additionally require an explicit target identifier. See [mobile implementation](../05-frontend/mobile/README.md).

## React/TypeScript

Open [the admin module](../../modules/frontend/sangita-admin-web) in your editor and use the workspace TypeScript version. Install with Bun. Use the checked-in ESLint, TypeScript, Vite, Vitest and Playwright configuration.

```bash
cd modules/frontend/sangita-admin-web
bun install --frozen-lockfile
bun run typecheck
bun run test:unit
```

Use `bun run dev` for the development server. Its API routing/environment rules are documented in [configuration](../08-operations/config.md).

## Python worker

Use the interpreter/environment created by uv for [the worker package](../../tools/krithi-extract-enrich-worker/README.md), with the required Python version and locked development extras. Match the repository's Ruff and mypy settings. Set the working directory to the worker package for its local CLI and `.env` loading behavior.

## Database and Markdown

Connect database tooling to the intended local database, using your local environment rather than committed credentials. Apply schema changes through Flyway/Make, not the SQL editor or an archived runner. Test against disposable databases where appropriate.

Enable Markdown and Mermaid preview for docs. Run `make check-docs` after link/path changes; that gate validates relative file targets, not external URLs or rendered diagram layout.

## Shared repository rules

Read [CLAUDE.md](../../CLAUDE.md) and the appropriate layer skill before code changes. Follow [Conductor](../../conductor/tracks.md) for existing work and [quality gates](../07-quality/README.md) for verification. Keep IDE-generated files and local environment values out of commits.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
