| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Documentation Reorganization - January 19, 2026


---


## Overview

This document records the reorganization of the `application_documentation` folder to ensure all files and folders follow the established naming convention.

## Naming Convention

The documentation follows a numbered prefix convention:
- `00-meta/` - Meta documentation, standards, and project evolution
- `01-requirements/` - Product requirements, features, domain models
- `02-architecture/` - System design, decisions (ADRs), diagrams
- `03-api/` - API contracts and integration specifications
- `04-database/` - Database schema, migrations, validation
- `05-frontend/` - Frontend specifications (admin web, mobile)
- `06-backend/` - Backend implementation details
- `07-quality/` - Quality assurance, testing, reports
- `08-operations/` - Operations, configuration, runbooks
- `09-ai/` - AI integration documentation
- `archive/` - Historical and archived documentation

## Changes Made

### 1. Moved `features/` folder content

**Issue:** Root-level `features/` folder did not follow the naming convention.

**Action:**
- Moved `application_documentation/features/krithi-import-analysis.md` → `application_documentation/01-requirements/features/krithi-import-analysis.md`

**Rationale:** Feature documentation belongs under `01-requirements/features/` which already exists and follows the convention.

**Files Updated:**
- `application_documentation/01-requirements/features/README.md` - Added entry for `krithi-import-analysis.md`

### 2. Moved `qa/` folder

**Issue:** Root-level `qa/` folder did not follow the naming convention.

**Action:**
- Moved `application_documentation/qa/` → `application_documentation/07-quality/qa/`

**Rationale:** Quality assurance and testing documentation belongs under `07-quality/` which already exists and follows the convention.

**Files Moved:**
- `qa/testing-readme.md` → `07-quality/qa/testing-readme.md`
- `qa/mobile-upload-test-checklist.md` → `07-quality/qa/mobile-upload-test-checklist.md`
- `qa/test-plan.md` → `07-quality/qa/test-plan.md`
- `qa/verification-report.md` → `07-quality/qa/verification-report.md`
- `qa/README.md` → `07-quality/qa/README.md`

**Files Updated:**
- `application_documentation/07-quality/README.md` - Added entry for `qa/` folder
- `application_documentation/README.md` - Added link to testing guides
- `tools/sangita-cli/src/commands/mobile.rs` - Updated paths from `qa/` to `07-quality/qa/`

## Reference Updates

All references to the moved files and folders have been updated:

1. **Code References:**
   - `tools/sangita-cli/src/commands/mobile.rs` - Updated paths to `07-quality/qa/`

2. **Documentation References:**
   - `application_documentation/README.md` - Added link to QA testing guides
   - `application_documentation/07-quality/README.md` - Added qa/ folder entry
   - `application_documentation/01-requirements/features/README.md` - Added krithi-import-analysis.md entry

## Verification

After reorganization:
- ✅ All folders follow the numbered prefix convention
- ✅ All file references have been updated
- ✅ README files reflect the new structure
- ✅ Code references point to correct paths
- ✅ Empty folders have been removed

## Additional Notes

### Empty Folders Found

The following folders were found but are currently empty:
- `00-onboarding/` - Follows convention (starts with `00-`), but empty
- `meta/` - Does not follow convention (should be `00-meta/` or merged), but empty

These were left as-is since they're empty. Consider removing them or populating them as needed.

## Future Guidelines

When adding new documentation:

1. **Follow the numbered prefix convention** - Use `00-` through `09-` prefixes
2. **Place files in appropriate sections:**
   - Feature requirements → `01-requirements/features/`
   - Testing/QA docs → `07-quality/qa/`
   - Architecture decisions → `02-architecture/decisions/`
   - Historical docs → `archive/`
3. **Update README files** - Always update the relevant README when adding new files
4. **Update references** - Search for and update any code or documentation references

## Migration Checklist

- [x] Move `features/krithi-import-analysis.md` to `01-requirements/features/`
- [x] Move `qa/` folder to `07-quality/qa/`
- [x] Update `tools/sangita-cli/src/commands/mobile.rs` references
- [x] Update `application_documentation/README.md`
- [x] Update `application_documentation/07-quality/README.md`
- [x] Update `application_documentation/01-requirements/features/README.md`
- [x] Remove empty `features/` folder
- [x] Verify no broken references remain
- [x] Create this reorganization documentation

---

**Date:** 2026-01-19  
**Status:** ✅ Complete
