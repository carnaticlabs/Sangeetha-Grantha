"""TRACK-108 index-maintenance contract (validation findings 3, 4, 11, 12, 13).

Runs against the Flyway-migrated container from ``conftest.py`` so the
``embedding_profiles`` / ``search_documents`` / ``document_embeddings`` schema
(V58) and the ``audit_log`` table are the real ones.
"""

from __future__ import annotations

from collections.abc import Iterator

import psycopg
import pytest

from src.embeddings.catalogue_index import (
    STORAGE_DIMENSIONS,
    activate_profile,
    ensure_profile,
    find_obsolete_documents,
    find_profile,
    retire_documents,
    validate_dimensions,
)

UNIT_VECTOR = "[" + ",".join("1" if i == 0 else "0" for i in range(STORAGE_DIMENSIONS)) + "]"


@pytest.fixture()
def conn(database_url: str) -> Iterator[psycopg.Connection]:
    with psycopg.connect(database_url) as c:
        yield c
        c.rollback()
        with c.cursor() as cur:
            cur.execute("DELETE FROM document_embeddings")
            cur.execute("DELETE FROM search_documents")
            cur.execute("DELETE FROM embedding_profiles")
            cur.execute("DELETE FROM krithis WHERE title = 'W4 Index Maintenance Krithi'")
            cur.execute("DELETE FROM composers WHERE name = 'W4 Index Maintenance Composer'")
            cur.execute("DELETE FROM audit_log WHERE entity_table IN ('embedding_profiles', 'search_documents')")
        c.commit()


def _insert_krithi(conn: psycopg.Connection) -> str:
    with conn.cursor() as cur:
        cur.execute(
            "INSERT INTO composers (name, name_normalized) VALUES (%s, %s) RETURNING id",
            ("W4 Index Maintenance Composer", "w4 index maintenance composer"),
        )
        composer_id = cur.fetchone()[0]  # type: ignore[index]
        cur.execute(
            """
            INSERT INTO krithis (title, title_normalized, composer_id, primary_language)
            VALUES (%s, %s, %s, 'sa') RETURNING id
            """,
            ("W4 Index Maintenance Krithi", "w4 index maintenance krithi", composer_id),
        )
        krithi_id = str(cur.fetchone()[0])  # type: ignore[index]
    conn.commit()
    return krithi_id


def _insert_document(conn: psycopg.Connection, krithi_id: str, profile_id: str | None, chunk: int) -> str:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO search_documents
                (krithi_id, document_kind, source_chunk_index, original_content, indexed_content, content_hash)
            VALUES (%s, 'COMPOSITION_OVERVIEW', %s, 'orig', 'indexed', %s) RETURNING id
            """,
            (krithi_id, chunk, f"hash-{chunk}"),
        )
        doc_id = str(cur.fetchone()[0])  # type: ignore[index]
        if profile_id:
            cur.execute(
                """
                INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                VALUES (%s, %s, %s::vector(768), %s)
                """,
                (doc_id, profile_id, UNIT_VECTOR, f"hash-{chunk}"),
            )
    conn.commit()
    return doc_id


def _audit_actions(conn: psycopg.Connection, entity_table: str) -> list[str]:
    with conn.cursor() as cur:
        cur.execute(
            "SELECT action FROM audit_log WHERE entity_table = %s ORDER BY changed_at",
            (entity_table,),
        )
        return [r[0] for r in cur.fetchall()]


def test_validate_dimensions_rejects_widths_the_column_cannot_store() -> None:
    validate_dimensions(STORAGE_DIMENSIONS)
    with pytest.raises(ValueError, match="vector\\(768\\)"):
        validate_dimensions(512)


def test_first_profile_is_active_and_replacement_is_created_inactive(conn: psycopg.Connection) -> None:
    assert find_profile(conn, "gemini-embedding-2", 768) is None  # read-only path used by --dry-run

    first = ensure_profile(conn, "gemini-embedding-2", 768, actor="test")
    assert first.is_active is True

    replacement = ensure_profile(conn, "future-embedding-model", 768, actor="test")
    assert replacement.is_active is False
    assert replacement.id != first.id

    # Idempotent: a second call returns the same row without creating another.
    assert ensure_profile(conn, "future-embedding-model", 768, actor="test").id == replacement.id

    assert _audit_actions(conn, "embedding_profiles") == ["CREATE_EMBEDDING_PROFILE", "CREATE_EMBEDDING_PROFILE"]


def test_activate_profile_switches_atomically_and_refuses_empty_index(conn: psycopg.Connection) -> None:
    current = ensure_profile(conn, "gemini-embedding-2", 768, actor="test")
    replacement = ensure_profile(conn, "future-embedding-model", 768, actor="test")

    with pytest.raises(ValueError, match="no embeddings"):
        activate_profile(conn, replacement.id, actor="test")
    conn.rollback()

    krithi_id = _insert_krithi(conn)
    _insert_document(conn, krithi_id, replacement.id, chunk=0)

    deactivated = activate_profile(conn, replacement.id, actor="test")
    assert deactivated == [current.id]

    with conn.cursor() as cur:
        cur.execute("SELECT id::text, is_active FROM embedding_profiles ORDER BY created_at")
        rows: dict[str, bool] = dict(cur.fetchall())
    assert rows == {current.id: False, replacement.id: True}
    assert "ACTIVATE_EMBEDDING_PROFILE" in _audit_actions(conn, "embedding_profiles")


def test_retire_documents_removes_only_documents_outside_the_desired_set(conn: psycopg.Connection) -> None:
    profile = ensure_profile(conn, "gemini-embedding-2", 768, actor="test")
    krithi_id = _insert_krithi(conn)
    keep = _insert_document(conn, krithi_id, profile.id, chunk=0)
    stale = _insert_document(conn, krithi_id, profile.id, chunk=1)

    assert find_obsolete_documents(conn, krithi_id, [keep]) == [stale]
    assert find_obsolete_documents(conn, krithi_id, [keep, stale]) == []

    assert retire_documents(conn, [stale]) == 1
    conn.commit()

    with conn.cursor() as cur:
        cur.execute("SELECT id::text FROM search_documents WHERE krithi_id = %s", (krithi_id,))
        assert [r[0] for r in cur.fetchall()] == [keep]
        cur.execute("SELECT count(*) FROM document_embeddings")
        assert cur.fetchone()[0] == 1  # type: ignore[index]  # stale vector cascaded
