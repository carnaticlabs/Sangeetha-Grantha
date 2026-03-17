-- TRACK-097: Reset attempts counter for guru-guha extraction queue entries
-- so the Python worker can re-claim and re-process them with fixed parsers.
UPDATE extraction_queue
SET attempts = 0, updated_at = NOW()
WHERE source_url LIKE '%guru-guha.blogspot%'
  AND status = 'PENDING'
  AND attempts >= max_attempts;
