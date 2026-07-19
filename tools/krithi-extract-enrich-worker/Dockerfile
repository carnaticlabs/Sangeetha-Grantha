# Sangita Grantha PDF Extraction Service
# Multi-format document extraction: PDF (PyMuPDF), OCR (Tesseract), DOCX
# Runs as a Docker container alongside PostgreSQL, polling extraction_queue

FROM python:3.14-slim

LABEL maintainer="Sangita Grantha <sangita-grantha@example.com>"
LABEL description="Multi-format document extraction service for Carnatic music compositions"

# Install system dependencies for OCR and Indic text processing
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Tesseract OCR engine
    tesseract-ocr \
    # Indic language packs for OCR
    tesseract-ocr-san \
    tesseract-ocr-tam \
    tesseract-ocr-tel \
    tesseract-ocr-kan \
    tesseract-ocr-mal \
    # PostgreSQL client library (required by psycopg)
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Install uv — pinned, because an unpinned `:latest` would undermine the
# reproducible-build goal this stage exists to serve (TRACK-131).
COPY --from=ghcr.io/astral-sh/uv:0.11.25 /uv /uvx /bin/

WORKDIR /app

# Install Python dependencies first (layer caching)
COPY pyproject.toml uv.lock ./
RUN uv sync --frozen --no-dev

# Copy application source
COPY src/ ./src/

# Activate virtualenv for the entrypoint
ENV PATH="/app/.venv/bin:$PATH"

# Create cache directory for downloaded PDFs
RUN mkdir -p /app/cache

# Non-root user for security
RUN useradd --create-home --shell /bin/bash extractor
RUN chown -R extractor:extractor /app
USER extractor

# Health check: verify Python can import core modules
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD python -c "from src.worker import health_check; health_check()"

# Production entry point: DB-queue polling worker
ENTRYPOINT ["python", "-m", "src.worker"]
