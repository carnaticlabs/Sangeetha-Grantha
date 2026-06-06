| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-06-06 |
| **Author** | Principal Data & AI Engineering review (for Seshadri) |
| **Baseline reviewed** | `current-versions.md` v1.1.0 (2026-03-10) |
| **Review window** | 2026-03-10 → 2026-06-06 (~3 months) |

# Sangeetha Grantha — State of the Nation, Mid-2026

> A dependency and AI-ecosystem review after a ~3 month gap. The goal is a short list of changes that are **practical, relevant, and worth your time** — and an equally explicit list of changes that are **noise you should ignore**. Findings are severity-rated. Nothing here is recommended on novelty alone.

---

## 1. Executive Summary

The codebase itself has aged gracefully — most of your library versions are still current or one minor release behind, which is fine. **The risk is not in your build files; it is in your AI layer.** Two of the three Gemini models this project depends on have reached or passed Google's retirement dates, and the Python AI SDK you ship in the extraction worker was deprecated last year with its migration deadline already lapsed. These are not "upgrade when convenient" items — they are correctness-and-availability items that will cause hard failures (HTTP 404 on the model endpoint, no security patches on the SDK) if left.

The good news is that the same churn that created these breaks also opened three genuinely useful, low-hype opportunities that map directly onto features already named in your own `09-ai/integration-opportunities.md`: **Batch Mode (50% cheaper bulk processing), first-class JSON-Schema structured output (kills your hand-rolled prompt-and-pray JSON parsing), and a GA embeddings model that finally makes the "semantic search" opportunity buildable without standing up a separate ML stack.**

### Findings at a glance

| # | Finding | Severity | Area |
|---|---------|----------|------|
| F1 | Gemini 2.0 Flash (your primary model) reached retirement on 1 June 2026 | **Blocker** | AI |
| F2 | Gemini 1.5 Pro (musicological validation) is deprecated | **Critical** | AI |
| F3 | `google-generativeai` 0.8.6 Python SDK deprecated; migration deadline already passed | **Critical** | AI / Python |
| F4 | No structured-output (JSON Schema) usage — extraction relies on prompt-coerced JSON | **Major** | AI |
| F5 | Bulk transliteration/scraping runs at full synchronous price; Batch Mode would halve it | **Major** | AI / Cost |
| F6 | "Semantic search" opportunity remains unbuilt; embeddings are now GA and trivial to adopt | **Major** | AI / Feature |
| F7 | Backend one minor behind (Kotlin 2.4, Ktor 3.5 available) | **Minor** | Backend |
| F8 | Frontend already on the new `@google/genai` SDK — no action, just verify model strings | **Observation** | Frontend |
| F9 | Gemma 4 12B (open-weight) — not a fit for the core path; one eval-gated fine-tune angle worth watching | **Observation** | AI (see §5) |

---

## 2. Section A — AI Ecosystem (the part that actually matters)

The model lineup moved a full generation in three months. Here is the lineage relevant to you:

```
Gemini 1.5 Pro      → deprecated (your validation model)
Gemini 2.0 Flash    → RETIRED 1 Jun 2026 (your primary model)
        ↓ replaced by
Gemini 2.5 Flash    → stable, but itself slated to shut down 16 Oct 2026
        ↓ replaced by
Gemini 3.5 Flash    → GA since 19 May 2026, no shutdown date announced
```

### F1 — Gemini 2.0 Flash has been retired [BLOCKER]

`gemini-selection-rationale.md` selects **Gemini 2.0 Flash** as the workhorse for transliteration, scraping, and metadata normalisation, and **Gemini 1.5 Pro** for musicological validation. Google retired `gemini-2.0-flash-001` (and `-flash-lite-001`) on **1 June 2026**. Any code path still pinning that model string is now calling a dead endpoint or being silently redirected — neither is acceptable in a system of record.

**Recommendation — skip a generation, do not chase the treadmill.** Migrating to **Gemini 2.5 Flash** buys you only ~4.5 months before its own 16 Oct 2026 shutdown forces a second migration. Go straight to **Gemini 3.5 Flash** (GA, no announced retirement). It is reported to be ~4× faster on output tokens than the 2.x line, beats the previous 3.1 *Pro* tier on hard benchmarks, and lands around **$1.50 / million input tokens**. For your batch-heavy, extraction-and-transliteration workload that is the right tier on speed, cost, and longevity simultaneously.

This is a config-and-eval change, not a re-architecture — but treat the eval seriously: re-run your Indic transliteration golden set (the La/Lla/Zha and Grantha-vs-Tamil distinctions you flagged) before flipping production. Newer ≠ automatically better on *your* niche; verify, don't assume.

### F2 — Gemini 1.5 Pro validation path is deprecated [CRITICAL]

The "musicological validation" task (checking Arohanam/Avarohanam against lyrics) is the one place you deliberately chose a heavier reasoning model. With 1.5 Pro deprecated, consolidate onto **Gemini 3.5 Flash** here too and A/B it against the (soon-to-roll-out) 3.5 Pro tier only if Flash's reasoning proves insufficient on your validation set. In practice the 3.x Flash reasoning is now in the range that previously required a Pro model, so you may be able to **collapse two model tiers into one** — simpler billing, one prompt library, one place to monitor drift.

### F3 — Python `google-generativeai` SDK is deprecated [CRITICAL]

Your extraction worker (`tools/krithi-extract-enrich-worker`) ships `google-generativeai==0.8.6`. That package was **deprecated on 30 November 2025**, and the Gemini-API migration deadline (**31 August 2025**) has already passed. It does not receive new features (Batch Mode, Live API, the newest structured-output surface) and is on borrowed time for fixes.

**Migrate to the unified `google-genai` SDK** (currently ~1.62.x). This is not cosmetic — it is the precondition for F4–F6 below. The migration is mechanical but real: the old SDK configured a global client implicitly (`genai.configure(api_key=...)`); the new one is client-first (`client = genai.Client(...)` then `client.models.generate_content(...)`). One client, both backends (Gemini Developer API *and* Vertex AI) behind the same interface — which matters because `gemini-selection-rationale.md` already anticipates a Vertex AI / unified-billing path on GCP. The new SDK lets you switch Developer-API ↔ Vertex with a constructor argument instead of a rewrite.

> Frontend note (F8): the admin web already depends on `@google/genai 1.34.0` — that *is* the new unified JS SDK, so the frontend is on the right side of this split. The only action there is to confirm no hard-coded `gemini-2.0-flash` / `gemini-1.5-pro` strings remain.

### F4 — Adopt native JSON-Schema structured output [MAJOR]

Your integration docs show transliteration/extraction prompts that end with *"Provide ONLY the … text, no explanations"* — i.e. you are coercing format through prompt wording and then parsing optimistically. Google has since added **full JSON Schema support across all current Gemini models**, and the unified SDKs accept a **Pydantic model (Python) or Zod schema (JS) directly as `response_schema`**. You already run **Pydantic 2.12** in the extraction worker, so this is nearly free leverage:

- Replace "please return JSON" prompts with a declared `response_schema=KrithiExtraction` Pydantic model.
- The model is constrained to your schema at decode time → fewer malformed payloads landing in `imported_krithis.parsed_payload`, less defensive parsing, fewer reviewer rejections caused by format drift rather than content.
- Schema-constrained output is also more stable across model upgrades — which directly de-risks the F1/F2 model swaps.

This is the single highest **effort-to-payoff** item in the report.

### F5 — Move bulk jobs to Batch Mode for ~50% cost [MAJOR]

The Gemini **Batch API** processes large asynchronous jobs (results within 24h) at **50% of synchronous price**, and Batch requests can carry the same `response_schema` from F4. Your scraping/transliteration backfills (the "thousands of compositions" ambition in the rationale doc) are the textbook fit: they are not latency-sensitive, they are embarrassingly parallel, and they run as backfills not user-interactive calls.

Keep synchronous calls only where a human is waiting (the "Generate Variants" button in `KrithiEditor.tsx`). Route everything that originates from the import pipeline or a scheduled backfill through Batch. On Gemini 3.5 Flash pricing, halving the bulk spend is a material, recurring saving with no quality trade-off — the same model, just scheduled.

### F6 — Build semantic search on GA embeddings, finally [MAJOR]

Opportunity #5 in your own `integration-opportunities.md` ("semantic search beyond keyword matching") has been parked. The blocker — needing an embeddings model and a vector store — is now much smaller:

- **`gemini-embedding-001` is generally available**, multilingual across 100+ languages (covers your Dravidian + Sanskrit corpus), with **Matryoshka Representation Learning**: emit 3072-dim vectors and *truncate to 1536 or 768* with minimal quality loss. Start at **768 dims** to keep your index small and cheap; raise only if recall demands it. (Google has also announced **Gemini Embedding 2**, a natively multimodal successor — note it, but build on the GA `001` model today; don't gate a shippable feature on a just-announced one.)
- **You do not need a new database.** PostgreSQL 18 + `pgvector` keeps embeddings beside your relational data — no separate vector store, no new operational surface, consistent with the "open-source, minimise integration debt" posture. A `krithi_embedding` table (krithi_id, section_id, vector, model_version) indexed with HNSW is enough to ship "find similar krithis / search by meaning" over the existing catalogue.
- Embeddings are cheap and batchable (F5 applies), so backfilling the whole corpus is a one-off offline job.

This is the one *new feature* (vs. maintenance) I'd actively pull forward, because it converts work you've already documented into a user-visible capability with a small, well-understood footprint.

### A note on what to *ignore* (anti-hype)

- **Agentic / "autonomous agent" framing.** Gemini 3.x is marketed heavily on agentic coding and long-horizon autonomy. Your AI surface is bounded extraction, transliteration, and validation with a human-in-the-loop review queue — that is exactly right for a system of record. Do not bolt on agent frameworks; they add non-determinism where you specifically want auditability.
- **Live API / streaming voice / multimodal generation.** Real capabilities, zero fit for a krithi archive right now. Skip.
- **Chasing every model point-release.** Pin to **3.5 Flash**, wrap the model string in one config constant, and re-evaluate on a schedule — not on every Google blog post. The lesson of F1 is that the treadmill is real; the defence is indirection (one config value) plus a golden-set eval, not constant migration.

---

## 3. Section B — Library & Dependency Deltas

Most of the stack is current. Listing only what moved and whether it's worth doing.

### Backend (Kotlin / JVM)

| Library | You have | Now available | Action | Notes |
|---|---|---|---|---|
| Kotlin | 2.3.0 | **2.4.0** (Jun 2026); 2.3.20 also out | Optional minor | Routine. Adopt on your normal cadence; no forcing function. |
| Ktor | 3.4.0 | **3.5.0** (15 May 2026) | Optional minor | 3.4 added OpenAPI generation, Zstd compression, structured-concurrency request lifecycle. Worth reading the 3.5 changelog for the OpenAPI gen — you maintain `openapi/sangita-grantha.openapi.yaml` by hand today. |
| Exposed | 1.0.0 | 1.0.x patches | Hold / patch only | You're already on the landmark 1.0 (R2DBC support, the major rewrite). Good place to be. Just take patch releases. |
| Koin, Coroutines, HikariCP, Logback, Caffeine, PostgreSQL driver | current-ish | minor/patch bumps | Batch as housekeeping | None urgent. Roll up in one dependency-bump PR. |

**Verdict:** no backend item rises above **Minor**. Do a single housekeeping bump PR when convenient; don't interrupt feature work for it. Exposed 1.0 was the only thing that could have been disruptive and you already absorbed it.

### Frontend (React / TypeScript)

| Library | You have | Status | Action |
|---|---|---|---|
| React | 19.2.4 | 19.x still current | Patch only |
| Vite | 7.3.1 | 7.x current | Patch only |
| Tailwind CSS | 4.2.1 | 4.x current | Patch only |
| TanStack Query | 5.90.x | v5 current | Patch only |
| React Router | 7.13.x | v7 current | Patch only |
| `@google/genai` | 1.34.0 | **already the new unified SDK** | Verify model strings only |

**Verdict:** the frontend is in good shape and notably already past the SDK split that bites the Python worker. No structural work. Only confirm there are no retired model strings hard-coded in the "Generate Variants" path.

### Python tooling

| Library | You have | Now | Action | Severity |
|---|---|---|---|---|
| `google-generativeai` | 0.8.6 | **deprecated** → `google-genai` ~1.62.x | **Migrate** | Critical (F3) |
| Pydantic | 2.12.5 | 2.12.x stable (2.13 in beta) | Hold; leverage for F4 `response_schema` | — |
| PyMuPDF | 1.27.1 | 1.27.2.x patches | Patch | Minor |
| pdfplumber, psycopg, RapidFuzz, HTTPX, structlog, etc. | current | minor/patch | Housekeeping | Minor |
| Ruff / mypy / pytest | current | minor | Housekeeping | Minor |

**Verdict:** one Critical (the SDK migration, F3), everything else is housekeeping. Bundle the housekeeping with the SDK migration since you'll be in `pyproject.toml`/`uv.lock` anyway.

---

## 4. Section C — Recommended Sequence

Ordered by *urgency × payoff*, with rough effort. This is a maintenance-plus-one-feature plan, not a rewrite.

**Now / this sprint (availability & correctness — non-negotiable):**

1. **F3 — Migrate the extraction worker to `google-genai`.** ~0.5–1 day. Unblocks everything else.
2. **F1 + F2 — Repoint all model strings to `gemini-3.5-flash`** behind a single config constant; re-run the Indic transliteration + raga-validation golden set before promoting. ~1 day incl. eval. Track it as a Conductor `TRACK-XXX` per your `09-ai/README.md` convention, and update `current-versions.md` + the three sync targets in `CLAUDE.md`.

**Next (cost & quality, high payoff, low risk):**

3. **F4 — Convert extraction/transliteration prompts to Pydantic `response_schema`.** ~1–2 days. Biggest reliability win per unit effort.
4. **F5 — Route import-pipeline and backfill jobs through Batch Mode.** ~1–2 days. Recurring ~50% cost cut on bulk spend.

**Then (the one new capability worth pulling forward):**

5. **F6 — Semantic search via `gemini-embedding-001` + `pgvector`.** New `krithi_embedding` table, offline backfill job, similarity endpoint. ~1 week for a usable v1 at 768 dims. Register as its own Conductor track; this is a feature, not maintenance.

**Housekeeping (any time, one PR each):**

6. Backend minor bumps (Kotlin 2.4 / Ktor 3.5) + evaluate Ktor's OpenAPI generation against your hand-maintained spec.
7. Frontend + Python patch roll-up; confirm no retired model strings in `@google/genai` calls.

---

## 5. Section D — Evaluating Gemma 4 (12B) for the AI Workload

> Requested follow-up: can an open-weight **Gemma 4 12B** model take over any of the AI work — including function calling and multimodal — and where is that a good idea versus a distraction? Verdict up front, evidence after.

### The facts (June 2026)

Gemma 4 12B shipped **3 June 2026** and is a genuinely strong release on paper: **Apache 2.0 licence** (no more custom "Gemma Terms" carve-outs — you can fine-tune, redistribute, and run it commercially with only attribution obligations), an **encoder-free unified multimodal** architecture (text/image/audio/video in, text out), a **256K context window**, **140+ languages**, a **custom tool-call protocol** for function calling, and a memory footprint small enough to run on **~16 GB VRAM or unified memory** at 4-bit. It beats Gemma 3 27B on standard benchmarks and nearly matches the 2×-larger 26B MoE. It is available **hosted on Vertex AI Model Garden / Cloud Run** *or* fully self-hosted (vLLM for GPU throughput, llama.cpp/GGUF for Apple Silicon).

None of that is in dispute. The question is whether it fits *this* product, and the honest answer is: **mostly no for the core path, with two narrow, eval-gated exceptions.** [Severity: **Observation** — opportunity, not obligation]

### Critical assessment against your actual workload

**1. The core task is Indic transliteration fidelity, and that is exactly where a general 12B open model is weakest.** Your entire Gemini rationale (`gemini-selection-rationale.md`) hinges on niche correctness — preserving La/Lla/Zha distinctions and not bleeding Tamil into Grantha. "140+ languages" is a statement about *breadth*, not *depth* on Dravidian-script edge cases. A 12B model that is excellent at GPQA and DocVQA tells you nothing about whether it renders a Sanskritised Telugu kriti correctly. Assume it is **worse than Gemini 3.5 Flash here until a golden-set eval proves otherwise** — do not take the benchmark-leader framing at face value for your domain.

**2. The cost case for self-hosting does not close at your volume.** This is the part the marketing ("60–80% cheaper at scale", "runs on a laptop") obscures. The savings are real *only when a GPU stays busy*. Gemini 3.5 Flash is ~$1.50/M input tokens, **halved again via Batch Mode (F5)** — you are already on a near-floor price for bursty work. Self-hosting means a 16–24 GB GPU (L4/A10-class) running ~$0.5–1.0/hr; left on, that is **~$400–700/month before you process a single token**. Your profile is a *curated archive of thousands of compositions processed largely once*, plus occasional interactive edits — bursty and low-duty-cycle. To break even against managed Flash-Batch you would need sustained, high-volume throughput you do not have. **For your FinOps reality, the managed API is the cheaper option, not the more expensive one.** Self-hosting to "save money" here would *increase* total cost once you price in the GPU and the engineer-hours.

**3. It adds operational surface that contradicts your stated posture.** Self-hosting means GPU provisioning, a serving stack (vLLM), quantisation management, drift/throughput monitoring, and scaling — net-new ops for a small team on a system of record that explicitly values determinism and auditability. Every hour spent on model-serving infra is an hour not spent on the catalogue. The managed path keeps your AI surface a stateless API call behind a config constant.

**4. Function calling solves a problem you don't have.** Gemma 4's tool-calling is competent (the widely-cited ~17/20 chain-completion figure was measured on the **larger 31B dense** variant, not the 12B — be skeptical of transferring it down). But your AI surface is bounded *extraction → transliteration → validation → human review*. That is not an agent orchestrating tools; it is structured generation. **Native JSON-Schema structured output (F4) is the right primitive for your need, and Gemini already does it well.** Adopting Gemma for its function-calling would be buying a capability your architecture deliberately doesn't use.

**5. Multimodal is a narrow, real, *future* adjacency — not a reason to switch now.** Two honest possibilities: (a) **Vision** could OCR *scanned/image-only PDFs* where PyMuPDF/pdfplumber return nothing — a genuine fallback for poor manuscript scans, but a niche tail, not the main pipeline. (b) **Audio/ASR** is interesting *if* the product ever ingests recorded renditions to derive notation — a speculative roadmap item, not current scope. Neither justifies displacing Gemini today.

### Where Gemma 4 *does* earn a place

Two specific, defensible bets — both eval-gated, neither urgent:

- **A fine-tuned, domain-specialised transliteration model (the one compelling angle).** Apache 2.0 + fine-tunability means you could train Gemma 4 12B on your *own* curated transliteration gold pairs and plausibly **beat a general Flash model on your exact La/Lla/Zha / Grantha edge cases — and own the weights**, immune to the model-retirement treadmill that just broke F1. This is the legitimate strategic reason to care about Gemma. But it is premature: it requires a labelled corpus you haven't built, a training + eval harness, and sustained volume to amortise. Revisit once the catalogue is large and your golden set is mature. Until then, it's a research spike, not a plan.
- **On-device / offline for the mobile app — but with the *edge* variants, not 12B.** Your KMP app is the natural home for local inference (offline transliteration/search). 12B is too heavy for phones; the story there is **Gemma 4 E2B/E4B**, not 12B. Worth a separate future track if offline-first becomes a product goal.

For **embeddings (F6), Gemma is not the tool** — it is a generative model, not an embedding model. Keep `gemini-embedding-001`. Don't conflate the two because they share a brand.

### Two cheap experiments worth running (not commitments)

1. **Benchmark stock Gemma 4 12B against your transliteration + raga-validation golden set, hosted on Vertex Model Garden (zero self-host ops).** ~0.5 day once the golden set from F1 exists. The only purpose is to *measure the fidelity gap* and decide whether a future fine-tune is even worth it. Cheap knowledge, no infrastructure.
2. **Spike Gemma 4 vision as an OCR fallback** on a handful of your worst scanned PDFs, only if you actually have image-only sources that PyMuPDF fails on.

### Verdict

**Do not move core transliteration, validation, or extraction off Gemini 3.5 Flash onto a self-hosted Gemma 4 12B.** The fidelity risk is real, the cost case is inverted at your volume, the ops burden contradicts your posture, and the function-calling/agentic capabilities answer a question your architecture doesn't ask. The *only* strategically interesting use is a **future fine-tuned, domain-owned transliteration model** — and that is a deliberate, eval-gated investment to make *after* you have a labelled corpus, not a swap to do now. Run experiment #1 to quantify the gap; treat everything else as a watching brief.

---

## 6. Risk Register

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Production extraction fails on retired 2.0 Flash endpoint | High (already past retirement) | High | F1 — repoint model string immediately |
| Model swap degrades Indic transliteration fidelity on edge scripts | Medium | High | Golden-set eval **before** promotion; keep prompts schema-constrained (F4) for stability |
| `google-genai` migration introduces auth/client regressions | Low | Medium | Migrate in a branch; the worker already isolates the Gemini client |
| Re-migration churn (3.5 Flash later retired) | Low | Low | Single config constant for model string; scheduled re-eval, not reactive chasing |
| pgvector adds operational load | Low | Low | Stays inside existing Postgres 18; HNSW index; no new datastore |
| Batch Mode 24h latency leaks into user-interactive paths | Low | Medium | Restrict Batch to import/backfill; keep "Generate Variants" synchronous |
| Premature self-hosting of Gemma 4 12B increases cost & ops for no gain | Medium (tempting on paper) | Medium | Section D — managed Gemini is cheaper at your volume; gate any Gemma move on golden-set eval + a real fine-tune corpus |

---

## 7. Honest Bottom Line

You did not fall behind on libraries — your build files are fine and the few minors can wait. You fell behind on **AI platform lifecycle**, which moves faster than dependency semver and breaks harder. Spend the next sprint on F1–F3 (availability), bank the cheap reliability/cost wins in F4–F5, and treat F6 as the single feature investment that turns documented intent into something users can feel. Ignore the agentic/multimodal hype entirely for this product — a curated system of record wants determinism and auditability, and that is exactly what the boring, schema-constrained, batch-processed path gives you.

---

## Appendix — Sources

AI ecosystem:
- [Gemini deprecations — Google AI for Developers](https://ai.google.dev/gemini-api/docs/deprecations)
- [Google Retires Gemini 2.0 Flash-001, Replace with 2.5 Flash — AI Weekly](https://aiweekly.co/alerts/google-retires-gemini-20-flash-001-replace-with-25-flash)
- [Introducing Gemini 3 Flash — Google Blog](https://blog.google/products/gemini/gemini-3-flash/)
- [Gemini 3.5 Flash — Google DeepMind](https://deepmind.google/models/gemini/flash/)
- [Migrate to the Google GenAI SDK — Google AI for Developers](https://ai.google.dev/gemini-api/docs/migrate)
- [google-genai · PyPI](https://pypi.org/project/google-genai/)
- [deprecated-generative-ai-python — GitHub](https://github.com/google-gemini/deprecated-generative-ai-python)
- [Structured outputs — generateContent API](https://ai.google.dev/gemini-api/docs/structured-output)
- [JSON Schema support in Gemini API — Google Blog](https://blog.google/innovation-and-ai/technology/developers-tools/gemini-api-structured-outputs/)
- [Batch API — Google AI for Developers](https://ai.google.dev/gemini-api/docs/batch-api)
- [Batch Mode in the Gemini API — Google Developers Blog](https://developers.googleblog.com/scale-your-ai-workloads-batch-mode-gemini-api/)
- [Gemini Embedding GA — Google Developers Blog](https://developers.googleblog.com/gemini-embedding-available-gemini-api/)
- [gemini-embedding-001 model — Google AI for Developers](https://ai.google.dev/gemini-api/docs/models/gemini-embedding-001)
- [Gemini Embedding 2 — Google Blog](https://blog.google/innovation-and-ai/models-and-research/gemini-models/gemini-embedding-2/)

Libraries:
- [Kotlin 2.4.0 Released — Kotlin Blog](https://blog.jetbrains.com/kotlin/2026/06/kotlin-2-4-0-released/)
- [Ktor releases — Ktor Documentation](https://ktor.io/docs/releases.html)
- [Pydantic v2.12 Release](https://pydantic.dev/articles/pydantic-v2-12-release)
- [PyMuPDF releases — GitHub](https://github.com/pymupdf/PyMuPDF/releases)
- [Google Gen AI Python SDK — GitHub](https://github.com/googleapis/python-genai)

Gemma 4:
- [Introducing Gemma 4 12B: a unified, encoder-free multimodal model — Google Blog](https://blog.google/innovation-and-ai/technology/developers-tools/introducing-gemma-4-12B/)
- [Welcome Gemma 4: Frontier multimodal intelligence on device — Hugging Face](https://huggingface.co/blog/gemma4)
- [Gemma 4 model card — Google AI for Developers](https://ai.google.dev/gemma/docs/core/model_card_4)
- [Google releases Gemma 4 under Apache 2.0 — VentureBeat](https://venturebeat.com/technology/google-releases-gemma-4-under-apache-2-0-and-that-license-change-may-matter)
- [Gemma 4 available on Google Cloud — Google Cloud Blog](https://cloud.google.com/blog/products/ai-machine-learning/gemma-4-available-on-google-cloud)
- [Gemma 4 12B: The Developer Guide — Google Developers Blog](https://developers.googleblog.com/gemma-4-12b-the-developer-guide/)
