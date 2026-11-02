"""Macro search document indexer for canonical cycles and kshetras (TRACK-145).

Generates and embeds CYCLE_OVERVIEW documents (anchored to tag_id) and
KSHETRA_OVERVIEW documents (anchored to temple_id), including documented
gap shrines like Kanyakumari.
"""

from __future__ import annotations

import hashlib
import logging
from dataclasses import dataclass
from typing import Any

import psycopg
from psycopg.rows import dict_row

from .context_formatter import TYAGARAJA_CYCLE_SLUGS, format_cycle_overview, format_kshetra_overview
from .gemini_embedder import GeminiEmbedder

logger = logging.getLogger(__name__)


def md5_hash(text: str) -> str:
    return hashlib.md5(text.encode("utf-8")).hexdigest()


def reviewed_cycle_composer(slug: str, members: list[dict[str, Any]]) -> str | None:
    """Derive attribution from members; newly curated groups require claim approval.

    Existing Dikshitar membership behavior is preserved. Empty/mixed attribution
    and unapproved Syama Sastri or Tyagaraja groups abstain instead of generating false macros.
    """
    composers = {m.get("composer_name") for m in members}
    if len(composers) != 1 or not all(composers):
        return None
    if slug.startswith("syama-sastri-"):
        if composers != {"Syama Sastri"} or not members:
            return None
        for member in members:
            attrs = member.get("discriminative_attributes") or {}
            if not isinstance(attrs, dict) or attrs.get("provenance_owner") != "TRACK-146":
                return None
            if attrs.get("review_state") != "reviewed" or not attrs.get("claim_id"):
                return None
            approval = attrs.get("approval")
            if not isinstance(approval, dict) or approval.get("decision") != "approve":
                return None
            if not all(approval.get(k) for k in ("reviewer", "accepted_at", "note")):
                return None
    elif slug in TYAGARAJA_CYCLE_SLUGS:
        if composers != {"Tyagaraja"} or not members:
            return None
        for member in members:
            attrs = member.get("discriminative_attributes") or {}
            if not isinstance(attrs, dict) or attrs.get("provenance_owner") != "TRACK-147":
                return None
            if attrs.get("review_state") != "reviewed" or not attrs.get("claim_id"):
                return None
            approval = attrs.get("approval")
            if not isinstance(approval, dict) or approval.get("decision") != "approve":
                return None
            if not all(approval.get(k) for k in ("reviewer", "accepted_at", "note")):
                return None
    return str(next(iter(composers)))


@dataclass(frozen=True)
class MacroDocument:
    """One regenerated macro document: what `search_documents` and its embedding should currently hold."""

    kind: str  # 'CYCLE_OVERVIEW' or 'KSHETRA_OVERVIEW'
    anchor_id: Any
    label: str
    original_content: str
    overview_text: str
    content_hash: str
    detail: str


def build_cycle_documents(conn: psycopg.Connection) -> tuple[list[MacroDocument], int]:
    """Regenerates every eligible CYCLE_OVERVIEW from current source data (no writes, no embedding).

    Returns the documents and the number of cycles that abstained (empty, mixed-composer or unapproved).
    """
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
            'nottusvara-sahitya',
            'syama-sastri-swarajathi-ratnatrayam',
            'syama-sastri-navaratnamalika',
            'tyagaraja-pancharatnam'
        )
        ORDER BY display_name_en
    """
    abstained = 0
    docs: list[MacroDocument] = []

    with conn.cursor(row_factory=dict_row) as cur:
        cur.execute(query_tags)
        cycle_tags = cur.fetchall()

    for tag in cycle_tags:
        tag_id = tag["id"]
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
                c.name AS composer_name,
                r.name AS raga,
                t.name AS tala,
                k.vibhakti_case
            FROM krithi_cycle_memberships kcm
            JOIN krithis k ON kcm.krithi_id = k.id
            JOIN composers c ON c.id = k.composer_id
            LEFT JOIN ragas r ON k.primary_raga_id = r.id
            LEFT JOIN talas t ON k.tala_id = t.id
            WHERE kcm.tag_id = %s
            ORDER BY kcm.sequence_order ASC, k.title ASC
        """
        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(query_members, (tag_id,))
            members = cur.fetchall()

        composer = reviewed_cycle_composer(tag["slug"], members)
        if composer is None:
            abstained += 1
            logger.warning("Skipped empty, mixed-composer or unapproved cycle %s", tag["slug"])
            continue

        overview_text = format_cycle_overview(
            cycle_name=name,
            composer=composer,
            description=desc,
            members=members,
        )
        docs.append(
            MacroDocument(
                kind="CYCLE_OVERVIEW",
                anchor_id=tag_id,
                label=name,
                original_content=desc,
                overview_text=overview_text,
                content_hash=md5_hash(overview_text),
                detail=f"{len(members)} members",
            )
        )
    return docs, abstained


def build_kshetra_documents(conn: psycopg.Connection, composer: str | None = None) -> list[MacroDocument]:
    """Regenerates every KSHETRA_OVERVIEW from current source data (no writes, no embedding)."""
    query_temples = """
        SELECT id, name, city, state, mandalam, bhuta, deity_posture, notes
        FROM temples
        WHERE place_kind IN ('COMPLEX', 'SANNIDHI')
        ORDER BY name
    """
    docs: list[MacroDocument] = []

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
        if composer:
            query_kritis = """
                SELECT k.title, r.name AS raga, t.name AS tala, k.vibhakti_case
                FROM krithis k
                JOIN composers c ON k.composer_id = c.id
                LEFT JOIN ragas r ON k.primary_raga_id = r.id
                LEFT JOIN talas t ON k.tala_id = t.id
                WHERE k.temple_id = %s AND c.name = %s
                ORDER BY k.title ASC
            """
            params: tuple[Any, ...] = (temple_id, composer)
        else:
            query_kritis = """
                SELECT k.title, r.name AS raga, t.name AS tala, k.vibhakti_case
                FROM krithis k
                LEFT JOIN ragas r ON k.primary_raga_id = r.id
                LEFT JOIN talas t ON k.tala_id = t.id
                WHERE k.temple_id = %s
                ORDER BY k.title ASC
            """
            params = (temple_id,)

        with conn.cursor(row_factory=dict_row) as cur:
            cur.execute(query_kritis, params)
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
            composer=composer,
        )
        docs.append(
            MacroDocument(
                kind="KSHETRA_OVERVIEW",
                anchor_id=temple_id,
                label=f"{name} ({city})",
                original_content=notes,
                overview_text=overview_text,
                content_hash=md5_hash(overview_text),
                detail=f"gap={is_gap}, {len(kritis)} kritis",
            )
        )
    return docs


def stale_reasons(conn: psycopg.Connection, doc: MacroDocument, profile_id: str) -> list[str]:
    """Why the stored document or its vector no longer matches `doc`; empty when fully up to date.

    Database reads only. The indexers skip a document exactly when this is empty, and `--check` reports
    these reasons, so the two cannot disagree.
    """
    anchor_column = "tag_id" if doc.kind == "CYCLE_OVERVIEW" else "temple_id"
    with conn.cursor() as cur:
        cur.execute(
            f"""
            SELECT sd.id, de.id, sd.content_hash, de.content_hash, md5(sd.indexed_content)
            FROM search_documents sd
            LEFT JOIN document_embeddings de ON de.document_id = sd.id AND de.profile_id = %s
            WHERE sd.{anchor_column} = %s AND sd.document_kind = %s
            ORDER BY sd.source_chunk_index
            LIMIT 1
            """,  # noqa: S608 - column name is chosen from two constants above
            (profile_id, doc.anchor_id, doc.kind),
        )
        row = cur.fetchone()
    if row is None:
        return ["no search document"]
    reasons: list[str] = []
    if row[1] is None:
        reasons.append("no embedding for the active profile")
    elif row[3] != doc.content_hash:
        reasons.append("embedding hash differs from the regenerated document")
    if row[2] != doc.content_hash:
        reasons.append("source data changed since the document was stored")
    if row[4] != row[2]:
        reasons.append("document hash differs from its indexed text")
    return reasons


def check_macro_freshness(
    conn: psycopg.Connection, profile_id: str
) -> tuple[int, list[tuple[MacroDocument, list[str]]]]:
    """Compares every regenerated macro document with what is stored. Reads and local hashing only.

    Returns the number of documents checked and the stale ones with their reasons.
    """
    cycle_docs, _ = build_cycle_documents(conn)
    docs = cycle_docs + build_kshetra_documents(conn)
    stale = [(d, r) for d in docs if (r := stale_reasons(conn, d, profile_id))]
    return len(docs), stale


def _upsert_and_embed(
    conn: psycopg.Connection, embedder: GeminiEmbedder | None, profile_id: str, doc: MacroDocument
) -> None:
    if embedder is None:
        raise RuntimeError("An embedder is required unless dry_run is set")
    anchor_column = "tag_id" if doc.kind == "CYCLE_OVERVIEW" else "temple_id"
    prefix = "Cycle" if doc.kind == "CYCLE_OVERVIEW" else "Kshetra"
    vector = embedder.embed_document(doc.overview_text, title=f"{prefix}: {doc.label}")
    with conn.cursor() as cur:
        cur.execute(
            f"""
            INSERT INTO search_documents (
                {anchor_column}, document_kind, original_content, indexed_content, content_hash
            ) VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (
                krithi_id, temple_id, tag_id, section_id,
                variant_id, document_kind, source_chunk_index
            )
            DO UPDATE SET original_content = EXCLUDED.original_content,
                          indexed_content = EXCLUDED.indexed_content,
                          content_hash = EXCLUDED.content_hash,
                          updated_at = clock_timestamp()
            RETURNING id
            """,  # noqa: S608 - column name is chosen from two constants above
            (doc.anchor_id, doc.kind, doc.original_content, doc.overview_text, doc.content_hash),
        )
        doc_row = cur.fetchone()
        if not doc_row:
            raise RuntimeError(f"Failed to upsert search document for {doc.kind} {doc.label}")
        cur.execute(
            """
            INSERT INTO document_embeddings (document_id, profile_id, embedding, content_hash)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (document_id, profile_id)
            DO UPDATE SET embedding = EXCLUDED.embedding, content_hash = EXCLUDED.content_hash
            """,
            (doc_row[0], profile_id, vector, doc.content_hash),
        )
    conn.commit()


def index_cycle_overviews(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder | None,
    profile_id: str,
    force: bool = False,
    dry_run: bool = False,
) -> dict[str, int]:
    """Generates and embeds CYCLE_OVERVIEW documents anchored to tags.id."""
    docs, abstained = build_cycle_documents(conn)
    stats = {"embedded": 0, "skipped": abstained}
    for doc in docs:
        if not force and not stale_reasons(conn, doc, profile_id):
            stats["skipped"] += 1
            continue
        if not dry_run:
            _upsert_and_embed(conn, embedder, profile_id, doc)
        stats["embedded"] += 1
        logger.info("Embedded CYCLE_OVERVIEW for %s (%s)", doc.label, doc.detail)
    return stats


def index_kshetra_overviews(
    conn: psycopg.Connection,
    embedder: GeminiEmbedder | None,
    profile_id: str,
    force: bool = False,
    dry_run: bool = False,
    composer: str | None = None,
) -> dict[str, int]:
    """Generates and embeds KSHETRA_OVERVIEW documents anchored to temples.id."""
    stats = {"embedded": 0, "skipped": 0}
    for doc in build_kshetra_documents(conn, composer):
        if not force and not stale_reasons(conn, doc, profile_id):
            stats["skipped"] += 1
            continue
        if not dry_run:
            _upsert_and_embed(conn, embedder, profile_id, doc)
        stats["embedded"] += 1
        logger.info("Embedded KSHETRA_OVERVIEW for %s [%s]", doc.label, doc.detail)
    return stats
