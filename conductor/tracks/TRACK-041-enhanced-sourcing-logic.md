| Metadata | Value |
|:---|:---|
| **Status** | Active |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-02-07 |
| **Author** | Sangita Grantha Architect |

# TRACK-041: Enhanced Sourcing Logic & Structural Voting

## 1. Objective
Harden the Krithi import pipeline by implementing multi-source structural voting and authoritative source prioritization.

## 2. Context
- **Goal**: Ensure the "System of Record" always uses the most musicologically accurate structure.
- **Reference**: [Proposed Sourcing Hierarchy](../application_documentation/07-quality/results/krithi-structural-audit-2026-02.md#5-proposed-sourcing-hierarchy--logic)

## 3. Implementation Plan
- [ ] Update `ImportService` with `ComposerSourcePriority` map.
- [ ] Implement `StructuralVotingEngine` to compare multi-source extractions.
- [ ] Enhance `KrithiStructureParser` for technical headers (Madhyama Kala, Chittaswaram).
- [ ] Add support for script-specific subscript normalization (Tamil subscripts 1-4).
- [ ] Integrate "Authority Source" validation into the `ImportReview` UI.

## 4. Progress Log
- **2026-02-07**: Track created to operationalize sourcing findings from TRACK-039.
