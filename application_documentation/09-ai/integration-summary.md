| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# AI and extraction capabilities

---

AI supports extraction, transliteration, and retrieval in Sangeetha Grantha. Source fidelity, canonical identity, and publication remain controlled application/editorial responsibilities. The current architecture does not treat model output as a scholarly authority.

## Implemented surfaces

| Capability | Current path | Boundary |
|:---|:---|:---|
| Source extraction | Python HTML/PDF strategies, canonical Pydantic output, Kotlin consumption | Deterministic/source-aware parsing is the primary path; supported strategies govern formats |
| Optional enrichment | Worker Gemini integration | Explicit opt-in and credentials; default disabled |
| Admin transliteration | Kotlin transliteration service and editor operation | Does not generate new reader text in Rasika automatically |
| Reference candidates | Normalization, aliases, candidate matching, curator resolution | Similarity is not proof of musical identity |
| Hybrid/semantic retrieval | Document/query embeddings, pgvector, profile compatibility, rank fusion | Separate indexing operation; not a conversational mobile assistant |

The [worker README](../../tools/krithi-extract-enrich-worker/README.md), [ingestion guide](../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md), and [search guide](../03-api/search.md) describe the operating paths.

## Provider and contract handling

Generation/enrichment and embeddings use different configuration and contracts. Keep model choice, response validation, retries, timeouts, and index compatibility explicit. Worker enrichment uses typed/configured SDK behavior; backend service calls retain their own clients/configuration.

Canonical payloads must preserve source text, script/language, section order, variants, unknown classification, and raga ordering. Retain extractor/model context where the pipeline provides it and inspect low-confidence or conflicting results in curation.

## Evaluation

Use deterministic fixtures and controlled provider failures for code tests. Use source comparison and a dated corpus/query benchmark for extraction/retrieval quality. Historical cost, throughput, or accuracy estimates in old plans are not current measured performance or vendor pricing.

Record the model/profile, corpus scope, revision, commands, and observed results. Keep provider-backed evaluations separate from offline test totals. [Quality](../07-quality/README.md) and [TRACK-108 evidence](../10-implementations/track-108-validation-2026-09-08.md) show the reporting boundary.

## Future work

Conversational discovery, broader automated musicological validation, and expanded editorial automation require separate specifications and evidence. They are not implied by the existing embedding index or the presence of a validation route.

[AI opportunities](./integration-opportunities.md) · [Configuration](../08-operations/config.md) · [Feature map](../01-requirements/features/README.md)

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
