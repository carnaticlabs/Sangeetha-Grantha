# Documentation Retention and Archiving Plan

| Metadata | Value |
|:---|:---|
| **Status** | Current |
| **Version** | 1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | Engineering Team |
| **Related Documents** | - [Documentation Standards](./standards.md)<br>- [Documentation Index](../README.md) |

## Overview

This document outlines the retention and archiving strategy for Sangita Grantha documentation. The goal is to maintain a clean, current documentation tree while preserving historical context.

## Archive Location

All archived documentation is stored in `application_documentation/archive/` with the following structure:

```
archive/
├── database-archive/      # Legacy database schemas and migrations
├── graph-explorer/        # Archived graph database evaluation docs
├── requirements-spec/     # Legacy requirement specifications
├── ui-ux/                 # Archived UI/UX documentation
└── [feature-name]/        # Feature-specific archived docs
```

## When to Archive

Documentation should be archived when:

1. **Superseded**: Replaced by newer, canonical documentation
2. **Obsolete**: No longer reflects current implementation or plans
3. **Historical**: Preserved for reference but not actively maintained
4. **Experimental**: From features that were evaluated but not implemented

## Archiving Process

### Step 1: Create Archive Directory

If archiving a feature or topic, create a subdirectory in `archive/`:

```bash
mkdir -p application_documentation/archive/[feature-name]
```

### Step 2: Add README

Create a `README.md` in the archive directory explaining:
- What was archived and why
- Date of archival
- Link to canonical replacement (if applicable)
- Brief summary of contents

Example:
```markdown
# [Feature Name] Archive


## Overview

[Brief description of what was archived]

## Contents

- [List of files and their purpose]

## Archive Rationale

[Why these documents were archived]

## Canonical Replacement

[Link to current documentation, if applicable]
```

### Step 3: Move Files

Move files to the archive directory:

```bash
mv application_documentation/[path]/[file].md \
   application_documentation/archive/[feature-name]/
```

### Step 4: Update References

- Update any cross-references in active documentation
- Add tombstone notes in original locations if needed
- Update the main documentation index if the file was listed

### Step 5: Update Status

Set the document status to "Archived" in front matter:

```markdown
```

## Current Archive Contents

### Database Archive (`archive/database-archive/`)

Legacy database schemas, DDL scripts, and design documents from earlier iterations.

### Graph Explorer Archive (`archive/graph-explorer/`)

Documentation from the Neo4j graph database evaluation phase. The feature was not implemented, but the evaluation documents are preserved for reference.

### Requirements Spec Archive (`archive/requirements-spec/`)

Legacy requirement specifications that have been superseded by the current PRD structure.

### UI/UX Archive (`archive/ui-ux/`)

Archived UI/UX documentation and specifications.

## Retention Guidelines

### Keep Active

- Current PRDs and requirements
- Active architecture documentation
- Current API contracts
- Database schema documentation (current)
- Backend and frontend implementation guides
- Operations runbooks
- Test plans and quality reports

### Archive

- Superseded specifications
- Experimental feature evaluations (not implemented)
- Legacy database schemas
- Obsolete UI mockups or designs
- Historical planning documents

### Delete (Rare)

Only delete documentation if:
- It contains sensitive information that should not be preserved
- It's a duplicate with no historical value
- It's a temporary scratch file

**Note**: When in doubt, archive rather than delete.

## Maintenance

### Quarterly Review

Review the documentation tree quarterly to:
- Identify obsolete documents for archiving
- Verify archive READMEs are accurate
- Check for broken cross-references
- Update retention plan if needed

### Archive Index

The main documentation index (`application_documentation/README.md`) references the retention plan but does not list all archived documents. Archive directories should maintain their own README files for discoverability.

## References

- [Documentation Standards](./standards.md)
- [Documentation Index](../README.md)
- [ADR-001: Spec-Driven Documentation](../02-architecture/decisions/ADR-001-spec-driven-docs.md)
