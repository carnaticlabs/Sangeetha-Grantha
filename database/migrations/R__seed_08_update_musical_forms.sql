-- TRACK-146: Retire automatic surname/title musical-form corrections.
-- Existing stored forms are preserved. Reviewed composition-content corrections
-- must use the audited canon/import path, retaining source and revision history.
-- This historical repeatable remains as a no-op so Flyway can record retirement.
-- Ref: application_documentation/01-requirements/features/musicological-classification-contract.md
SELECT 1;
