| Metadata | Value |
|:---|:---|
| **Status** | Complete — 0 Unknown tālas remaining (100% resolved) |
| **Version** | 2.0.0 |
| **Last Updated** | 2026-09-26 |
| **Author** | Sangeetha Grantha Team |

# TRACK-144 catalogue review and missing tāla resolution

Reviewed the uncommitted TRACK-144 changes against the running catalogue on 24 September 2026. The user subsequently accepted the correction plan. Implementation and verification were completed across 25–26 September 2026. Search documents for all repaired compositions match their source text. All 343 previously Unknown tālas are now 100% source-backed and resolved (0 Unknown remaining).

## Final outcome — 26 September 2026

- **100% Tāla Resolution:** All 343 previously Unknown tālas fully resolved with verified source evidence (`database/data/track144-tala-evidence-tyagaraja-complete.json`).
- **Tāla Taxonomy Harmonization:**
  - `Isra Capu` / `Isra Chapu` merged into canonical `Misra Capu` (29 compositions).
  - `Ad` merged into canonical `Adi` (1 composition: *kana kana rucirA*).
  - `Ekam` / `Caturasra Ekam` refined to canonical `Catusra Ekam` (3 compositions: *vAsu dEva vara guNa*, *bAlAmbikayA kaTAkshitOhaM*, *gaNESa kumAra*).
  - `English` Nottuswara refined to `Catusra Ekam` (*Cintaya Citta*); `English` deleted from `talas`.
  - Active catalogue taxonomy is 100% pure across 19 canonical Carnatic tālas (+ sentinel `Unknown` at count 0).
- **Consolidation:** Migrations collapsed into 3 canonical Flyway scripts (`V61`, `V62`, `V63`).
- **Implementation Ref:** [track-144-rasika-field-feedback.md](../10-implementations/track-144-rasika-field-feedback.md).

## Implementation outcome — 25 September 2026 (Historical Snapshot)

**62 previously Unknown tālas are now source-backed; the backlog fell from the measured 343 to 281.** Sri Narada Nada was additionally corrected from Adi to Rupaka. No second script was invented. The original reported 345 was not the measured starting count.

| Repair | Applied result |
|:---|:---|
| V63 | Sri Narada Nada → Rupaka; Rama Rama Rama Sita → Adi; Sumadyuti nomenclature relation now cites the SSP edition. |
| V64 | 31 reviewed repairs: 30 from Shivkumar notation pages, one from Syama Krishna Vaibhavam. |
| V65 | 11 further Tyagaraja repairs recovered from explicit source headers with the tāla label after the value. |
| V66 | 14 further notation matches manually resolved across raga spelling differences. |
| V67–V68 | Audited PDF source registration and four visually verified Dikshitar repairs from P. P. Narayanaswami’s compilation, pages 17–19 and 77. |
| V69 | Entani Ne, Mukhāri → Rupaka. Shivkumar notation page and index agree. |
| Audit | V63–V68: 62 composition before/after entries, 60 evidence entries, one relation audit and one source-registration audit. V69 adds one composition audit and one evidence audit. |
| Remaining Unknown | 281 Tyagaraja; none Muthuswami Dikshitar or Syama Sastri. |

The applied migrations remain immutable. Each new composition repair checks UUID, composer, raga, stored Latin pallavi and expected prior tāla. Missing corpus UUIDs are no-ops on reference-only installations; identity changes raise an exception. No title-only backfill, inferred tāla subtype, or cross-script lyric write is used. The review manifests are [V64 evidence](../../database/data/track144-tala-evidence.json) [V65 evidence](../../database/data/track144-tala-evidence-suffix.json), [V66 notation review](../../database/data/track144-tala-evidence-notation-review.json), [V68 PDF review](../../database/data/track144-tala-evidence-dikshitar-pdf.json), and [V69 Entani Ne](../../database/data/track144-tala-evidence-entani-ne.json). Each includes the source URL, source checksum (decoded UTF-8 HTML or raw PDF bytes), locator, exact stored pallavi and review decision. Review attribution identifies the agent, not an independent human musicologist.

The shared raga directory now puts ragas without a melakarta number or established parent under **Other ragas**. Sumadyuti is no longer presented as a janya merely because its hierarchy is unspecified. Gamakakriya, Poorvi Kalyani and Bowli remain distinct identities. The Sumadyuti–Simhendramadhyamam relationship is nomenclature evidence, not a new parent assignment.

The synchronous indexer now also checks the stored document hash, actual indexed-text hash and original source text, so a matching vector hash cannot hide a damaged document. A real PostgreSQL regression reproduces and repairs that case without external API calls.

The production metadata parser now preserves initial `m` in raga/tāla names, fills individual missing fields from the title, handles the incomplete `(tALa tripuTa,` declaration, and recognizes `(miSra cApu tALa)`. The read-only auditor requires an explicit tāla label and never publishes candidates. The manifest compiler rejects duplicate decisions, unsupported canonical labels, unreviewed rows and SQL delimiter injection; it only creates a new file.

### Verification completed

- Full worker suite: `424 passed, 16 skipped`; Ruff passed for changed Python files and mypy passed for the four new source/script modules. Five PostgreSQL/Flyway index-maintenance integration tests also passed in a disposable testcontainer. Skipped tests and third-party deprecation warnings remain visible.
- `make test-mobile`: `BUILD SUCCESSFUL`; JVM results contain 25 mobile-data and 55 presentation tests, with zero failures/errors/skips, including the unknown-hierarchy regression.
- Populated isolated database copy: V63–V68 applied successfully; clean isolated Flyway replay: `Successfully applied 72 migrations … now at version v65`. The integration testcontainer subsequently migrated the complete schema through V68. Local Flyway applied V63–V68. All 60 manifest rows match the live title, raga and tāla and has source evidence.
- Local DB checks at that point: 282 Unknown; zero primary-raga junction gaps, zero per-variant section-count mismatches, zero HTTP(S) URLs in lyric sections or full variants, zero uses of the old combined Bhoopalam/Bowli raga, one Sri Narada Nada script variant. After V69 the live Unknown count is 281.
- Stack rebuilt/restarted through `make dev-down` / `make dev`; backend `/health` and frontend both returned 200. Public detail APIs verified Sri Narada Nada/Rupaka, Rama Rama Rama Sita/Adi, and aDugu varamula/Misra Capu.
- `make check-docs` still reports the link to the untracked TRACK-144 file: its target exists, but the checker inventories Git-tracked paths. No files were staged or committed to suppress this finding.
- Two native iOS journeys passed on iPhone 17 / iOS 26.5 (`C0104896-CD64-4533-A919-1E5F22036561`): Explore search to stored reading, and bookmark persistence through Library/relaunch. These use synthetic fixtures; they do not prove live-corpus correctness or a VoiceOver pass.

### Work still open

1. **Search rebuild:** completed on 25 September. The second pass embedded 1,421 documents across 58 compositions. After V69, the verifier reports 161 compositions, 3,085 documents, and zero problems. The stack was restarted; Entani Ne is served as Mukhāri / Rupaka.
2. **Remaining 281 tālas:** the blog inventory has 287 evidence URLs. The cached source audit reports 229 pages without an explicit tāla and 58 requiring identity/alias review. A later Shivkumar index comparison found no further row where the index and the notation page agree on an explicit tāla. Bhava Nuta’s page says Adi and its index says Rupakam. Manavinaalakincha’s page says Deshadi and its index says Adi. nanu pAlimpa’s notation URL returns 404. Consult a different notation edition. Keep the three demonstrated wrong-composition imports quarantined.
3. **Conflicting and incomplete evidence:** excluded notation candidates include Mitri (index/page disagreement), Daya Leni and Pakkala (alternatives), Deshadi-labelled pieces (no automatic conversion to Adi), Enaati Nomu and Dvaitamu (missing stored pallavi), and nanu Palimpa (page unavailable). Akhilandesvari/Dwijavanthi was resolved to Adi using agreement between its full notation page and the printed compilation, while retaining the conflicting index attribution in evidence. Existing “Isra Capu” assignments need a separate identity-aware provenance audit; the parser fix does not retroactively assert their values.
4. **Historical V61 portability/audit limitations:** the old broad mutations cannot be made reversible by inventing before-images. The current corpus has no demonstrated non-Latin Marugelara corruption. A deployment carrying other variants needs a preflight before replaying V61; the clean reference-only replay cannot prove safety on every populated deployment.

### Repeatable continuation workflow

From the repository root, export the current unresolved inventory (read-only):

```bash
mkdir -p output/track-144
docker compose exec -T db psql -U postgres -d sangita_grantha -Atf /dev/stdin \
  < database/audits/audit_unknown_talas.sql \
  > output/track-144/remaining-tala-inventory.json
```

From `tools/krithi-extract-enrich-worker`:

```bash
.venv/bin/python scripts/audit_missing_talas.py \
  --inventory ../../output/track-144/remaining-tala-inventory.json \
  --output ../../output/track-144/remaining-tala-candidates.json \
  --cache ../../output/track-144/source-cache --offline
```

Omit `--offline` only when fetching the approved public blog sources is intended. Fetch failures and missing URLs are represented explicitly; partial results are written atomically. The cache is keyed by URL; the manifest checksum identifies its decoded UTF-8 HTML content, not the original HTTP bytes. Local `output/` inventories and caches are ignored artifacts, so preserve them separately if needed for review.

For a subsequent batch, review composer, canonical raga/alias and source pallavi for each candidate; record conflicts rather than choosing one silently. Create a new manifest following the checked-in examples, then run `scripts/compile_tala_repairs.py <manifest.json> <new-versioned-migration.sql>`. Validate on a populated isolated copy and a clean database through Flyway before applying. Keep existing migration and manifest pairs unchanged. Verify per-row evidence/audit, DB/API values and search after application. No auto-accept threshold is provided.

## Original review findings — pre-repair snapshot

### 1. P1 — Bugs / Compliance: Sri Narada Nada was assigned the wrong tāla

`database/migrations/V61__rasika_field_catalogue_repairs.sql:29–34` assigns Adi to both Marugelaraa and Sri Narada Nada. The latter is the Kanada composition whose pallavi begins “SrI nArada nAda sarasI-ruha”. The live DB and public catalogue API both now expose Adi.

[Sruti's 2017 Tyagaraja Aradhana souvenir, PDF page 8](https://www.sruti.org/wp-content/uploads/2020/01/TAS2017.pdf#page=8) explicitly identifies this composition with Rupaka. It separately identifies Sri Narada Muni in Bhairavi with Adi. The [stored lyric source](https://thyagaraja-vaibhavam.blogspot.com/2007/08/thyagaraja-kriti-sri-naarada-nada-raga.html) establishes the incipit and Kanada identity but provides no tāla supporting V61's assertion.

Correct the Kanada composition to canonical Rupaka through an additive, audited Flyway repair, guarded by composer, raga, incipit and expected current value. Preserve the citation. Do not edit an already-applied migration checksum. Do not apply the same correction to similarly named Narada compositions.

### 2. P1 — Bugs: repairs leave search documents and embeddings inconsistent

`V61:30–89` changes catalogue metadata and lyrics but only strips URL strings from existing search documents. It neither reconstructs affected documents nor updates hashes or embeddings. Read-only queries reproduce these discrepancies:

- dIna janAvana's overview still contains `[Raga: bhUpALaM - bhauLi]`.
- Sri Narada Nada and Marugelaraa overviews still contain `[Tala: Unknown]`.
- Marugelaraa's overview omits the inserted pallavi and has no corresponding `SECTION_PASSAGE` document.
- Across the current search corpus, 194 documents have `content_hash <> md5(indexed_content)`. This is a current inconsistency count, not proof that V61 caused every mismatch.

Use the existing indexing workflow to rebuild affected overviews and passages and regenerate their embeddings; verify the source text, document hash and embedding hash agree. Merely recalculating hashes over changed text would incorrectly mark old vectors as current. Verify both lexical and semantic retrieval after repair.

### 3. P2 — Bugs / Compliance: unknown raga classification is presented as janya

`modules/shared/presentation/src/commonMain/kotlin/com/sangita/grantha/shared/presentation/explore/ExploreFilters.kt:40–47` puts every raga without its own melakarta number into the janya group. The live `sumadyuti` row has neither a number nor a parent, despite the new nomenclature relation to Simhendramadhyamam. Its four Dikshitar compositions therefore appear under a misleading janya classification.

Keep unclassified ragas separate and represent alternate raga systems deliberately. A nomenclature relation does not populate hierarchy fields. The [SSP translation](https://guruguha.org/wp-content/uploads/2022/03/ssp_7to12.pdf) identifies Sumadyuti as raganga 57. A regression case must cover this actual catalogue shape, not only well-populated synthetic melakarta/janya rows.

### 4. P2 — Security / Compliance: mutation audit is incomplete

`V61:92–122` inserts a relation without an audit entry and records URL cleanup as one entry with a null entity ID. There are no individual changed lyric IDs or before/after values. The current TRACK-144 audit consists of four `krithis` entries and one blanket `krithi_lyric_sections` entry; variant, section, junction, relation and search-document mutations are not individually reconstructable from it.

Future repair migrations should capture changed rows and old/new values, operation and evidence URL, and audit only actual mutations in the same transaction. Historic deleted text cannot be reconstructed from the blanket entry alone; do not fabricate a retrospective before-image.

### 5. P2 — Bugs: the pallavi repair writes Latin into every script

`V61:58–72` inserts the same Latin pallavi into every lyric variant and prepends it to every variant's full lyrics. There is no script guard. The current Marugelaraa has only Latin, so this has not corrupted a non-Latin variant here. Applying this migration to a populated multilingual deployment would mix scripts. Its title-only scalar selectors also fail on multiple same-title rows, and title-only UPDATEs may affect other composers.

Future repairs must uniquely establish the composition and variant, require the expected prior section structure, restrict supplied text to its actual script, and reject ambiguous matches. Test against a multilingual fixture and a same-title different-composer fixture.

## Validation of the requested claims

| Claim | Result |
|:---|:---|
| Sri Narada Nada has Adi | Stored and served, but musically contradicted by the cited Rupaka source; correction required. |
| It has one script, with no invented second script | Confirmed: one Latin variant. The cited source actually contains other scripts, so a source-backed enrichment can supply them after section validation. |
| dIna janAvana now points to Bhoopalam | Confirmed in the primary FK, junction and API; Bowli is separate and the combined raga has zero junction uses. Search still needs repair. |
| Bhoopalam and Bowli should not be conflated | Correct. The [composition source](https://thyagaraja-vaibhavam.blogspot.com/2009/01/tyagaraja-kriti-deena-janaavana-raga.html) gives alternative raga attributions. Preserve that evidence when choosing Bhoopalam as the displayed tradition; a combined name is not a raga identity. |
| Parenthetical source URLs were removed | Current lyric sections, normalized text and full variants contain zero HTTP(S) URLs; Meenakshi's anupallavi was checked. No pre-migration snapshot was available to prove every deletion preserved all text. |
| Dikshitar uses Gamakakriya and Sumadyuti | Confirmed: five and four compositions respectively. The Sumadyuti–Simhendramadhyamam nomenclature relation exists. |
| Poorvikalyani is a janya of that melakarta | If “that” means Simhendramadhyamam, incorrect. The DB has both Poorvi Kalyani and Gamakakriya under Gamanashrama (53). Keeping separate identities is defensible, but the rationale needs correction. [Guruguha's SSP analysis](https://guruguha.org/gamakakriya-contribution-guru-sreshta/) explains the historical relationship and differences in melodic usage; janya status alone does not disprove that relationship. |
| 345 unknown tālas remain | Current count is 343; there are no null-tāla rows in the audited cohort query. The count is consistent with V61 filling two values, but the historic 345 snapshot was not independently available. |

Marugelaraa is currently Jayanthashri and has pallavi, anupallavi and charanam. V62 repairs V61's erroneous Jayantasena change. Across the catalogue, primary-raga junction gaps and per-variant section-count mismatches are both zero. Count parity alone does not establish correct text or script assignment.

## Concrete resolution plan for the remaining tālas

The review exported every unresolved composition, UUID, composer, raga, linked imports and evidence URLs to `output/track-144/unknown-tala-inventory.json`, with its reproducible read-only query beside it. The export contains 343 distinct IDs. These local output artifacts are not committed.

| Composer | Unknown tālas | Existing evidence host |
|:---|---:|:---|
| Tyagaraja | 337 | thyagaraja-vaibhavam.blogspot.com |
| Muthuswami Dikshitar | 5 | guru-guha.blogspot.com |
| Syama Sastri | 1 | syamakrishnavaibhavam.blogspot.com |

All 343 have source-evidence and mapped-import records. All have an import saying Unknown. Four also have a non-Unknown import, but only one is a straightforward candidate:

| Composition / UUID | Import value | Disposition |
|:---|:---|:---|
| Rama Rama Rama Sita / `42d35ca5-4110-4fd8-bece-9ced6a37014e` | Adi | Candidate verified against the [Saveri source page](https://thyagaraja-vaibhavam.blogspot.com/2007/04/thyagaraja-kriti-sri-rama-rama-rama.html); compare stored pallavi before applying. |
| Sri Narada Muni / `76706063-efdd-49a7-b091-4817afed5b57` | Rupakam | Reject direct reuse: stored Bhairavi, imported Pantuvarali. Sruti independently identifies the Bhairavi composition as Adi; verify its stored incipit. |
| SrI nArasiMha / `12058429-320a-4040-b682-78c26399e0ae` | Isra Capu | Identity conflict: stored Phalaranjani, imported Bilahari. |
| Sri Rama Jaya Rama / `1dfe4864-6224-460a-845a-5328aed9b638` | Isra Capu | Identity conflict: stored Madhyamavati, imported Varali. |

Recommended implementation sequence:

1. Build a read-only candidate pass over the frozen inventory. Fetch/cache existing evidence pages and extract explicit tāla declarations with source location and a short evidence excerpt. Distinguish absent source metadata from parser failure. Do not default to Adi.
2. Require composition identity using composer, validated raga identity/relationship, and pallavi or incipit. Title similarity and an existing `mapped_krithi_id` are insufficient: the three conflicts above demonstrate why.
3. Resolve names against a reviewed canonical vocabulary. Rupakam/Rupaka is a spelling normalization; bare Capu does not identify its subtype. The catalogue already has 28 assignments to “Isra Capu”; quarantine that label for provenance review rather than assuming it means Misra or Tisra.
4. For sources that omit tāla, consult notation editions or other independently identified sources. Preserve competing traditions and disagreements for musicological review. Do not infer tāla from lyrics, raga or composer, or treat an LLM's confidence as evidence.
5. Produce a review manifest: composition UUID and identity, expected old tāla, proposed canonical ID, raw source spelling, URL/page, source checksum, extraction method, agreement/conflict status and reviewer decision. Track counts for verified, conflicting, source-missing and unresolved records.
6. Apply accepted rows with a new Flyway migration and row-specific audit/evidence. Guard the expected current value to avoid overwriting intervening editorial changes. Rebuild affected search documents and embeddings, then verify DB and API values against the manifest.
7. Add meaningful regression fixtures for wrong-composition imports, tala subtypes, missing metadata and explicit-source extraction. Improve matching/extraction at the point of failure to prevent recurrence.

This is a credible route to reducing the backlog, not a claim that all 343 can be determined without further evidence. Even the small initial candidate set shows why an unrestricted SQL backfill would publish wrong data.

## Original review verification and limits — 24 September

`make test-mobile` completed successfully:

```text
BUILD SUCCESSFUL in 2s
26 actionable tasks: 6 executed, 20 up-to-date
mobile-data: 25 tests, 0 failures, 0 errors, 0 skipped
presentation: 54 tests, 0 failures, 0 errors, 0 skipped
```

Read-only PostgreSQL checks and three public catalogue API detail checks completed. The mobile-data test task was up-to-date; presentation JVM tests executed. These suites do not prove data correctness. `make check-docs` failed on the existing link from `conductor/tracks.md` to the untracked TRACK-144 file; the checker inventories Git-tracked files. No device UI journey, clean migration replay, embedding regeneration, or Android/iOS native build was performed in this review. At that review point the track recorded Draft Intent, Spec and Plan, and no commits or database writes had been made. The accepted implementation and later writes are recorded above.
