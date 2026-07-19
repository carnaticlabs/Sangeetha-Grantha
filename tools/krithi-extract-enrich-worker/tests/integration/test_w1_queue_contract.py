"""W1 (TRACK-113): the Kotlin ↔ Python queue contract, against the real schema.

The Kotlin backend INSERTs PENDING rows; the Python worker claims them with
SELECT ... FOR UPDATE SKIP LOCKED and completes them. These tests exercise
ExtractionQueueDB against a Flyway-migrated PostgreSQL 18 container — the
same DDL (V27 + V31) both sides run in production.
"""

from __future__ import annotations

import json

from src.config import ExtractorConfig
from src.db import ExtractionQueueDB

from .conftest import fetch_task_row, insert_pending_task


def test_claim_transitions_pending_to_processing(queue_db: ExtractionQueueDB) -> None:
    task_id = insert_pending_task(queue_db)

    task = queue_db.claim_pending_task()

    assert task is not None
    assert task.id == task_id
    assert task.source_url == "https://example.org/krithi/1"
    assert task.source_format == "HTML"
    assert task.request_payload == {"composerHint": "Dikshitar"}
    # The claimed task reports the attempt in progress (post-increment)
    assert task.attempts == 1

    row = fetch_task_row(queue_db, task_id)
    assert row["status"] == "PROCESSING"
    assert row["attempts"] == 1
    assert row["claimed_at"] is not None
    assert row["claimed_by"] == ExtractorConfig().hostname


def test_claim_returns_none_on_empty_queue(queue_db: ExtractionQueueDB) -> None:
    assert queue_db.claim_pending_task() is None


def test_claim_skips_tasks_that_exhausted_max_attempts(queue_db: ExtractionQueueDB) -> None:
    insert_pending_task(queue_db, attempts=3, max_attempts=3)
    assert queue_db.claim_pending_task() is None


def test_claim_orders_by_created_at(queue_db: ExtractionQueueDB) -> None:
    first = insert_pending_task(queue_db, source_url="https://example.org/first")
    insert_pending_task(queue_db, source_url="https://example.org/second")

    task = queue_db.claim_pending_task()

    assert task is not None
    assert task.id == first


def test_mark_done_persists_result_payload_and_metadata(queue_db: ExtractionQueueDB) -> None:
    task_id = insert_pending_task(queue_db)
    task = queue_db.claim_pending_task()
    assert task is not None

    payload = [{"title": "vAtApi gaNapatim", "composer": "Dikshitar"}]
    queue_db.mark_done(
        task_id=task.id,
        result_payload=payload,
        extraction_method="HTML_JSOUP",
        confidence=0.85,
        duration_ms=1234,
        source_checksum="sha256:abc",
    )

    row = fetch_task_row(queue_db, task_id)
    assert row["status"] == "DONE"
    assert row["result_payload"] == payload
    assert row["result_count"] == 1
    assert row["extraction_method"] == "HTML_JSOUP"
    assert float(row["confidence"]) == 0.85
    assert row["duration_ms"] == 1234
    assert row["source_checksum"] == "sha256:abc"


def test_mark_failed_persists_diagnostics_without_partial_results(queue_db: ExtractionQueueDB) -> None:
    task_id = insert_pending_task(queue_db)
    task = queue_db.claim_pending_task()
    assert task is not None

    error = {"message": "HTTP 500 from source", "type": "HTTPStatusError", "attempt": 1}
    queue_db.mark_failed(task.id, error)

    row = fetch_task_row(queue_db, task_id)
    assert row["status"] == "FAILED"
    assert row["error_detail"] == error
    assert row["last_error_at"] is not None
    # No partial writes: the result side of the row stays empty
    assert row["result_payload"] is None
    assert row["result_count"] is None


def test_skip_locked_two_workers_never_claim_the_same_task(
    queue_db: ExtractionQueueDB, database_url: str, monkeypatch
) -> None:
    """Two concurrent claimers (separate connections) must get distinct tasks."""
    a = insert_pending_task(queue_db, source_url="https://example.org/a")
    b = insert_pending_task(queue_db, source_url="https://example.org/b")

    monkeypatch.setenv("DATABASE_URL", database_url)
    second_worker = ExtractionQueueDB(ExtractorConfig())
    try:
        # Hold the first claim's row lock open mid-transaction by claiming
        # without committing on a raw connection.
        with queue_db.conn.cursor() as cur:
            cur.execute(
                """
                SELECT id FROM extraction_queue
                WHERE status = 'PENDING' AND attempts < max_attempts
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
                """
            )
            locked_row = cur.fetchone()
            assert locked_row is not None
            locked_id = locked_row["id"]

            # While that lock is held, the second worker's full claim cycle
            # must skip the locked row and take the other task.
            other = second_worker.claim_pending_task()
            assert other is not None
            assert other.id != locked_id
            assert {locked_id, other.id} == {a, b}
        queue_db.conn.rollback()
    finally:
        second_worker.close()


def test_queue_stats_reflect_status_counts(queue_db: ExtractionQueueDB) -> None:
    insert_pending_task(queue_db)
    done_id = insert_pending_task(queue_db, source_url="https://example.org/done")
    task = None
    # claim the older row first, then mark whichever we need
    while (claimed := queue_db.claim_pending_task()) is not None:
        if claimed.id == done_id:
            task = claimed
    assert task is not None
    queue_db.mark_done(task.id, [], "HTML_JSOUP", 0.5, 10)

    stats = queue_db.get_queue_stats()
    assert stats.done == 1
    assert stats.processing == 1
    assert stats.pending == 0


def test_result_payload_jsonb_roundtrip_preserves_unicode(queue_db: ExtractionQueueDB) -> None:
    """Devanagari lyric text must survive the jsonb write/read cycle intact."""
    task_id = insert_pending_task(queue_db)
    task = queue_db.claim_pending_task()
    assert task is not None

    devanagari = "वातापि गणपतिं भजेऽहं"
    queue_db.mark_done(task.id, [{"title": devanagari}], "HTML_JSOUP", 0.9, 5)

    row = fetch_task_row(queue_db, task_id)
    assert row["result_payload"][0]["title"] == devanagari
    # And it is real jsonb, not a double-encoded string
    assert not isinstance(row["result_payload"], str)
    json.dumps(row["result_payload"])  # sanity: JSON-serializable as returned
