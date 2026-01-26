| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangita Grantha Architect |

# TRACK-018: Q1 2026 Library Upgrades

## Goal
Upgrade project dependencies to substantial new versions available in Jan 2026 to maintain security, performance, and access to new features.

## Scope
- **Android Gradle Plugin**: 8.13.2 -> 9.0.0
- **Compose Multiplatform**: 1.9.3 -> 1.10.0
- **Kotlinx Serialization**: 1.7.3 -> 1.10.0
- **Kotlinx Coroutines**: 1.9.0 -> 1.10.2
- **Kotlinx Datetime**: 0.6.1 -> 0.7.1
- **Logback**: 1.5.20 -> 1.5.25
- **PostgreSQL Driver**: 42.7.8 -> 42.7.9

## Strategy
Upgrades will be executed in 3 isolated batches to minimize complexity and simplify debugging.

## Implementation Plan

### Batch 1: Core Libraries
Updates to standard libraries that are generally backward compatible but essential.
- [x] **Step 1**: Update `gradle/libs.versions.toml`
  - `kotlinxCoroutinesCore`: "1.10.2"
  - `kotlinxDatetime`: "0.7.1" (Reverted to 0.6.1)
  - `kotlinxSerializationJson`: "1.10.0" (verify breaking changes)
  - `logback`: "1.5.25"
  - `postgresql`: "42.7.9"
- [x] **Step 2**: Sync Gradle and Run Tests
  - `./gradlew :modules:backend:api:test`
  - `./gradlew :modules:shared:domain:clean :modules:shared:domain:assemble`

### Batch 2: UI Framework (Compose)
Updates to the UI layer.
- [x] **Step 1**: Update `gradle/libs.versions.toml`
  - `compose`: "1.10.0"
- [x] **Step 2**: Verify Mobile Build
  - `./gradlew :modules:mobile:androidApp:assembleDebug`

### Batch 3: Build System (High Risk)
Major update to AGP.
- [x] **Step 1**: Update `gradle/libs.versions.toml`
  - `agp`: "9.0.0"
- [x] **Step 2**: Verify Project Build
  - `./gradlew clean build`

## Progress Log

- **2026-01-26**: Created track and analysis.
- **2026-01-26**: Batch 1 Executed. 
  - Upgraded: `kotlinxCoroutines` (1.10.2), `kotlinxSerialization` (1.10.0), `logback` (1.5.25), `postgresql` (42.7.9).
  - Reverted: `kotlinxDatetime` to `0.6.1` due to type mismatch in backend DAL.
  - Fixed: Missing `kotlinx-datetime` dependency in `backend:dal`.
  - Fixed: Missing import in `AutoApprovalServiceTest.kt` due to serialization upgrade.
- **2026-01-26**: Batch 2 Executed.
  - Upgraded: `compose` (1.10.0).
  - Verified via `shared:presentation` build.
- **2026-01-26**: Batch 3 Executed.
  - Upgraded: `agp` (9.0.0).
  - Upgraded: Gradle Wrapper to `9.1.0` (required by AGP 9.0.0).
  - Verified build successfully.

