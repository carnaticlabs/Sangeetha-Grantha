# Sangita Grantha PDF Extraction Service
# Multi-format document extraction: PDF (PyMuPDF), OCR (Tesseract), DOCX
# Runs as a Docker container alongside PostgreSQL, polling extraction_queue

FROM python:3.11-slim

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

WORKDIR /app

# Install Python dependencies first (layer caching)
COPY pyproject.toml .
RUN pip install --no-cache-dir -e .

# Copy application source
COPY src/ ./src/

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
