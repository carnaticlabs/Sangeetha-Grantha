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
from datetime import datetime, timezone
from pathlib import Path

import click

from .extractor import PdfExtractor
from .metadata_parser import MetadataParser
from .page_segmenter import PageSegmenter
from .schema import (
    CanonicalExtraction,
    CanonicalLyricSection,
    CanonicalLyricVariant,
    CanonicalRaga,
    ExtractionMethod,
    MusicalForm,
)
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
    """Extract Krithis from a PDF file into canonical JSON format."""
    pdf_extractor = PdfExtractor()
    segmenter = PageSegmenter()
    structure_parser = StructureParser()
    metadata_parser = MetadataParser()
    transliterator = Transliterator()

    # Parse page range
    parsed_range = None
    if page_range:
        parts = page_range.split("-")
        if len(parts) == 2:
            parsed_range = (int(parts[0]) - 1, int(parts[1]) - 1)
        elif len(parts) == 1:
            page_num = int(parts[0]) - 1
            parsed_range = (page_num, page_num)

    click.echo(f"Extracting from: {input_path}")

    # Extract
    document = pdf_extractor.extract_document(input_path, parsed_range)
    click.echo(f"Extracted {len(document.pages)} pages (checksum: {document.checksum[:12]}...)")

    # Segment
    segments = segmenter.segment(document)
    click.echo(f"Detected {len(segments)} Krithi segments")

    # Process each segment
    results: list[dict] = []
    for i, segment in enumerate(segments):
        metadata = metadata_parser.parse(
            segment.body_text[:500],
            title_hint=segment.title_text,
        )
        detected_sections = structure_parser.parse_sections(segment.body_text)
        canonical_sections = structure_parser.to_canonical_sections(detected_sections)
        lyric_sections = structure_parser.to_canonical_lyric_sections(detected_sections)

        script = transliterator.detect_script(segment.body_text) or "devanagari"
        language = "sa" if script == "devanagari" else "en"

        extraction = CanonicalExtraction(
            title=metadata.title,
            alternateTitle=metadata.alternate_title,
            composer=metadata.composer or composer or "Unknown",
            musicalForm=MusicalForm.KRITHI,
            ragas=[CanonicalRaga(name=metadata.raga or "Unknown")],
            tala=metadata.tala or "Unknown",
            sections=canonical_sections,
            lyricVariants=[
                CanonicalLyricVariant(
                    language=language,
                    script=script,
                    sections=lyric_sections,
                )
            ]
            if lyric_sections
            else [],
            deity=metadata.deity,
            temple=metadata.temple,
            templeLocation=metadata.temple_location,
            sourceUrl=input_path,
            sourceName=source_name,
            sourceTier=source_tier,
            extractionMethod=ExtractionMethod.PDF_PYMUPDF,
            extractionTimestamp=datetime.now(timezone.utc).isoformat(),
            pageRange=segment.page_range_str,
            checksum=document.checksum,
        )

        results.append(extraction.to_json_dict())
        click.echo(
            f"  [{i + 1}/{len(segments)}] {metadata.title} "
            f"(Raga: {metadata.raga or '?'}, Tala: {metadata.tala or '?'}, "
            f"Sections: {len(canonical_sections)}, Pages: {segment.page_range_str})"
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
