-- TRACK-011: Add quality scoring columns to imported_krithis
-- Quality scores enable review prioritization, auto-approval automation, and data quality assessment

-- Note: quality_score and quality_tier are created earlier by V15 (quality_tier as
-- quality_tier_enum); only the four scoring columns unique to this migration are added here.
ALTER TABLE imported_krithis
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
