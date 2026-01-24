| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-01-24 |
| **Author** | Sangeetha Grantha Team |

# Python vs Existing Technical Capabilities - Analysis

| Metadata | Value |
|:---|:---|
| **Status** | Draft |
| **Version** | 0.1.0 |
| **Last Updated** | 2026-01-20 |
| **Author** | System |

---


| Date | 2026-01-20 |
|:---|:---|
| **Status** | Technical Analysis |
| **Question** | Why was Python suggested over existing project capabilities? |

---

## Executive Summary

The original consolidation summary suggested using **Python** for CSV parsing in Phase 1. This analysis compares Python against the project's existing technical stack and recommends **Rust (extending `sangita-cli`)** as the optimal choice, with Kotlin as a viable alternative.

**Recommendation**: Use **Rust** to extend the existing `sangita-cli` tool rather than introducing Python as a new dependency.

---

## Why Python Was Initially Suggested

The Python suggestion likely came from:

1. **Quick Scripting**: Python's `csv` module makes CSV parsing trivial
2. **One-Time Task**: Phase 1 is a one-time data transformation (CSV → SQL seed files)
3. **Familiarity**: Python is commonly used for data transformation scripts
4. **Low Friction**: No need to add dependencies to existing build systems

However, this approach introduces a **new language dependency** that doesn't align with the project's existing architecture.

---

## Project's Existing Technical Stack

### Current Technologies

| Component | Technology | Purpose |
|:---|:---|:---|
| **Backend API** | Kotlin (JVM) + Ktor | REST API, business logic |
| **CLI Tool** | Rust (`tools/sangita-cli`) | Database management, migrations, seeding |
| **Database** | PostgreSQL | Data persistence |
| **Frontend** | React + TypeScript | Admin UI |
| **Build System** | Gradle (Kotlin) | Backend dependencies |

### Existing CLI Capabilities (`sangita-cli`)

The Rust CLI tool (`tools/sangita-cli`) already handles:

- ✅ Database migrations (`cargo run -- db migrate`)
- ✅ Database seeding (`cargo run -- db seed`)
- ✅ Database reset (`cargo run -- db reset`)
- ✅ Development server management (`cargo run -- dev`)
- ✅ Testing workflows (`cargo run -- test`)

**Key Insight**: The CLI is the **canonical tool** for database operations. Extending it for CSV ingestion aligns with existing patterns.

---

## Technology Comparison

### Option 1: Python Script ❌ (Originally Suggested)

**Pros:**
- ✅ Simple CSV parsing (`csv` module is built-in)
- ✅ Quick to write for one-time tasks
- ✅ Good for data transformation scripts
- ✅ No build system integration needed

**Cons:**
- ❌ **New language dependency** (project uses Kotlin + Rust)
- ❌ **No integration** with existing tooling
- ❌ Requires Python runtime installation
- ❌ Doesn't leverage existing `sangita-cli` infrastructure
- ❌ Inconsistent with project architecture
- ❌ Harder to maintain long-term (separate toolchain)

**Example Code:**
```python
# tools/scripts/ingest_csv_manifest.py
import csv
import uuid

def generate_sql(csv_file, source_id):
    with open(csv_file, 'r') as f:
        reader = csv.DictReader(f)
        for row in reader:
            # Generate INSERT statements
            ...
```

---

### Option 2: Rust (Extending `sangita-cli`) ✅ **RECOMMENDED**

**Pros:**
- ✅ **Aligns with existing architecture** (CLI tool is Rust)
- ✅ **Reuses existing infrastructure** (database connection, config loading)
- ✅ **Integrated with existing workflows** (`cargo run -- db seed`)
- ✅ **No new dependencies** (Rust already in project)
- ✅ **Type safety** and performance benefits
- ✅ **Consistent tooling** (all DB operations via CLI)
- ✅ **Maintainable** (single codebase for DB operations)

**Cons:**
- ⚠️ Requires adding CSV parsing crate (`csv` crate is well-maintained)
- ⚠️ Slightly more verbose than Python for simple scripts

**Example Code:**
```rust
// tools/sangita-cli/src/commands/import.rs
use csv::Reader;
use sqlx::PgPool;

pub async fn ingest_csv_manifest(
    csv_path: &Path,
    source_id: Uuid,
    pool: &PgPool
) -> Result<()> {
    let mut reader = Reader::from_path(csv_path)?;
    for result in reader.records() {
        let record = result?;
        // Generate and execute INSERT statements
        ...
    }
    Ok(())
}
```

**Integration:**
```rust
// tools/sangita-cli/src/commands/db.rs
#[derive(Subcommand)]
enum DbCommands {
    // ... existing commands
    /// Ingest CSV manifest files
    IngestCsv {
        #[arg(long)]
        csv_dir: PathBuf,
    },
}
```

---

### Option 3: Kotlin (Backend Service) ⚠️ Alternative

**Pros:**
- ✅ **Backend language** (already in use)
- ✅ **Integrated with existing services** (`ImportService`, `ImportRepository`)
- ✅ **Type safety** and null safety
- ✅ **Can be exposed as API endpoint** for future runtime imports

**Cons:**
- ❌ Requires adding CSV library to Gradle (`com.github.doyuchen:kotlin-csv`)
- ❌ **Overkill for one-time seed generation** (better for runtime imports)
- ❌ **Separate execution context** (not integrated with CLI seeding workflow)
- ❌ Would need separate script runner or API endpoint

**Example Code:**
```kotlin
// modules/backend/api/src/main/kotlin/com/sangita/grantha/backend/api/scripts/CsvIngestionScript.kt
import com.github.doyuchen.kotlin.csv.CsvReader

suspend fun ingestCsvManifest(csvPath: String, sourceId: Uuid): List<ImportedKrithiDto> {
    val reader = CsvReader(csvPath)
    val entries = reader.readAll()
    // Use existing ImportService
    return importService.submitImports(entries.map { ... })
}
```

**Best Use Case**: Phase 2+ (runtime CSV imports via API), not Phase 1 (seed file generation).

---

### Option 4: SQL COPY Command ⚠️ Limited

**Pros:**
- ✅ **Direct database operation** (no intermediate code)
- ✅ **Fast bulk loading** (PostgreSQL native)
- ✅ **No code dependencies**

**Cons:**
- ❌ **Limited data transformation** (can't easily map CSV columns to DB schema)
- ❌ **No validation logic** (SQL escaping, URL validation)
- ❌ **Requires staging table** or complex SQL transformations
- ❌ **Less maintainable** (SQL for data transformation is verbose)

**Example:**
```sql
-- Limited: Requires pre-processing or staging table
COPY imported_krithis (source_key, raw_title, raw_raga)
FROM '/path/to/csv'
WITH (FORMAT csv, HEADER true);
```

**Verdict**: Not suitable for this use case (needs data transformation and validation).

---

## Detailed Comparison Matrix

| Criteria | Python | Rust (CLI) | Kotlin (Backend) | SQL COPY |
|:---|:---:|:---:|:---:|:---:|
| **Architecture Alignment** | ❌ | ✅ | ⚠️ | ⚠️ |
| **Existing Infrastructure** | ❌ | ✅ | ✅ | ✅ |
| **Integration with CLI** | ❌ | ✅ | ❌ | ⚠️ |
| **Type Safety** | ❌ | ✅ | ✅ | N/A |
| **Maintainability** | ⚠️ | ✅ | ✅ | ❌ |
| **Development Speed** | ✅ | ⚠️ | ⚠️ | ❌ |
| **Data Transformation** | ✅ | ✅ | ✅ | ❌ |
| **Validation Logic** | ✅ | ✅ | ✅ | ❌ |
| **Future Extensibility** | ⚠️ | ✅ | ✅ | ❌ |
| **Dependency Management** | ❌ | ✅ | ⚠️ | ✅ |

---

## Recommendation: Use Rust (Extending `sangita-cli`)

### Rationale

1. **Architectural Consistency**: The CLI tool is already the canonical interface for database operations. CSV ingestion is a database operation.

2. **Infrastructure Reuse**: The CLI already has:
   - Database connection management (`DatabaseManager`)
   - Configuration loading (`AppConfig`)
   - Migration/seed execution workflows
   - Error handling patterns

3. **Workflow Integration**: CSV ingestion fits naturally into the existing `db seed` workflow:
   ```bash
   cargo run -- db seed  # Runs all seed files including CSV-generated ones
   ```

4. **Future-Proof**: If CSV ingestion needs evolve (validation, transformation, API endpoints), the Rust CLI can be extended. Python would remain a separate tool.

5. **No New Dependencies**: Rust is already in the project. Adding `csv = "1.3"` to `Cargo.toml` is trivial.

### Implementation Approach

**Step 1**: Add CSV parsing dependency
```toml
# tools/sangita-cli/Cargo.toml
[dependencies]
csv = "1.3"  # Add this
```

**Step 2**: Create new CLI command
```rust
// tools/sangita-cli/src/commands/import.rs
pub struct ImportArgs {
    #[command(subcommand)]
    command: ImportCommands,
}

#[derive(Subcommand)]
enum ImportCommands {
    /// Generate SQL seed files from CSV manifests
    GenerateSeed {
        #[arg(long)]
        csv_dir: PathBuf,
        #[arg(long)]
        output: PathBuf,
    },
}
```

**Step 3**: Integrate with existing `db seed` workflow
- Generate SQL files in `database/seed_data/`
- Existing `cargo run -- db seed` will pick them up automatically

**Step 4**: Add to CLI main
```rust
// tools/sangita-cli/src/main.rs
#[derive(Subcommand)]
enum Commands {
    // ... existing commands
    Import(import::ImportArgs),  // Add this
}
```

---

## Alternative: Kotlin for Runtime Imports (Phase 2+)

While Rust is recommended for **Phase 1** (seed file generation), **Kotlin** is the better choice for **Phase 2+** when CSV imports become runtime operations:

- **API Endpoints**: `POST /v1/admin/imports/csv/upload`
- **Service Integration**: Leverage existing `ImportService` and `WebScrapingService`
- **Business Logic**: Entity resolution, validation, de-duplication

**Recommendation**: Use Rust for Phase 1 (seed generation), Kotlin for Phase 2+ (runtime imports).

---

## Migration Path from Python Suggestion

If Python was already partially implemented, migration to Rust is straightforward:

1. **CSV Parsing Logic**: Port CSV reading logic (similar patterns)
2. **SQL Generation**: Port string formatting for SQL INSERT statements
3. **File I/O**: Rust's `std::fs` and `std::path` are similar to Python's

**Estimated Effort**: 2-4 hours to port a Python script to Rust CLI command.

---

## Conclusion

**Primary Recommendation**: **Rust (extending `sangita-cli`)** for Phase 1 CSV ingestion.

**Rationale**:
- ✅ Aligns with existing architecture
- ✅ Reuses existing infrastructure
- ✅ Integrates with existing workflows
- ✅ No new language dependency
- ✅ Maintainable and extensible

**Secondary Recommendation**: **Kotlin** for Phase 2+ runtime CSV imports via API.

**Not Recommended**: Python (introduces unnecessary dependency) or SQL COPY (insufficient for transformation needs).

---

## Updated Implementation Plan

### Phase 1: CSV Manifest Ingestion (Updated)

**Deliverables**:
1. ✅ **Rust CLI command**: `tools/sangita-cli/src/commands/import.rs`
   - Add `csv = "1.3"` dependency
   - Implement CSV parsing and SQL generation
   - Generate `database/seed_data/04_initial_manifest_load.sql`

2. ✅ **Source registry**: `database/seed_data/03_import_sources.sql`
   - Register 3 blogspot sources with stable UUIDs

3. ✅ **Integration**: Extend `DbCommands` to include CSV ingestion
   ```bash
   cargo run -- import generate-seed \
     --csv-dir database/for_import \
     --output database/seed_data/04_initial_manifest_load.sql
   ```

4. ✅ **Verification**: 
   ```bash
   cargo run -- db seed  # Runs all seed files including CSV-generated ones
   ```

**Success Criteria**:
- ✅ All 3 CSV files parse successfully
- ✅ ~1,240 entries loaded into `imported_krithis` table
- ✅ Source attribution correctly mapped
- ✅ No new language dependencies introduced
- ✅ Integrated with existing CLI workflow

---

## References

- [CSV Import Strategy](./01-strategy/csv-import-strategy.md)
- [Rust CLI Tool](../../../../tools/sangita-cli/)
- [Database Seeding Workflow](../../../../tools/sangita-cli/src/commands/db.rs)
- [Project Architecture Rules](../../../../.cursorrules)
