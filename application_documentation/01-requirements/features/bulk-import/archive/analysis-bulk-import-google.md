| Metadata | Value |
|:---|:---|
| **Status** | Archived |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-03 |
| **Author** | Sangeetha Grantha Team |

# Krithi Bulk Import: Detailed Strategy & Design


---



---

## 1. Executive Summary

We have a set of CSV files (`Dikshitar`, `Syama Sastri`, `Thyagaraja`) containing metadata for Krithis (Title, Raga, Source URL). The goal is to ingest this data into the Sangeetha Grantha platform to populate the system of record.

This is not just a database seed operation but the entry point for a comprehensive **Import Pipeline**. The CSVs act as the "Manifest" for ingestion. The pipeline must:
1.  **Ingest Manifest:** Load CSV data into a staging area (`imported_krithis`).
2.  **Enrich:** Visit the source URLs to scrape full lyrics and metadata (using the defined `WebScrapingService`).
3.  **Resolve & Normalize:** Map raw text (e.g., "Kalyani") to canonical entities (`Raga: Kalyani`, `Composer: Dikshitar`).
4.  **Review:** Present data for admin approval before finalizing into the `krithis` table.

## 2. Source Data Analysis

We have three primary CSV files in `database/for_import/`:

1.  **Dikshitar-Krithi-For-Import.csv**
    *   *Columns*: Krithi, Raga, Hyperlink
    *   *Source*: Guru Guha Vaibhavam (blogspot)
2.  **Syama-Sastri-Krithi-For-Import.csv**
    *   *Columns*: Krithi, Raga, Hyperlink
    *   *Source*: Syama Krishna Vaibhavam (blogspot)
3.  **Thyagaraja-Krithi-For-Import.csv**
    *   *Columns*: Krithi, Raga, Hyperlink
    *   *Source*: Thyagaraja Vaibhavam (blogspot)

**Key Observation:** All sources are Blogspot-based archives with similar structures. This simplifies the scraping logic (Phase 2) but requires robust handling of the initial CSV parsing (Phase 1).

## 3. Implementation Strategy

We will adopt a **Staged Ingestion** approach using the `ImportPipelineService` pattern defined in the architecture docs.

### Phase 1: Manifest Ingestion (Current Scope)
*Goal: Get CSV rows into `imported_krithis` table as "Pending" records.*

1.  **Source Registration:** Ensure `import_sources` table has entries for the three blogs.
2.  **Parsing:** Create a tool/script to parse the CSVs.
3.  **Loading:** Insert data into `imported_krithis`:
    *   `source_key` = Hyperlink (Unique ID)
    *   `raw_title` = Krithi Name
    *   `raw_raga` = Raga Name
    *   `import_status` = 'pending'
    *   `parsed_payload` = `{ "original_csv_row": ... }`

### Phase 2: Enrichment (Future Scope)
*Goal: Turn "Pending" records into fully populated drafts.*

1.  **Scraping:** The system picks up 'pending' records.
2.  **Fetching:** Visits `source_key` (URL).
3.  **Parsing:** Extracts lyrics/metadata using `WebScrapingService`.
4.  **Update:** Updates `imported_krithis` with `raw_lyrics`, `raw_tala`, etc.

## 4. Detailed Design: Phase 1 (Manifest Ingestion)

We will use a **Python script** (`tools/scripts/ingest_csv_manifest.py`) to generate SQL seed files. This approach is preferred over runtime parsing for the initial seed because it's reproducible, version-controllable, and uses our existing `sangita-cli` seed mechanism.

### 4.1. Data Mapping

| CSV Column | Database Column (`imported_krithis`) | Logic |
|:---|:---|:---|
| `Hyperlink` | `source_key` | Primary Identifier. Must be unique per source. |
| `Krithi` | `raw_title` | Direct map. |
| `Raga` | `raw_raga` | Direct map. |
| *Derived* | `import_source_id` | Lookup based on filename (e.g., Dikshitar -> Guru Guha ID). |
| *Derived* | `raw_composer` | Constant based on file (e.g., "Muthuswami Dikshitar"). |

### 4.2. Source Registry (`import_sources`)

We need to define stable UUIDs for these sources to ensure idempotency.

```sql
-- database/seed_data/03_import_sources.sql
INSERT INTO import_sources (id, name, base_url, description) VALUES
('...uuid-1...', 'Guru Guha Vaibhavam', 'http://guru-guha.blogspot.com', 'Archive of Dikshitar Krithis'),
('...uuid-2...', 'Syama Krishna Vaibhavam', 'http://syamakrishnavaibhavam.blogspot.com', 'Archive of Syama Sastri Krithis'),
('...uuid-3...', 'Thyagaraja Vaibhavam', 'http://thyagaraja-vaibhavam.blogspot.com', 'Archive of Thyagaraja Krithis')
ON CONFLICT (id) DO NOTHING;
```

### 4.3. Script Logic

The script will:
1.  Read each CSV.
2.  Clean data (strip whitespace, handle encoding).
3.  Escape SQL strings (critical for titles with quotes).
4.  Generate `04_initial_manifest_load.sql` containing `INSERT` statements.

## 5. Execution Plan

1.  **Preparation**:
    *   Verify `import_sources` table exists (Migration `04__import-pipeline.sql` is already applied?).
    *   Generate UUIDs for the 3 sources.
2.  **Development**:
    *   Create `tools/scripts/ingest_csv.py`.
    *   Run script to generate SQL files in `database/seed_data/`.
3.  **Verification**:
    *   Run `tools/sangita-cli -- db seed`.
    *   Check `imported_krithis` count.
    *   Verify a sample record (e.g., "Vatapi Ganapatim") matches CSV.

## 6. Future Considerations (Phase 2 Preview)

Once data is in `imported_krithis`, the **Conductor** track will move to "Enrichment". We will need to implement the `KarnatikSourceHandler` or `BlogspotSourceHandler` (as hinted in `import-pipeline-technical-implementation-guide-goose.md`) to parse the specific HTML structure of these blogspots.

---

**Next Immediate Step:** Confirm migration status and generate the SQL seed files.
