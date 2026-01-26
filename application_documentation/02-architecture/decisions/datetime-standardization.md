| Metadata | Value |
|:---|:---|
| **Status** | Accepted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangita Grantha Architect |

# Decision: Standardization on `kotlin.time.Instant`

## Context
Kotlin 2.0+ (specifically 2.3.0) and `kotlinx-datetime` 0.7.0+ have introduced a major shift in date-time handling. The representation of "an instantaneous point on the timeline" has moved from the external `kotlinx-datetime` library to the Kotlin Standard Library.

### The Two Instants

1.  **`kotlinx.datetime.Instant`** (Legacy)
    - **Origin**: `org.jetbrains.kotlinx:kotlinx-datetime` library.
    - **Status**: Deprecated/Removed in 0.7.0+ in favor of stdlib.
    - **Usage**: Was the standard for KMP projects previously.

2.  **`kotlin.time.Instant`** (Recommended)
    - **Origin**: Kotlin Standard Library (`kotlin-stdlib`).
    - **Status**: Stable in Kotlin 2.3.0.
    - **Usage**: The new standard for all Kotlin platforms.

## Comparison

| Feature | `kotlinx.datetime.Instant` | `kotlin.time.Instant` |
| :--- | :--- | :--- |
| **Source** | External Library (`kotlinx-datetime`) | Standard Library (`stdlib`) |
| **Dependency** | Requires `kotlinx-datetime` | No extra dependency |
| **Kotlin Version** | Any | Kotlin 2.1+ (Stable in 2.3.0) |
| **Interoperability** | Good with `kotlinx-serialization` < 1.10.0 | Native in `kotlinx-serialization` 1.10.0+ |
| **Future Proof** | ❌ No | ✅ Yes |

## The "Type Mismatch" Issue
In `kotlinx-datetime` 0.7.0, `Instant` becomes a typealias to `kotlin.time.Instant`. However, binary compatibility issues can arise if:
1.  Dependencies (like `Exposed`) are compiled against the old version.
2.  Code explicitly imports `kotlinx.datetime.Instant` but APIs expect `kotlin.time.Instant` (or vice versa during the transition).

## Recommendation

**We should standardise on `kotlin.time.Instant` across all modules.**

### Migration Strategy
1.  **Update Imports**: Replace `import kotlinx.datetime.Instant` with `import kotlin.time.Instant`.
2.  **Update Serializers**: Ensure `kotlinx-serialization` 1.10.0+ is used (which supports stdlib Instant).
3.  **Update Extensions**: Update `ResultRowExtensions.kt` to return `kotlin.time.Instant`.
4.  **Backend DTOs**: Update all domain DTOs to use `kotlin.time.Instant`.

### Impact
- **Modules**: `shared:domain`, `backend:dal`, `backend:api`, `mobile`.
- **Effort**: Medium (Rename/Refactor across DTOs).
- **Risk**: Serialization compatibility (ensure JSON format remains ISO-8601).

## Implementation
This migration should be tracked as **TRACK-019**.
