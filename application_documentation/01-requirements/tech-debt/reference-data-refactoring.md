| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-26 |
| **Author** | Sangeetha Grantha Team |

# Tech Debt: Reference Data Repository Refactoring


---


## 1. Overview
Standardize all reference data repositories (Composer, Raga, Tala, Temple) to use consistent patterns, strict typing (UUID), and standard CRUD operations.

## 2. Changes
- Implement `normalize()` helper in all repos.
- Use `OffsetDateTime` for timestamps.
- Ensure strict `UUID` handling (vs Strings).
- Standardize `create`, `update`, `delete`, `findById` signatures.
