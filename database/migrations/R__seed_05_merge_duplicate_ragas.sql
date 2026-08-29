-- Repeatable: re-apply TRACK-132 raga merges after R__seed_04.
--
-- Versioned V50 runs before repeatable seeds, so on a fresh `make db-reset`
-- Wikipedia keepers do not exist yet and V50 no-ops most pairs. This repeatable
-- runs after R__seed_04 and folds V40 / import twins into the keepers.
-- Idempotent: track132_merge_raga is a no-op when either side is missing.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md

SELECT track132_apply_raga_merges();
