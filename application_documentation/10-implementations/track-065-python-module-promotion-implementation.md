| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-19 |
| **Author** | Sangeetha Grantha Team |

# TRACK-065: Python Extraction Module First-Class Promotion & Naming

## Purpose

Promotes the Python extraction worker from a legacy `tools/pdf-extractor/` path to `tools/krithi-extract-enrich-worker/`, reflecting its expanded scope (PDF + HTML + OCR + Gemini enrichment). Pins Python 3.11 in root `.mise.toml` for consistent toolchain management.

## Categorization

| Layer | Scope |
|:---|:---|
| Toolchain | `.mise.toml` Python 3.11 pin |
| Infrastructure | `compose.yaml`, `start-sangita.sh` path/service updates |
| CLI (Rust) | `sangita-cli` extraction/dev/test/services command path updates |
| Python Module | Deletion of `tools/pdf-extractor/`, addition of `tools/krithi-extract-enrich-worker/` |
| Documentation | 12+ markdown files updated with new paths and naming |
| Conductor | `tracks.md` registry, TRACK-065 track file, related track metadata refreshes |

## Code Changes Summary

| File | Change |
|:---|:---|
| `.mise.toml` | Added `python = "3.11"` to `[tools]` section |
| `compose.yaml` | Updated build context and service name to `krithi-extract-enrich-worker` |
| `start-sangita.sh` | Updated service references from `pdf-extractor` to `krithi-extract-enrich-worker` |
| `tools/pdf-extractor/**` | Deleted entire legacy directory (source, tests, config, lockfile) |
| `tools/krithi-extract-enrich-worker/**` | New module directory with renamed/restructured content |
| `tools/sangita-cli/src/commands/extraction.rs` | Updated paths and container names |
| `tools/sangita-cli/src/commands/dev.rs` | Updated extraction worker references |
| `tools/sangita-cli/src/commands/test.rs` | Updated test path references |
| `tools/sangita-cli/src/services.rs` | Updated service directory and container constants |
| `tools/sangita-cli/README.md` | Updated documentation references |
| `tools/test_integration_pipeline.sh` | Updated script paths |
| `application_documentation/**` (12 files) | Updated all references from `pdf-extractor` to `krithi-extract-enrich-worker` |
| `conductor/tracks.md` | Added TRACK-065 entry, updated version/date |
| `conductor/tracks/TRACK-041,054,055,059,060,064` | Refreshed path references and metadata headers |

## Commit Reference

```
Ref: application_documentation/10-implementations/track-065-python-module-promotion-implementation.md
```
