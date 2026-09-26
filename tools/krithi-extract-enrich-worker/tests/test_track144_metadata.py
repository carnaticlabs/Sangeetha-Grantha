"""Regressions from TRACK-144: preserve initials and fill missing fields."""

import pytest

from src.metadata_parser import MetadataParser


@pytest.mark.parametrize("tala", ["miSra cApu", "tiSra Ekam", "maTya"])
def test_tala_label_does_not_eat_next_name_initial(tala):
    result = MetadataParser().parse(f"Example\nrAga bhairavi (tALa {tala})")
    assert result.tala == tala.title()


def test_raga_label_does_not_eat_next_name_initial():
    result = MetadataParser().parse("Example\nrAga mohana - tALa Adi")
    assert result.raga == "Mohana"
    assert result.tala == "Adi"


def test_title_supplies_missing_tala_without_replacing_body_raga():
    result = MetadataParser().parse(
        "Example\nRaga: Bhairavi",
        title_hint="Example - rAga bhairavi (tALa miSra cApu)",
    )
    assert result.raga == "Bhairavi"
    assert result.tala == "Misra Capu"


def test_talam_suffix_and_garbled_suffix_still_work():
    for label in ("tALaM", "t¯al.a ˙m"):
        result = MetadataParser().parse(f"Example\nrAgaM bhairavi - {label} - Adi")
        assert result.tala == "Adi"


def test_missing_close_parenthesis_stops_at_comma_before_prose():
    result = MetadataParser().parse("Example\nrAga kalyANi (tALa tripuTa, the composer prays to Mother.")
    assert result.raga == "Kalyani"
    assert result.tala == "Triputa"
