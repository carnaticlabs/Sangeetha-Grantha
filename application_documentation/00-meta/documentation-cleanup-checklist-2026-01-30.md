| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Sangita Grantha Architect |

# Documentation Cleanup Checklist (2026-01-30)

---

## Purpose

Definitive actionable checklist derived from:
- [`documentation-audit-report-2026-01-30.md`](./documentation-audit-report-2026-01-30.md)
- [`current-versions.md`](./current-versions.md) (auto-generated source of truth)

---

## Completed Actions

### Phase 0: Centralized Version Management ✅ DONE

- [x] **Created `sangita-cli docs sync-versions`** - Rust CLI command that extracts versions from source files
- [x] **Generated [`current-versions.md`](./current-versions.md)** - Single source of truth for all version information
- [x] **Updated `tools/sangita-cli/README.md`** - Documented new `docs` command and removed hardcoded versions
- [x] **Confirmed canonical sources:**
  - Backend/Mobile: `gradle/libs.versions.toml`
  - Frontend: `modules/frontend/sangita-admin-web/package.json`
  - Toolchain: `.mise.toml`

---

## Remaining Actions

### Phase 1: Update Documentation to Reference `current-versions.md`

**Priority: High** | **Effort: Medium**

These files hardcode versions and should be updated to reference `current-versions.md`:

#### Agent Configuration Files (Critical - causes AI confusion)

- [ ] **`CLAUDE.md`**
  - Remove hardcoded: Java 25, Rust 1.92.0, Bun 1.3.0
  - Add: "For current versions, see [Current Versions](application_documentation/00-meta/current-versions.md)"
  - Fix: Remove reference to non-existent `development-setup.md`

- [ ] **`.claude-context.md`**
  - Remove entire "Tech Stack Versions" section
  - Add link to `current-versions.md`
  - Fix incorrect paths:
    - `application_documentation/backend/architecture.md` → `application_documentation/02-architecture/backend-system-design.md`
    - `application_documentation/requirements/prd/admin-web-prd.md` → `application_documentation/01-requirements/admin-web/prd.md`

- [ ] **`.goose-context.md`**
  - Remove hardcoded versions (Ktor 3.3.1, Exposed 1.0.0-rc-2, etc.)
  - Add link to `current-versions.md`

- [ ] **`.cursorrules`**
  - Remove hardcoded versions
  - Fix path: `database/rust/` → `tools/sangita-cli/`
  - Add link to `current-versions.md`

- [ ] **`.ai-quick-reference.md`**
  - Replace all hardcoded versions with link to `current-versions.md`

#### Core Documentation Files

- [ ] **`README.md` (project root)**
  - Replace hardcoded toolchain versions with link to `current-versions.md`
  - Keep brief summary, reference details

- [ ] **`application_documentation/02-architecture/tech-stack.md`**
  - Replace all hardcoded versions with link to `current-versions.md`
  - Keep structure/organization, just remove specific version numbers
  - Update kotlinx-datetime: 0.6.1 → (reference current-versions.md)

- [ ] **`application_documentation/00-onboarding/getting-started.md`**
  - Update JDK requirement: "21+" → "See [Current Versions](../00-meta/current-versions.md)"
  - Add link to `current-versions.md` in Prerequisites section

---

### Phase 2: Fix Broken References

**Priority: High** | **Effort: Low**

- [ ] **`CLAUDE.md:95`** - References non-existent `development-setup.md`
  - Option A: Change to `getting-started.md`
  - Option B: Create `development-setup.md` as alias

- [ ] **`application_documentation/00-onboarding/ide-setup.md`**
  - Fix link to missing `../02-architecture/overview.md`
  - Should reference `../02-architecture/README.md` or `backend-system-design.md`

- [ ] **`application_documentation/archive/database-archive/README.md`**
  - Fix link to `Database Design.pdf` (encode space: `Database%20Design.pdf`)

---

### Phase 3: Index and Presentation Fixes

**Priority: Medium** | **Effort: Low**

#### Add Missing Entries to Main Index

Update `application_documentation/README.md` to include:

- [ ] `00-onboarding/ide-setup.md`
- [ ] `00-onboarding/troubleshooting.md`
- [ ] `02-architecture/diagrams/c4-model.md`
- [ ] `03-api/api-examples.md`
- [ ] `03-api/openapi-sync.md`
- [ ] `07-quality/qa/e2e-testing.md`
- [ ] `07-quality/qa/performance-testing.md`
- [ ] `08-operations/deployment.md`
- [ ] `08-operations/monitoring.md`
- [ ] `08-operations/runbooks/incident-response.md`
- [ ] `08-operations/runbooks/database-runbook.md`

#### Fix README Headings

- [ ] `application_documentation/03-api/README.md` - Change "03 Api" → "03 API"
- [ ] `application_documentation/07-quality/qa/README.md` - Change "Qa" → "QA"

---

### Phase 4: Archival Moves

**Priority: Medium** | **Effort: Low**

#### Move to `archive/quality-reports/`

Create directory `application_documentation/archive/quality-reports/` and move:

- [ ] `07-quality/bulk-import-tracks-technical-review-2026-01-23.md`
- [ ] `07-quality/implementation-summary-2026-01-23.md`
- [ ] `07-quality/implementation-summary-remaining-work-2026-01-23.md`
- [ ] `07-quality/track-008-013-implementation-summary-2026-01-23.md`
- [ ] `07-quality/track-010-implementation-summary-2026-01-23.md`

#### Move to `01-requirements/features/bulk-import/archive/`

- [ ] `01-requirements/features/bulk-import/CONSOLIDATION-SUMMARY.md`
- [ ] `01-requirements/features/bulk-import/CONSOLIDATION-SUMMARY-goose.md`
- [ ] `01-requirements/features/bulk-import/bulk-import-orchestration-ops-plan-goose.md`

#### Update Metadata on Archived Files

- [ ] Set `Status: Archived` in metadata table for all files in `archive/` directories

---

### Phase 5: Metadata Hygiene

**Priority: Low** | **Effort: Low**

Add required metadata table to files missing it:

- [ ] `application_documentation/00-meta/documentation-cleanup-walkthrough.md`
- [ ] `application_documentation/07-quality/reports/frontend-pages-refactor-checklist.md`
- [ ] `application_documentation/07-quality/reports/frontend-pages-code-review.md`
- [ ] `modules/frontend/sangita-admin-web/README.md`

---

### Phase 6: CI Automation

**Priority: Low** | **Effort: Medium**

#### Add Version Sync Validation

Add to GitHub Actions workflow (`.github/workflows/ci.yml` or similar):

```yaml
- name: Verify version sync
  run: |
    cargo run --manifest-path tools/sangita-cli/Cargo.toml -- docs sync-versions --check
```

#### Future: Link Validation

- [ ] Implement `sangita-cli docs validate-links` command
- [ ] Add to CI pipeline

---

## Quick Reference: Version Sources of Truth

| Domain | Source File | CLI to Regenerate |
|--------|-------------|-------------------|
| Backend/Mobile (Kotlin) | `gradle/libs.versions.toml` | `sangita-cli docs sync-versions` |
| Frontend (React/TS) | `modules/frontend/sangita-admin-web/package.json` | `sangita-cli docs sync-versions` |
| Toolchain (Java/Rust/Bun) | `.mise.toml` | `sangita-cli docs sync-versions` |
| **Documentation** | `application_documentation/00-meta/current-versions.md` | Auto-generated |

---

## Workflow for Future Version Updates

1. **Update source file** (`gradle/libs.versions.toml`, `package.json`, or `.mise.toml`)
2. **Regenerate versions doc**: `sangita-cli docs sync-versions`
3. **Commit both files** together
4. **CI validates** sync is correct

---

## Summary Statistics

| Category | Count | Status |
|----------|-------|--------|
| Agent config files to update | 5 | Pending |
| Core docs to update | 3 | Pending |
| Broken references to fix | 3 | Pending |
| Index entries to add | 11 | Pending |
| Files to archive | 8 | Pending |
| Files needing metadata | 4 | Pending |
| **Centralized versioning** | ✅ | **DONE** |

---

## References

- [Documentation Audit Report](./documentation-audit-report-2026-01-30.md)
- [Current Versions (Auto-Generated)](./current-versions.md)
- [Documentation Standards](./standards.md)
- [sangita-cli README](../../tools/sangita-cli/README.md)
