# ADR-001 — Spec-Driven Documentation Architecture

> **Status**: Accepted | **Version**: ? | **Last Updated**: ?
> **Owners**: Architecture Working Group

**Related Documents**
- [Admin Web Prd](../requirements/prd/admin-web-prd.md)
- [Mobile App Prd](../requirements/prd/mobile-app-prd.md)
- [Readme](../../archive/requirements-spec/README.md)

# Context

Documentation was fragmented across module-level folders, duplicated PRDs, and ad-hoc notes that became stale. Engineers and stakeholders struggled to identify the canonical source of truth for product requirements, API contracts, database specs, and operational runbooks.

The `app-documentation-re-org-suggestions.md` proposal recommended consolidating documentation into a spec-driven structure under `application_documentation/`, backed by a central index and consistent front matter.

# Decision

Adopt a spec-driven documentation architecture with:
- Canonical docs grouped by domain (requirements, API, database, backend, frontend, ops, QA, decisions).
- Front matter metadata (Title, Status, Version, Last Updated, Owners, Related Docs) on all authoritative specs.
- Archived legacy docs stored under `application_documentation/archive/` with tombstones pointing back to canonical files.
- Module-level stubs that link to central documents instead of duplicating content.

# Consequences

- Contributors have a single index (`application_documentation/README.md`) for navigation.
- Documentation changes are co-located with code changes and reviewed alongside PRs.
- Legacy locations remain as lightweight tombstones, reducing broken links while steering readers to canonical specs.
- Future doc tooling (e.g., MkDocs) can ingest the structured layout without large refactors.

# Follow-up

- Automate linting to ensure new docs include required front matter.
- Explore CI checks that verify cross-links remain valid when files move.
