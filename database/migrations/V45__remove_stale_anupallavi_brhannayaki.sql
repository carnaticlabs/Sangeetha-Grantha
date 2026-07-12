-- bRhannAyaki vara dAyaki has a stale ANUPALLAVI section at order_index 3.
-- The composition genuinely has only Pallavi + Samashti Charanam (2 sections).
-- The ANUPALLAVI was created by a prior extraction that mis-identified the structure.

DELETE FROM krithi_lyric_sections
WHERE section_id = '78fdb89e-b112-4fab-b17b-834cfbb9a63f';

DELETE FROM krithi_sections
WHERE id = '78fdb89e-b112-4fab-b17b-834cfbb9a63f';

INSERT INTO audit_log (entity_table, entity_id, action, diff)
VALUES (
    'krithi_sections',
    '78fdb89e-b112-4fab-b17b-834cfbb9a63f',
    'DELETE',
    '{"reason": "stale ANUPALLAVI at order_index 3 for bRhannAyaki vara dAyaki — composition has only Pallavi + Samashti Charanam"}'
);
