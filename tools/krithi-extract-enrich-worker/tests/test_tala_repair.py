import json
from pathlib import Path

import pytest
from pydantic import ValidationError

from src.tala_repair import TalaRepairManifest, compile_migration

MANIFEST = Path(__file__).resolve().parents[3] / "database/data/track144-tala-evidence.json"


def data():
    return json.loads(MANIFEST.read_text())


def test_manifest_compiles_to_checked_in_migration():
    manifest = TalaRepairManifest.model_validate(data())
    actual = MANIFEST.parents[1] / "migrations/V64__source_verified_tala_backfill.sql"
    assert compile_migration(manifest) == actual.read_text()


def test_duplicate_decisions_rejected():
    doc = data()
    doc["rows"].append(doc["rows"][0])
    with pytest.raises(ValidationError, match="Duplicate composition"):
        TalaRepairManifest.model_validate(doc)


def test_unreviewed_candidate_cannot_compile():
    doc = data()
    doc["rows"][0]["decision"] = "candidate"
    with pytest.raises(ValidationError):
        TalaRepairManifest.model_validate(doc)


def test_unknown_or_damaged_tala_cannot_compile():
    doc = data()
    doc["rows"][0]["proposed_tala"] = "Isra Capu"
    with pytest.raises(ValidationError, match="reviewed canonical vocabulary"):
        TalaRepairManifest.model_validate(doc)


def test_quotes_remain_inside_sql_literal():
    doc = data()
    doc["rows"][0]["title"] = "Saint's composition"
    sql = compile_migration(TalaRepairManifest.model_validate(doc))
    assert "Saint''s composition" in sql


def test_dollar_body_terminator_rejected():
    doc = data()
    doc["rows"][0]["title"] = "$track144$"
    with pytest.raises(ValueError, match="reserved SQL dollar delimiter"):
        compile_migration(TalaRepairManifest.model_validate(doc))


def test_suffix_manifest_compiles_to_migration():
    path = MANIFEST.with_name("track144-tala-evidence-suffix.json")
    manifest = TalaRepairManifest.model_validate_json(path.read_text())
    actual = MANIFEST.parents[1] / "migrations/V65__source_tala_suffix_backfill.sql"
    assert compile_migration(manifest, path.name) == actual.read_text()


def test_evidence_filename_cannot_inject_sql():
    with pytest.raises(ValueError, match="plain JSON basename"):
        compile_migration(TalaRepairManifest.model_validate(data()), "evidence.json\nSELECT 1;")


@pytest.mark.parametrize(
    "manifest_name,migration_name",
    [
        ("track144-tala-evidence-notation-review.json", "V66__reviewed_notation_tala_backfill.sql"),
        ("track144-tala-evidence-dikshitar-pdf.json", "V68__dikshitar_pdf_tala_backfill.sql"),
    ],
)
def test_additional_reviewed_manifests_compile(manifest_name, migration_name):
    path = MANIFEST.with_name(manifest_name)
    manifest = TalaRepairManifest.model_validate_json(path.read_text())
    actual = MANIFEST.parents[1] / "migrations" / migration_name
    assert compile_migration(manifest, path.name) == actual.read_text()
    if manifest.source_format == "PDF":
        assert "'PDF', 'MANUAL'" in actual.read_text()
        assert "raw PDF bytes" in actual.read_text()
        assert "cached UTF-8 HTML" not in actual.read_text()
