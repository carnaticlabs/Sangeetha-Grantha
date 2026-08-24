---
name: sangita-restart-on-kotlin-change
description: Restart the Sangita Grantha stack after modifying Kotlin backend, shared-domain, or Python extraction-worker code. Use when editing any .kt file under modules/backend/ or modules/shared/, any .py file under tools/krithi-extract-enrich-worker/, or when the user asks to restart the app.
---

# Restart Sangita after backend or worker changes

Kotlin (API, DAL, shared domain) and the Python extraction worker run in Docker Compose. Those processes do not pick up source edits until the stack is restarted. Frontend-only edits under `modules/frontend/` hot-reload; do not bounce the stack for those.

**Do not** use the archived Rust CLI (`tools/sangita-cli`, `cargo run -- dev`). The interface is Make ([CLAUDE.md](../../../CLAUDE.md), skill `monorepo-orchestration`).

## When to restart

- `modules/backend/**/*.kt`
- `modules/shared/**/*.kt`
- `tools/krithi-extract-enrich-worker/**/*.py`
- `tools/krithi-extract-enrich-worker/Dockerfile`
- `tools/krithi-extract-enrich-worker/pyproject.toml`

## Procedure

```bash
make dev-down
make dev
```

Wait until the backend answers on `http://localhost:8080/health` and the admin UI on `http://localhost:5001`. Prefer the named launch configs in `.claude/launch.json` (`full-stack`, `backend`, `frontend`) over raw background shells.

If a container still serves old code: rebuild that service (`docker compose --profile dev build <service>`), then `make dev` again; clear Gradle caches before assuming the edit never compiled.
