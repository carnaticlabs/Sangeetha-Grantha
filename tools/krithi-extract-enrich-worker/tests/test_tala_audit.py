from src.tala_audit import InventoryRow, TalaAuditor

URL = "https://thyagaraja-vaibhavam.blogspot.com/example"


def examine(header, **kwargs):
    row = InventoryRow(
        id="00000000-0000-4000-8000-000000000001", title="Example", composer="Tyagaraja", raga="Bhairavi", **kwargs
    )
    html = f"<div class='post-body'>Example<br>{header}<br>pallavi<br>words</div>"
    return TalaAuditor().examine(row, URL, html)


def test_explicit_tala_is_candidate_not_approval():
    result = examine("rAga bhairavi (tALa miSra cApu)")
    assert result.proposed_tala == "Misra Capu"
    assert result.review_required
    assert result.source_checksum


def test_lyric_reference_does_not_supply_missing_tala():
    result = examine("pallavi<br>words<br>Notes: another song rAga mohana (tALa Adi)")
    assert result.status == "no_explicit_tala"
    assert result.proposed_tala is None


def test_wrong_raga_link_is_not_accepted():
    result = examine(
        "rAga pantuvarali (tALa rUpakam)",
        imports=[
            {
                "import_id": "00000000-0000-4000-8000-000000000002",
                "raga": "Pantuvarali",
                "tala": "Rupakam",
                "source_url": URL,
            }
        ],
    )
    assert result.status == "identity_review"
    assert result.review_required


def test_damaged_tala_is_not_guessed():
    result = examine("rAga bhairavi (tALa isra capu)")
    assert result.status == "unrecognized_tala"
    assert result.proposed_tala is None


def test_transliteration_alphabet_does_not_end_the_header():
    result = examine("Transliteration-Telugu<br>p ph b bh m<br>rAga bhairavi (tALa Adi)")
    assert result.proposed_tala == "Adi"


def test_unlabeled_translation_heading_is_not_tala():
    result = examine("Transliteration-Telugu")
    assert result.raw_tala is None


def test_word_talaci_is_not_a_tala_label():
    result = examine("Transliteration-Telugu<br>talaci the name")
    assert result.raw_tala is None


def test_tala_label_after_value_in_source_prose():
    result = examine("Transliteration–Telugu<br>In the kRti, rAga Arabhi (miSra cApu tALa), SrI tyAgarAja")
    assert result.raw_tala == "Misra Capu"
    assert result.source_raga == "Arabhi"
    assert result.proposed_tala == "Misra Capu"


def test_inventory_without_source_is_reported(tmp_path):
    import asyncio
    import json

    from scripts.audit_missing_talas import SourceCache, run
    from src.tala_audit import Inventory

    row = InventoryRow(id="00000000-0000-4000-8000-000000000001", title="Example", composer="Tyagaraja")
    output = tmp_path / "result.json"
    results = asyncio.run(run(Inventory(rows=[row]), output, SourceCache(tmp_path / "cache", offline=True)))
    assert results[0].status == "source_missing"
    assert json.loads(output.read_text())["complete"] is True
