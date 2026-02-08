| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# Broken Tests Remediation Plan

## Overview
Several backend service tests are currently failing at runtime. While the compilation issues related to service refactoring (interfaces and constructor changes) have been resolved, the tests fail during execution when using the H2 in-memory database.

## Current Status
The following tests compile but fail at runtime:
- `EntityResolutionServiceTest`
- `ImportServiceTest`
- `KrithiServiceTest`
- `QualityScoringServiceTest`
- `TempleScrapingServiceTest`
- `WebScrapingServiceTest`

## Root Cause: H2 Compatibility
The primary cause of failure is the usage of PostgreSQL-specific features in the Data Access Layer (DAL) that are not natively supported or behave differently in H2:
1.  **Custom Enum Types**: Tables using `pgEnum` (e.g., `language_code_enum`, `musical_form_enum`) fail to create or fail during data conversion in H2.
2.  **JSONB Columns**: While H2 has some JSON support, the interaction with Exposed's PostgreSQL JSONB handling can be brittle.
3.  **Complex Constraints**: Certain PostgreSQL-specific constraints or indices may not map perfectly to H2.

## Remediation Strategy
Instead of "hacking" H2 to support PostgreSQL features, we should adopt one of the following production-grade testing strategies:

### Option A: Testcontainers (Recommended)
Migrate the test suite to use **Testcontainers** with a real PostgreSQL 15+ instance. This ensures:
- 100% feature parity with production.
- No need for manual enum or type creation in the test setup.
- Reliability for JSONB and complex query testing.

### Option B: Dedicated Test Database
Configure a dedicated PostgreSQL database for testing (e.g., `sangita_grantha_test`) that is reset before each test run.

## Action Items
1.  [ ] Setup Testcontainers dependency in `modules/backend/api/build.gradle.kts`.
2.  [ ] Update `TestDatabaseFactory.kt` to use `PostgreSQLContainer`.
3.  [ ] Remove H2-specific "CREATE TYPE" or "CREATE DOMAIN" hacks.
4.  [ ] Ensure migrations are run via `sangita-cli` or a compatible migration tool during test setup to keep schema in sync.

## Conclusion
The decision has been made to defer fixing these tests for H2. Future efforts should focus on migrating the testing infrastructure to use a real PostgreSQL environment via Testcontainers to ensure high-fidelity verification of business logic and data integrity.