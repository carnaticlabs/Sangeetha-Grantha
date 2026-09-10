| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-09-10 |
| **Author** | Sangeetha Grantha Team |
| **Document Type** | Evidence record |

# Extraction Worker Local File Path Support

---

> [!NOTE]
> Historical evidence: results, counts, commands, and observations below belong to the original work described here. The editorial update date is not a new test or corpus verification. For present behavior, use [current feature map](./../01-requirements/features/README.md).

## Purpose

Enable the extraction worker to read PDFs from a Docker volume-mounted directory, removing the HTTP file-serving workaround.

## Implementation Details

- Added `_resolve_local_path()` static method to detect `file://` URIs and bare absolute paths
- Local files bypass the httpx download and cache, reading directly from the filesystem
- Docker Compose volume mount `./data/pdfs:/app/pdfs:ro` makes host PDFs available to the extraction container
- `.gitignore` updated to exclude `data/pdfs/*.pdf` (binary files)

## Code Changes

| File | Change |
|------|--------|
| `worker.py` | `_resolve_local_path()` method, local path branch in `_download_source()` |
| `compose.yaml` | `./data/pdfs:/app/pdfs:ro` volume mount on extraction service |
| `.gitignore` | `data/pdfs/*.pdf` exclusion |
| `data/pdfs/.gitkeep` | New — placeholder for mount directory |

## Usage

Place PDFs in `data/pdfs/` and submit extraction with source path `/app/pdfs/<filename>.pdf`.

Ref: application_documentation/10-implementations/track-081-extraction-file-path-support.md

---

[Section index](./README.md) · [Documentation home](./../README.md) · [Feature status](./../01-requirements/features/README.md)
