-- Sample Krithi: Endaro Mahanubhavulu

DO $$
DECLARE
    v_tyagaraja_id UUID;
    v_sri_id UUID;
    v_adi_id UUID;
    v_rama_id UUID;
    v_krithi_exists BOOLEAN;
BEGIN
    SELECT id INTO v_tyagaraja_id FROM composers WHERE name_normalized = 'tyagaraja';
    SELECT id INTO v_sri_id FROM ragas WHERE name_normalized = 'sri';
    SELECT id INTO v_adi_id FROM talas WHERE name_normalized = 'adi';
    SELECT id INTO v_rama_id FROM deities WHERE name_normalized = 'rama';
    
    -- Check if krithi exists
    SELECT EXISTS(SELECT 1 FROM krithis WHERE title = 'Endaro Mahanubhavulu' AND composer_id = v_tyagaraja_id) INTO v_krithi_exists;
    
    IF NOT v_krithi_exists AND v_tyagaraja_id IS NOT NULL AND v_sri_id IS NOT NULL THEN
        INSERT INTO krithis (
            id, title, title_normalized, composer_id, primary_raga_id, tala_id, deity_id, 
            primary_language, musical_form, sahitya_summary, 
            created_at, updated_at
        ) VALUES (
            gen_random_uuid(), 
            'Endaro Mahanubhavulu', 
            'endaro mahanubhavulu', 
            v_tyagaraja_id, 
            v_sri_id, 
            v_adi_id, 
            v_rama_id, 
            'te', 
            'KRITHI', 
            'Salutations to all great men...',
            NOW(), 
            NOW()
        );
    END IF;
END $$;
