"""Browser-assisted batch extraction pipeline.

Takes HTML files saved from browser navigation and runs them through
the existing HtmlTextExtractor + StructureParser to produce
CanonicalExtraction JSON output.

Usage:
    python -m src.browser_batch_extract \
        --html-dir ../../database/for_import/raw_pages/syama-sastri \
        --csv ../../database/for_import/Syama-Sastri-Krithi-For-Import.csv \
        --composer "Syama Sastri" \
        --output ../../database/for_import/extracted/Syama-Sastri-Lyrics-Extracted.json
"""

from __future__ import annotations

import argparse
import csv
import json
import logging
import os
import re
import sys
from pathlib import Path
from typing import Any

# Add parent to path for imports
sys.path.insert(0, str(Path(__file__).parent.parent))

from src.html_extractor import HtmlTextExtractor
from src.structure_parser import StructureParser

logger = logging.getLogger(__name__)

# ─── Source blog metadata ────────────────────────────────────────────────────

SOURCE_BLOG_MAP = {
    "Syama Sastri": {
        "source_name": "Shyama Krishna Vaibhavam",
        "source_tier": 2,
        "primary_language": "te",
    },
    "Tyagaraja": {
        "source_name": "Thyagaraja Vaibhavam",
        "source_tier": 2,
        "primary_language": "te",
    },
    "Muthuswami Dikshitar": {
        "source_name": "Guru Guha",
        "source_tier": 2,
        "primary_language": "sa",
    },
}

# ─── Output JSON schema (matches user's requested format) ───────────────────


def build_output_entry(
    krithi_name: str,
    raga: str,
    composer: str,
    source_url: str,
    html_path: str,
    html_extractor: HtmlTextExtractor,
    structure_parser: StructureParser,
) -> dict[str, Any]:
    """Parse a single saved HTML file and produce the output JSON entry."""

    try:
        with open(html_path, encoding="utf-8") as f:
            html_content = f.read()
    except Exception as e:
        return _failure_entry(krithi_name, raga, composer, source_url, f"Failed to read HTML: {e}")

    if not html_content.strip():
        return _failure_entry(krithi_name, raga, composer, source_url, "Empty HTML file")

    # Step 1: Extract text from HTML
    try:
        extracted = html_extractor.extract(html_content, base_url=source_url)
        text = extracted.text
    except Exception as e:
        return _failure_entry(krithi_name, raga, composer, source_url, f"HTML extraction error: {e}")

    if not text or len(text.strip()) < 50:
        return _failure_entry(krithi_name, raga, composer, source_url, "Extracted text too short")

    # Step 2: Parse structure (sections + lyric variants)
    try:
        parse_result = structure_parser.parse(text)
    except Exception as e:
        return _failure_entry(krithi_name, raga, composer, source_url, f"Structure parse error: {e}")

    if not parse_result.sections:
        return _failure_entry(krithi_name, raga, composer, source_url, "No sections detected")

    # Step 3: Build the output JSON
    sections_out = []
    for s in parse_result.sections:
        section_entry: dict[str, Any] = {
            "section_type": s.section_type.value,
            "order_index": s.order,
            "label": s.label,
            "madhyama_kala_text": None,
        }
        # Check if section text contains embedded MKS
        if "[Madhyama Kala Sahitya]" in s.text:
            parts = s.text.split("[Madhyama Kala Sahitya]")
            mks_text = parts[1].strip() if len(parts) > 1 else None
            if mks_text:
                # Build MKS text per language variant
                section_entry["madhyama_kala_text"] = {"en": mks_text}
        sections_out.append(section_entry)

    lyric_variants_out = []
    script_display_names = {
        "latin": "Latin",
        "devanagari": "Devanagari",
        "tamil": "Tamil",
        "telugu": "Telugu",
        "kannada": "Kannada",
        "malayalam": "Malayalam",
    }

    for variant in parse_result.lyric_variants:
        variant_sections = []
        for vs in variant.sections:
            section_text = vs.text
            # Strip MKS marker from main text for clean output
            if "[Madhyama Kala Sahitya]" in section_text:
                section_text = section_text.split("[Madhyama Kala Sahitya]")[0].strip()
            variant_sections.append(
                {
                    "section_type": vs.section_type.value,
                    "order_index": vs.order,
                    "text": section_text,
                }
            )
        if variant_sections:
            lyric_variants_out.append(
                {
                    "language": variant.language,
                    "script": script_display_names.get(variant.script, variant.script),
                    "sections": variant_sections,
                }
            )

    # Determine extraction status
    has_pallavi = any(s["section_type"] == "PALLAVI" for s in sections_out)
    variant_count = len(lyric_variants_out)
    if has_pallavi and variant_count >= 2:
        status = "success"
    elif has_pallavi:
        status = "partial"
    else:
        status = "partial"

    notes_parts = []
    if not has_pallavi:
        notes_parts.append("No PALLAVI section detected")
    if variant_count < 6:
        notes_parts.append(f"Only {variant_count} script variant(s) extracted (expected up to 6)")

    return {
        "krithi_name": krithi_name,
        "raga": raga,
        "composer": composer,
        "source_url": source_url,
        "extraction_status": status,
        "extraction_notes": "; ".join(notes_parts) if notes_parts else None,
        "sections": sections_out,
        "lyric_variants": lyric_variants_out,
    }


def _failure_entry(krithi_name: str, raga: str, composer: str, source_url: str, reason: str) -> dict[str, Any]:
    return {
        "krithi_name": krithi_name,
        "raga": raga,
        "composer": composer,
        "source_url": source_url,
        "extraction_status": "failed",
        "extraction_notes": reason,
        "sections": [],
        "lyric_variants": [],
    }


def safe_filename(url: str) -> str:
    """Convert a URL to a filesystem-safe filename."""
    # Take the path portion and clean it
    path = url.split("blogspot.com/")[-1] if "blogspot.com/" in url else url
    safe = re.sub(r"[^\w\-.]", "_", path)
    return safe[:200] + ".html"


def run_batch(
    csv_path: str,
    html_dir: str,
    composer: str,
    output_path: str,
) -> dict[str, Any]:
    """Run batch extraction on saved HTML files."""

    html_extractor = HtmlTextExtractor()
    structure_parser = StructureParser()

    # Read CSV
    with open(csv_path, encoding="utf-8") as f:
        reader = csv.DictReader(f)
        rows = list(reader)

    results = []
    failures = []
    success_count = 0
    partial_count = 0
    fail_count = 0

    for row in rows:
        krithi_name = row.get("Krithi", "").strip()
        raga = row.get("Raga", "").strip()
        url = row.get("Hyperlink", "").strip()

        if not url:
            continue

        html_filename = safe_filename(url)
        html_path = os.path.join(html_dir, html_filename)

        if not os.path.exists(html_path):
            entry = _failure_entry(krithi_name, raga, composer, url, "HTML file not found (page not yet fetched)")
            results.append(entry)
            failures.append({"krithi_name": krithi_name, "url": url, "reason": "not_fetched"})
            fail_count += 1
            continue

        entry = build_output_entry(
            krithi_name=krithi_name,
            raga=raga,
            composer=composer,
            source_url=url,
            html_path=html_path,
            html_extractor=html_extractor,
            structure_parser=structure_parser,
        )
        results.append(entry)

        if entry["extraction_status"] == "success":
            success_count += 1
        elif entry["extraction_status"] == "partial":
            partial_count += 1
        else:
            fail_count += 1
            failures.append(
                {
                    "krithi_name": krithi_name,
                    "url": url,
                    "reason": entry.get("extraction_notes", "unknown"),
                }
            )

        logger.info(
            "[%s] %s — %s",
            entry["extraction_status"].upper(),
            krithi_name,
            entry.get("extraction_notes", "OK"),
        )

    # Save results
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    # Save failures if any
    if failures:
        failures_path = output_path.replace(".json", "-failures.json")
        with open(failures_path, "w", encoding="utf-8") as f:
            json.dump(failures, f, indent=2, ensure_ascii=False)

    summary = {
        "total": len(results),
        "success": success_count,
        "partial": partial_count,
        "failed": fail_count,
        "output_path": output_path,
    }

    logger.info("Batch extraction complete: %s", json.dumps(summary))
    return summary


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s: %(message)s")

    parser = argparse.ArgumentParser(description="Browser-assisted batch extraction")
    parser.add_argument("--csv", required=True, help="Path to the composer CSV file")
    parser.add_argument("--html-dir", required=True, help="Directory containing saved HTML files")
    parser.add_argument("--composer", required=True, help="Composer name")
    parser.add_argument("--output", required=True, help="Output JSON path")
    args = parser.parse_args()

    summary = run_batch(
        csv_path=args.csv,
        html_dir=args.html_dir,
        composer=args.composer,
        output_path=args.output,
    )
    print(json.dumps(summary, indent=2))


if __name__ == "__main__":
    main()
