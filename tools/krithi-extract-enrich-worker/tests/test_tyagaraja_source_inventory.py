"""TRACK-147 source inventory: every CSV row is a subject or carries a curated disposition."""

from __future__ import annotations

from pathlib import Path

import pytest
from pydantic import ValidationError

from src.tyagaraja_classification import (
    SNAPSHOT_DATE,
    ImportCensusRow,
    InventoryDisposition,
    SourceInventoryRow,
    TyagarajaClassificationError,
    assert_track147_census,
    build_master,
    build_source_inventory,
    load_master,
    validate_master,
    verify_source,
)

ROOT = Path(__file__).resolve().parents[3]
MASTER_PATH = ROOT / "database" / "data" / "track147-tyagaraja-master.json"
URL = "https://thyagaraja-vaibhavam.blogspot.com/"


def _row(krithi_id: str, import_id: str) -> ImportCensusRow:
    return ImportCensusRow(
        krithi_id=krithi_id,
        title=f"title {import_id}",
        primary_language="en",
        musical_form="KRITHI",
        workflow_state="published",
        is_ragamalika=False,
        canonical_raga="Sāveri",
        canonical_tala="Adi",
        has_deity=False,
        has_temple=False,
        import_id=import_id,
        raw_raga="Saveri",
        raw_tala="Adi",
        source_url=URL + import_id,
        import_status="approved",
        pancharatnam_tag=False,
        evidence_id=None,
        pallavi_count=1,
        anupallavi_count=1,
        charanam_count=1,
        swara_sahitya_count=0,
    )


def _csv(tmp_path: Path, urls: list[str]) -> Path:
    path = tmp_path / "inventory.csv"
    lines = ["Krithi,Raga,Hyperlink", *[f"title {url.rsplit('/', 1)[-1]},Saveri,{url}" for url in urls]]
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    return path


def _not_imported() -> SourceInventoryRow:
    return SourceInventoryRow(
        row=1, title="", raw_raga="", url="", disposition=InventoryDisposition.NOT_IMPORTED, note="No import row."
    )


def test_csv_rows_map_to_subjects_and_curated_rows(tmp_path: Path):
    master = build_master([_row("krithi-a", "a"), _row("krithi-b", "b")], snapshot_date=SNAPSHOT_DATE)
    csv_path = _csv(tmp_path, [URL + "a", URL + "missing", URL + "b"])
    inventory = build_source_inventory(
        master, csv_path, relative_path="inventory.csv", outside_rows={URL + "missing": _not_imported()}
    )
    assert [row.disposition for row in inventory.rows] == [
        InventoryDisposition.CLASSIFICATION_SUBJECT,
        InventoryDisposition.NOT_IMPORTED,
        InventoryDisposition.CLASSIFICATION_SUBJECT,
    ]
    assert inventory.rows[1].row == 2 and inventory.rows[1].url == URL + "missing"
    with_inventory = master.model_copy(update={"source_inventory": inventory})
    validate_master(with_inventory)
    verify_source(with_inventory, tmp_path)

    csv_path.write_text(csv_path.read_text(encoding="utf-8") + "extra,Saveri,x\n", encoding="utf-8")
    with pytest.raises(TyagarajaClassificationError):
        verify_source(with_inventory, tmp_path)


def test_uncurated_csv_row_is_rejected(tmp_path: Path):
    master = build_master([_row("krithi-a", "a")], snapshot_date=SNAPSHOT_DATE)
    with pytest.raises(TyagarajaClassificationError):
        build_source_inventory(master, _csv(tmp_path, [URL + "a", URL + "missing"]), outside_rows={})


def test_inventory_cannot_hide_or_drop_a_subject(tmp_path: Path):
    master = build_master([_row("krithi-a", "a"), _row("krithi-b", "b")], snapshot_date=SNAPSHOT_DATE)
    csv_path = _csv(tmp_path, [URL + "a", URL + "b"])
    hidden = build_source_inventory(master, csv_path, relative_path="inventory.csv", outside_rows={})
    rows = list(hidden.rows)
    rows[1] = _not_imported().model_copy(update={"row": 2, "title": rows[1].title, "url": rows[1].url})
    with pytest.raises(TyagarajaClassificationError):
        validate_master(master.model_copy(update={"source_inventory": hidden.model_copy(update={"rows": rows})}))


def test_cross_composer_row_needs_mapped_identity_and_note():
    with pytest.raises(ValidationError):
        SourceInventoryRow(
            row=1,
            title="karuNa jUDavamma",
            raw_raga="tODi",
            url=URL + "x",
            disposition=InventoryDisposition.CROSS_COMPOSER_MAPPING,
            import_id="import-x",
        )


def test_live_inventory_matches_hashed_csv():
    master = load_master(MASTER_PATH)
    assert_track147_census(master)
    verify_source(master, ROOT)
    assert master.source_inventory is not None
    outside = {
        row.row: (row.disposition, row.mapped_composer)
        for row in master.source_inventory.rows
        if row.disposition != InventoryDisposition.CLASSIFICATION_SUBJECT
    }
    assert outside == {
        241: (InventoryDisposition.CROSS_COMPOSER_MAPPING, "Syama Sastri"),
        557: (InventoryDisposition.NOT_IMPORTED, None),
        587: (InventoryDisposition.CROSS_COMPOSER_MAPPING, "Muthuswami Dikshitar"),
    }
    assert [gap.reported_count for gap in master.external_gap] == [692]
