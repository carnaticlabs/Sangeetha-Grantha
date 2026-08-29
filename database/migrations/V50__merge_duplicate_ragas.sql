-- V50: TRACK-132 raga deduplication (Batch A + Batch B)
--
-- Merges duplicate raga rows created by V40 and by Trinity import matching,
-- per ADR-016 (keep the Wikipedia-form row) and the adjudicated list in
-- TRACK-132 §0c / §0h.
--
-- ORDER per pair: krithi_ragas (by order_index) → krithis.primary_raga_id →
-- ragas.parent_raga_id → DELETE loser. Never DISTINCT-de-dupe the junction
-- first — PK is (krithi_id, raga_id, order_index) and ragamalikas legitimately
-- repeat a raga at different indices (viSva nAthaM bhajEhaM).
--
-- Idempotent: missing loser or keeper is a no-op (fresh db-reset runs this
-- BEFORE R__seed_04, so keepers often do not exist yet). R__seed_05 re-invokes
-- the same function after the Wikipedia seed lands.
--
-- Ref: application_documentation/02-architecture/decisions/ADR-016-raga-naming-authority.md

CREATE OR REPLACE FUNCTION track132_lookup_raga(p_name text)
RETURNS uuid
LANGUAGE sql
STABLE
AS $$
    SELECT id FROM ragas WHERE name = p_name LIMIT 1
$$;

CREATE OR REPLACE FUNCTION track132_merge_raga(p_loser_name text, p_keeper_name text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_loser  uuid;
    v_keeper uuid;
    v_links  int;
    v_primary int;
    v_children int;
BEGIN
    IF p_loser_name = p_keeper_name THEN
        RETURN;
    END IF;

    v_loser  := track132_lookup_raga(p_loser_name);
    v_keeper := track132_lookup_raga(p_keeper_name);

    IF v_loser IS NULL OR v_keeper IS NULL THEN
        RAISE NOTICE 'TRACK-132: skip merge % → % (loser_present=%, keeper_present=%)',
            p_loser_name, p_keeper_name, (v_loser IS NOT NULL), (v_keeper IS NOT NULL);
        RETURN;
    END IF;

    IF v_loser = v_keeper THEN
        RETURN;
    END IF;

    -- A same-index collision would destroy a ragamalika slot. Fail loudly.
    IF EXISTS (
        SELECT 1
        FROM krithi_ragas loser
        JOIN krithi_ragas keeper
          ON loser.krithi_id = keeper.krithi_id
         AND loser.order_index = keeper.order_index
        WHERE loser.raga_id = v_loser
          AND keeper.raga_id = v_keeper
    ) THEN
        RAISE EXCEPTION 'TRACK-132: krithi_ragas order_index collision merging % into %',
            p_loser_name, p_keeper_name;
    END IF;

    SELECT COUNT(*) INTO v_links FROM krithi_ragas WHERE raga_id = v_loser;
    SELECT COUNT(*) INTO v_primary FROM krithis WHERE primary_raga_id = v_loser;
    SELECT COUNT(*) INTO v_children FROM ragas WHERE parent_raga_id = v_loser;

    UPDATE krithi_ragas SET raga_id = v_keeper WHERE raga_id = v_loser;
    UPDATE krithis SET primary_raga_id = v_keeper WHERE primary_raga_id = v_loser;
    UPDATE ragas SET parent_raga_id = v_keeper, updated_at = NOW() WHERE parent_raga_id = v_loser;

    INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
    VALUES (
        'ragas', v_keeper, 'MERGE_DUPLICATE_RAGA',
        jsonb_build_object(
            'loser_id', v_loser,
            'loser_name', p_loser_name,
            'keeper_name', p_keeper_name
        ),
        jsonb_build_object(
            'migration', 'V50',
            'krithi_ragas_moved', v_links,
            'primary_raga_repointed', v_primary,
            'children_reparented', v_children
        )
    );

    DELETE FROM ragas WHERE id = v_loser;
    RAISE NOTICE 'TRACK-132: merged % → % (links=%, primary=%, children=%)',
        p_loser_name, p_keeper_name, v_links, v_primary, v_children;
END;
$$;

CREATE OR REPLACE FUNCTION track132_apply_raga_merges()
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_count int;
    v_keeper uuid;
    v_loser uuid;
    v_krithi uuid;
    v_from uuid;
    v_to uuid;
BEGIN
    -- Batch A: adjudicated MERGE list (TRACK-132 §0c + §0h)
    PERFORM track132_merge_raga('Todi', 'Hanumatodi');
    PERFORM track132_merge_raga('Kalyāni', 'Mechakalyāni');
    PERFORM track132_merge_raga('Riti Gaula', 'Reethigowla');
    PERFORM track132_merge_raga('rItigauLa - Abheri', 'Reethigowla');
    PERFORM track132_merge_raga('Gaula', 'Gowla');
    PERFORM track132_merge_raga('Nāta', 'Nāṭṭai');
    PERFORM track132_merge_raga('Gauri', 'Gowri');
    PERFORM track132_merge_raga('khamAs', 'Kamās');
    PERFORM track132_merge_raga('vIra vasanta', 'Veeravasantham');
    PERFORM track132_merge_raga('vIra vasantaM', 'Veeravasantham');
    PERFORM track132_merge_raga('Mohana', 'Mohanam');
    PERFORM track132_merge_raga('Bhairava', 'Bhairavam');
    PERFORM track132_merge_raga('Gauri Manohari', 'Gourimanohari');
    PERFORM track132_merge_raga('dhAmavati', 'Dharmavati');
    PERFORM track132_merge_raga('Dhāmavathi', 'Dharmavati');
    PERFORM track132_merge_raga('Bauli', 'Bowli');
    PERFORM track132_merge_raga('hindOla vasantaM', 'Hindolavasanta');
    PERFORM track132_merge_raga('Andali', 'Andhali');
    PERFORM track132_merge_raga('ghurjari', 'Gurjari');
    PERFORM track132_merge_raga('Brindāvana Sāranga', 'bRndAvana sAranga');
    PERFORM track132_merge_raga('Pūrvi', 'Poorvi');
    PERFORM track132_merge_raga('Gamakapriyā', 'Gamakakriyā');
    PERFORM track132_merge_raga('Gamanapriyā', 'Gamakakriyā');
    PERFORM track132_merge_raga('Mālāvashree', 'Mālavashree');
    -- Canonicalise the ITRANS twin onto its own corrected display name
    -- (Nārīrītigowla, §0h H2). This is NOT the §0e non-merge: nArīrītigowla stays
    -- DISTINCT from Reethigowla — we only fold its transliteration variant here.
    -- The corrupt-spelled row is renamed below (not a merge target), so there is
    -- deliberately no `→ Nārērētigowla` merge.
    PERFORM track132_merge_raga('nArI rItigauLa', 'Nārīrītigowla');
    PERFORM track132_merge_raga('Jujāvanti', 'Dwijāvanthi /Jujāvanthi');
    PERFORM track132_merge_raga('Jujāvanti', 'Dwijavanthi');

    -- Batch B: one-sided Wikipedia-form keepers (same aggressive fold, unique keeper)
    PERFORM track132_merge_raga('udaya ravicandrika', 'Udayaravichandrika');
    PERFORM track132_merge_raga('jaya Suddha mALavi', 'Jayashuddhamālavi');
    PERFORM track132_merge_raga('yadukula kAmbhOji', 'Yadukula Kāmbhoji');
    PERFORM track132_merge_raga('nArAyaNa dESAkshi', 'Nārāyanadeshākshi');
    PERFORM track132_merge_raga('sindhu rAmakriyA', 'Sindhu Rāmakriya');
    PERFORM track132_merge_raga('navarasa kannaDa', 'Navarasa kannada');
    PERFORM track132_merge_raga('gAngEya bhUshaNi', 'Gāngeyabhuśani');
    PERFORM track132_merge_raga('lalita pancamaM', 'Lalitapanchamam');
    PERFORM track132_merge_raga('mALava pancamaM', 'Mālavapanchamam');
    PERFORM track132_merge_raga('mangaLa kaiSiki', 'Mangalakaishiki');
    PERFORM track132_merge_raga('Suddha dhanyASi', 'Suddha Dhanyāsi');
    PERFORM track132_merge_raga('Kamala Manohari', 'Kamalā Manohari');
    PERFORM track132_merge_raga('Karnataka Behag', 'Karnātaka Behāg');
    PERFORM track132_merge_raga('dESi siMhAravaM', 'Deshisimhāravam');
    PERFORM track132_merge_raga('shaNmukha priya', 'Śanmukhapriyā');
    PERFORM track132_merge_raga('nAsikA bhUshaNi', 'Nāsikābhūśaṇi');
    PERFORM track132_merge_raga('Suddha mukhAri', 'Suddha Mukhāri');
    PERFORM track132_merge_raga('karNATaka kApi', 'Karnātaka Kāpi');
    PERFORM track132_merge_raga('Suddha Bangala', 'Suddha Bangāla');
    PERFORM track132_merge_raga('Kuntala Varali', 'Kunthalavarāli');
    PERFORM track132_merge_raga('kASi rAmakriya', 'Kāshirāmakriyā');
    PERFORM track132_merge_raga('rishabha priya', 'Riśabhapriyā');
    PERFORM track132_merge_raga('gEya hejjajji', 'Geya Hejjajji');
    PERFORM track132_merge_raga('Citta Ranjani', 'Chittaranjani');
    PERFORM track132_merge_raga('dEva manOhari', 'Deva Manohari');
    PERFORM track132_merge_raga('madhyamAvati', 'Madhyamāvathi');
    PERFORM track132_merge_raga('Kapi Narayani', 'Kāpi Nārāyani');
    PERFORM track132_merge_raga('Suddha sAvEri', 'Suddha Sāveri');
    PERFORM track132_merge_raga('pantuvarALi', 'Panthuvarāli');
    PERFORM track132_merge_raga('bindu mAlini', 'Bindhumālini');
    PERFORM track132_merge_raga('Jayanta Sena', 'Jayanthasena');
    PERFORM track132_merge_raga('Nalina Kanti', 'Nalinakānthi');
    PERFORM track132_merge_raga('janaranjani', 'Jana Ranjani');
    PERFORM track132_merge_raga('jaganmOhanaM', 'Jaganmohana');
    PERFORM track132_merge_raga('kumudakriyA', 'Kumudhakriyā');
    PERFORM track132_merge_raga('caturangiNi', 'Chaturāngini');
    PERFORM track132_merge_raga('cakravAkaM', 'Chakravākam');
    PERFORM track132_merge_raga('phEnadyuti', 'Phenadhyuti');
    PERFORM track132_merge_raga('stava rAjaM', 'Sthavarājam');
    PERFORM track132_merge_raga('vAcaspati', 'Vāchaspati');
    PERFORM track132_merge_raga('kalakaNThi', 'Kalākānti');
    PERFORM track132_merge_raga('Ardra dESi', 'Ardhradesi');
    PERFORM track132_merge_raga('chAyAvati', 'Chāyāvathi');
    PERFORM track132_merge_raga('ravikriyA', 'Ravi Kriyā');
    PERFORM track132_merge_raga('cintAmaNi', 'Chintāmani');
    PERFORM track132_merge_raga('kaikavaSi', 'Kaikavashi');
    PERFORM track132_merge_raga('rati priya', 'Rathipriyā');
    PERFORM track132_merge_raga('Carukesi', 'Chārukesi');
    PERFORM track132_merge_raga('nava ratna vilAsaM', 'Navarathna Vilāsam');
    PERFORM track132_merge_raga('nAma dESi', 'Nāmadeshi');
    PERFORM track132_merge_raga('SyAmaLaM', 'Shyāmalam');
    PERFORM track132_merge_raga('kuntaLaM', 'Kunthalam');
    PERFORM track132_merge_raga('Siva pantuvarALi', 'Shivapanthuvarāli');
    PERFORM track132_merge_raga('vasanta', 'Vasanthā');
    PERFORM track132_merge_raga('Kalyana Vasanta', 'Kalyāna Vasantam');
    PERFORM track132_merge_raga('santAna manjari', 'Santhāna Manjari');
    PERFORM track132_merge_raga('cAmaraM', 'Chāmaram');
    PERFORM track132_merge_raga('binna pancamaM', 'Bhinnapanchamam');
    PERFORM track132_merge_raga('vijaya vasanta', 'Vijayavasantham');
    PERFORM track132_merge_raga('nAga svarAvaLi', 'Nāgaswarāvali');
    PERFORM track132_merge_raga('ravi candrika', 'Ravi Chandrikā');
    PERFORM track132_merge_raga('Sruti ranjani', 'Shruthiranjani');
    PERFORM track132_merge_raga('kOkila dhvani', 'Kokiladhwani');
    PERFORM track132_merge_raga('candra jyOti', 'Chandrajyothi');
    PERFORM track132_merge_raga('Suddha dESi', 'Shuddha Desi');
    PERFORM track132_merge_raga('aThANa', 'Atāna');
    PERFORM track132_merge_raga('haMsa dhvani', 'Hamsadhwani');
    PERFORM track132_merge_raga('jyOti', 'Jyothi');
    PERFORM track132_merge_raga('vATI vasanta bhairavi', 'Vātee Vasantabhairavi');
    PERFORM track132_merge_raga('nAga dhvani', 'Nāgadhwani');
    PERFORM track132_merge_raga('vaMSavati', 'Vamshavathi');
    PERFORM track132_merge_raga('Saila dESAkshi', 'Shailadeshākshhi');
    PERFORM track132_merge_raga('SarAvatI', 'Sharāvathi');
    PERFORM track132_merge_raga('sarasvati manOhari', 'Saraswathi Manohari');
    PERFORM track132_merge_raga('svarAvaLi', 'Swarāvali');
    PERFORM track132_merge_raga('gambhIra vANi', 'Gambheeravani');
    PERFORM track132_merge_raga('pUrva varALi', 'Poorvavarāli');
    PERFORM track132_merge_raga('Purna Lalita', 'Poornalalita');
    PERFORM track132_merge_raga('tIvra vAhini', 'Teevravāhini');
    PERFORM track132_merge_raga('pUrNa pancamaM', 'Poornapanchamam');
    PERFORM track132_merge_raga('kiraNAvaLi', 'Keeranāvali');
    PERFORM track132_merge_raga('pUrNa candrika', 'Poornachandrika');
    PERFORM track132_merge_raga('bhUshAvaLi', 'Bhooshāvali');

    -- H2: rename the katapayadi-corrupt display name (also corrected in R__seed_04).
    UPDATE ragas
    SET name = 'Nārīrītigowla',
        name_normalized = 'nariritigowla',
        updated_at = NOW()
    WHERE name = 'Nārērētigowla'
       OR name_normalized = 'nareretigowla';

    -- C4: rename the Wikipedia dual-form row to the expert-preferred spelling.
    -- Skip if a row already occupies name_normalized = dwijavanthi (the merge above
    -- already folded Jujāvanti into it).
    UPDATE ragas
    SET name = 'Dwijavanthi',
        name_normalized = 'dwijavanthi',
        updated_at = NOW()
    WHERE name = 'Dwijāvanthi /Jujāvanthi'
      AND NOT EXISTS (
          SELECT 1 FROM ragas other
          WHERE other.name_normalized = 'dwijavanthi'
            AND other.id <> ragas.id
      );

    -- H1: Veeravasantham avarohanam — vakra janya, not the parent krama.
    UPDATE ragas
    SET avarohanam = 'S N3 P M1 G2 R2 S',
        updated_at = NOW()
    WHERE name = 'Veeravasantham'
      AND avarohanam IS DISTINCT FROM 'S N3 P M1 G2 R2 S';

    -- Merge #17 scale correction on whichever Brindavana row survived.
    UPDATE ragas
    SET arohanam = 'S R2 M1 P N3 S',
        avarohanam = 'S N2 P M1 R2 S',
        updated_at = NOW()
    WHERE name IN ('bRndAvana sAranga', 'Brindāvana Sāranga')
      AND (
          arohanam IS DISTINCT FROM 'S R2 M1 P N3 S'
          OR avarohanam IS DISTINCT FROM 'S N2 P M1 R2 S'
      );

    -- H3: Dikshitar kalAvati kamalAsana is Kalāvathi (mela 31), not Kalāvati (16).
    SELECT id INTO v_from FROM ragas WHERE name = 'Kalāvati' LIMIT 1;
    SELECT id INTO v_to   FROM ragas WHERE name = 'Kalāvathi' LIMIT 1;
    IF v_from IS NOT NULL AND v_to IS NOT NULL THEN
        FOR v_krithi IN
            SELECT k.id
            FROM krithis k
            WHERE k.title_normalized LIKE '%kamalasana%'
               OR k.title ILIKE '%kamal%asana%'
        LOOP
            UPDATE krithi_ragas
            SET raga_id = v_to
            WHERE krithi_id = v_krithi AND raga_id = v_from;
            UPDATE krithis
            SET primary_raga_id = v_to, updated_at = NOW()
            WHERE id = v_krithi AND primary_raga_id = v_from;
        END LOOP;
    END IF;

    -- DISTINCT guards: these pairs must still be two rows.
    PERFORM track132_assert_distinct('Kanadā', 'Kannada');
    PERFORM track132_assert_distinct('Kalāvathi', 'Kalāvati');
    PERFORM track132_assert_distinct('Shreemati', 'Srimati');
    PERFORM track132_assert_distinct('Bhairavi', 'Bhairavam');
    PERFORM track132_assert_distinct('Abhogi', 'Bhogi');
    PERFORM track132_assert_distinct('Gāyakapriyā', 'Gamakakriyā');

    -- Ragamalika integrity (§4): only when the Trinity import is present.
    -- viSva nAthaM bhajEhaM is Dikshitar's Chaturdasa Ragamalika — 34 rows,
    -- contiguous order_index 1..34, arranged as a palindrome. Repointing by
    -- order_index must never lose or duplicate a slot, so assert count, the
    -- 1..34 contiguity (MIN/MAX), and that no order_index collapsed (a merge
    -- landing two rows on the same slot would drop the DISTINCT count below).
    SELECT id INTO v_krithi FROM krithis WHERE id = '70623fa6-f90b-44f6-a61f-9136936e8be2';
    IF v_krithi IS NOT NULL THEN
        DECLARE
            v_min int;
            v_max int;
            v_distinct_idx int;
        BEGIN
            SELECT COUNT(*), MIN(order_index), MAX(order_index), COUNT(DISTINCT order_index)
              INTO v_count, v_min, v_max, v_distinct_idx
            FROM krithi_ragas WHERE krithi_id = v_krithi;
            IF v_count <> 34 OR v_min <> 1 OR v_max <> 34 OR v_distinct_idx <> 34 THEN
                RAISE EXCEPTION
                    'TRACK-132: viSva nAthaM bhajEhaM ragamalika broken — count=% min=% max=% distinct_index=% (expected 34/1/34/34)',
                    v_count, v_min, v_max, v_distinct_idx;
            END IF;
        END;
    END IF;
END;
$$;

CREATE OR REPLACE FUNCTION track132_assert_distinct(p_a text, p_b text)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_a int;
    v_b int;
BEGIN
    SELECT COUNT(*) INTO v_a FROM ragas WHERE name = p_a;
    SELECT COUNT(*) INTO v_b FROM ragas WHERE name = p_b;
    -- Fresh DB without seed yet: both missing is fine. One present is fine.
    -- Both present must remain two rows; neither disappearing after a merge is the failure.
    IF v_a = 0 AND v_b = 0 THEN
        RETURN;
    END IF;
    IF v_a = 1 AND v_b = 1 THEN
        RETURN;
    END IF;
    IF v_a >= 1 AND v_b >= 1 THEN
        RETURN;
    END IF;
    -- If exactly one of a known DISTINCT pair vanished, that is a false merge.
    -- Only raise when *one* side is missing *and* we expected both (post-seed).
    -- Pre-seed, R__ has not landed: skip. Post-seed both exist unless we merged.
    -- Detect false merge: one name gone while the other remains AND a seed-like
    -- companion (Hanumatodi) already exists, meaning R__ has run.
    IF EXISTS (SELECT 1 FROM ragas WHERE name = 'Hanumatodi') THEN
        IF v_a <> 1 OR v_b <> 1 THEN
            RAISE EXCEPTION 'TRACK-132: DISTINCT pair broken: % (count=%) / % (count=%)',
                p_a, v_a, p_b, v_b;
        END IF;
    END IF;
END;
$$;

SELECT track132_apply_raga_merges();
