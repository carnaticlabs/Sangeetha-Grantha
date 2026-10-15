| Metadata | Value |
|:---|:---|
| **Status** | Implemented — shared artifact contract |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-01 |
| **Author** | Sangita Grantha Team |

# Musicological classification master contract

Owner: [TRACK-146](../../../conductor/tracks/TRACK-146-syama-sastri-musicological-classification.md). TRACK-147 extends these conventions for Tyagaraja; its existing independent tooling remains intact until an explicit adapter migration is planned.

## Identity and accounting

A composition receives a persistent master ID of the form `PREFIX-NNNN` (Syama Sastri: `SS-0001` to `SS-0070`). It is assigned once, stored in the master, and never recomputed from catalogue UUIDs, row numbers or titles; the Syama Sastri IDs were minted in order of each identity's first CSV row in schema 1.1. Catalogue UUIDs (`canonical_id`, `composer_id`) are rebindable bindings: krithi and composer UUIDs are generated at import, so a rebuilt catalogue changes only those fields while master IDs, claim IDs and approvals stay intact. A composition may be unbound (`canonical_id` null). Claim IDs are `<master ID>:<axis>:<n>` and must start with their subject's master ID. Reordering source rows, claims or compositions must not change those identities or generated output. A source row is a locator within the hashed CSV, not a composition ID. Preserve every source row exactly once and explicitly map alias candidates. Excluded candidates stay in reconciliation without becoming additional composition identities. DB-only identities belong to the declared composer census even if they have no CSV row.

Report source rows, distinct composition identities, approved group membership claims and witness artifacts separately. The 71 Syama Sastri CSV rows map to 70 master identities because two labels (rows 1 and 22, `SS-0001`) share one witness and UUID. Canonical URL mappings are repository observations, not human musicological approval.

## Witnesses and claims

Reuse canonical UUIDs and source-evidence identities. The artifact master stores independent claims where current source/canon tables do not express adjudication granularity. Available witnesses require a SHA-256 artifact hash, URL/path, access date and status. Institutional scholarly witnesses from The Music Academy, Madras (Smt. Vidya Shankar's *Syama Sastry's Compositions*, JMA volumes), Sangeetha Kalanidhi T.K. Govinda Rao (*Compositions of Syama Sastri*), Prof. P. Sambamoorthy (*Great Composers: Syama Sastri*), and Subbarama Dikshitar's *Sangita Sampradaya Pradarsini* provide authoritative multi-witness corroboration for laya, swarasahitya, and shrine traditions. Unavailable witnesses may retain locatable research leads but cannot approve an assignment. The database snapshot is hashed separately; stored values are not scholarly authority.

A claim identifies its subject, axis, value, witness, precise locator, summary, evidence kind, review state and uncertainty. Evidence kinds are textual evidence, scholarly interpretation, documented tradition and automated suggestion. Review states are unreviewed, reviewed, unresolved, disputed and rejected. Reviewed claims require an explicit named human approval, date and decision note, a resolved canonical identity and a hashed available witness. Automated-only suggestions cannot become reviewed claims; obtain a separate evidenced claim. Plan acceptance does not approve individual claims.

Claim axes distinguish primary structural, aesthetic, and laya dimensions:
- `attribution`: Canonical composer identification.
- `form`: Musical form candidates (`KRITHI`, `VARNAM`, `SWARAJATHI`, `UNESTABLISHED`).
- `raga` and `tala`: Nominal labels as attested by witnesses.
- `laya`: Distinct rhythmic architecture, specifically separating `Viloma Chapu (4+3)` from standard `Misra Chapu (3+4)`, `Anagata Eduppu` offsets (1/2, 3/4, 1 1/2 matras), and `Arudi` landing points.
- `structure`: Structural innovations such as `Swarasahitya` (pairing chittaswara and sahitya note-for-syllable), `Swarajathi Ratnatrayam`, and `Swarakshara`.
- `raga_character`: Melodic status, such as Apex Sovereign raga (`Anandabhairavi`), Composer Invention (`Chintamani`), or rare/affective janya affinity (`Kalgada`, `Manji`, `Ahiri`, `Dhanyasi`, `Varali`, `Paraju`).
- `bhava`: Devotional posture, primarily `Apatya Bhava` (filial intimacy / child-mother yearning: *Mayamma*, *Talli*, *Amma*) and `Ananya Saranagati`.
- `mudra`: Signature ankitam (`Syama Krishna`, `Syama Krishna Sodari`, `Syama Krishna Palita`).
- `geography`: Supported shrine associations.
- `group`: Formal cycles (`Ratnatrayam`, `Navaratnamalika`).

Geography states distinguish UNREVIEWED, SPECIFIC_SHRINE_SUPPORTED, NON_SPECIFIC_ADDRESS, UNRESOLVED and DISPUTED. A composition's state is derived from its geography claims and the validator rejects any stored value that disagrees:

| State | Required geography claims |
|:---|:---|
| UNREVIEWED | None recorded |
| DISPUTED | At least one disputed claim |
| SPECIFIC_SHRINE_SUPPORTED | At least one approved claim naming a place, and no approved `NON_SPECIFIC_ADDRESS` |
| NON_SPECIFIC_ADDRESS | An approved claim with value `NON_SPECIFIC_ADDRESS`, and no approved named place |
| UNRESOLVED | Claims recorded, none approved or disputed |

A geography claim whose value is `NO_PLACE_STATED` records that a witness names no place. It is an observation and cannot be approved: absence in one passage is not proof of a non-specific address, and an addressee epithet such as *bangAru kAmAkshi* may still identify a shrine. A curator approves either a named place or `NON_SPECIFIC_ADDRESS` as a separate claim. A named address in one source is a candidate association, not a seedable temple UUID. A null temple_id cannot express review state. Group types distinguish formal groups, thematic collections and related musical settings, independently of existing tag categories. Nine historical slots do not justify nine canonical members when titles/ragas are unknown or conflicting.

## Rhythm and search

Source-stated tala can be recorded alone. Eduppu, arudi, kalai and subdivision require witness notation/rendering and explicit units, counting origin and convention. Multiple observations remain witness-specific. Do not derive rhythmic offsets from lyric text or treat a rendition as a universal composition property.

TRACK-145's existing formatter and macro indexer remain the shared search implementation, extended for composer-adaptive musicological headers. For Syama Sastri compositions, the context formatter incorporates micro-clauses for `Laya` (Viloma Chapu / Anagata Eduppu), `Structure` (Swarasahitya), `Raga Character`, `Bhava` (Apatya Bhava), and `Mudra` (Syama Krishna) alongside musical form, raga, tala, deity, and kshetra. Retain the strict 60-word header budget and short section passages.

Derive macro indexer composer attribution from members; empty or mixed-composer cycles abstain. Macro documents represent `syama-sastri-swarajathi-ratnatrayam` and `syama-sastri-navaratnamalika` with complete composition rosters, ragas, and laya structures. Syama Sastri membership attributes must carry TRACK-146 ownership, reviewed state, claim ID and explicit human approval. Legacy heuristic Ratnatrayam/Navaratnamalika tags are omitted from generated composition headers until approved membership evidence is supplied. No additional public search API is implied.

## Application and provenance

The generator emits only approved claims into an application manifest. An empty manifest has status awaiting_claim_approval and performs no database write. It is a review package, not an executable bulk mutation interface; application additionally needs expected before/after values, exact canonical UUID/composer/raga guards and provenance-scoped conflict detection. An approval may set `application: deferred` with a `deferral_reason` when no reviewed application path exists yet. The claim stays approved and listed in the manifest, but no seed or search path may apply it until the deferral is lifted.

Use Flyway for schema/reference dictionary and approved group seed changes. R__seed_07 now retains tag dictionary entries and unrelated composer behavior without reconstructing Syama Sastri links. R__seed_08 is retired as a no-op; it preserves existing forms and writes no artificial audit events. R__seed_10 applies the approved Swarajathi Ratnatrayam memberships from the manifest with provenance-scoped conflict guards (aborting if independently reviewed rows exist) and writes system-attributed audit_log entries only when rows actually change.

Composition-content, attribution, form and shrine corrections belong on existing audited canon/import paths to preserve source/revision history. Runtime database access stays inside DatabaseFactory.dbQuery and returns DTOs. Genuine changes require audit attribution; reruns must not manufacture audit events. No corpus reset or paid embedding run is part of offline implementation.

## Generation and verification

Implementation: `tools/krithi-extract-enrich-worker/src/musicological_classification.py` and `scripts/generate_classification_master.py`. Run from the worker directory:

```bash
uv run python scripts/generate_classification_master.py \
  --master ../../database/data/track146-syama-sastri-master.json \
  --output ../../application_documentation/01-requirements/features/syama-sastri-classification-master.md \
  --manifest ../../database/data/track146-syama-sastri-application-manifest.json \
  --check
```

The validator checks exact CSV row values/count/hash, unique IDs/UUIDs, master-ID format, subject-anchored claim IDs, source/claim references, composer boundaries, approval requirements, conflicting canonical forms and claim-derived geography state. Generated distributions retain observed catalogue values separately from source claims. Tests cover malformed accounting, unavailable witnesses, automated-only evidence, reorder stability, unknown rhythm conventions, cross-composer macro attribution and approval abstention.
