| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Documentation Scan Observations (2026-01-30)

---

## Scope

- `application_documentation/` (161 markdown files)
- Project-level `README.md` files (3)
- Link validation for relative links inside documentation
- Version drift validation against:
  - `gradle/libs.versions.toml`
  - `modules/frontend/sangita-admin-web/package.json`
  - `.mise.toml`
  - `tools/sangita-cli/rust-toolchain.toml`

## Summary of Findings

- **Metadata headers missing**: 3 docs under `application_documentation/` + 1 project README
- **Relative link issues**: 23 detected (22 actionable, 1 false-positive inside a code block)
- **Version drift**: multiple docs still reference older backend, frontend, and toolchain versions
- **Toolchain mismatch**: Rust/Bun versions differ across `.mise.toml` and `rust-toolchain.toml`
- **Archive status drift**: 22 docs in archive folders still marked Active/Draft
- **Index drift**: main documentation index missing several recently added docs
- **Content drift**: frontend UI specs mention pages/components not present in code
- **Minor presentation issues**: capitalization inconsistencies in headings

## Metadata Header Gaps

### Missing header table (must be fixed)

- `application_documentation/00-meta/documentation-cleanup-walkthrough.md`
- `application_documentation/07-quality/reports/frontend-pages-refactor-checklist.md`
- `application_documentation/07-quality/reports/frontend-pages-code-review.md`
- `modules/frontend/sangita-admin-web/README.md`

## Link Integrity Issues (Actionable)

### Broken or incorrect references

- `application_documentation/00-onboarding/ide-setup.md`
  - `../02-architecture/overview.md` (target does not exist)
- `application_documentation/07-quality/implementation-summary-2026-01-23.md`
  - Uses `:line` references in links (e.g., `BulkImport.tsx:231-312`) which do not resolve in GitHub-style markdown
- `application_documentation/07-quality/track-008-013-implementation-summary-2026-01-23.md`
  - `BulkImportWorkerService.kt` paths are outdated (moved to `services/bulkimport/BulkImportWorkerServiceImpl.kt`)
  - `ImportDtos.kt` file renamed to `ImportDto.kt` under `model/import/`
- `application_documentation/07-quality/track-010-implementation-summary-2026-01-23.md`
  - `BulkImportWorkerService.kt` paths are outdated (moved to `services/bulkimport/BulkImportWorkerServiceImpl.kt`)
- `application_documentation/archive/database-archive/README.md`
  - `Database Design.pdf` link fails due to unescaped space

### Known false-positive (no action needed)

- `application_documentation/00-onboarding/ide-setup.md` includes a JSON regex inside a code block that matches the link scanner pattern.

## Version Drift (Docs vs. Sources of Truth)

### Backend versions (source: `gradle/libs.versions.toml`)

- Kotlin: **2.3.0**
- Ktor: **3.4.0**
- Exposed: **1.0.0**
- Kotlinx DateTime: **0.7.1**

Docs still referencing old versions:
- `application_documentation/01-requirements/features/searchable-deity-temple-management.md` (Kotlin 2.2.20, Ktor 3.3.1, Exposed 1.0.0-rc-2)
- `application_documentation/01-requirements/features/database-layer-optimization.md` (Exposed 1.0.0-rc-4)
- `application_documentation/02-architecture/tech-stack.md` (Kotlinx DateTime 0.6.1)
- `application_documentation/archive/graph-explorer/*.md` (Ktor 3.3.1)
- `application_documentation/01-requirements/features/bulk-import/01-strategy/master-analysis.md` (Ktor 3.3.1)

### Frontend versions (source: `modules/frontend/sangita-admin-web/package.json`)

- React: **19.2.3**
- TypeScript: **5.8.2**
- Vite: **6.2.0**
- Tailwind CSS: **4.1.18**
- React Router: **7.11.0**

Docs still referencing older values:
- `application_documentation/02-architecture/decisions/ADR-002-frontend-architecture.md`
- `application_documentation/05-frontend/admin-web/ui-specs.md`
- `application_documentation/03-api/ui-to-api-mapping.md`
- `application_documentation/archive/react_admin_web_specifications.md`
- `README.md`

### Toolchain versions (conflict)

- `.mise.toml`: Rust **1.93.0**, Bun **1.3.6**
- `tools/sangita-cli/rust-toolchain.toml`: Rust **1.92.0**

Docs currently referencing 1.92.0/1.3.0 need alignment after deciding the canonical source.

## Archive Status Drift

Docs already under `application_documentation/archive/` and `application_documentation/01-requirements/features/bulk-import/archive/` still show **Status: Active/Draft**. These should be switched to **Archived** (or **Deprecated**) in metadata.

## Content Drift (Frontend Specs)

`application_documentation/05-frontend/admin-web/ui-specs.md` lists pages that are no longer present (`KrithiDetail.tsx`, `KrithiEdit.tsx`, `NotationEditor.tsx`) and omits current pages such as:

- `AutoApproveQueue.tsx`
- `ImportsPage.tsx`
- `RolesPage.tsx`, `TagsPage.tsx`, `UsersPage.tsx`

## Index Gaps

`application_documentation/README.md` is missing links to recent docs (API examples, OpenAPI sync, C4 model, E2E/performance testing, deployment/monitoring, incident response, database runbook, IDE setup, troubleshooting).

## Minor Presentation Issues

- `application_documentation/03-api/README.md` heading should be `API` (not `Api`)
- `application_documentation/07-quality/qa/README.md` heading should be `QA` (not `Qa`)

## Recommended Next Actions

1. Resolve toolchain source of truth for Rust/Bun, then align all references.
2. Fix missing headers and update archive statuses.
3. Repair broken links and update line-reference formats.
4. Update frontend specs and tech stack references to actual dependency versions.
5. Update the documentation index to include missing entries.

## Related References

- `application_documentation/00-meta/documentation-audit-report-2026-01-30.md`
- `application_documentation/00-meta/documentation-quality-evaluation-2026-01-29.md`
- `application_documentation/00-meta/standards.md`