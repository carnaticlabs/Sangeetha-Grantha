"""Macro search document indexer for canonical cycles and kshetras (TRACK-145).

Generates and embeds CYCLE_OVERVIEW documents (anchored to tag_id) and
KSHETRA_OVERVIEW documents (anchored to temple_id), including documented
gap shrines like Kanyakumari.
"""

from __future__ import annotations

import hashlib
import logging
from typing import Any

import psycopg
from psycopg.rows import dict_row

from .context_formatter import format_cycle_overview, format_kshetra_overview
from .gemini_embedder import GeminiEmbedder

logger = logging.getLogger(__name__)


def md5_hash(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def index_cycle_overviews(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
    profile_id: str,
    force: bool = False,
    dry_run: bool = False,
) -> dict[str, int]:
    """Generates and embeds CYCLE_OVERVIEW documents anchored to tags.id."""
    query_tags = """
        SELECT id, slug, display_name_en, description_en
        FROM tags
        WHERE slug IN (
            'kamalamba-navavarnam',
            'guruguha-vibhakti',
            'tyagaraja-vibhakti',
            'nilotpalamba-vibhakti',
            'abhayamba-vibhakti',
            'navagraha-krithis',
            'pancha-bhuta-sthala',
            'tiruvarur-panchalinga',
            'shodasa-ganapati',
            'nottusvara-sahitya'
        )
        ORDER BY display_name_en
    """

    stats = {"embedded": 0, "skipped": 0}

    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query_tags)
        cycle_tags = cur.fetchall()

    for tag in cycle_tags:
        tag_id = tag["id"]
        slug = tag["slug"]
        name = tag["display_name_en"]
        desc = tag["description_en"] or ""

        # Fetch cycle members
        query_members = """
            SELECT 
                kcm.sequence_order,
                kcm.role,
                kcm.axis_value,
                kcm.discriminative_attributes,
                k.title,
                r.name AS raga,
                t.name AS tala,
                k.vibhakti_case
            FROM krithi_cycle_memberships kcm
            JOIN krithis k ON kcm.krithi_id = k.id
            LEFT JOIN ragas r ON k.primary_raga_id = r.id
            LEFT JOIN talas t ON k.tala_id = t.id
            WHERE kcm.tag_id = %s
            ORDER BY kcm.sequence_order ASC, k.title ASC
        """
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(query_members, (tag_id,))
            members = cur.fetchall()

        overview_text = format_cycle_overview(
            cycle_name=name,
            composer="Muthuswami Dikshitar",
            description=desc,
            members=members,
        )
        content_hash = md5_hash(overview_text)

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT sd.id, de.id, sd.content_hash
                FROM search_documents sd
                LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
                WHERE sd.tag_id = %s AND sd.document_kind = 'CYCLE_OVERVIEW'
                """,
                (profile_id, tag_id),
            )
            existing = cur.fetchone()

            if existing and existing[1] and existing[2] == content_hash and not force:
                stats["skipped"] += 1
                continue

            if not dry_run:
                vector = embedder.embed_document(overview_text, title=f"Cycle: {name}")
                cur.execute(
                    """
                    INSERT INTO search_documents (
                        tag_id, document_kind, original_content, indexed_content, content_hash
                    ) VALUES (%s, 'CYCLE_OVERVIEW', %s, %s, %s)
                    ON CONFLICT (krithi_id, temple_id, tag_id, section_id, variant_id, document_kind, source_chunk_index)
                    DO UPDATE SET original_content = EXCLUDED.original_content,
                                  indexed_content = EXCLUDED.indexed_content,
                                  content_hash = EXCLUDED.content_hash,
                                  updated_at = clock_timestamp()
                    RETURNING id
                    """,
                    (tag_id, desc, overview_text, content_hash),
                )
                doc_row = cur.fetchone()
                if not doc_row:
                    raise RuntimeError(f"Failed to upsert search document for cycle {name}")
                doc_id = doc_row[0]

                cur.execute(
                    """
                    INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT (document_id, profile_id)
                    DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
                    """,
                    (doc_id, profile_id, vector, content_hash),
                )
                conn.commit()
            stats["embedded"] += 1
            logger.info("Embedded CYCLE_OVERVIEW for %s (%d members)", name, len(members))

    return stats


def index_kshetra_overviews(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder,
    profile_id: str,
    force: bool = False,
    dry_run: bool = False,
) -> dict[str, int]:
    """Generates and embeds KSHETRA_OVERVIEW documents anchored to temples.id."""
    query_temples = """
        SELECT id, name, city, state, mandalam, bhuta, deity_posture, notes
        FROM temples
        WHERE place_kind IN ('COMPLEX', 'SANNIDHI')
        ORDER BY name
    """

    stats = {"embedded": 0, "skipped": 0}

    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query_temples)
        temples = cur.fetchall()

    for temple in temples:
        temple_id = temple["id"]
        name = temple["name"]
        city = temple["city"] or ""
        state = temple["state"] or ""
        mandalam = temple["mandalam"]
        bhuta = temple["bhuta"]
        posture = temple["deity_posture"]
        notes = temple["notes"] or ""

        # Fetch kritis for this temple
        query_kritis = """
            SELECT k.title, r.name AS raga, t.name AS tala, k.vibhakti_case
            FROM krithis k
            LEFT JOIN ragas r ON k.primary_raga_id = r.id
            LEFT JOIN talas t ON k.tala_id = t.id
            WHERE k.temple_id = %s
            ORDER BY k.title ASC
        """
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(query_kritis, (temple_id,))
            kritis = cur.fetchall()

        is_gap = "kanyakumari" in name.lower() or "kanyakumari" in city.lower() or len(kritis) == 0

        overview_text = format_kshetra_overview(
            temple_name=name,
            city=city,
            state=state,
            mandalam=mandalam,
            bhuta=bhuta,
            deity_posture=posture,
            notes=notes,
            kritis=kritis,
            is_gap=is_gap,
        )
        content_hash = md5_hash(overview_text)

        with conn.cursor() as cur:
            cur.execute(
                """
                SELECT sd.id, de.id, sd.content_hash
                FROM search_documents sd
                LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
                WHERE sd.temple_id = %s AND sd.document_kind = 'KSHETRA_OVERVIEW'
                """,
                (profile_id, temple_id),
            )
            existing = cur.fetchone()

            if existing and existing[1] and existing[2] == content_hash and not force:
                stats["skipped"] += 1
                continue

            if not dry_run:
                vector = embedder.embed_document(overview_text, title=f"Kshetra: {name} ({city})")
                cur.execute(
                    """
                    INSERT INTO search_documents (
                        temple_id, document_kind, original_content, indexed_content, content_hash
                    ) VALUES (%s, 'KSHETRA_OVERVIEW', %s, %s, %s)
                    ON CONFLICT (krithi_id, temple_id, tag_id, section_id, variant_id, document_kind, source_chunk_index)
                    DO UPDATE SET original_content = EXCLUDED.original_content,
                                  indexed_content = EXCLUDED.indexed_content,
                                  content_hash = EXCLUDED.content_hash,
                                  updated_at = clock_timestamp()
                    RETURNING id
                    """,
                    (temple_id, notes, overview_text, content_hash),
                )
                doc_row = cur.fetchone()
                if not doc_row:
                    raise RuntimeError(f"Failed to upsert search document for temple {name}")
                doc_id = doc_row[0]

                cur.execute(
                    """
                    INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT (document_id, profile_id)
                    DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
                    """,
                    (doc_id, profile_id, vector, content_hash),
                )
                conn.commit()
            stats["embedded"] += 1
            logger.info("Embedded KSHETRA_OVERVIEW for %s (%s) [gap=%s, %d kritis]", name, city, is_gap, len(kritis))

    return stats
