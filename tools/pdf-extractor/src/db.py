"""Database operations for the extraction_queue table.

Provides claim/update/query operations that the worker loop uses to:
1. Claim PENDING tasks (SELECT ... FOR UPDATE SKIP LOCKED)
2. Mark tasks as PROCESSING, DONE, or FAILED
3. Query queue statistics for health monitoring
"""

from __future__ import annotations

import json
import logging
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Optional
from uuid import UUID

import psycopg
from psycopg.rows import dict_row

from .config import ExtractorConfig

logger = logging.getLogger(__name__)


@dataclass
class ExtractionTask:
    """A task claimed from the extraction_queue."""

    id: UUID
    source_url: str
    source_format: str
    source_name: Optional[str]
    source_tier: Optional[int]
    request_payload: dict[str, Any]
    page_range: Optional[str]
    import_batch_id: Optional[UUID]
    import_task_run_id: Optional[UUID]
    attempts: int


@dataclass
class QueueStats:
    """Queue depth statistics."""

    pending: int = 0
    processing: int = 0
    done: int = 0
    failed: int = 0
    cancelled: int = 0


class ExtractionQueueDB:
    """Database operations for the extraction_queue table."""

    def __init__(self, config: ExtractorConfig) -> None:
        self._config = config
        self._conn: Optional[psycopg.Connection] = None

    def connect(self) -> None:
        """Establish database connection."""
        self._conn = psycopg.connect(
            self._config.database_url,
            row_factory=dict_row,
            autocommit=False,
        )
        logger.info("Connected to database")

    def close(self) -> None:
        """Close database connection."""
        if self._conn:
            self._conn.close()
            logger.info("Database connection closed")

    @property
    def conn(self) -> psycopg.Connection:
        if self._conn is None or self._conn.closed:
            self.connect()
        assert self._conn is not None
        return self._conn

    def claim_pending_task(self) -> Optional[ExtractionTask]:
        """Claim one PENDING task using SELECT ... FOR UPDATE SKIP LOCKED.

        Returns the claimed task or None if the queue is empty.
        The task is atomically transitioned to PROCESSING status.
        """
        with self.conn.cursor() as cur:
            cur.execute(
                """
                SELECT id, source_url, source_format, source_name, source_tier,
                       request_payload, page_range, import_batch_id,
                       import_task_run_id, attempts
                FROM extraction_queue
                WHERE status = 'PENDING' AND attempts < max_attempts
                ORDER BY created_at
                FOR UPDATE SKIP LOCKED
                LIMIT 1
                """,
            )
            row = cur.fetchone()
            if row is None:
                self.conn.rollback()
                return None

            task_id = row["id"]

            # Atomically transition to PROCESSING
            cur.execute(
                """
                UPDATE extraction_queue
                SET status = 'PROCESSING',
                    claimed_at = NOW(),
                    claimed_by = %(hostname)s,
                    attempts = attempts + 1,
                    updated_at = NOW()
                WHERE id = %(id)s
                """,
                {"id": task_id, "hostname": self._config.hostname},
            )
            self.conn.commit()

            return ExtractionTask(
                id=row["id"],
                source_url=row["source_url"],
                source_format=row["source_format"],
                source_name=row["source_name"],
                source_tier=row["source_tier"],
                request_payload=row["request_payload"] if isinstance(row["request_payload"], dict) else json.loads(row["request_payload"]),
                page_range=row["page_range"],
                import_batch_id=row["import_batch_id"],
                import_task_run_id=row["import_task_run_id"],
                attempts=row["attempts"],
            )

    def mark_done(
        self,
        task_id: UUID,
        result_payload: list[dict[str, Any]],
        extraction_method: str,
        confidence: float,
        duration_ms: int,
        source_checksum: Optional[str] = None,
    ) -> None:
        """Mark a task as successfully completed with results."""
        with self.conn.cursor() as cur:
            cur.execute(
                """
                UPDATE extraction_queue
                SET status = 'DONE',
                    result_payload = %(result)s::jsonb,
                    result_count = %(count)s,
                    extraction_method = %(method)s,
                    extractor_version = %(version)s,
                    confidence = %(confidence)s,
                    duration_ms = %(duration)s,
                    source_checksum = %(checksum)s,
                    updated_at = NOW()
                WHERE id = %(id)s
                """,
                {
                    "id": task_id,
                    "result": json.dumps(result_payload),
                    "count": len(result_payload),
                    "method": extraction_method,
                    "version": self._config.extractor_version,
                    "confidence": confidence,
                    "duration": duration_ms,
                    "checksum": source_checksum,
                },
            )
            self.conn.commit()
        logger.info(
            "Task completed",
            extra={"task_id": str(task_id), "result_count": len(result_payload), "duration_ms": duration_ms},
        )

    def mark_failed(self, task_id: UUID, error_detail: dict[str, Any]) -> None:
        """Mark a task as failed with error details."""
        with self.conn.cursor() as cur:
            cur.execute(
                """
                UPDATE extraction_queue
                SET status = 'FAILED',
                    error_detail = %(error)s::jsonb,
                    last_error_at = NOW(),
                    updated_at = NOW()
                WHERE id = %(id)s
                """,
                {
                    "id": task_id,
                    "error": json.dumps(error_detail),
                },
            )
            self.conn.commit()
        logger.warning(
            "Task failed",
            extra={"task_id": str(task_id), "error": error_detail.get("message", "unknown")},
        )

    def get_queue_stats(self) -> QueueStats:
        """Get current queue depth by status."""
        with self.conn.cursor() as cur:
            cur.execute(
                """
                SELECT status::text, COUNT(*) as cnt
                FROM extraction_queue
                GROUP BY status
                """
            )
            stats = QueueStats()
            for row in cur.fetchall():
                status = row["status"].lower()
                setattr(stats, status, row["cnt"])
            return stats

    def health_check(self) -> bool:
        """Verify database connectivity."""
        try:
            with self.conn.cursor() as cur:
                cur.execute("SELECT 1")
                return True
        except Exception:
            return False
