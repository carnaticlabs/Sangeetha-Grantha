| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Commit Guardrails and Workflow Enforcement System


---


## Executive Summary

This document defines the requirements for a **Commit Guardrails and Workflow Enforcement System** that ensures all code changes are properly documented, logically grouped, and traceable to specific feature requests, bug fixes, or change requests. The system leverages Rust's capabilities to create robust, fast, and cross-platform tooling that integrates seamlessly with Git hooks and modern IDEs.

**Key Objectives:**
- Enforce that every commit references a unique documentation entry in `application_documentation`
- Ensure commits are logically grouped and sequenced (one reference per commit session)
- Prevent accidental commit of sensitive credentials (API keys, secrets) via pre-commit scanning
- Provide pre-commit validation that developers can run manually
- Seamless integration with IDEs (VS Code, IntelliJ, etc.)

**Technology Stack:** Rust-based CLI tool (`sangita-cli`) integrated with Git hooks (`commit-msg`, `pre-commit`)

---

## 1. Feature Requirements

### 1.1 User Goals

**Primary Users:** All developers working on the codebase.

**User Stories:**
- As a developer, I want to ensure my commits are always linked to a documented feature/change request so that code changes are traceable.
- As a developer, I want to validate my commit message format before committing to avoid rejected commits.
- As a team lead, I want to ensure all code changes are properly documented and grouped to maintain code quality and traceability.
- As a developer, I want the validation to work seamlessly in my IDE without interrupting my workflow.

### 1.2 Functional Requirements

#### 1.2.1 Documentation Reference Enforcement
**Requirement:** All commits must be against a unique reference entry in the `application_documentation` folder.

**Details:**
- The reference must be a valid file path within `application_documentation/`.
- The referenced file must exist.
- The reference can be specified in the commit message using the format:
  `Ref: application_documentation/01-requirements/features/my-feature.md` or relative paths.
- The system must validate that the file exists and is accessible.

#### 1.2.2 Single Reference Per Commit Session
**Requirement:** Commits must be logically grouped - a developer cannot commit changes that reference multiple documentation entries simultaneously.

**Details:**
- The system will reject any commit message containing multiple `Ref:` tags if strict 1:1 mapping is enforced.
- Encourages atomic commits that map 1:1 to a unit of documented work.

#### 1.2.3 Pre-commit & Developer Experience
**Requirement:** Validation must run automatically via Git hooks and be available as a manual check command.

**Details:**
- **Automatic Validation:** Git `commit-msg` hook validates commit message format and reference.
- **Manual Validation:** Developers can run `sangita-cli commit check` to validate before committing.
- **Speed:** Validation must be fast (< 500ms) to avoid disrupting developer workflow.

#### 1.2.4 IDE Integration
**Requirement:** The system must integrate seamlessly with popular IDEs without requiring special configuration.

**Details:**
- Works with Git hooks that are automatically recognized by IDEs (VS Code, IntelliJ IDEA, etc.).
- IDE Git integrations should show validation errors in the commit message interface.

#### 1.2.5 Sensitive Data Protection
**Requirement:** The system must detect and mask/strip specific API keys and sensitive tokens (e.g., `SG_GEMINI_API_KEY`) from files being committed.

**Details:**
- **Detection:** Scan staged files for patterns or specific variable names associated with high-risk credentials.
- **Action:** If a key is detected, the system should either:
    - **Block:** Reject the commit with a robust error message pointing to the file and line.
    - **Mask (Auto-fix):** Automatically strip or mask the key (e.g., replace value with `******`) and require the user to stage the safe version.
- **Scope:** Primarily targets configuration files, source code, and markdown documentation where keys might be pasted accidentally.

---

## 2. Technical Architecture

### 2.1 System Components

#### 2.1.1 Rust CLI Tool Extension (`sangita-cli`)
The logic resides in `tools/sangita-cli/src/commands/commit.rs`. It is responsible for:
- Parsing commit messages and extracting documentation references.
- Validating that referenced files exist in `application_documentation/`.
- Providing manual validation and hook installation commands.
- Scanning staged files for sensitive data (API keys, secrets).

**Implementation Status:** ✅ **Implemented** (2026-01-05)

See `tools/sangita-cli/README.md` for usage instructions and `tools/sangita-cli/src/commands/commit.rs` for implementation details.

#### 2.1.2 Git Hooks
- **`commit-msg` Hook**: Triggered when a commit message is being finalized. Blocks the commit if validation fails.
- **`pre-commit` Hook**: Responsible for scanning staged files for sensitive data (API keys) and other content checks before the commit message editor is even invoked.

### 2.2 Commit Message Format

**Standard Format:**
```text
<subject line>

Ref: application_documentation/01-requirements/features/my-feature.md

<optional body>
```

**Examples:**

*Feature Implementation:*
```text
Implement graph explorer API endpoints

Ref: application_documentation/01-requirements/features/graph-explorer.md

- Add GET /v1/graph/entities endpoint
- Add GET /v1/graph/neighborhood endpoint
```

*Bug Fix:*
```text
Fix raga validation in krithi editor

Ref: application_documentation/01-requirements/features/ragamalika-validation.md

Resolves issue where raga validation was not checking for valid raga IDs.
```

---

## 3. Workflow Examples

### 3.1 Standard Feature Development Workflow

1. **Developer creates/updates feature documentation:**
   `application_documentation/01-requirements/features/my-feature.md`
2. **Developer makes code changes** and stages them (`git add ...`).
3. **Developer commits with reference:**
   `git commit -m "Implement feature X ... Ref: ..."`
4. **Hook validates commit automatically:**
   - ✅ Reference exists and is valid -> Commit proceeds.

### 3.2 Invalid Commit (Missing Reference)

1. **Developer commits without reference:**
   `git commit -m "Fix bug in API"`
2. **Hook rejects commit:**
   - Error displayed: "Commit message must include a reference to application_documentation".
3. **Developer fixes and retries** using `git commit --amend` or correcting the message.

### 3.3 Manual Validation

Developers can validate messages manually before committing using the CLI tool.
*(See `tools/sangita-cli/README.md` for command usage)*.

---

## 4. Success Metrics

- **Adoption Rate:** 100% of commits include valid documentation references within 2 weeks.
- **Security:** 0 incidents of committed API keys or secrets.
- **Performance:** Hook execution time < 500ms (p95).
- **Error Rate:** < 1% false positives.
- **Documentation Coverage:** All significant features/changes have documentation entries.

---

## 5. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Developers find hooks disruptive | High | Make hooks fast, provide clear error messages. |
| False positives | High | Graceful handling of edge cases (merges, rebases). |
| IDE compatibility issues | Medium | Use standard Git hooks which are universally supported. |

---

## 6. Implementation Details

### 6.1 Implementation Status

✅ **Status**: Implemented (2026-01-05)  
**Location**: `tools/sangita-cli/src/commands/commit.rs`  
**Version**: 0.2.0

### 6.2 Commands

The following commands are available:

- `cargo run -- commit check [--message <msg>]` - Validate commit message format
- `cargo run -- commit scan-sensitive` - Scan staged files for sensitive data
- `cargo run -- commit install-hooks` - Install Git hooks
- `cargo run -- commit uninstall-hooks` - Remove Git hooks

### 6.3 Technical Details

**Dependencies:**
- `regex = "1.11.1"` - For pattern matching in commit messages and sensitive data detection

**Key Features:**
- Case-insensitive reference matching (`Ref:` or `ref:`)
- Path normalization and validation
- Automatic binary detection for hooks (release/debug fallback to cargo run)
- Cross-platform support (Unix/Windows)
- Fast validation (< 500ms target achieved)

**Sensitive Data Patterns Detected:**
- API keys: `api[_-]?key`, `apikey`, `SG_GEMINI_API_KEY`
- Secrets: `secret`, `password`, `token`
- AWS credentials: `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`

**Error Handling:**
- Clear, actionable error messages
- File location and line number reporting for sensitive data
- Helpful format examples when validation fails

### 6.4 Usage Examples

**Valid Commit:**
```bash
git commit -m "Implement feature X

Ref: application_documentation/01-requirements/features/my-feature.md

- Added new endpoint
- Updated tests"
```

**Invalid Commit (will be rejected):**
```bash
git commit -m "Fix bug"
# Error: Commit message must include a reference to application_documentation
```

**Manual Validation:**
```bash
echo "My commit message

Ref: application_documentation/01-requirements/features/my-feature.md" | \
  cargo run -- commit check
```

### 6.5 Integration

The hooks integrate seamlessly with:
- VS Code Git integration
- IntelliJ IDEA Git integration
- Command-line Git
- Any IDE that respects standard Git hooks

No special configuration required beyond running `cargo run -- commit install-hooks` once.

---

## 7. References

- [Standards Documentation](../../00-meta/standards.md) - Development standards and guidelines
- [Tech Stack Documentation](../../02-architecture/tech-stack.md) - Technology stack overview
