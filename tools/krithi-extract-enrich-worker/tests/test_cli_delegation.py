"""TRACK-130: the CLI must run the same pipeline as the worker.

`cli.py extract` used to re-implement segmentation and parsing, and had drifted
from `PdfExtractionStrategy` — losing ragamalika sub-raga detection and the
raga/tala name cleanup. These tests pin the delegation so it cannot drift again.
"""

from __future__ import annotations

import json
from pathlib import Path

import fitz
import pytest
from click.testing import CliRunner

from src.cli import cli
from src.extraction_strategies import parse_page_range

FIXTURE = Path(__file__).parent / "fixtures" / "structure_parser" / "ragamalika_multi_variant.txt"


@pytest.fixture()
def ragamalika_pdf(tmp_path):
    pdf = tmp_path / "ragamalika.pdf"
    doc = fitz.open()
    page = doc.new_page()
    y = 50
    page.insert_text((50, y), "SrI viSva nAthaM bhajEhaM", fontsize=15)
    y += 22
    page.insert_text((50, y), "rAgaM: rAgamAlika  tALaM: Adi", fontsize=10)
    y += 18
    for line in FIXTURE.read_text(encoding="utf-8").split("\n")[:40]:
        if y > 780:
            page = doc.new_page()
            y = 50
        page.insert_text((50, y), line[:95], fontsize=8)
        y += 11
    doc.save(str(pdf))
    doc.close()
    return pdf


def test_cli_extract_gains_ragamalika_subraga_detection(ragamalika_pdf, tmp_path) -> None:
    """The pre-TRACK-130 CLI emitted a single raga named "Ragamalika  Talam: Adi"."""
    out = tmp_path / "out.json"
    result = CliRunner().invoke(cli, ["extract", "-i", str(ragamalika_pdf), "-o", str(out)])

    assert result.exit_code == 0, result.output
    payload = json.loads(out.read_text(encoding="utf-8"))
    assert len(payload) == 1

    ragas = payload[0]["ragas"]
    assert len(ragas) > 1, f"expected ragamalika sub-ragas, got {ragas}"
    names = [r["name"] for r in ragas]
    assert "Arabhi" in names and "gauLa" in names, names
    # Each sub-raga is attributed to its parent section.
    assert all(r.get("section") for r in ragas), ragas
    # The un-cleaned metadata blob must not survive as a raga name.
    assert not any("Talam" in n for n in names), names


def test_cli_reports_the_path_the_user_supplied(ragamalika_pdf, tmp_path) -> None:
    """Delegation resolves paths internally; output still shows the given path."""
    out = tmp_path / "out.json"
    result = CliRunner().invoke(
        cli, ["extract", "-i", str(ragamalika_pdf), "-o", str(out), "--source-name", "cli-test"]
    )

    assert result.exit_code == 0, result.output
    payload = json.loads(out.read_text(encoding="utf-8"))
    assert payload[0]["sourceUrl"] == str(ragamalika_pdf)
    assert payload[0]["sourceName"] == "cli-test"


@pytest.mark.parametrize(
    ("value", "expected"),
    [
        ("3-7", (2, 6)),
        ("5", (4, 4)),
        ("1-1", (0, 0)),
        (None, None),
        ("", None),
    ],
)
def test_shared_page_range_helper(value, expected) -> None:
    """One definition, used by both the CLI and the PDF strategy."""
    assert parse_page_range(value) == expected
