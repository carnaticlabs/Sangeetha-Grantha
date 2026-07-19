"""Integration-test substrate (TRACK-113 Phase 1).

A session-scoped PostgreSQL 18 testcontainer, migrated by the *same* Flyway
engine (CLI container) that drives dev/CI — per ADR-013 there is exactly one
migration engine, so these tests run against the real schema including the
`R__` reference seed data.

Escape hatch (integration-tests approach §2): set TEST_DATABASE_URL to target
an externally provided, already-migrated database (e.g. CI service container
or a developer's `make db`) and skip container provisioning entirely.
"""

from __future__ import annotations

import os
import shutil
import subprocess
from collections.abc import Iterator
from pathlib import Path
from typing import Any

import pytest

from src.config import ExtractorConfig
from src.db import ExtractionQueueDB

REPO_ROOT = Path(__file__).resolve().parents[4]
MIGRATIONS_DIR = REPO_ROOT / "database" / "migrations"
GOLDEN_FIXTURE = REPO_ROOT / "shared" / "domain" / "model" / "import" / "fixtures" / "canonical-extraction-golden.json"
CANONICAL_SCHEMA = REPO_ROOT / "shared" / "domain" / "model" / "import" / "canonical-extraction-schema.json"

# Match the versions pinned in compose.yaml / gradle/libs.versions.toml
POSTGRES_IMAGE = "postgres:18.3-alpine"
FLYWAY_IMAGE = "flyway/flyway:12.9.0-alpine"


def _docker_available() -> bool:
    if shutil.which("docker") is None:
        return False
    result = subprocess.run(["docker", "info"], capture_output=True, timeout=30)
    return result.returncode == 0


def _flyway_migrate(host_port: int, dbname: str, user: str, password: str) -> None:
    """Apply all V__ + R__ migrations via the Flyway CLI container (ADR-013)."""
    cmd = [
        "docker",
        "run",
        "--rm",
        # Reaches the testcontainer's host-mapped port from inside the Flyway
        # container: native on Docker Desktop, host-gateway alias on Linux CI.
        "--add-host=host.docker.internal:host-gateway",
        "-v",
        f"{MIGRATIONS_DIR}:/flyway/sql:ro",
        FLYWAY_IMAGE,
        f"-url=jdbc:postgresql://host.docker.internal:{host_port}/{dbname}",
        f"-user={user}",
        f"-password={password}",
        "-locations=filesystem:/flyway/sql",
        "-connectRetries=10",
        "migrate",
    ]
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=600)
    if result.returncode != 0:
        raise RuntimeError(f"Flyway migrate failed (exit {result.returncode}):\n{result.stdout}\n{result.stderr}")


@pytest.fixture(scope="session")
def database_url() -> Iterator[str]:
    external = os.environ.get("TEST_DATABASE_URL")
    if external:
        yield external
        return

    if not _docker_available():
        pytest.skip("Docker unavailable — integration tests need a container runtime")

    from testcontainers.postgres import PostgresContainer

    with PostgresContainer(
        POSTGRES_IMAGE,
        username="postgres",
        password="postgres",
        dbname="sangita_grantha_test",
        driver=None,
    ) as pg:
        host_port = int(pg.get_exposed_port(5432))
        _flyway_migrate(host_port, "sangita_grantha_test", "postgres", "postgres")
        yield pg.get_connection_url()


@pytest.fixture()
def queue_db(database_url: str, monkeypatch: pytest.MonkeyPatch) -> Iterator[ExtractionQueueDB]:
    """A connected ExtractionQueueDB against the migrated container, cleaned after each test."""
    monkeypatch.setenv("DATABASE_URL", database_url)
    db = ExtractionQueueDB(ExtractorConfig())
    yield db
    try:
        db.ensure_connected()
        with db.conn.cursor() as cur:
            # CASCADE: variant_match holds an FK into extraction_queue
            cur.execute("TRUNCATE extraction_queue CASCADE")
        db.conn.commit()
    finally:
        db.close()


def insert_pending_task(
    db: ExtractionQueueDB,
    *,
    source_url: str = "https://example.org/krithi/1",
    source_format: str = "HTML",
    max_attempts: int = 3,
    attempts: int = 0,
    status: str = "PENDING",
) -> str:
    """Insert a queue row the way the Kotlin backend does; returns the task id."""
    db.ensure_connected()
    with db.conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO extraction_queue
                (source_url, source_format, source_name, source_tier,
                 request_payload, status, attempts, max_attempts)
            VALUES (%(url)s, %(fmt)s, 'guruguha.org', 2,
                    '{"composerHint": "Dikshitar"}'::jsonb, %(status)s::extraction_status,
                    %(attempts)s, %(max_attempts)s)
            RETURNING id
            """,
            {
                "url": source_url,
                "fmt": source_format,
                "status": status,
                "attempts": attempts,
                "max_attempts": max_attempts,
            },
        )
        row = cur.fetchone()
        assert row is not None
        task_id = row["id"]
    db.conn.commit()
    return task_id


def fetch_task_row(db: ExtractionQueueDB, task_id: str) -> dict[str, Any]:
    db.ensure_connected()
    with db.conn.cursor() as cur:
        cur.execute("SELECT * FROM extraction_queue WHERE id = %(id)s", {"id": task_id})
        row = cur.fetchone()
    db.conn.rollback()
    assert row is not None
    return row
