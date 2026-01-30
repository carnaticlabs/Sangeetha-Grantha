| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 3.0.0 |
| **Last Updated** | 2026-01-30 |
| **Author** | Claude Code Documentation Audit |

# Sangita Grantha Documentation Audit Report

## Executive Summary

This comprehensive audit reviews the documentation across the Sangita Grantha project to identify areas needing updates, archival candidates, broken links, version inconsistencies, and opportunities to achieve best-in-class software engineering documentation practices.

**Overall Assessment: B+ (85/100)**

The documentation demonstrates strong foundations with a recent quality improvement initiative (TRACK-028) that brought the score from 7.5/10 to 9.0/10. The most critical issue identified was version duplication across 14+ files.

### Implementation Completed

**Centralized Version Management** has been implemented as a Rust CLI command:
- Created `sangita-cli docs sync-versions` - extracts versions from source files
- Generated [`current-versions.md`](./current-versions.md) - single source of truth for documentation
- Integrated into the existing `sangita-cli` toolchain (part of mise-managed development environment)
- All documentation should now reference this file instead of hardcoding versions

---

## 1. Centralized Version Management Strategy (Critical Recommendation)

### 1.1 The Problem: Version Duplication Anti-Pattern

Currently, version numbers are **duplicated across 14+ files**, leading to:
- Inevitable drift when dependencies are updated
- Maintenance burden to update multiple files
- Inconsistent information confusing developers and AI assistants
- Already visible: 20+ version mismatches identified

### 1.2 Sources of Truth (Already Exist)

The project already has three canonical sources for version information:

| Domain | Source of Truth | Location |
|--------|-----------------|----------|
| **Backend/Mobile (Kotlin/JVM)** | Gradle Version Catalog | `gradle/libs.versions.toml` |
| **Frontend (React/Node)** | Package manifest | `modules/frontend/sangita-admin-web/package.json` |
| **Development Toolchain** | Mise configuration | `.mise.toml` |

### 1.3 Current Actual Versions (From Sources of Truth)

#### From `gradle/libs.versions.toml`:
```toml
kotlin = "2.3.0"
ktor = "3.4.0"
exposed = "1.0.0"
kotlinxCoroutinesCore = "1.10.2"
kotlinxDatetime = "0.7.1"
kotlinxSerializationJson = "1.10.0"
compose = "1.10.0"
agp = "9.0.0"
postgresql = "42.7.9"
hikaricp = "7.0.2"
logback = "1.5.25"
jwt = "4.5.0"
koin = "3.5.6"
shadow = "9.2.2"
```

#### From `package.json`:
```json
"react": "^19.2.3"
"react-dom": "^19.2.3"
"react-router-dom": "^7.11.0"
"typescript": "~5.8.2"
"vite": "^6.2.0"
"tailwindcss": "^4.1.18"
"@tanstack/react-query": "^5.90.20"
```

#### From `.mise.toml`:
```toml
java = "temurin-25"
rust = "1.93.0"
bun = "1.3.6"
docker-compose = "latest"
```

### 1.4 Recommended Solution: Reference-Based Documentation

**Instead of duplicating versions, documentation should reference the source files.**

#### Option A: Direct Reference (Recommended)

Replace hardcoded versions with references:

```markdown
## Tech Stack

### Backend
- **Kotlin**: See `gradle/libs.versions.toml` → `kotlin`
- **Ktor**: See `gradle/libs.versions.toml` → `ktor`
- **Exposed**: See `gradle/libs.versions.toml` → `exposed`

### Frontend
- **React**: See `package.json` → `dependencies.react`
- **Vite**: See `package.json` → `devDependencies.vite`

### Toolchain
- **Java**: See `.mise.toml` → `tools.java`
- **Rust**: See `.mise.toml` → `tools.rust`
```

#### Option B: Auto-Generated Version Summary

Create a single auto-generated file that extracts versions:

**`application_documentation/00-meta/current-versions.md`** (auto-generated)

```markdown
<!-- AUTO-GENERATED: Do not edit manually. Run `./scripts/sync-versions.sh` -->
| Component | Version | Source |
|-----------|---------|--------|
| Kotlin | 2.3.0 | gradle/libs.versions.toml |
| Ktor | 3.4.0 | gradle/libs.versions.toml |
| React | 19.2.3 | package.json |
| Java | temurin-25 | .mise.toml |
...
```

All other documentation files then link to this single file.

#### Option C: CI-Enforced Validation

Add a GitHub Action that:
1. Extracts versions from source files
2. Scans all markdown files for version references
3. Fails if any hardcoded version doesn't match source of truth

### 1.5 Files That Should Reference `current-versions.md`

These files currently hardcode versions and should be refactored to reference [current-versions.md](./current-versions.md):

| File | Current Issue | Action Required |
|------|---------------|-----------------|
| `CLAUDE.md` | Hardcodes Java 25, Rust 1.92.0, Bun 1.3.0 | Add link: "See [Current Versions](application_documentation/00-meta/current-versions.md)" |
| `.claude-context.md` | Hardcodes 15+ versions (many outdated) | Replace version section with link to `current-versions.md` |
| `.goose-context.md` | Hardcodes 15+ versions (many outdated) | Replace version section with link to `current-versions.md` |
| `.cursorrules` | Hardcodes 10+ versions | Replace version section with link to `current-versions.md` |
| `.ai-quick-reference.md` | Hardcodes all versions | Replace with link to `current-versions.md` |
| `02-architecture/tech-stack.md` | Hardcodes all versions | Replace with link to `current-versions.md` |
| `README.md` | Hardcodes toolchain versions | Add link to `current-versions.md` |
| `00-onboarding/getting-started.md` | Hardcodes versions | Add link to `current-versions.md`|

### 1.6 Implementation Completed

The version sync has been implemented as a **Rust CLI command** in `sangita-cli`, consistent with the project's tooling architecture:

1. **`sangita-cli docs sync-versions`** - Rust command that:
   - Extracts versions from `gradle/libs.versions.toml` (Backend/Mobile)
   - Extracts versions from `modules/frontend/sangita-admin-web/package.json` (Frontend)
   - Extracts versions from `.mise.toml` (Development Toolchain)
   - Generates `application_documentation/00-meta/current-versions.md`
   - Supports `--check` flag for CI validation (exits with error if out of sync)

2. **Source Files:**
   - `tools/sangita-cli/src/commands/docs.rs` - Main implementation
   - Uses `serde` for TOML and JSON parsing
   - Uses `chrono` for timestamp generation

3. **`application_documentation/00-meta/current-versions.md`** - Auto-generated file containing:
   - Development Toolchain versions (Java, Rust, Bun)
   - Backend Stack versions (Kotlin, Ktor, Exposed, etc.)
   - Frontend Stack versions (React, Vite, Tailwind, etc.)
   - Mobile Stack versions
   - Cloud & External Services versions

4. **Usage:**
   ```bash
   # Generate/update current-versions.md
   sangita-cli docs sync-versions

   # Check if versions are in sync (for CI)
   sangita-cli docs sync-versions --check
   ```

5. **Future Enhancement:** `sangita-cli docs validate-links` (placeholder for link validation)

---

## 2. Current Version Inconsistencies (Evidence of the Problem)

### 2.1 Backend Versions

| Dependency | Source of Truth | Wrong Values Found In |
|------------|-----------------|----------------------|
| **Ktor** | `3.4.0` | `.claude-context.md` (3.3.1), `.goose-context.md` (3.3.1), `.ai-quick-reference.md` (3.3.1) |
| **Exposed** | `1.0.0` | `.claude-context.md` (1.0.0-rc-2), `.goose-context.md` (1.0.0-rc-2), `.ai-quick-reference.md` (1.0.0-rc-2) |
| **kotlinx-datetime** | `0.7.1` | `02-architecture/tech-stack.md` (0.6.1) |

### 2.2 Frontend Versions

| Dependency | Source of Truth (`package.json`) | Wrong Values Found In |
|------------|----------------------------------|----------------------|
| **React** | `19.2.3` | Multiple docs say `19.2.0` |
| **Vite** | `6.2.0` | `02-architecture/tech-stack.md` (7.1.7), `.cursorrules` (6.2.0 - actually correct!) |
| **Tailwind CSS** | `4.1.18` | `02-architecture/tech-stack.md` (3.4.13) |
| **TypeScript** | `5.8.2` | Some docs say `5.8.3` |
| **react-router-dom** | `7.11.0` | `.claude-context.md` says `6.28.0` |

### 2.3 Toolchain Versions

| Tool | Source of Truth (`.mise.toml`) | Wrong Values Found In |
|------|-------------------------------|----------------------|
| **Java** | `temurin-25` | `00-onboarding/getting-started.md` says "21+" |
| **Rust** | `1.93.0` | `CLAUDE.md` says `1.92.0`, `README.md` says `1.92.0` |
| **Bun** | `1.3.6` | `CLAUDE.md` says `1.3.0`, `README.md` says `1.3.0` |

---

## 3. Critical Issues Requiring Immediate Attention

### 3.1 Broken/Missing File References

| File | Issue | Severity |
|------|-------|----------|
| `CLAUDE.md:95` | References `application_documentation/00-onboarding/development-setup.md` which **does not exist** | 🔴 Critical |
| `.claude-context.md` | References `application_documentation/backend/architecture.md` - wrong path | 🔴 Critical |
| `.claude-context.md` | References `application_documentation/database/migrations-runner-rust.md` - doesn't exist | 🔴 Critical |
| `.cursorrules` | References `database/rust/` but actual path is `tools/sangita-cli/` | 🟡 High |

### 3.2 Path Corrections Needed

| File | Incorrect Path | Correct Path |
|------|---------------|--------------|
| `.claude-context.md` | `application_documentation/backend/architecture.md` | `application_documentation/02-architecture/backend-system-design.md` |
| `.claude-context.md` | `application_documentation/requirements/prd/admin-web-prd.md` | `application_documentation/01-requirements/admin-web/prd.md` |
| `.cursorrules` | `database/rust/` | `tools/sangita-cli/` |
| `.cursorrules` | `database/rust/migrations/` | `database/migrations/` |

---

## 4. Archival Candidates

### 4.1 Files That Should Be Archived

| File | Reason | Action |
|------|--------|--------|
| `07-quality/bulk-import-tracks-technical-review-2026-01-23.md` | Point-in-time review | Move to `archive/quality-reports/` |
| `07-quality/implementation-summary-2026-01-23.md` | Historical summary | Move to `archive/quality-reports/` |
| `07-quality/implementation-summary-remaining-work-2026-01-23.md` | Historical summary | Move to `archive/quality-reports/` |
| `07-quality/track-008-013-implementation-summary-2026-01-23.md` | Historical summary | Move to `archive/quality-reports/` |
| `07-quality/track-010-implementation-summary-2026-01-23.md` | Historical summary | Move to `archive/quality-reports/` |
| `01-requirements/features/bulk-import/CONSOLIDATION-SUMMARY.md` | Complete | Move to archive |
| `01-requirements/features/bulk-import/CONSOLIDATION-SUMMARY-goose.md` | Duplicate | Move to archive |
| `01-requirements/features/bulk-import/bulk-import-orchestration-ops-plan-goose.md` | Superseded | Move to archive |

### 4.2 Already Correctly Archived

- `01-requirements/features/bulk-import/archive/*.md` (12 files) ✅
- `archive/graph-explorer/*.md` (6 files) ✅
- `archive/database-archive/*.md` ✅
- `archive/requirements-spec/*.md` ✅
- `archive/ui-ux/*.md` ✅

---

## 5. Index File Coverage Gaps

The main `application_documentation/README.md` is missing links to documents created in TRACK-028:

| Missing Document | Path |
|------------------|------|
| API Examples | `03-api/api-examples.md` |
| OpenAPI Sync | `03-api/openapi-sync.md` |
| C4 Model | `02-architecture/diagrams/c4-model.md` |
| E2E Testing | `07-quality/qa/e2e-testing.md` |
| Performance Testing | `07-quality/qa/performance-testing.md` |
| Deployment | `08-operations/deployment.md` |
| Monitoring | `08-operations/monitoring.md` |
| Incident Response | `08-operations/runbooks/incident-response.md` |
| Database Runbook | `08-operations/runbooks/database-runbook.md` |
| IDE Setup | `00-onboarding/ide-setup.md` |
| Troubleshooting | `00-onboarding/troubleshooting.md` |

---

## 6. AI Agent Configuration Files

### 6.1 Current State: Fragmented and Outdated

| File | Purpose | Status |
|------|---------|--------|
| `CLAUDE.md` | Claude Code instructions | ⚠️ Broken reference, outdated toolchain versions |
| `.claude-context.md` | Detailed Claude context | 🔴 Wrong versions, wrong paths, dated Dec 2025 |
| `.goose-context.md` | Goose AI context | 🔴 Wrong versions |
| `.cursorrules` | Cursor AI rules | 🔴 Wrong versions, wrong paths |
| `.ai-quick-reference.md` | Quick reference | 🔴 Wrong versions |
| `.chatgpt-config.md` | ChatGPT config | ⚠️ Review needed |
| `.gemini-context.md` | Gemini context | ⚠️ Review needed |
| `CODEX.md` | Codex entrypoint | ⚠️ Review needed |
| `AGENTS.md` | Agent index | ✅ OK (meta-reference) |

### 6.2 Recommendation: Consolidate Agent Files

**Proposed Structure:**

```
project/
├── CLAUDE.md                    # Primary agent instructions (canonical)
├── .agent-versions.md           # Auto-generated version summary (referenced by all)
└── .agent/
    ├── rules/                   # Shared rules
    ├── skills/                  # Agent skills
    └── workflows/               # Workflows
```

**Eliminate or auto-generate:**
- `.claude-context.md` → Merge into `CLAUDE.md`
- `.goose-context.md` → Auto-generate from `CLAUDE.md` template
- `.cursorrules` → Auto-generate from `CLAUDE.md` template
- `.ai-quick-reference.md` → Auto-generate from source files

---

## 7. Recommended Action Plan

### Phase 1: Implement Centralized Versioning (Priority: Critical) ✅ COMPLETED

1. **✅ Created Rust CLI command**
   - Location: `tools/sangita-cli/src/commands/docs.rs`
   - Command: `sangita-cli docs sync-versions`
   - Reads from: `gradle/libs.versions.toml`, `package.json`, `.mise.toml`
   - Outputs to: `application_documentation/00-meta/current-versions.md`

2. **Pending: Update documentation to reference `current-versions.md`**
   - [ ] Modify `02-architecture/tech-stack.md` to link to `current-versions.md`
   - [ ] Update agent files (`.claude-context.md`, `.goose-context.md`, etc.)
   - [ ] Update `CLAUDE.md` toolchain section
   - [ ] Update `README.md` toolchain section
   - [ ] Update `00-onboarding/getting-started.md`

3. **Pending: Add CI validation**
   - Add to GitHub Actions workflow:
   ```yaml
   - name: Verify version sync
     run: |
       sangita-cli docs sync-versions --check
   ```

### Phase 2: Fix Broken References (Priority: High)

1. Fix `CLAUDE.md` reference to non-existent file
2. Fix all incorrect paths in `.claude-context.md`
3. Fix migration path in `.cursorrules`

### Phase 3: Consolidate Agent Files (Priority: Medium)

1. Make `CLAUDE.md` the canonical agent instructions
2. Create template for generating other agent files
3. Eliminate redundant context files

### Phase 4: Archive and Index Updates (Priority: Medium)

1. Move 8 historical files to archive
2. Update main README.md with missing links
3. Fix README capitalizations

### Phase 5: Automation (Priority: Low)

1. Implement CI link validation
2. Consider MkDocs/Docusaurus for search
3. Add pre-commit hooks for version validation

---

## 8. Summary

### Key Findings

1. **20+ version mismatches** due to duplication anti-pattern
2. **4 broken file references** in agent configuration files
3. **11 missing index entries** in main documentation
4. **8 files** should be archived
5. **8 agent config files** need consolidation

### Critical Recommendation

**Stop duplicating versions.** Create a single auto-generated version summary file and have all documentation reference it. This eliminates drift and reduces maintenance burden.

### Files Summary

| Category | Count |
|----------|-------|
| Documentation files analyzed | 232 |
| Files with version issues | 14 |
| Files needing path fixes | 4 |
| Files to archive | 8 |
| Index entries to add | 11 |

---

## References

- [Documentation Standards](./standards.md)
- [Retention Plan](./retention-plan.md)
- [TRACK-028: Documentation Quality Improvements](../../conductor/tracks/TRACK-028-documentation-quality-improvements.md)
- Source files: `gradle/libs.versions.toml`, `package.json`, `.mise.toml`
