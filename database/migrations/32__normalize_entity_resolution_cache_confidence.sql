-- Normalize entity_resolution_cache confidence to integer (0..100).
-- Some environments still carry DECIMAL(3,2) from older migrations.

SET search_path TO public;

DO $$
DECLARE
    has_table BOOLEAN;
    column_type TEXT;
    max_confidence NUMERIC;
    has_confidence_check BOOLEAN;
BEGIN
    SELECT to_regclass('public.entity_resolution_cache') IS NOT NULL
    INTO has_table;

    IF NOT has_table THEN
        RETURN;
    END IF;

    SELECT data_type
    INTO column_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'entity_resolution_cache'
      AND column_name = 'confidence';

    IF column_type = 'numeric' THEN
        EXECUTE 'SELECT COALESCE(MAX(confidence), 0) FROM public.entity_resolution_cache'
        INTO max_confidence;

        IF max_confidence <= 1 THEN
            EXECUTE '
                ALTER TABLE public.entity_resolution_cache
                ALTER COLUMN confidence TYPE INTEGER
                USING ROUND(confidence * 100)::INTEGER
            ';
        ELSE
            EXECUTE '
                ALTER TABLE public.entity_resolution_cache
                ALTER COLUMN confidence TYPE INTEGER
                USING ROUND(confidence)::INTEGER
            ';
        END IF;
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'entity_resolution_cache_confidence_check'
          AND conrelid = 'public.entity_resolution_cache'::regclass
    )
    INTO has_confidence_check;

    IF NOT has_confidence_check THEN
        ALTER TABLE public.entity_resolution_cache
            ADD CONSTRAINT entity_resolution_cache_confidence_check
            CHECK (confidence >= 0 AND confidence <= 100);
    END IF;
END $$;
