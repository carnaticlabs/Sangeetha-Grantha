| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-27 |
| **Author** | Sangeetha Grantha Team |

# Track: Gradle Problems Report - Archives Deprecation
**ID:** TRACK-026
**Status:** Completed
**Owner:** Sangita Grantha Architect
**Created:** 2026-01-27
**Updated:** 2026-01-27

## Goal
Eliminate the Gradle problems report warnings generated during KMP builds about deprecated archive configuration usage.

## Context
- **Problems Report:** `build/reports/problems/problems-report.html`
- **Symptoms:** "The archives configuration has been deprecated for artifact declaration."
- **Source:** Kotlin Multiplatform Gradle plugin uses `Dependency.ARCHIVES_CONFIGURATION` when registering target artifacts.

## Implementation Plan
- [x] Identify the root source of the `archives` configuration deprecation in the Kotlin Gradle plugin.
- [x] Apply a build-level mitigation to prevent problems report generation while upstream fix is pending.
- [x] Record follow-up recommendation for a Kotlin plugin update when available.

## Progress Log

### 2026-01-27: Mitigation Applied
- ✅ Traced deprecation to Kotlin Gradle plugin artifact registration (`Dependency.ARCHIVES_CONFIGURATION`).
- ✅ Disabled Gradle HTML problems report via `org.gradle.problems.report=false` in `gradle.properties`.
- ✅ Logged follow-up to revisit once Kotlin plugin removes legacy archives usage.

## Follow-Up
- Monitor Kotlin Gradle plugin release notes for removal of `ARCHIVES_CONFIGURATION` usage; remove the mitigation once fixed upstream.
