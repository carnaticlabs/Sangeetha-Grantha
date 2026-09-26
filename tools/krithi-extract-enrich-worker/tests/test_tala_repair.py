import json
from pathlib import Path

import pytest
from pydantic import ValidationError

from src.tala_repair import TalaRepairManifest, compile_migration

MANIFEST = Path(__file__).resolve().parents[3] / "database/data/track144-tala-evidence.json"


def data():
    return json.loads(MANIFEST.read_text())


def test_manifest_compiles_to_valid_migration():
    manifest = TalaRepairManifest.model_validate(data())
    sql = compile_migration(manifest)
    assert sql.startswith("-- corpus-data-fix: allow")
    assert "DO $track144$" in sql
    assert "INSERT INTO audit_log" in sql
    assert "jsonb_to_recordset" in sql
    assert str(manifest.rows[0].krithi_id) in sql


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
    sql = compile_migration(manifest, path.name)
    assert sql.startswith("-- corpus-data-fix: allow")
    assert path.name in sql
    assert "INSERT INTO audit_log" in sql


def test_evidence_filename_cannot_inject_sql():
    with pytest.raises(ValueError, match="plain JSON basename"):
        compile_migration(TalaRepairManifest.model_validate(data()), "evidence.json\nSELECT 1;")


@pytest.mark.parametrize(
    "manifest_name",
    [
        "track144-tala-evidence-notation-review.json",
        "track144-tala-evidence-dikshitar-pdf.json",
    ],
)
def test_additional_reviewed_manifests_compile(manifest_name):
    path = MANIFEST.with_name(manifest_name)
    manifest = TalaRepairManifest.model_validate_json(path.read_text())
    sql = compile_migration(manifest, path.name)
    assert "DO $track144$" in sql
    assert "INSERT INTO audit_log" in sql
    if manifest.source_format == "PDF":
        assert "'PDF', 'MANUAL'" in sql
        assert "raw PDF bytes" in sql
        assert "cached UTF-8 HTML" not in sql


def test_v64_migration_structure():
    v64_path = (
        Path(__file__).resolve().parents[3]
        / "database/migrations/V64__track144_catalogue_audit_and_vector_refresh_repair.sql"
    )
    assert v64_path.exists(), "V64 migration file must exist"
    sql = v64_path.read_text()
    assert "DO $track144_v64$" in sql
    assert "Populated beat_count and anga_structure for Catusra Ekam" in sql
    assert "Standardized notation variant tala to canonical Catusra Ekam" in sql
    assert "TRACK-144: composition title mismatch" in sql
    assert "TRACK-144: composition composer mismatch" in sql
    assert "TRACK-144: composition raga mismatch" in sql
    assert "TRACK-144: stored Latin incipit does not match" in sql
    assert "STALE_TRACK_144_NEEDS_REBUILD" in sql


def test_rebuild_track144_embeddings_cli():
    import subprocess
    import sys

    script = Path(__file__).resolve().parents[1] / "scripts/rebuild_track144_embeddings.py"
    assert script.exists()
    res = subprocess.run([sys.executable, str(script), "--help"], capture_output=True, text=True)
    assert res.returncode == 0
    assert "--dry-run" in res.stdout
    assert "Targeted forced rebuild of TRACK-144 embeddings" in res.stdout
