| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |

# Krithi extraction and enrichment worker

---

This Python package processes supported source documents into canonical composition payloads. The Kotlin backend owns editorial orchestration, reference resolution, and canonical persistence. The worker claims extraction requests from PostgreSQL and writes results back for Kotlin to consume.

Start with the [ingestion architecture](../../application_documentation/01-requirements/features/bulk-import/02-implementation/technical-implementation-guide.md) for the end-to-end flow, or [configuration](../../application_documentation/08-operations/config.md) for environment setup.

## Run with the application

From the repository root:

```bash
make dev
```

Compose starts the extraction worker after the database and Flyway migration service. Source code is mounted under `/app/src`; downloaded artifacts use the extraction cache volume; local PDFs under `data/pdfs` are mounted read-only at `/app/pdfs`. A path submitted to a container must exist inside that container.

The Docker health check verifies module imports. It does not prove database connectivity, successful processing, or an empty failure queue.

## Local development

Python 3.14 or newer is required by [pyproject.toml](./pyproject.toml). Dependencies are locked in [uv.lock](./uv.lock).

```bash
cd tools/krithi-extract-enrich-worker
uv sync --frozen --extra dev
uv run python -m src.cli --help
```

Extract a local PDF into JSON without writing a canonical composition:

```bash
uv run python -m src.cli extract \
  --input /absolute/path/to/source.pdf \
  --output /tmp/krithi-extraction.json \
  --pages 1-3
```

The CLI reuses the PDF extraction strategy. Its PDF extraction command does not run Gemini enrichment. For queue processing, configure the intended database before running `uv run python -m src.worker`; this consumes queued work and writes extraction results.

## Components

| Area | Source | Role |
|:---|:---|:---|
| Worker and queue | [worker.py](./src/worker.py), [db.py](./src/db.py) | Claim requests, run processing, record results/failures |
| Extraction strategies | [extraction_strategies.py](./src/extraction_strategies.py) | Source-format routing and shared extraction pipeline |
| PDF parsing | [extractor.py](./src/extractor.py), [page_segmenter.py](./src/page_segmenter.py) | Text extraction, page boundaries, segmentation |
| HTML parsing | [html_extractor.py](./src/html_extractor.py) | Source-aware HTML extraction |
| Structure | [structure_parser.py](./src/structure_parser.py) | Musical sections and source labels |
| Canonical payload | [schema.py](./src/schema.py) | Pydantic contract and validation |
| Configuration | [config.py](./src/config.py) | Frozen pydantic-settings configuration |
| Search indexing | [embeddings](./src/embeddings) | Context, embeddings, profile/document maintenance |

Source support depends on the implemented strategy. A format appearing in a schema or an old proposal does not guarantee a complete adapter.

## Environment

| Variable | Purpose |
|:---|:---|
| `DATABASE_URL` | PostgreSQL connection for the extraction queue |
| `SG_GEMINI_API_KEY` | Optional enrichment credentials |
| `SG_ENABLE_GEMINI_ENRICHMENT` | Enable enrichment; default false |
| `SG_GEMINI_MODEL` | Enrichment model selection |
| `SG_ENABLE_IDENTITY_DISCOVERY` | Candidate discovery; default true |
| `SG_IDENTITY_MIN_SCORE`, `SG_IDENTITY_MAX_COUNT` | Candidate threshold and maximum count |
| `SG_IDENTITY_CACHE_TTL_SECONDS` | Candidate cache lifetime |
| `EXTRACTION_POLL_INTERVAL_S` | Poll interval; default 5 seconds |
| `EXTRACTION_CACHE_DIR` | Artifact cache directory |
| `EXTRACTOR_VERSION` | Version tag stored with extraction evidence |
| `LOG_LEVEL` | Logging verbosity |

The worker reads `.env` in its working directory through pydantic-settings; process environment wins. It does not automatically read the backend's `config/local.env`. Container credentials must be passed explicitly by deployment configuration.

## Verify changes

```bash
uv run ruff check .
uv run ruff format --check .
uv run mypy .
uv run pytest
```

The suite includes tests that require Docker/Testcontainers. Match the repository's [CI workflow](../../.github/workflows/ci.yml) and inspect source fixtures when changing parsing. After worker changes, restart the application as described in [onboarding](../../application_documentation/00-onboarding/getting-started.md).

## Indexing and diagnostics

Embedding scripts are separate from normal extraction. Start with `uv run python scripts/embed_catalogue.py --limit 5 --dry-run` and the [search guide](../../application_documentation/03-api/search.md). A real indexing run writes database records and can incur provider usage.

For failures, trace source URL → extraction ID → canonical payload → Kotlin processor → import/variant decision → persisted sections. Keep unknown classifications, source variants, and ordered raga membership intact. See [post-import verification](../../application_documentation/07-quality/qa/test-plan.md).

For document construction, indexing commands, profile activation, and coverage checks, read [Embedding pipeline and index operations](./../../application_documentation/09-ai/embeddings.md).
