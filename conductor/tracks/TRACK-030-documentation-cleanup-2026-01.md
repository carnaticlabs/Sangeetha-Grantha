| Metadata | Value |
|:---|:---|
| **Status** | Planned |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Sangita Grantha Architect |

# TRACK-030: Documentation Cleanup 2026-01

## 1. Goal

Perform a structured documentation cleanup to eliminate version drift, fix broken links, restore metadata compliance, and align active docs with current code and toolchain state.

## 2. Context

- Audit report: `application_documentation/00-meta/documentation-audit-report-2026-01-30.md`
- Scan observations: `application_documentation/00-meta/documentation-scan-observations-2026-01-30.md`
- Execution checklist: `application_documentation/00-meta/documentation-cleanup-checklist-2026-01-30.md`

## 3. Requirements

- All edited docs must include the required metadata header and updated Last Updated date.
- All internal doc links must be relative and valid.
- Version references must match the designated sources of truth.
- Archive docs must be marked Archived/Deprecated and moved if required.

## 4. Implementation Plan

### Phase 0: Decisions
- [ ] Decide canonical Rust/Bun toolchain sources (resolve `.mise.toml` vs `rust-toolchain.toml` conflict).
- [ ] Decide whether to implement the auto-generated versions file.
- [ ] Confirm archive target folder for historical reports.

### Phase 1: Metadata and Status Hygiene
- [ ] Add missing metadata headers.
- [ ] Update archive statuses for all docs in archive folders.

### Phase 2: Link Integrity
- [ ] Fix missing or incorrect paths.
- [ ] Update line-reference link format to GitHub-compatible anchors.

### Phase 3: Version Alignment
- [ ] Update backend versions (Kotlin/Ktor/Exposed/DateTime).
- [ ] Update frontend versions (React/TS/Vite/Tailwind/Router).
- [ ] Align toolchain versions.

### Phase 4: Content Drift
- [ ] Update frontend UI specs to match current pages/components.

### Phase 5: Index and Presentation
- [ ] Update `application_documentation/README.md` index coverage.
- [ ] Fix heading capitalization and minor presentation issues.

### Phase 6: Archival Moves
- [ ] Move or reclassify historical reports and superseded docs.

### Phase 7: Automation (Optional)
- [ ] Add version/link validation tooling if approved.

## 5. Definition of Done

- All checklist items in `documentation-cleanup-checklist-2026-01-30.md` are complete.
- No broken links in `application_documentation/`.
- No version drift relative to designated sources of truth.
- Archive folder docs are marked Archived/Deprecated.

## 6. Progress Log

- 2026-01-30: Track created with checklist and observation report references.
