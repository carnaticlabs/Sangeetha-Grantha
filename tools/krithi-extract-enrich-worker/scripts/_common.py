"""Shared helpers for the worker's operational scripts.

These scripts are run by hand from the worker root, not shipped in the package:

    cd tools/krithi-extract-enrich-worker
    PYTHONPATH=. uv run python scripts/<script>.py [args]

`PYTHONPATH=.` is what puts both `src` and `scripts` on the import path, which
is why none of these scripts need a `sys.path` hack (TRACK-127).
"""

from __future__ import annotations

import logging
import os

# The dev-stack DSN from compose.yaml. Every script resolves through here so a
# stray per-script default cannot point an operator at the wrong database
# (TRACK-127 removed one such default from repair_sections.py).
_DEFAULT_DATABASE_URL = "postgresql://postgres:postgres@localhost:5432/sangita_grantha"


def database_url() -> str:
    """Return the DSN from DATABASE_URL, falling back to the dev-stack default."""
    return os.environ.get("DATABASE_URL", _DEFAULT_DATABASE_URL)


def setup_logging(level: int = logging.INFO) -> None:
    """Configure root logging for a script run."""
    logging.basicConfig(level=level, format="%(levelname)s: %(message)s")
