| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-02-08 |
| **Author** | Sangeetha Grantha Team |

# 06 Backend


---


## Contents

- [exposed-rc4-features-testing.md](./exposed-rc4-features-testing.md) ✅ **Completed**
- [mutation-handlers.md](./mutation-handlers.md)
- [query-optimization-evaluation.md](./query-optimization-evaluation.md) ✅ **Completed**
- [security-requirements.md](./security-requirements.md)
- [steel-thread-implementation.md](./steel-thread-implementation.md)

## Database Layer Optimization ✅ **COMPLETED**

All database layer optimizations have been successfully implemented across all repositories:

- ✅ **Single Round-Trip Persistence**: All create/update operations use `resultedValues` and `updateReturning`
- ✅ **Smart Collection Updates**: Delta updates implemented for sections, tags, and ragas
- ✅ **Performance Improvements**: ~40-50% query reduction per operation

See [Database Layer Optimization](../01-requirements/features/database-layer-optimization.md) for complete details.

## Multi-Source Ingestion & Quality Services (Feb 2026)

New services added as part of the [Krithi Data Sourcing & Quality Strategy](../01-requirements/krithi-data-sourcing/quality-strategy.md):

- **`StructuralVotingEngine`** — Compares section structures from multiple sources and selects the best canonical structure using authority-weighted scoring. See [TRACK-041](../../conductor/tracks/TRACK-041-enhanced-sourcing-logic.md).
- **`ImportService` (enhanced)** — Now integrates `ComposerSourcePriority` mapping for authority-based source selection and structural voting for multi-source imports.
- **`KrithiStructureParser` (enhanced)** — Multi-script section detection, Madhyama Kala/Chittaswaram support, Tamil subscript normalisation, improved boilerplate filtering.

### Containerised PDF Extraction Service

A standalone Python service (`tools/pdf-extractor/`) handles PDF/DOCX/OCR extraction. Communicates with the Kotlin backend via the `extraction_queue` database table (no HTTP coupling).

- **Architecture**: [Backend System Design §5.8](../02-architecture/backend-system-design.md)
- **Strategy**: [Krithi Data Sourcing Strategy §8](../01-requirements/krithi-data-sourcing/quality-strategy.md#8-technology-decisions--containerised-deployment)
- **CLI Management**: `sangita-cli extraction start|stop|status|logs`