# ADR-001: Spec-Driven Documentation Architecture

| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Documentation Index](../../README.md)<br>- [Admin Web Prd](../../01-requirements/admin-web/prd.md)<br>- [Mobile App Prd](../../01-requirements/mobile/prd.md) |

## Context

Documentation was fragmented across module-level folders, duplicated PRDs, and ad-hoc notes that became stale. Engineers and stakeholders struggled to identify the canonical source of truth for product requirements, API contracts, database specs, and operational runbooks.

The proposal recommended consolidating documentation into a spec-driven structure under `application_documentation/`, backed by a central index and consistent front matter.

## Decision

Adopt a spec-driven documentation architecture with:
- Canonical docs grouped by domain (requirements, API, database, backend, frontend, ops, QA, decisions, AI).
- Front matter metadata (Title, Status, Version, Last Updated, Owners, Related Docs) on all authoritative specs.
- Archived legacy docs stored under `application_documentation/archive/` with tombstones pointing back to canonical files.
- Module-level stubs that link to central documents instead of duplicating content.

## Rationale

This structure provides:
- **Single Source of Truth**: All documentation lives in one place with clear ownership.
- **Version Control**: Documentation changes are tracked alongside code changes in git.
- **Discoverability**: Central index (`application_documentation/README.md`) provides clear navigation.
- **Consistency**: Standardized front matter enables tooling and automation.
- **Maintainability**: Legacy content is archived but preserved, reducing broken links.

## Implementation Details

The documentation structure is organized as follows:

```
application_documentation/
├── 00-meta/              # Standards, retention plans, quick references
├── 01-requirements/      # PRDs, domain models, feature requirements
├── 02-architecture/      # System design, tech stack, ADRs, diagrams
├── 03-api/              # API contracts, integration specs
├── 04-database/         # Schema, migrations, audit logs
├── 05-frontend/         # UI specs for admin web and mobile
├── 06-backend/          # Backend patterns, security, mutation handlers
├── 07-quality/          # Test plans, coverage reports, quality metrics
├── 08-operations/       # Config, runbooks, operational procedures
├── 09-ai/               # AI integration docs, knowledge bases
└── archive/             # Legacy documentation with tombstones
```

**Front Matter Format:**
All authoritative documents include front matter with:
- Status (Current, Draft, Deprecated)
- Version (semantic versioning)
- Last Updated (ISO date)
- Owners (team or individual)
- Related Documents (cross-references)

**Current Status**: ✅ **Implemented** - The documentation structure is in place and actively maintained. All new documentation follows this structure, and legacy content has been archived.

## Consequences

### Positive
- Contributors have a single index (`application_documentation/README.md`) for navigation.
- Documentation changes are co-located with code changes and reviewed alongside PRs.
- Legacy locations remain as lightweight tombstones, reducing broken links while steering readers to canonical specs.
- Future doc tooling (e.g., MkDocs) can ingest the structured layout without large refactors.
- Clear ownership and versioning enable better maintenance and collaboration.

### Negative
- Initial migration required effort to reorganize existing documentation.
- Contributors must learn the structure (mitigated by clear index and examples).

### Neutral
- Documentation is version-controlled alongside code (no separate wiki or docs site needed).

## Follow-up

- ✅ Documentation structure implemented and maintained
- ⏳ Automate linting to ensure new docs include required front matter (planned)
- ⏳ Explore CI checks that verify cross-links remain valid when files move (planned)

## References

- [Documentation Index](../../README.md)
- [Standards Document](../../00-meta/standards.md)
