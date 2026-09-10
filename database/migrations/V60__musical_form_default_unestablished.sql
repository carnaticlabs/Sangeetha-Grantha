-- Separate from enum addition so the value is committed before use.
-- No inference or backfill of historical classifications is authorized.
ALTER TABLE krithis ALTER COLUMN musical_form SET DEFAULT 'UNESTABLISHED'::musical_form_enum;
