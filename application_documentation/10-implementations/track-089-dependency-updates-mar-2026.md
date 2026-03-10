| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |

# Purpose
Bundle together the Q1 March 2026 dependency updates that span across the frontend, backend, and extraction worker. This changeset directly applies the versions cataloged previously in `TRACK-088: Current Versions & Gitignore Update`.

# Changes Summary
| File | Change |
|:---|:---|
| `gradle/libs.versions.toml` | Bumped PostgreSQL Driver (42.7.10), Logback (1.5.32), Jsoup (1.22.1), and Caffeine (3.2.3) |
| `modules/frontend/sangita-admin-web/package.json` | Bumped React Query (5.90.21), React Router (7.13.1), and TailwindCSS (4.2.1) |
| `tools/db-migrate/pyproject.toml` | Updated lockfiles and pinned extraction dependencies |
| `tools/krithi-extract-enrich-worker/Dockerfile` | Updated python and tooling base versions |
| `tools/krithi-extract-enrich-worker/pyproject.toml` | Updated dependency versions |
| `tools/krithi-extract-enrich-worker/uv.lock` | Regenerated lock file with updated hashes and dependencies |
