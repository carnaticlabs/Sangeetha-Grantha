"""Shared index-maintenance helpers for the catalogue embedding scripts.

Both `scripts/embed_catalogue.py` and `scripts/batch_embed_catalogue.py` mutate
`embedding_profiles`, `search_documents`, and `document_embeddings`. The rules
that keep those tables coherent live here so the two entry points cannot drift:

* Storage is fixed at ``vector(768)`` (V58); any other dimension is rejected
  before a single API call is made.
* A *replacement* profile (a second model/dimension combination) is created
  **inactive**. Retrieval binds to exactly one active profile, so a backfill
  for a new model never leaks into live search until it is switched on with
  :func:`activate_profile`, which flips the active flag atomically.
* Dry runs resolve profiles read-only and never create one.
* Every write records an ``audit_log`` row (repository mutation-audit rule).
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass
from typing import Any

import psycopg

logger = logging.getLogger(__name__)

STORAGE_DIMENSIONS = 768
"""Dimension of the `document_embeddings.embedding vector(768)` column (V58)."""

DEFAULT_TASK_TYPE = "RETRIEVAL_DOCUMENT"


@dataclass(frozen=True)
class EmbeddingProfile:
    id: str
    model_name: str
    dimensions: int
    is_active: bool


def validate_dimensions(dimensions: int) -> None:
    """Rejects dimensions that cannot be stored in the fixed-width vector column."""
    if dimensions != STORAGE_DIMENSIONS:
        raise ValueError(
            f"Requested {dimensions} dimensions but document_embeddings stores vector({STORAGE_DIMENSIONS}); "
            "a different width needs a new schema migration, not a new profile."
        )


def write_audit(
    conn: psycopg.Connection,
    *,
    entity_table: str,
    entity_id: str | None,
    action: str,
    diff: dict[str, Any] | None = None,
    metadata: dict[str, Any] | None = None,
) -> None:
    """Appends an AUDIT_LOG row inside the caller's current transaction."""
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
            VALUES (%s, %s, %s, %s::jsonb, %s::jsonb)
            """,
            (
                entity_table,
                entity_id,
                action,
                json.dumps(diff) if diff is not None else None,
                json.dumps(metadata) if metadata is not None else None,
            ),
        )


def find_profile(
    conn: psycopg.Connection,
    model_name: str,
    dimensions: int,
    task_type: str = DEFAULT_TASK_TYPE,
) -> EmbeddingProfile | None:
    """Read-only lookup of the profile for a model/dimension pair (safe for dry runs)."""
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id, model_name, dimensions, is_active
            FROM embedding_profiles
            WHERE model_name = %s AND dimensions = %s AND task_type = %s
            """,
            (model_name, dimensions, task_type),
        )
        row = cur.fetchone()
    if not row:
        return None
    return EmbeddingProfile(id=str(row[0]), model_name=row[1], dimensions=int(row[2]), is_active=bool(row[3]))


def ensure_profile(
    conn: psycopg.Connection,
    model_name: str,
    dimensions: int,
    *,
    actor: str,
    task_type: str = DEFAULT_TASK_TYPE,
) -> EmbeddingProfile:
    """Returns the profile for the model, creating it if needed.

    The first profile in an empty table becomes active. Any later profile is
    created inactive so retrieval keeps serving the current generation until
    :func:`activate_profile` switches over. Commits its own transaction.
    """
    validate_dimensions(dimensions)
    existing = find_profile(conn, model_name, dimensions, task_type)
    if existing:
        return existing

    with conn.cursor() as cur:
        cur.execute("SELECT count(*) FROM embedding_profiles WHERE is_active = true")
        row = cur.fetchone()
        active_count = int(row[0]) if row else 0
        make_active = active_count == 0

        cur.execute(
            """
            INSERT INTO embedding_profiles (model_name, dimensions, task_type, is_active)
            VALUES (%s, %s, %s, %s)
            RETURNING id
            """,
            (model_name, dimensions, task_type, make_active),
        )
        inserted = cur.fetchone()
        if not inserted:
            raise RuntimeError("Failed to insert embedding profile")
        profile_id = str(inserted[0])

    write_audit(
        conn,
        entity_table="embedding_profiles",
        entity_id=profile_id,
        action="CREATE_EMBEDDING_PROFILE",
        diff={"model_name": model_name, "dimensions": dimensions, "task_type": task_type, "is_active": make_active},
        metadata={"actor": actor},
    )
    conn.commit()

    if make_active:
        logger.info(
            "Created embedding profile %s (%s, %d dims) as the active profile", profile_id, model_name, dimensions
        )
    else:
        logger.warning(
            "Created embedding profile %s (%s, %d dims) INACTIVE because another profile is active. "
            "Retrieval keeps using the current profile; run with --activate-profile after evaluating the new index.",
            profile_id,
            model_name,
            dimensions,
        )
    return EmbeddingProfile(id=profile_id, model_name=model_name, dimensions=dimensions, is_active=make_active)


def activate_profile(conn: psycopg.Connection, profile_id: str, *, actor: str) -> list[str]:
    """Makes *profile_id* the single active profile in one transaction.

    Returns the ids of profiles that were deactivated. Raises if the target
    does not exist or has no embeddings yet (switching to an empty index would
    blank out search).
    """
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT p.model_name, p.dimensions, p.is_active,
                   (SELECT count(*) FROM document_embeddings e WHERE e.profile_id = p.id) AS embedding_count
            FROM embedding_profiles p
            WHERE p.id = %s
            """,
            (profile_id,),
        )
        row = cur.fetchone()
        if not row:
            raise ValueError(f"Embedding profile {profile_id} does not exist")
        model_name, dimensions, already_active, embedding_count = row[0], int(row[1]), bool(row[2]), int(row[3])
        if embedding_count == 0:
            raise ValueError(f"Embedding profile {profile_id} has no embeddings; refusing to activate an empty index")

        cur.execute(
            "UPDATE embedding_profiles SET is_active = false WHERE is_active = true AND id <> %s RETURNING id",
            (profile_id,),
        )
        deactivated = [str(r[0]) for r in cur.fetchall()]
        cur.execute("UPDATE embedding_profiles SET is_active = true WHERE id = %s", (profile_id,))

    write_audit(
        conn,
        entity_table="embedding_profiles",
        entity_id=profile_id,
        action="ACTIVATE_EMBEDDING_PROFILE",
        diff={"is_active": {"before": already_active, "after": True}, "deactivated": deactivated},
        metadata={
            "actor": actor,
            "model_name": model_name,
            "dimensions": dimensions,
            "embedding_count": embedding_count,
        },
    )
    conn.commit()
    logger.info(
        "Activated embedding profile %s (%s, %d dims, %d vectors); deactivated %s",
        profile_id,
        model_name,
        dimensions,
        embedding_count,
        deactivated or "none",
    )
    return deactivated


def find_obsolete_documents(conn: psycopg.Connection, krithi_id: str, keep_doc_ids: list[str]) -> list[str]:
    """Search documents for *krithi_id* that are not in the desired set.

    A document becomes obsolete when its source text dropped below the
    eligibility threshold, its lyric-section junction was removed, or its
    section/variant no longer exists. Read-only; usable in dry runs.
    """
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT id FROM search_documents
            WHERE krithi_id = %s AND NOT (id = ANY(%s::uuid[]))
            """,
            (krithi_id, keep_doc_ids),
        )
        return [str(r[0]) for r in cur.fetchall()]


def retire_documents(conn: psycopg.Connection, doc_ids: list[str]) -> int:
    """Deletes obsolete search documents (embeddings cascade). No commit."""
    if not doc_ids:
        return 0
    with conn.cursor() as cur:
        cur.execute("DELETE FROM search_documents WHERE id = ANY(%s::uuid[])", (doc_ids,))
        return cur.rowcount
