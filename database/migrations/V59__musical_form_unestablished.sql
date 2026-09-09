-- TRACK-140: unknown classification is explicit; existing rows are untouched.
ALTER TYPE musical_form_enum ADD VALUE IF NOT EXISTS 'UNESTABLISHED';
