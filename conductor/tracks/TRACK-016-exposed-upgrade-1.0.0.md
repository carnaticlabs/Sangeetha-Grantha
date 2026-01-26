# Track: Exposed Upgrade to 1.0.0

| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

**ID:** TRACK-016
**Status:** Completed
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-26
**Updated:** 2026-01-26

## Goal

Upgrade Exposed ORM from 1.0.0-rc-4 to 1.0.0 (stable release) to benefit from stability improvements and finalized APIs.

## Context

- **Current Version:** 1.0.0-rc-4
- **Target Version:** 1.0.0
- **Affected Modules:**
  - `modules/backend/dal` - Primary Exposed usage (tables, repositories)
  - `modules/backend/api` - Transitive dependency
- **Version Catalog:** `gradle/libs.versions.toml`

## Implementation Plan

### Phase 1: Version Update
- [x] Update `exposed` version in `gradle/libs.versions.toml` from 1.0.0-rc-4 to 1.0.0
- [x] Run `./gradlew build` to identify any compilation errors

### Phase 2: Breaking Changes Review
- [x] Review Exposed 1.0.0 release notes for changes from rc-4
- [x] Update any deprecated API usage
- [x] Fix any compilation errors in DAL layer

**Breaking Changes Fixed:**
1. **UUIDTable import path changed**: `org.jetbrains.exposed.v1.core.dao.id.UUIDTable` → `org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable`
2. **UUID column function renamed**: Default `uuid()` now returns Kotlin UUID. For Java UUID, use `javaUUID()` from `org.jetbrains.exposed.v1.core.java.javaUUID`
3. Updated all `uuid(...)` calls in `CoreTables.kt` to `javaUUID(...)` to maintain Java UUID compatibility

### Phase 3: Testing
- [ ] Run backend tests: `./gradlew :modules:backend:api:test`
- [ ] Run database migrations: `cargo run -- db migrate`
- [ ] Run steel-thread test: `cargo run -- test steel-thread`
- [ ] Verify CRUD operations work correctly

### Phase 4: Documentation
- [x] Update `.cursorrules` if version references exist
- [ ] Update any documentation referencing Exposed version

## Progress Log

### 2026-01-26: Breaking Changes Fixed
- Identified breaking changes in Exposed 1.0.0:
  - UUID-related classes moved to `java` subpackage
  - Default `uuid()` function now returns Kotlin UUID
- Fixed `CoreTables.kt`:
  - Updated UUIDTable import to `org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable`
  - Added import for `org.jetbrains.exposed.v1.core.java.javaUUID`
  - Replaced all `uuid(...)` calls with `javaUUID(...)` (56 occurrences)
- Build successful after fixes
- Updated `.cursorrules` with correct Exposed version

### 2026-01-26: Track Created
- Created track for Exposed 1.0.0 upgrade
- Starting implementation

## Rollback Plan

If issues are encountered:
1. Revert `gradle/libs.versions.toml` change
2. Run `./gradlew clean build`
