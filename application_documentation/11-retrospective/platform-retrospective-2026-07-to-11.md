| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-13 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Critical Retrospective: July to November 2026

---

**Period:** 2026-07-14 (the Tyagaraja import post-mortem, the last retrospective in this folder) to 2026-11-13.
**Scope:** the whole platform: the Rasika app, the Curator Console, the backend and database, the Python worker, the musicological data tracks, and the documentation.
**Method:** git history, the conductor track files, and a documentation sweep run on 2026-11-13. No interviews were held and no production telemetry was used. Counts that come from keyword matching on commit titles are labelled as estimates.

> [!NOTE]
> Historical evidence: the counts and observations below describe the period named above. For present behavior, use the [feature map](../01-requirements/features/README.md).

## 1. Summary

The period was highly productive: 228 non-merge commits across TRACK-125 to TRACK-159, taking Rasika from a first discovery release to collections, kshetras, lakshana facets, search cards and lyric search, adding Mobile Preview to the Curator Console, and classifying the Dikshitar, Syama Sastri and Tyagaraja corpora.

It was not a clean period. The same few kinds of defect kept reaching the main branch or the user, and each was found late:

- a search mode that promised something it did not do, found by accident;
- thresholds and layouts tuned on too little data and reworked within a day;
- lint, type and iOS-compile failures caught by CI rather than before the push;
- seeded content corrected by later migrations;
- summary documentation that fell weeks behind while every automated documentation gate passed;
- two tracks whose goal turned out to be unachievable, discovered weeks in.

Section 3 lists these with evidence. Section 5 proposes changes.

## 2. What the numbers say

| Measure | Value | Note |
|:---|:---|:---|
| Non-merge commits | 228 | 2026-07-14 to 2026-11-13 |
| Tracks touched | 39 | Mostly TRACK-125 to TRACK-159 |
| Commits that look like fixes (estimate) | 48 (about 21%) | Keyword match on `fix`, `correct`, `repair`, `lint`, `format`, `mypy`, `stale`; some are planned repairs, so this overstates rework |
| Tooling fix-ups (estimate) | 14 | Lint, format, strict mypy, iOS test compile, Android lint |
| Content or naming corrections (estimate) | 14 | Seeded data, wording, region and temple names |
| Busiest tracks by commits | TRACK-149 (24), TRACK-151 (16), TRACK-152 (12), TRACK-154 (12), TRACK-157 (11) | |
| Ten Rasika and search tracks, TRACK-150 to TRACK-159 | Landed 2026-10-16 to 2026-11-03 | About three weeks |
| Tracks still In Progress | TRACK-148, TRACK-149 | Open since 2026-10-15 |

A fifth of commits being corrections is not alarming for data-heavy work, but the tooling share should be near zero (see 3.4).

## 3. Critical findings

### 3.1 A search label shipped that the code did not honour

Rasika's lexical mode was labelled **Titles & lyrics**, but the query had no lyric clause. The raga and composer pickers loaded only the first 30 entries, so 30 of 1,014 ragas could be chosen. Neither defect was found by testing. Both were noticed in November by reading code while designing Mobile Preview (TRACK-158 findings D7 and D8, fixed by TRACK-159).

Why it matters: the label was visible to every user from the first release, and the TRACK-140 acceptance journeys exercised search but never asserted that the label matched the behavior. A catalogue whose purpose is to be trusted for text shipped a text search that did not search text.

### 3.2 Thresholds and layouts were tuned on too little data

- **Search card floor.** TRACK-152 shipped a relevance floor of 0.83 calibrated from two data points. The weakest real card scored 0.836, a 0.006 margin, while the exact-title kriti probes peaked at 0.787. TRACK-157 recalibrated the floor per profile and added a gap condition within days of TRACK-152 closing.
- **Kshetra list.** TRACK-155 found, in the iOS simulator one day after TRACK-151 completed, that the Tiruvarur page (114 kritis) was not scrollable like a list and that the Kshetras list lost its place on Back. The fixtures behind the tests were small; real data volume was never in the loop.

Common cause: the acceptance data was smaller and kinder than the production catalogue.

### 3.3 Tracks were declared complete before they were accepted on device

TRACK-138 and TRACK-140 are marked Completed in the registry. The mobile requirements, the README and the experience guide all say native-device execution and TalkBack/VoiceOver proof remain release gates. Both statements are true, which is the problem: "Completed" means the code and shared tests landed, not that the release gate was met, and a reader of the registry cannot tell the difference. The same gap exists for every Rasika track since.

### 3.4 The same tooling failures reached CI repeatedly

Despite TRACK-126 (zero ruff and mypy with CI enforcement, July), separate fix-up commits for worker lint, format or strict mypy appear for TRACK-144, 145, 146, 147, 149 (twice), 152 and 154. The iOS test-compile failure fixed as "use `assertTrue` so tests compile for iOS" appears for TRACK-153 on 2026-10-27 and again for TRACK-151 on 2026-10-28: the same mistake, in two tracks, a day apart. CI caught all of them, which is what it is for, but each is a pushed commit, a CI cycle, and an extra line in history that a local check would have avoided.

### 3.5 Seeded content needed correction after it was seeded

- The Saraswati temple was recorded in the wrong place and corrected to Basara (V69, with documentation sync).
- Poornalalita and Poornalalitha were duplicate ragas, merged by V70; the parent of Poornalalitha is still recorded as unverified because sources conflict.
- Region naming was changed from Nadu to Desam across the Rasika kshetra work after the first implementation.
- Two seeded overviews contained developer wording that users would have read; TRACK-152 replaced it and added a guard.

Because committed migrations are never edited, each correction is a new forward migration or a new seed, which is the right discipline but means the first version of the data stays in history. The recurring cause is that reviewed content was applied before an independent read-through of how it would look to a listener.

### 3.6 Two tracks were scoped around sources nobody had

TRACK-148 and TRACK-149 were opened on 2026-10-15 to consult institutional print editions and journal volumes. On 2026-11-03 an owner amendment recorded that no print edition is available, so only public sources could be used. The result is honest but thin: of 40 Syama Sastri leads, 15 were approved (deferred) and 24 left unresolved with one disputed; of 94 Tyagaraja leads, 25 were approved (deferred) and 69 left unresolved. Roughly three weeks passed before the premise was tested. The real outputs of the tracks (the 27 of 29 structural repairs, the collision split, the shared manifest-driven path) are valuable but were not the stated goal.

### 3.7 Summary documentation drifted for six weeks while every gate passed

This is the most instructive finding because it was found by the sweep that preceded this retrospective. The root README was last updated 2026-09-30. During those weeks ten Rasika and search tracks landed. When the sweep started, `make check-doc-impact` reported "no undocumented changes" and the link check passed. The sweep still found:

| Document | Stale content |
|:---|:---|
| Root README | Collections described as planned; no kshetras, facets, search cards or Mobile Preview |
| Feature map | Maps link described as waiting for TRACK-154 (already shipped); no collections, facets or Mobile Preview |
| Kshetra exploration doc | Status "In Progress" and the Maps link "pending" after TRACK-151 and TRACK-154 completed |
| Migrations guide | `V01` to `V60` and six seeds; the repository had `V70` and fourteen |
| Product PRD | Said Rasika "uses catalogue queries rather than vector-search endpoints" |
| Mobile UI spec | Described Decompose or Voyager, SQLDelight, Coil and `/v1` search, none of which Rasika used |
| UI libraries | Five of eight admin libraries and two of five mobile libraries listed were not dependencies |

Why the gates passed: the impact gate maps code paths to documents and is satisfied by any edit to a mapped document. Each track did update its own implementation record, so the gate was satisfied, while the roll-up documents that readers open first were not touched. Several documents also carried a banner saying they "may include proposals or earlier implementation assumptions", which made wrong statements look acceptable.

### 3.8 Registry and working tree disagreed

At the start of the 2026-11-13 session the working tree on `main` held uncommitted changes for the google-genai 2.x upgrade (a new `gemini_transport.py`, edits to the embedder, enricher, lockfile and track file) while the registry lists TRACK-124 as Completed. Whether this is a reopened track or unfinished work is not recorded anywhere. Project rules say not to commit on `main`; uncommitted work sitting there for an unknown time is the other half of the same risk.

## 4. What worked and should be kept

- **Evidence-first data work.** Deferring unresolved claims rather than inventing them, recording the owner's decision on each lead (134 leads decided on 2026-11-03), and recording the Poornalalitha parent as unverified are the right behavior for a scholarly catalogue.
- **Reviewed place ids.** TRACK-154 confirmed 84 of 85 reviewed temples with 91 of a 150-call budget, kept Google-derived coordinates out of the database, and recorded the approver.
- **Honest copy.** Recorded gaps, open sets and "completeness not established" are shown rather than hidden.
- **Mobile Preview.** Building it surfaced defects D7 and D8 and now lets a curator see a stale search index. It paid for itself within its own track.
- **Guarded test edits.** The test-edit gate was used once, deliberately, in TRACK-148; the guard worked as designed, although the test it blocked in TRACK-146 stayed outstanding for weeks.
- **Forward-only migrations and a single engine.** V69 and V70 repaired data without rewriting history.

## 5. Recommendations

These are proposals. None is in a plan yet; each would need a track or an owner decision.

| # | Proposal | Addresses |
|:---|:---|:---|
| 1 | Add a test per search mode that asserts the visible label against behavior (a lyric-only phrase for Titles & lyrics; the full raga list reachable from the picker) | 3.1 |
| 2 | Add one realistic-volume fixture (a 114-kriti temple, the full raga list) to shared tests and to the simulator checklist | 3.2 |
| 3 | Require recorded score distributions and a stated margin on both sides before any new relevance threshold ships (TRACK-157 already does this for macro cards) | 3.2 |
| 4 | Split "Completed" into code-complete and device-accepted in the registry, or add a column for the native gate | 3.3 |
| 5 | Add a single local verification target that runs worker ruff and mypy and an iOS test compile for changed layers, and run it before push | 3.4 |
| 6 | Read seeded user-facing text once as a listener before applying it; keep the developer-wording guard | 3.5 |
| 7 | At Intent acceptance, confirm that every named source is obtainable; if not, scope the track to what is | 3.6 |
| 8 | Extend the documentation checks with facts that can be computed: highest migration number and repeatable-seed count against the migrations guide; declared dependencies against the libraries guide; tracks marked Completed against the feature map; and a staleness warning when the root README is older than the newest completed track | 3.7 |
| 9 | Run `/update-docs` when a batch of tracks closes, and stop using the "may include proposals" banner on documents that claim to describe current behavior | 3.7 |
| 10 | Record in the registry whether an uncommitted or reopened track is in progress, and resolve the TRACK-124 state | 3.8 |

## 6. Limits of this retrospective

- Commit-title keyword counts are estimates and include planned repairs.
- It does not measure production behavior, review latency or user feedback; none was available.
- The documentation findings come from one sweep and a partial verification of the rewritten documents; other documents not examined may be stale as well, in particular the quality reports, the sourcing requirements and the state-of-nation document.
- Section 3.8 rests on the repository state at the start of one session and has not been confirmed with the track owner.

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
