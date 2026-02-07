| Metadata | Value |
|:---|:---|
| **Status** | Completed |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-06 |
| **Author** | Sangeetha Grantha Team |

# TRACK-038: Dependency Updates (Feb 2026)

## Goal
Update project dependencies across Backend, Frontend, and Rust toolchain to their latest stable versions, prioritizing security fixes and major version upgrades that offer significant improvements.

## Implementation Plan

### High Priority (Security/Breaking)
- [x] Logback 1.5.25 -> 1.5.27 (CVE-2026-1225 fix)
- [x] React Router 7.11.0 -> 7.13.0 (Security fixes in 7.12.0)
- [x] React 19.2.3 -> 19.2.4 (DoS mitigation)

### Medium Priority (Significant Version Gaps)
- [x] Koin 3.5.6 -> 4.1.1 (Major upgrade, API changes, needs migration)
- [x] Micrometer 1.12.3 -> 1.15.2
- [x] Google Auth 1.23.0 -> 1.41.0
- [x] AWS SDK 2.29.0 -> 2.41.22
- [x] Vite 6.2.0 -> 7.3.1 (Major upgrade)
- [x] reqwest 0.12.24 -> 0.13.1
- [x] Commons CSV 1.10.0 -> 1.14.1

### Low Priority (Minor/Patch)
- [x] Shadow Plugin 9.2.2 -> 9.3.1
- [x] MockK 1.14.7 -> 1.14.9
- [x] TypeScript 5.8.2 -> 5.9.x
- [x] clap 4.5.53 -> 4.5.57
- [x] tokio 1.48.0 -> 1.49.0
- [x] anyhow 1.0.100 -> 1.1.0 (Used 1.0.95)
- [x] Bun 1.3.6 -> 1.3.7

### Caution/Review Needed
- [x] Logstash Encoder 8.0 -> 9.0 (Requires Jackson 3.0+) - **Skipped**. Rationale: Jackson 3.0 involves a major package rename (`tools.jackson`) and is not yet the default for Ktor 3.x. Version 8.0 is stable and secure when paired with Logback 1.5.27.
- [x] Verify Koin 4.x migration guide - Completed

## Progress Log
- 2026-02-06: Created track and initialized checklist. Updated all dependencies. Fixed backend compilation issues (Koin 4.x types, Micrometer imports). Verified all builds.
- 2026-02-07: Final verification of backend and frontend builds. Closure of track.
