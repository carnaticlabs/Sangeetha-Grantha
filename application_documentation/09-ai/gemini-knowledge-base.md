| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Project Knowledge Base

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.2.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


TODO: Update this document for Sangita Grantha.

## 1. Project Overview

TODO: Update this section.

## 2. Tech Stack & Architecture

TODO: Update this section.

### Backend (JVM)

TODO: Update this section.

### Data Layer

TODO: Update this section.

### Frontend (Admin Web)

TODO: Update this section.

### Mobile (Participant)

TODO: Update this section.

## 3. Core Modules

TODO: Update this section.

## 4. Key Workflows

TODO: Update this section.

### Authentication

TODO: Update this section.

### Steel Thread (End-to-End)

TODO: Update this section.

## 5. Coding Standards

TODO: Update this section.

### Backend

TODO: Update this section.

### Database

TODO: Update this section.

## 6. Operations

TODO: Update this section.

---

## Conductor Tracking for AI Knowledge Work

This knowledge base underpins AI/Gemini usage across the project. Any change that:

- Introduces or modifies **AI/Gemini services**, **prompts**, or **pipelines** (for example, `TransliterationService`, `WebScrapingService`, `MetadataExtractionService`, `SectionDetectionService`, `ValidationService`), or
- Adjusts **AI-related configuration**, **governance policies**, or other behaviour described in `application_documentation/09-ai/`

must be tracked via **Conductor**:

- **Create/Update a Track**
  - Add or update an entry in `conductor/tracks.md` with a unique `TRACK-XXX` ID, name, and status.
  - For substantial changes, create or update `conductor/tracks/TRACK-XXX-*.md` (see `conductor/tracks/TRACK-001-bulk-import-krithis.md` for the canonical template).
- **Reflect the Track in Documentation**
  - When updating this knowledge base with new AI patterns, models, or constraints, reference the relevant `TRACK-XXX` in the document (for example, in a \"Related Tracks\" or \"Change Log\" subsection).
  - Ensure that the corresponding TRACK file’s **Progress Log** is updated whenever a documented AI capability moves from idea → implemented → refined.

See also:

- `conductor/index.md`
- `conductor/tracks.md`
- `application_documentation/09-ai/integration-summary.md` (for the AI roadmap and tracked phases)
