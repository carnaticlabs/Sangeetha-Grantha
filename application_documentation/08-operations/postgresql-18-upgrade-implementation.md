| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-28 |
| **Author** | Sangeetha Grantha Team |

# PostgreSQL 18 Upgrade Implementation Summary

## Purpose
This document summarizes the changes made to upgrade the project's PostgreSQL database from version 15 to 18.3 across all environments (local, staging, production), addressing TRACK-072.

## Categorization of Changes
- **Infrastructure:** Docker Compose image and volume mount updates for PG18 layout.
- **Database:** UUID v7 defaults introduced for all tables via migration `37__pg18_uuidv7_defaults.sql`.
- **Documentation:** Updated runbooks, tech stack definitions, and agent rules to reflect PG 18+.
- **Tooling:** Updated `test.rs` in `sangita-cli` to reflect any test environment changes.

## Code Changes Summary

| File | Change |
|:---|:---|
| `compose.yaml` | Bumped postgres image to `18`, updated data volume mount point to `/var/lib/postgresql` |
| `database/migrations/37__pg18_uuidv7_defaults.sql` | Added migration to switch 27 tables' UUID primary key defaults to `uuidv7()` |
| `application_documentation/02-architecture/tech-stack.md` | Updated PostgreSQL version to 18+ |
| `application_documentation/08-operations/deployment.md` | Updated Cloud SQL reference to `POSTGRES_18` |
| `application_documentation/08-operations/runbooks/database-runbook.md` | Updated version table from 15 to 18 |
| `tools/sangita-cli/src/commands/test.rs` | Updated toolchain commands/tests for PG18 |
| `.cursorrules` / `CLAUDE.md` | Updated PostgreSQL references to 18+ |
| `conductor/tracks/TRACK-072-postgresql-18-upgrade.md` | Created track file for PostgreSQL 18 upgrade |
| `conductor/tracks.md` | Registered TRACK-072 |

Ref: application_documentation/08-operations/postgresql-18-upgrade-implementation.md
