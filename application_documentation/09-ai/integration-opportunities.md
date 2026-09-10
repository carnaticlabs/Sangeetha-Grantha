| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 1.3.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# AI roadmap and evaluation criteria

---

The current AI/extraction capabilities are described in [Integration summary](./integration-summary.md). This document frames extensions to those capabilities; it is not an implementation checklist or a current pricing estimate.

## Existing foundations

The repository has source-aware HTML/PDF parsing, optional worker enrichment, admin transliteration, reference candidate resolution, canonical payload validation, and hybrid/semantic search. The pipeline retains evidence and supports human review. [Ingestion](../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) and [search](../03-api/search.md) describe what is operational today.

## Opportunities to evaluate

| Opportunity | Reader/curator benefit | Evidence needed before delivery |
|:---|:---|:---|
| Better difficult-source extraction | Less manual repair of OCR/encoding/section mistakes | Source-held-out fixtures, script-specific error analysis, preserved provenance |
| Assisted review | Quicker identification of inconsistent sections or metadata | Explainable evidence, false-positive analysis, reversible curator decisions |
| Conversational discovery | Natural-language navigation beyond ranked results | Grounded answers, stable entity links, published-only visibility, query benchmark |
| Expanded transliteration workflows | Easier preparation of explicitly labeled variants | Script fidelity review, clear source/generated distinction, repeatable evaluation |
| Quality prioritization | Direct curator attention to the most useful corrections | Defined quality dimensions, real measurements, no substitution of scores for evidence |

## Design constraints

Model output should remain distinguishable from source content. Keep language, script, tradition, and source identity separate. Do not guess unknown forms or create raga identities solely from similarity. Any accepted corpus correction should retain canon/audit provenance.

Provider requests need configured credentials, bounded failure handling, and validated response contracts. Embedding changes require profile/index compatibility; generation model changes are not index migrations. The browser must not receive service credentials.

## Evaluation and cost

Define representative source/query sets and a baseline before comparing approaches. Report accuracy/failure modes per script/source type, curator effort, observed request latency, actual usage, and corpus coverage. Retrieve current provider pricing when budgeting a real run; old per-composition estimates in the documentation history are not an operating budget.

Use [Conductor](../../conductor/tracks.md) to specify a bounded feature, acceptance evidence, and delivery plan. [TRACK-108 analysis](../10-implementations/track-108-conversational-discovery-analysis-sep-2026.md) preserves a related discovery investigation; it does not establish a shipped conversational experience.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
