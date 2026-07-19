"""CLI entry point for local development and testing.

Usage:
    python -m src.cli extract --input <pdf_path_or_url> --output <json_path> [--pages 1-10]
    python -m src.cli transliterate --text "..." --from devanagari --to tamil

For production use, the worker.py entry point polls the extraction_queue database table.
"""

from __future__ import annotations

import json
import logging
import sys
import uuid
from pathlib import Path
from typing import Any

import click

from .config import ExtractorConfig
from .db import ExtractionTask
from .extraction_strategies import PdfExtractionStrategy
from .extractor import PdfExtractor
from .metadata_parser import MetadataParser
from .ocr_fallback import OcrFallback
from .page_segmenter import PageSegmenter
from .schema import CanonicalExtraction
from .structure_parser import StructureParser
from .transliterator import Transliterator

logger = logging.getLogger(__name__)


@click.group()
@click.option("--verbose", "-v", is_flag=True, help="Enable verbose logging")
def cli(verbose: bool) -> None:
    """Sangita Grantha PDF Extraction Service CLI."""
    level = logging.DEBUG if verbose else logging.INFO
    logging.basicConfig(
        level=level,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
        stream=sys.stderr,
    )


@cli.command()
@click.option("--input", "-i", "input_path", required=True, help="PDF file path or URL")
@click.option("--output", "-o", "output_path", required=True, help="Output JSON file path")
@click.option("--pages", "-p", "page_range", default=None, help="Page range (e.g. '1-10')")
@click.option("--composer", "-c", default=None, help="Composer hint (e.g. 'Muthuswami Dikshitar')")
@click.option("--source-name", default="local", help="Source name for provenance")
@click.option("--source-tier", default=5, type=int, help="Source authority tier (1-5)")
def extract(
    input_path: str,
    output_path: str,
    page_range: str | None,
    composer: str | None,
    source_name: str,
    source_tier: int,
) -> None:
    """Extract Krithis from a PDF file into canonical JSON format.

    TRACK-130: this delegates to `PdfExtractionStrategy` — the same pipeline the
    worker runs — instead of re-implementing segmentation and parsing. The CLI
    had already drifted from it (no ragamalika handling, no diacritic
    normalisation of raga/tala names).
    """
    # The strategy resolves bare/absolute paths and file:// URIs locally, but a
    # relative path would be mistaken for a URL, so anchor it first.
    source_ref = input_path
    if "://" not in input_path:
        source_ref = str(Path(input_path).resolve())

    config = ExtractorConfig()

    def finalize(extraction: CanonicalExtraction, _source_text: str, _source_format: str) -> CanonicalExtraction:
        # No enrichment in the CLI; just report the path the user actually gave.
        extraction.source_url = input_path
        extraction.source_name = source_name
        extraction.source_tier = source_tier
        return extraction

    strategy = PdfExtractionStrategy(
        config,
        finalize,
        pdf_extractor=PdfExtractor(),
        page_segmenter=PageSegmenter(),
        ocr_fallback=OcrFallback(),
        structure_parser=StructureParser(),
        metadata_parser=MetadataParser(),
        transliterator=Transliterator(),
    )

    task = ExtractionTask(
        id=uuid.uuid4(),
        source_url=source_ref,
        source_format="PDF",
        source_name=source_name,
        source_tier=source_tier,
        request_payload={"composerHint": composer} if composer else {},
        page_range=page_range,
        import_batch_id=None,
        import_task_run_id=None,
        attempts=0,
    )

    click.echo(f"Extracting from: {input_path}")
    try:
        extractions = strategy.extract(task)
    finally:
        strategy.close()

    results: list[dict[str, Any]] = []
    for i, extraction in enumerate(extractions):
        results.append(extraction.to_json_dict())
        click.echo(
            f"  [{i + 1}/{len(extractions)}] {extraction.title} "
            f"(Raga: {extraction.ragas[0].name if extraction.ragas else '?'}, "
            f"Tala: {extraction.tala}, Sections: {len(extraction.sections)}, "
            f"Pages: {extraction.page_range})"
        )

    # Write output
    output_file = Path(output_path)
    output_file.parent.mkdir(parents=True, exist_ok=True)
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    click.echo(f"\nWrote {len(results)} extractions to {output_path}")


@cli.command()
@click.option("--text", "-t", required=True, help="Text to transliterate")
@click.option("--from", "from_script", required=True, help="Source script (devanagari, tamil, etc.)")
@click.option("--to", "to_script", required=True, help="Target script")
def transliterate(text: str, from_script: str, to_script: str) -> None:
    """Transliterate text between Indic scripts."""
    t = Transliterator()
    if not t.is_available:
        click.echo("Error: indic-transliteration library not installed", err=True)
        sys.exit(1)

    result = t.transliterate(text, from_script, to_script)
    if result:
        click.echo(result)
    else:
        click.echo("Transliteration failed", err=True)
        sys.exit(1)


def main() -> None:
    """Entry point for the CLI."""
    cli()


if __name__ == "__main__":
    main()
