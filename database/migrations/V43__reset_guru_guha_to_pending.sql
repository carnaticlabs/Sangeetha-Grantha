-- TRACK-097: Reset guru-guha extraction queue entries to PENDING with attempts=0
-- so the Python worker can re-claim them with the fixed HTML/structure parsers.
UPDATE extraction_queue
SET status = 'PENDING', attempts = 0, updated_at = NOW()
WHERE source_url LIKE '%guru-guha.blogspot%';
