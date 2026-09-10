| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Design reference |

# Tech Debt: Reference Data Repository Refactoring

---

> [!NOTE]
> Design/reference material: this page may include proposals or earlier implementation assumptions. Use [current feature map](./../features/README.md) for implemented behavior and current operating steps.


---


## 1. Overview
Standardize all reference data repositories (Composer, Raga, Tala, Temple) to use consistent patterns, strict typing (UUID), and standard CRUD operations.

## 2. Changes
- Implement `normalize()` helper in all repos.
- Use `OffsetDateTime` for timestamps.
- Ensure strict `UUID` handling (vs Strings).
- Standardize `create`, `update`, `delete`, `findById` signatures.

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../features/README.md)
