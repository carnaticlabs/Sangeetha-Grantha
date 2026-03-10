| Metadata | Value |
|:---|:---|
| **Track ID** | TRACK-081 |
| **Title** | Extraction Worker Local File Path Support |
| **Status** | Completed |
| **Created** | 2026-03-10 |
| **Author** | Sangeetha Grantha Team |
| **Depends On** | TRACK-079 (E2E Pipeline) |

# TRACK-081: Extraction Worker Local File Path Support

## Objective

Enable the extraction worker to read PDF files from a Docker volume-mounted directory, removing the requirement to serve PDFs over HTTP.

## Problem Statement

The extraction worker uses `httpx` to download source files. `file://` URIs and bare filesystem paths fail with "UnsupportedProtocol." This blocked `mdskt.pdf` (Sanskrit) extraction since PDFs had to be served via a separate HTTP server.

## Scope

- `worker.py`: Add `_resolve_local_path()` method to detect `file://` URIs and bare `/` paths, read directly from filesystem instead of HTTP download
- `compose.yaml`: Add `./data/pdfs:/app/pdfs:ro` volume mount to extraction service
- `.gitignore`: Add `data/pdfs/*.pdf` to exclude binary PDFs from git
- `data/pdfs/.gitkeep`: Placeholder for the PDF mount directory

## Usage

Place PDFs in `data/pdfs/` and submit extraction with source path `/app/pdfs/<filename>.pdf`.

## Verification

- Extraction worker accepts `/app/pdfs/mdskt.pdf` as source URL
- File is read directly from mounted volume without HTTP
