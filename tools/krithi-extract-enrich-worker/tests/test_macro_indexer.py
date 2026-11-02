"""Composer attribution and approval gates for existing macro search infrastructure."""

from src.embeddings.context_formatter import format_composition_overview
from src.embeddings.macro_indexer import reviewed_cycle_composer

SLUG = "syama-sastri-swarajathi-ratnatrayam"
APPROVAL = {"decision": "approve", "reviewer": "Fixture curator", "accepted_at": "2026-10-01", "note": "Test only"}
ATTRS = {"provenance_owner": "TRACK-146", "review_state": "reviewed", "claim_id": "fixture", "approval": APPROVAL}


def test_macro_composer_is_derived_and_mixed_groups_abstain():
    assert (
        reviewed_cycle_composer("kamalamba-navavarnam", [{"composer_name": "Muthuswami Dikshitar"}])
        == "Muthuswami Dikshitar"
    )
    assert reviewed_cycle_composer(SLUG, []) is None
    assert (
        reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri"}, {"composer_name": "Subbaraya Sastri"}])
        is None
    )
    assert reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri"}]) is None
    assert (
        reviewed_cycle_composer(SLUG, [{"composer_name": "Syama Sastri", "discriminative_attributes": ATTRS}])
        == "Syama Sastri"
    )


def test_sastri_cycle_abstains_without_individual_approval():
    overview = format_composition_overview(
        title="Kamakshi anudinamu",
        composer="Syama Sastri",
        raga="Bhairavi",
        musical_form="SWARAJATHI",
        tags=["Swarajathi Ratnatrayam", "Navaratnamalika"],
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": {}},
    )
    assert "[Cycle:" not in overview
    assert "Ratnatrayam" not in overview
    assert "Navaratnamalika" not in overview
    assert "[Kshetra:" not in overview


def test_approved_cycle_preserves_budget_and_short_passage():
    overview = format_composition_overview(
        title="Kamakshi anudinamu",
        composer="Syama Sastri",
        raga="Bhairavi",
        musical_form="SWARAJATHI",
        primary_lyrics="Kamakshi",
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": ATTRS},
    )
    assert "[Cycle: Ratnatrayam]" in overview
    assert len(overview.splitlines()[0].split()) <= 60
    assert "Kamakshi" in overview
    other = format_composition_overview(
        title="Kamakshi",
        composer="Subbaraya Sastri",
        cycle_info={"slug": SLUG, "display_name": "Ratnatrayam", "attrs": ATTRS},
    )
    assert "[Cycle:" not in other


# --- TRACK-157: freshness check and macro-only mode -------------------------------------------------

import argparse  # noqa: E402
import logging  # noqa: E402
from typing import Any  # noqa: E402

import pytest  # noqa: E402

from scripts import embed_catalogue  # noqa: E402
from src.embeddings import macro_indexer  # noqa: E402
from src.embeddings.catalogue_index import EmbeddingProfile  # noqa: E402
from src.embeddings.macro_indexer import MacroDocument, md5_hash  # noqa: E402


class _Cursor:
    def __init__(self, row):
        self._row = row

    def __enter__(self):
        return self

    def __exit__(self, *exc):
        return False

    def execute(self, *args, **kwargs):
        pass

    def fetchone(self):
        return self._row


class _Conn:
    """Returns one canned row for every lookup: (doc id, embedding id, doc hash, embedding hash, md5(text))."""

    def __init__(self, row):
        self._row = row

    def cursor(self, **kwargs):
        return _Cursor(self._row)


_NO_CONN: Any = object()


def _conn(row: Any) -> Any:
    return _Conn(row)


def _doc(text="overview text"):
    return MacroDocument(
        kind="KSHETRA_OVERVIEW",
        anchor_id="t1",
        label="Temple (City)",
        original_content="n",
        overview_text=text,
        content_hash=md5_hash(text),
        detail="",
    )


def test_fresh_document_has_no_stale_reasons():
    h = md5_hash("overview text")
    assert macro_indexer.stale_reasons(_conn(("d", "e", h, h, h)), _doc(), "p") == []


@pytest.mark.parametrize(
    ("row", "expected"),
    [
        (None, "no search document"),
        (("d", None, "h", None, "h"), "no embedding"),
        (("d", "e", "h", "other", "h"), "embedding hash differs"),
        (("d", "e", "old", "old", "old"), "source data changed"),
        (("d", "e", md5_hash("overview text"), md5_hash("overview text"), "tampered"), "indexed text"),
    ],
)
def test_each_staleness_cause_is_reported(row, expected):
    reasons = macro_indexer.stale_reasons(_conn(row), _doc(), "p")
    assert any(expected in r for r in reasons), reasons


def test_check_counts_stale_documents(monkeypatch):
    fresh, stale = _doc("a"), _doc("b")
    monkeypatch.setattr(macro_indexer, "build_cycle_documents", lambda conn: ([fresh], 0))
    monkeypatch.setattr(macro_indexer, "build_kshetra_documents", lambda conn, composer=None: [stale])
    monkeypatch.setattr(
        macro_indexer, "stale_reasons", lambda conn, d, p: [] if d is fresh else ["source data changed"]
    )
    checked, found = macro_indexer.check_macro_freshness(_NO_CONN, "p")
    assert checked == 2 and [d for d, _ in found] == [stale]


def _args(**kw):
    base = dict(model="m", dims=768, check=False, dry_run=False, force=False, vertexai=False, project_id=None)
    base.update(kw)
    return argparse.Namespace(**base)


def test_macro_only_never_touches_krithi_candidates(monkeypatch):
    def boom(*a, **k):
        raise AssertionError("macro-only must not fetch krithi candidates")

    monkeypatch.setattr(embed_catalogue, "fetch_krithi_candidates", boom)
    monkeypatch.setattr(embed_catalogue, "index_krithi", boom)
    monkeypatch.setattr(embed_catalogue, "find_profile", lambda *a, **k: EmbeddingProfile("p", "m", 768, True))
    seen: dict[str, Any] = {}
    monkeypatch.setattr(
        embed_catalogue,
        "index_cycle_overviews",
        lambda conn, emb, pid, force, dry_run: seen.update(cycle_embedder=emb) or {"embedded": 1, "skipped": 0},
    )
    monkeypatch.setattr(
        embed_catalogue, "index_kshetra_overviews", lambda conn, emb, pid, force, dry_run: {"embedded": 2, "skipped": 3}
    )
    assert embed_catalogue.run_macro_only(_NO_CONN, _args(dry_run=True)) == 0
    assert seen["cycle_embedder"] is None  # dry run builds no embedding client


def test_check_exit_code_and_no_embedder(monkeypatch, caplog):
    monkeypatch.setattr(embed_catalogue, "find_profile", lambda *a, **k: EmbeddingProfile("p", "m", 768, True))
    monkeypatch.setattr(
        embed_catalogue, "GeminiEmbedder", lambda **k: (_ for _ in ()).throw(AssertionError("no embedder in --check"))
    )
    stale_doc = _doc()
    monkeypatch.setattr(embed_catalogue, "check_macro_freshness", lambda c, p: (3, [(stale_doc, ["x"])]))
    with caplog.at_level(logging.INFO):
        assert embed_catalogue.run_macro_only(_NO_CONN, _args(check=True)) == 1
    assert "stale = 1" in caplog.text
    monkeypatch.setattr(embed_catalogue, "check_macro_freshness", lambda c, p: (3, []))
    assert embed_catalogue.run_macro_only(_NO_CONN, _args(check=True)) == 0
