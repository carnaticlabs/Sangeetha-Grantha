CREATE TABLE IF NOT EXISTS public.temple_source_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_url TEXT NOT NULL UNIQUE,
    source_domain TEXT NOT NULL,
    temple_name TEXT NOT NULL,
    temple_name_normalized TEXT NOT NULL,
    deity_name TEXT,
    kshetra_text TEXT,
    city TEXT,
    state TEXT,
    country TEXT,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geo_source TEXT,
    geo_confidence TEXT,
    notes TEXT,
    raw_payload TEXT,
    fetched_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    error TEXT
);

CREATE INDEX idx_temple_source_cache_source_url ON public.temple_source_cache(source_url);
CREATE INDEX idx_temple_source_cache_temple_name_norm ON public.temple_source_cache(temple_name_normalized);
CREATE INDEX idx_temple_source_cache_deity_name ON public.temple_source_cache(deity_name);

-- Trigger to update 'updated_at'
CREATE OR REPLACE FUNCTION update_temple_source_cache_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_temple_source_cache_timestamp_trim
BEFORE UPDATE ON public.temple_source_cache
FOR EACH ROW
EXECUTE FUNCTION update_temple_source_cache_timestamp();
