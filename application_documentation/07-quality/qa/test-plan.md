| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.2.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Current guide |

# Import and catalogue acceptance checklist

---

Use this checklist after a bulk import, extraction repair, targeted reingestion, or corpus restoration. Record the source scope, composition IDs, application revision, extraction version, environment, and date. A successful queue run or row count alone does not establish a successful import.

## 1. Trace the input

- Confirm the intended source URL/document and extraction request.
- Inspect source format, page range, language/script, payload, parser version, and any failure/retry context.
- Confirm the worker consumed the intended artifact, particularly for container-local file paths.

## 2. Verify canonical identity and references

- Confirm matched/created composition identity; check likely duplicate title/composer/source candidates.
- Verify composer/raga/tala/deity/temple mappings against evidence.
- Resolve unknown/ambiguous raga names through the curator queue rather than guessed identity.
- Inspect `krithi_ragas`, including order and section associations; checking only `primary_raga_id` is insufficient.

## 3. Verify text and structure

- Compare representative source text with the canonical payload and stored reader text.
- Check section count, order, labels, and handling of Samashti Charanam/Madhyama Kala/repeated Pallavi text.
- Inspect every affected language/script/source variant and its lyric-section joins.
- Confirm empty/incomplete variants are presented honestly and distinct source readings were not silently merged.
- Verify Ragamalika order and justified section-raga mappings.

## 4. Verify provenance and review

- Check import review state and canonical mapping separately from extraction status.
- Verify accepted revision/source attribution and audit events where the change path creates them.
- Confirm the resulting workflow state is intentional.
- Record unresolved source disagreements and quality issues; empty placeholder dashboards are not scan results.

## 5. Verify APIs and clients

- Check composition and variant reads through the public V2 catalogue.
- Check V1 behavior for established versus unclassified forms.
- Confirm anonymous calls do not expose drafts or editorial data.
- Open the intended variant in the client and verify source labels, script, section sequence, and raga display.
- If content is indexed, verify/rebuild affected search documents and hashes before assessing semantic retrieval.

## 6. Record the outcome

Report scope and dated before/after observations, commands/tests actually run, representative source/API/UI checks, and unresolved items. Do not convert fixture counts into corpus counts or reuse an old “zero issues” result as current evidence.

[Schema](../../04-database/schema.md) · [Ingestion](../../01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) · [Raga identity](../../04-database/raga-identity.md) · [Quality gates](../README.md)

---

[Section index](./README.md) · [Documentation home](./../../README.md) · [Feature status](./../../01-requirements/features/README.md)
