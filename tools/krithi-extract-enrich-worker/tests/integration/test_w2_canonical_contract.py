"""W2 (TRACK-113): the cross-language canonical extraction contract.

One golden fixture — shared/domain/model/import/fixtures/canonical-extraction-golden.json —
is verified on three axes:

1. It validates against canonical-extraction-schema.json (the language-neutral contract).
2. The Python pydantic model round-trips it losslessly (aliases, snake_case
   normalized keys, enums — where the TRACK-096 bugs lived).
3. Written through mark_done into the real jsonb column, it reads back identical.

The Kotlin half (kotlinx.serialization decode of the same file + ingest via
ExtractionResultProcessor) lives in the backend test suite; together they
machine-verify the seam.
"""

from __future__ import annotations

import json

import jsonschema
import pytest

from src.db import ExtractionQueueDB
from src.schema import CanonicalExtraction

from .conftest import CANONICAL_SCHEMA, GOLDEN_FIXTURE, fetch_task_row, insert_pending_task


@pytest.fixture(scope="module")
def golden() -> dict:
    return json.loads(GOLDEN_FIXTURE.read_text(encoding="utf-8"))


@pytest.fixture(scope="module")
def schema() -> dict:
    return json.loads(CANONICAL_SCHEMA.read_text(encoding="utf-8"))


def test_golden_fixture_validates_against_canonical_schema(golden: dict, schema: dict) -> None:
    jsonschema.validate(instance=golden, schema=schema)


def test_pydantic_roundtrip_is_lossless(golden: dict) -> None:
    """model_validate → to_json_dict must reproduce the fixture byte-for-byte.

    This pins the exact wire shape the Kotlin side deserializes: camelCase
    aliases everywhere EXCEPT the four *_normalized keys, which are snake_case
    on both sides (@SerialName in Kotlin, no alias in pydantic).
    """
    extraction = CanonicalExtraction.model_validate(golden)
    assert extraction.to_json_dict() == golden


def test_roundtrip_output_still_validates_against_schema(golden: dict, schema: dict) -> None:
    payload = CanonicalExtraction.model_validate(golden).to_json_dict()
    jsonschema.validate(instance=payload, schema=schema)


def test_normalized_keys_stay_snake_case_on_the_wire(golden: dict) -> None:
    payload = CanonicalExtraction.model_validate(golden).to_json_dict()
    for key in ("title_normalized", "composer_normalized", "raga_normalized", "tala_normalized"):
        assert key in payload, f"{key} must serialize snake_case (Kotlin @SerialName contract)"
    for wrong in ("titleNormalized", "composerNormalized", "ragaNormalized", "talaNormalized"):
        assert wrong not in payload


def test_golden_payload_written_by_worker_reads_back_identical(
    queue_db: ExtractionQueueDB, golden: dict
) -> None:
    """The full DB leg of the seam: mark_done(jsonb) → SELECT → same document."""
    task_id = insert_pending_task(queue_db)
    task = queue_db.claim_pending_task()
    assert task is not None

    payload = [CanonicalExtraction.model_validate(golden).to_json_dict()]
    queue_db.mark_done(task.id, payload, "HTML_JSOUP", 0.9, 42)

    row = fetch_task_row(queue_db, task_id)
    assert row["status"] == "DONE"
    assert row["result_count"] == 1
    stored = row["result_payload"][0]
    assert stored == golden
    # And what came out of the database still honors the contract
    jsonschema.validate(instance=stored, schema=json.loads(CANONICAL_SCHEMA.read_text(encoding="utf-8")))
