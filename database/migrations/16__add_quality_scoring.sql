-- TRACK-011: Add quality scoring columns to imported_krithis
-- Quality scores enable review prioritization, auto-approval automation, and data quality assessment

ALTER TABLE imported_krithis
    ADD COLUMN IF NOT EXISTS quality_score DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS quality_tier VARCHAR(20),
    ADD COLUMN IF NOT EXISTS completeness_score DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS resolution_confidence DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS source_quality DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS validation_score DECIMAL(3,2);

-- Add index for quality tier filtering
CREATE INDEX IF NOT EXISTS idx_imported_krithis_quality_tier
    ON imported_krithis(quality_tier)
    WHERE quality_tier IS NOT NULL;

-- Add index for quality score sorting
CREATE INDEX IF NOT EXISTS idx_imported_krithis_quality_score
    ON imported_krithis(quality_score DESC NULLS LAST);
