| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Raga identity and curator resolution

---

A raga's display name is not a sufficient identity key. Spellings and transliterations can refer to the same raga, while similar names can refer to distinct musical identities. Sangeetha Grantha separates those cases using aliases, mela-qualified keys, and curator review.

## Identity model

| Concept | Stored representation | Meaning |
|:---|:---|:---|
| Canonical raga | `ragas` | Display name, lineage, available scale information, provenance |
| Normalized identity | `match_key`, `mela_disambiguator` | Search-normalized key qualified by own/parent melakarta context |
| Alternate surface form | `raga_aliases` | Another name for the same identity, with type and source |
| Key collision guard | `raga_identity_keys` | Shared uniqueness boundary across canonical and alias keys |
| Related nomenclature | `raga_relations` | A relationship between distinct identities, not an automatic merge |
| Unresolved import name | `raga_resolution_queue` | Unknown, ambiguous, or conflicting input requiring a decision |

A match key is generated from the name. `mela_disambiguator` uses a raga's own melakarta number, its parent's, or zero when absent. Display-normalized names are not globally unique identity keys.

Read [ADR-016](../02-architecture/decisions/ADR-016-raga-naming-authority.md) for naming authority and [ADR-017](../02-architecture/decisions/ADR-017-raga-reference-entity-identity-resolution.md) for the detailed identity decision. The schema is implemented across [V51–V56](../../database/migrations).

## Curator workflow

Open Curator Review and inspect the unresolved-raga queue. Use source evidence and musical context before choosing an action:

| Action | Use when | What it records |
|:---|:---|:---|
| Attach alias | An unknown spelling is an existing identity | Alias and source linked to the chosen raga |
| Confirm new | The source establishes a distinct identity | A new raga through the controlled service, with required scale/parent input |
| Disambiguate | Multiple candidates need a deliberate choice | The selected existing identity |
| Scan scale collisions | Investigating suspicious overlap | Queue entries for review |

Actions are mounted under `/v1/admin/curator/raga-queue` and are audited. The [resolution service](../../modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/services/RagaResolutionService.kt) validates action type, queue state, required inputs, and key collisions.

Do not turn a fuzzy score into an identity assertion. Preserve separate-scale nomenclature relationships where the domain requires them. Reingest affected imports through the normal pipeline after resolving the underlying mapping.

## Display and search

Admin reference data offers a melakarta/janya tree. Public raga directories expose aliases that matched the query and counts for compositions visible under the chosen catalogue version. Entity pages can display parent, scales, aliases, and nomenclature links when that information is stored.

Composition raga membership is an ordered relation in `krithi_ragas`. Ragamalika must retain the full sequence; setting only `primary_raga_id` loses that information.

## Checks

```bash
make raga-lakshana-checks
make mint-guard
```

The first command requires a migrated database and runs [standing SQL checks](../../database/checks/raga_lakshana.sql). The second checks repository SQL-writing surfaces for raga inserts outside migrations. Neither replaces a musicologist's assessment of an ambiguous identity.

Follow changes with [post-import verification](../07-quality/qa/test-plan.md). Earlier cleanup results are preserved in the [implementation archive](../10-implementations/README.md); they are dated observations, not live catalogue counts.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
