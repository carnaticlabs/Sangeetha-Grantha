-- corpus-data-fix: allow
-- TRACK-144: Additive corrective migration for persistent database environments.
-- 1. Audits Catusra Ekam structure update (talas beat_count=4, anga_structure='I4').
-- 2. Audits notation variant updates for merged talas with before/after diffs in audit_log.
-- 3. Strict identity validation and Latin incipit verification across all 343 compositions (fails closed).
-- 4. Resets prematurely synchronized document_embeddings.content_hash to 'STALE_TRACK_144_NEEDS_REBUILD'
--    so that the refresh checker detects them and triggers re-embedding.

DO $track144_v64$
DECLARE
    v_catusra_ekam_id uuid;
    v_misra_capu_id uuid;
    v_adi_id uuid;
    r record;
    before_row krithis%ROWTYPE;
    after_row krithis%ROWTYPE;
    notation_after_row krithi_notation_variants%ROWTYPE;
    target_tala_id uuid;
    source_id uuid;
    evidence_row krithi_source_evidence%ROWTYPE;
    tala_row talas%ROWTYPE;
BEGIN
    SELECT id INTO v_catusra_ekam_id FROM talas WHERE name = 'Catusra Ekam';
    SELECT id INTO v_misra_capu_id FROM talas WHERE name = 'Misra Capu';
    SELECT id INTO v_adi_id FROM talas WHERE name = 'Adi';

    -- On an empty/reference-only database with no talas yet, safely return
    IF v_catusra_ekam_id IS NULL OR v_adi_id IS NULL THEN
        RETURN;
    END IF;

    --------------------------------------------------------------------------
    -- Part 1: Audit Catusra Ekam Structure Update
    --------------------------------------------------------------------------
    SELECT * INTO tala_row FROM talas WHERE id = v_catusra_ekam_id;
    IF FOUND THEN
        IF tala_row.beat_count IS NULL OR tala_row.anga_structure IS NULL THEN
            UPDATE talas SET beat_count = 4, anga_structure = 'I4', updated_at = clock_timestamp()
            WHERE id = v_catusra_ekam_id;
            SELECT * INTO tala_row FROM talas WHERE id = v_catusra_ekam_id;
        END IF;

        IF NOT EXISTS (
            SELECT 1 FROM audit_log
            WHERE entity_table = 'talas' AND entity_id = v_catusra_ekam_id AND action = 'UPDATE'
              AND metadata->>'track' = 'TRACK-144'
        ) THEN
            INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
            VALUES (
                'talas',
                v_catusra_ekam_id,
                'UPDATE',
                jsonb_build_object(
                    'before', jsonb_build_object('id', v_catusra_ekam_id, 'name', 'Catusra Ekam', 'beat_count', null, 'anga_structure', null),
                    'after', to_jsonb(tala_row)
                ),
                '{"track":"TRACK-144","reason":"Populated beat_count and anga_structure for Catusra Ekam"}'::jsonb
            );
        END IF;
    END IF;

    --------------------------------------------------------------------------
    -- Part 2: Audit Notation Variant Updates for Merged Talas
    --------------------------------------------------------------------------
    FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Isra Capu', 'Isra Chapu')) FOR UPDATE LOOP
        UPDATE krithi_notation_variants SET tala_id = v_misra_capu_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;
        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{"track":"TRACK-144","reason":"Merged corrupt Isra Capu/Chapu to Misra Capu"}'::jsonb);
    END LOOP;

    FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id IN (SELECT id FROM talas WHERE name = 'Ad') FOR UPDATE LOOP
        UPDATE krithi_notation_variants SET tala_id = v_adi_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;
        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{"track":"TRACK-144","reason":"Merged corrupt Ad to Adi"}'::jsonb);
    END LOOP;

    FOR r IN SELECT * FROM krithi_notation_variants WHERE tala_id IN (SELECT id FROM talas WHERE name IN ('Ekam', 'Caturasra Ekam', 'English')) FOR UPDATE LOOP
        UPDATE krithi_notation_variants SET tala_id = v_catusra_ekam_id, updated_at = clock_timestamp() WHERE id = r.id RETURNING * INTO notation_after_row;
        INSERT INTO audit_log (entity_table, entity_id, action, diff, metadata)
        VALUES ('krithi_notation_variants', r.id, 'UPDATE', jsonb_build_object('before', to_jsonb(r), 'after', to_jsonb(notation_after_row)), '{"track":"TRACK-144","reason":"Standardized notation variant tala to canonical Catusra Ekam"}'::jsonb);
    END LOOP;

    --------------------------------------------------------------------------
    -- Part 3: Strict Composition Identity and Latin Incipit Validation (343 Compositions)
    --------------------------------------------------------------------------
    CREATE TEMP TABLE tmp_tala_backfill (
        krithi_id uuid PRIMARY KEY,
        title text NOT NULL,
        composer text NOT NULL,
        raga text NOT NULL,
        proposed_tala text NOT NULL,
        source_name text NOT NULL,
        source_url text NOT NULL,
        source_checksum text,
        source_locator text NOT NULL,
        raw_tala text NOT NULL,
        expected_pallavi text NOT NULL
    ) ON COMMIT DROP;

    INSERT INTO tmp_tala_backfill VALUES
        ('00a8d145-bb7e-487c-89e9-0f14e72b9618'::uuid, 'kASi viSvESvara', 'Muthuswami Dikshitar', 'Kāmbhoji', 'Ata', 'ibiblio.org/guruguha', 'https://www.ibiblio.org/guruguha/mdeng.pdf', 'c6d962e850f521c2f7ab28a8f9f49f047f58cb302be4107b13298c18efaf8dfa', 'Printed/PDF page 77, raga/tala header and pallavi visually checked; P. P. Narayanaswami compilation, August 2007; corroborated by Guruguha.org Kasi Visvesvara article (2019-06-10), which specifies khanda ata', 'aṭa', 'kASI viSvESvara Ehi mAM pAhi
karuNA nidhE sannidhEhi mudaM dEhi'),
        ('01166b8b-a60b-4eac-9895-1d3feae3f94f'::uuid, 'rAma rAma gOvinda', 'Tyagaraja', 'saurAshTraM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2765.shtml', '821f9a0e323c8f9683e155bdde2350fcdcb59e3843c332391a49c44fd38ff64f', 'Composition: raama raama gOvinda - sowraashTram; Talam: aadi', 'aadi', 'rAma rAma gOvinda 1 nanu rakshincu mukunda'),
        ('04ab08e7-0967-4a18-a070-0778232fb3dc'::uuid, 'vishNu vAhanuDu', 'Tyagaraja', 'SankarAbharaNaM', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2942.shtml', '2303269f437a09365513e90e9de01f66d369fec6a0aee801c5b92b891d5686b8', 'Composition: vishNu vaahanuNDidigO - shankaraabharaNam; Talam: roopakam', 'roopakam', 'vishNu vAhanuND(i)digO veDale jUDarE'),
        ('055d1261-b0bd-477f-a231-b69780ab60c9'::uuid, 'Koluvaiyunnaadae', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2403.shtml', 'fe9da6324b6d99ea524d9862ece3a9516b173ea80f70bb26888a2f4520de144b', 'Composition: koluvaiyunnaaDE - bhairavi; Talam: aadi', 'aadi', 'koluvai(y)unnADE kOdaNDa pANi'),
        ('0582b4fa-d55a-4756-9428-566fc120e361'::uuid, 'SrI raghu vara dASarathE', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2842.shtml', '3fe226cd4893df075e3240536835f9797e3a6d05275e3cf8b20015a7cd3dabf1', 'Composition: shree raghuvara daasharathE - shankaraabharaNam; Talam: aadi (tishra gati)', 'aadi (tishra gati)', 'SrI raghuvara dASarathE rAma'),
        ('0595d4fd-e943-450d-b3cd-12bc11dcaf7a'::uuid, 'Nee Bhakti', 'Tyagaraja', 'Jayamanohari', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2646.shtml', '8fcf94669a47463c6bf5babe697906d0a1f50a2d5c273eb139ea30914633e2bd', 'Composition: nee bhakti bhaagya - jayamanOhari; Talam: caapu', 'caapu', 'nI bhakti bhAgya sudhA
nidhin(I)dEdE janmamu'),
        ('06a450bb-c569-4409-aa2a-973ea4575797'::uuid, 'rAma nIvE kAni', 'Tyagaraja', 'Nārāyani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2762.shtml', '35c9bb4ead1a0e4d620d3f762076385d17d2c21801eca487729cc0bcb44db129', 'Composition: raama neevEgaani - naaraayaNi; Talam: aadi', 'aadi', 'rAma nIvE kAni nannu rakshincu 1 vAr(e)vvarE'),
        ('06f0c5bd-c58f-43bd-aa22-4efacbbc6351'::uuid, 'Saraseeruhaanana', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2813.shtml', '209e71794ef730964431fcd2fb64f21269b0e4569f1c7cef0d9dd73f2785cee0', 'Composition: saraseeruhaanana raama - mukhaari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sarasIruh(A)nana rAma
samayamu brOva 1 cid-ghana'),
        ('0762a13c-331d-4d0a-8837-28b8f94865db'::uuid, 'Nannu Brova Neekinta', 'Tyagaraja', 'Abhogi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2584.shtml', '627032ed1036f9547b6e8ec36e5459c9477dd9f9bb77dd1047d9a7034e88f184', 'Composition: nannu brOva - aabhOgi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nannu brOva nIk(i)nta tAmasamA
nApai nEram(E)mi palkumA'),
        ('0879def4-4c65-4d11-9377-6be31e34d0cb'::uuid, 'Raaraa Seetha Ramani', 'Tyagaraja', 'Hindolavasanta', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2791.shtml', '781ccf0bf075208d45e57cbdcf778a9cc9ca3044971fa7bdd5cbdd2b36fb2415', 'Composition: raaraa seetaa - hindOLavasantaa; Talam: roopakam', 'roopakam', 'nIraja nayana oka 2 mudd(I)ra dhIra mungala (rAra)'),
        ('09216653-9ebd-4df0-82db-c374e18068d7'::uuid, 'bhajana parulakEla', 'Tyagaraja', 'suraTi', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1396.shtml', '10770cfb2b0b65e0d9479afc9919902bd988bec099c11ce458620a8031ce2c55', 'Composition: bhajana parula - shuruTTi; Talam: roopakam', 'roopakam', 'bhajana parulak(E)la daNDa pANi
bhayamu manasA 1 rAma (bhajana)'),
        ('09266c45-7c28-4dba-921e-42d95393d6e8'::uuid, 'enta vEDukondu', 'Tyagaraja', 'Saraswathi Manohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2309.shtml', '5981a4aa266e12f4ff647fd45b3fc04529f1f6a5f39f617cd162f9874b3278f2', 'Composition: enta vEDukondu - saraswati manOhari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'enta vEDukondu rAghava
pantam(E)larA O rAghava'),
        ('09fc370e-ec19-482a-9519-41b0d903125c'::uuid, 'valla kAdanaka', 'Tyagaraja', 'SankarAbharaNaM', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2911.shtml', 'a21a3dc4e13e7b977c05390d9e2c2d0eb8cbf29d3a11800e413d5801065be6d1', 'Composition: vallagaadaanaka - harikaambhOji; Talam: roopakam. Resolved musicological variation: Karnatik.com lists raga Harikambhoji, but catalogue stores Sankarabharanam. V. Govindan''s Thyagaraja Vaibhavam (2008-03) explicitly resolves this scholarly variation: "In the kRti valla kAdanaka - rAga SankarAbharaNaM (or harikAmbhOji) ... In some books, the rAga is given as harikAmbhOji"; both traditions uniformly use Rupaka talam.', 'roopakam', 'valla kAd(a)naka sItA vallabha 1 brOvu nA'),
        ('0aae2382-ec72-4d8f-90e1-62aba112b37a'::uuid, 'Naadupai', 'Tyagaraja', 'Madhyamāvathi', 'Khanda Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/naadupai.htm', '77939aa0532dbb01bac5a7bfa3bbf8265df69198f7b1ed42ce1a7e7ea216a7f0', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Khanda Chapu', 'nAdupai palikEru 1 narulu'),
        ('0b16335f-76ce-40f6-8404-36ca524bc9c3'::uuid, 'Nadachi Nadachi', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2574.shtml', 'd70ec6bb23090c956806679128f04b7f72fdaace47ac894c51dd87e0690057bc', 'Composition: naDaci naDaci - kharaharapriyaa; Talam: aadi', 'aadi', 'naDaci naDaci jUcEr(a)yOdhyA
nagaramu kAnarE'),
        ('0bd37b74-295a-4dda-aa04-b3448f89b4a7'::uuid, 'Sogasu Jooda', 'Tyagaraja', 'kannaDa gauLa', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1353.shtml', '87dd1265a8e1143a868865aca2883b637d41635b2ffb6ce03ecf07a7e1b4dce5', 'Composition: sogasu jooDa - kannaDagowLa; Talam: rUpakam', 'rUpakam', 'sogasu jUDa taramA nI'),
        ('0c4d94e7-3ccd-4546-a153-7e2138792820'::uuid, 'aDugu varamula', 'Tyagaraja', 'Ārabhi', 'Misra Capu', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-adugu-varamula-raga.html', 'd72210980eab7eea50c5cca8b3cf7488af7c2d2d6dcab6232340b38394c4f999', 'Opening composition description and Latin pallavi; composer and raga verified', 'miSra cApu', 'aDugu varamulan( 1 i)ccedanu'),
        ('0d1fe89d-3fdf-408e-aa22-3e667614637e'::uuid, 'enduku peddala', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/endukupeddala.htm', '700e4a048dfd35be804e3930dafa09b663f57ee942cc9c3c38bd09bf7d4d0faf', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai )', 'enduku 1 peddala vale buddhi 2 iyyavu
endu pOdun(a)yya rAmayya'),
        ('0d9065ca-f1ce-4426-87f1-a02c2145ba33'::uuid, 'Maa Jaanaki', 'Tyagaraja', 'Kāmbhoji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/maajanaki.htm', '9f382a0d40e206d67686b244c1005a34719a9bca64fe5f44fb15861741da9242', 'Notation header Talam: Deshadi, stored as Adi; raga Kambodhi (Kamboji); pallavi mA jAnaki cETTa paTTaga', 'Deshadi', 'mA jAnaki 1 cETTa paTTaga
2 maharAjav(ai)tivi'),
        ('0e2cb747-b347-49a2-a477-da67dc559557'::uuid, 'Mridu Bhaashana', 'Tyagaraja', 'Maruvadhanyāsi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2563.shtml', '72b3a81cad10a1cf456179a7603ad1ffd6dc11f266f464f7de81583ad05fadfe', 'Composition: mrdu bhaashaNa - maruvadhanyaasi; Talam: aadi', 'aadi', 'mRdu bhAshaNa nata vibhIshaNa'),
        ('0e4f787a-94a0-42e3-be60-da868e838de4'::uuid, 'daNDamu', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2247.shtml', '3a2562d95c914f2f65a19df500ca04968f2f893defda08bfeaf077f14104c537', 'Composition: daNDamu beTTEdanuraa - baalahamsa; Talam: aadi', 'aadi', '1 daNDamu peTTedanurA kOdaNDa pANi jUDarA'),
        ('0ebeaa05-5481-4300-ac39-70986688efed'::uuid, 'Chentanae Sadaa', 'Tyagaraja', 'Kunthalavarāli', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2239.shtml', '25bf0a00b00150e1db41d3258c97950958724580bac38cac475c6dac52cc2886', 'Composition: centanE sadaa - kuntala varaaLi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'centanE sadA(y)uncukO(v)ayya'),
        ('0f06796a-2f9a-489b-9b41-18a659c30754'::uuid, 'Jo Jo Rama', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1733.shtml', 'cdadc009261860ce8bd4d44ed292bb8eb214f1f965ce82b2cca702f346f300b2', 'Composition: jO jO raamaa - reeti gowLa; Talam: aadi', 'aadi', 'jO jO rAma Ananda ghana'),
        ('1100e4b9-0f07-4e9c-996b-e5aedf9444a3'::uuid, 'Tanayandae Prema', 'Tyagaraja', 'Bhairavi', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c1094.shtml', 'a10607e7f6fcbb2338b8fd22a8ac756fe2e51a7ff898e9e306160454d10b190d', 'Composition: tanayandE prEma (nowkaa caritra keertanam) - bhairavi; Talam: tripuTa', 'tripuTa', 'tana(y)andE prEma(y)anucu viri bONulu
tala teliyakan(A)Dedaru'),
        ('110eac88-12e8-4638-a35a-f753fb39010e'::uuid, 'Evarunnaaru Brova', 'Tyagaraja', 'Mālavashree', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2327.shtml', 'f908e8567384b224a358106223a1fe098deb727969ad2af39dfb67dfde3c3aaf', 'Composition: evarunnaaru - maaLavashree; Talam: aadi', 'aadi', 'evar(u)nnAru brOva
inta tAmasam(E)lan(a)yya'),
        ('1156c31c-e939-4643-b650-9faa062bb33e'::uuid, 'Chakkani Raja', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/Chakkaniraja.htm', 'd0c2e662f069d86a3cc938e8d40cd962ee2215b19196eb6ba552d86ca0264990', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai )', 'cakkani rAja mArgamul(u)NDaga
1 sandula dUran(E)la O manasA'),
        ('12058429-320a-4040-b682-78c26399e0ae'::uuid, 'SrI nArasiMha', 'Tyagaraja', 'Phalaranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2840.shtml', 'ba3fb6c838b99d24e360ab33a12105bfdab9d515a34df86aa16e2a2b521a038f', 'Composition: shree narasimha - phalamanjari; Talam: aadi. Note: Phalamanjari / Phalaranjani are known pedagogical name variants for this raga scale; both agree on Adi tala.', 'aadi', 'SrI nArasiMha mAm pAhi
kshIr(A)bdhi kanyakA ramaNa'),
        ('14973eb2-3f3a-49b9-ac25-728a110ffa46'::uuid, 'Sanaatana', 'Tyagaraja', 'Phalamanjari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1812.shtml', 'cf531feab0e0fe1b942974fd32198310e099a457d179979689ba5b38864a9505', 'Composition: sanaatana - phalamanjari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sanAtana parama pAvana
ghanA-ghana varNa kamal(A)nana'),
        ('15f7defb-f208-4eea-b4ca-9cc671862c6a'::uuid, 'toli nEnu jEsina', 'Tyagaraja', 'Kokiladhwani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2899.shtml', '7e1604ca92323f198f8eecd643ce2d1792d73c22fb7b13268ee72e47d05c48ef', 'Composition: toli nEnu jEsina - kOkiladhwani; Talam: aadi', 'aadi', 'toli nEnu jEsina pUjA phalam(I)lAgE'),
        ('17cf2d35-e811-4f63-8b25-71d17e25e27d'::uuid, 'Manasaa Sri Ramuni', 'Tyagaraja', 'Māraranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2544.shtml', '395f552569daecaddd873fab76faa85b7a4fe2351efa4ec315741a60cbb1d861', 'Composition: manasaa shree raamuni - maararanjani; Talam: aadi', 'aadi', 'manasA SrI rAmuni daya lEka
mAyamaina 1 vidham(E)mE'),
        ('17e53b19-6e57-4c5b-b3c6-05cf6af2f3ff'::uuid, 'Maaru Palkaga', 'Tyagaraja', 'Ranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1281.shtml', 'c0673f303f64e5d97bf165783113e8112d308492252c76c5a85ce635695f1371', 'Composition: maarubalka kunna - shree ranjani; Talam: aadi', 'aadi', 'mAru palkag(u)nnAv(E)mirA
mA manO-ramaNa'),
        ('18202aae-9d3c-41e4-96fd-80914610cabf'::uuid, 'ninu vinA nA madi', 'Tyagaraja', 'Navarasa kannada', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1294.shtml', '30f574e59d65eb48c008fe9c0c18fd107620b44c3177d3b41fa1dfd65c9cac1a', 'Composition: ninuvinaa naamadi - navarasa kannaDa; Talam: roopakam', 'roopakam', 'ninu vinA nA 1 madi(y)endu niluvadE SrI hari hari'),
        ('189bc0d1-e96b-4e26-9de8-ff55f51321a3'::uuid, 'Ennaallu Tirigedi', 'Tyagaraja', 'Mālavashree', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2299.shtml', '378dca8729ce2517aeaf6590cc36d9ca0a31f53bc92e0644addfa42a8c524441', 'Composition: ennaaLLu tirigEdi - maaLavashree; Talam: aadi', 'aadi', 'ennALLu tirigEdi(y)ennALLu'),
        ('18ae493e-7724-40a7-b7c1-f1d861202b93'::uuid, 'Inkaa Daya', 'Tyagaraja', 'nArAyaNa gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2355.shtml', '34ab9a6b763813e691cabb9de5a06da05b9520f063a4afc9fecad1463496a2f4', 'Composition: inkaa daya raakuNTE - naaraayaNa gowLa; Talam: aadi', 'aadi', 'inkA daya rAk(u)NTE entani sairinturA'),
        ('198cf9c3-a2f0-4345-b0a7-c0be4f2f8d99'::uuid, 'prArabdham', 'Tyagaraja', 'Swarāvali', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2731.shtml', 'efbf019f21ed73509687b1d763f29bf604963ce9971112cf5cab6cf45eb109ad', 'Composition: praarabdha miTTuNDaga - swaraavaLi; Talam: jhampa', 'jhampa', '1 prArabdham(i)TT(u)NDagan(o)rulan(a)na
pani lEdu nIv(u)NDaga'),
        ('1a5f7193-3f44-4170-b510-d13b78ad9390'::uuid, 'Neeke Teliyaka', 'Tyagaraja', 'Anandabhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2631.shtml', '6d5d790f415ba39e99cb0fcf725b05394ede5cbecb8ce129eb4ec0c762e11180', 'Composition: neekE teliyakapOtE - aananda bhairavi; Talam: aadi', 'aadi', 'nIkE teliyaka pOtE
nEn(E)mi sEyudurA'),
        ('1b751137-9e78-4a63-9036-2f648bf2dcec'::uuid, 'adi kAdu bhajana', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1375.shtml', 'b731dd60b94a0cc926a618744133c1670db071bd51c49ea64b5461068b403915', 'Composition: adi kaadu bhajana - yadukula kaambhOji; Talam: aadi', 'aadi', 'adi kAdu bhajana manasA'),
        ('1b7baaeb-ee79-408c-b860-3a4c72e127b3'::uuid, 'Chootaamu Raareyee Vedkanu', 'Tyagaraja', 'Kāpi', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c30908.shtml', '64905dea8be9f7a48ef942a79f6a9ec02cff6b593ed48c28434a3f3697b95368', 'Composition: cUtAmurArE I veDkanu - kApi; Talam: cApu', 'cApu', 'cUtAmu rArE(y)I vEDkanu
sudatulAra nEDu'),
        ('1b89544a-16fb-4433-93c8-b3840dbfe10a'::uuid, 'Nalina Lochana', 'Tyagaraja', 'Madhyamāvathi', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2579.shtml', '4e58bc029010b28f4b6b38e82d7e75efed526fa3b66beef0951b5d4b9a061281', 'Composition: naLina lOcana - madyamaavati; Talam: caapu', 'caapu', 'naLina lOcana ninnu gAka anyula 1 nammi
nara janmam(I)DErunA'),
        ('1cc1eb20-bad0-45d7-8688-b4d32f6edea7'::uuid, 'Dvaitamu Sukhamaa', 'Tyagaraja', 'Reethigowla', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/dwaitamu.htm', '7d1f81274656baeceaa8bf4f3d0e7bbc67d9b01da73bea40b3efdf1ec1a54272', 'Notation header Talam: Adi (2 kalai); raga Reethigowlai; source pallavi Dwaitamu Sukhama. Corroborated against stored Latin anupallavi opening.', 'Adi (2 kalai)', 'caitanyamA vinu sarva sAkshi vistAramugAnu telpumu nAtO (dvaitamu)'),
        ('1cc88aff-8c5f-4d2d-b8ad-32d122bbcab8'::uuid, 'Tanayuni Brova', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2890.shtml', 'c67172affdf739195d642b073a99dd45a2e9153ab960ada66696e08fd029083e', 'Composition: tanayuni brOva - bhairavi; Talam: aadi', 'aadi', 'tanayuni brOva janani vaccunO
talli vadda bAluDu pOnO'),
        ('1d2d8060-66e8-48e4-933b-62a7ff17cb28'::uuid, 'Rama Paahi', 'Tyagaraja', 'Kāpi', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2763.shtml', '9f8027d7ae4f6bd2c496396d5b8ec5128ed41fc662b7b6481cf089485cf6c0d5', 'Composition: raama paahi - kaapi; Talam: caapu', 'caapu', 'rAma pAhi mEgha SyAma pAhi guNa
dhAma mAm pAhi O rAma'),
        ('1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4'::uuid, 'Ivasudha Neevanti', 'Tyagaraja', 'Sahāna', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2369.shtml', 'dc160afd3df2bdc18c933f0fbfd6e8ed68c7effde7bbb43794bff83f0280fd46', 'Composition: ee vasudha - sahaanaa; Talam: aadi', 'aadi', 'I 1 vasudha nIv(a)NTi 2 daivamun(e)ndu kAnarA'),
        ('1dfe4864-6224-460a-845a-5328aed9b638'::uuid, 'Sri Rama Jaya Rama', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2847.shtml', '854dc05d1282e29b157a1bda83c5b7dd8528cba3a070fe6e069a015c434b26ae', 'Composition: shree raama jayaraama shrngaara - madyamaavati; Talam: aadi', 'aadi', 'SrI rAma jaya rAma SRngAra rAma'),
        ('1e8717c9-7316-4de5-9ed4-5a026a9722bc'::uuid, 'ninu bAsi', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2669.shtml', 'ab096e76e246bcd1bc5d524b33f0e6774e710d12e06fc4a04a7723a2cc64dd4f', 'Composition: ninnu baasi - baalahamsa; Talam: aadi', 'aadi', 'ninu 1 bAsi(y)eTul(u)ndurO
nirmal(A)tmulau janulu'),
        ('1ef669fc-6fa2-446e-97de-1e9127778eb0'::uuid, 'SrI rAma raghu rAma', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2849.shtml', 'e14ed82ceaf0294c6a58300ffbe41f79102a1f6c29c8513d3bbd05411600288c', 'Composition: shree raama raghuraama - yadukula kaambhOji; Talam: jhampa', 'jhampa', 'SrI rAma 1 raghu rAma SRngAra rAma(y)ani
2 cintimpa rAdE O manasA'),
        ('1f53b74e-3fa8-4f0a-b0f6-ccde12faf39d'::uuid, 'SambhO Siva', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2802.shtml', '6c45eec59a0c1840578f3f9af951a2359f425aeb2c36a914b5b217322f47f2ec', 'Composition: shambhO shiva - shankaraabharaNam; Talam: aadi', 'aadi', 'SambhO Siva Sankara 1 guru ambhO-ruha nayana'),
        ('1f930f28-ac22-4e5a-a219-37ec6bd88e94'::uuid, 'Nannu Vidichi', 'Tyagaraja', 'Reethigowla', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2520.shtml', 'd9f0ddcfb09844b50532f952407e7fc992ed0d4bfcd75085f2b3c2a057e4611c', 'Composition: nannu viDaci - reeti gowLa; Talam: mishra caapu', 'mishra caapu', 'nannu viDici kadalakurA rAm(a)yya vadalakurA'),
        ('1fa2297e-2e46-4341-bfc7-faf18f161a78'::uuid, 'Narada Guru Svami', 'Tyagaraja', 'Darbar', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c31179.shtml', '5a87b70fcbe3f7c122544232d88d58c833feac3e89ad27fbd3477f35a43c48cb', 'Composition: nArada gurusvAmi - darbAr; Talam: Adi', 'Adi', 'nArada guru svAmi(y)ikanaina
nann(A)darimpav(E)mi I karav(E)mi'),
        ('208081e1-0c9c-44f1-90bf-ae4b337238b0'::uuid, 'Nagu Momu Kana Leni', 'Tyagaraja', 'Abheri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1001.shtml', 'cf713a1ee83cbe05533e6d1fbaad6d9d41b49f5e38d5b4d6499120914e32853c', 'Composition: nagumOmu ganalEni - aabhEri; Talam: aadi', 'aadi', 'nagu mOmu kana lEni nA jAli telisi
nannu 1 brOva rAdA SrI raghuvara nI (nagu)'),
        ('209b89f7-44a8-4d47-a939-c70b8db7e919'::uuid, 'Manavini Vinumaa', 'Tyagaraja', 'Jayanārāyani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2547.shtml', 'feb601c4fe561b34e1136420e081aae8126c41d14264d9d7027cd662edd87eec', 'Composition: manavini vinumaa - jayanaaraayaNi; Talam: aadi', 'aadi', 'manavini vinumA marava samayamA'),
        ('21e5206c-591b-42cc-81cf-0f59f78939b4'::uuid, 'Chinthisthunnaadae', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2244.shtml', 'f25283fcc3430471a8868ef6ab191324c279badd450875f0d8e0e66e4375bdd2', 'Composition: cintistunnaaDE - mukhaari; Talam: aadi', 'aadi', 'cintistunnADE yamuDu'),
        ('21f76636-f967-49e3-a299-ea95d6cb906a'::uuid, 'parulanu vEDanu', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2725.shtml', '6b86bafb1af4827dffb77c46cc3553db4cf4ff9f52088af6b6237518456464fb', 'Composition: parulanu vEDanu - balahamsa; Talam: aadi', 'aadi', 'parulanu vEDanu patita pAvanuDA'),
        ('22ad2fcd-21d3-4a11-9236-6a651049674f'::uuid, 'Paahi Parama Dayaalo', 'Tyagaraja', 'Kāpi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2696.shtml', '779075d2ac438291f5aedbc2b35a4fb3722a6926727ec407fc4f467b751e0c2d', 'Composition: paahi parama dayaaLO - kaapi; Talam: aadi; Kapi / Adi (Divyanama)', 'aadi', 'pAhi parama dayALO harE mAm'),
        ('23a58ec7-e5c1-46f5-a459-303300cf5002'::uuid, 'paripAlaya dASarathE', 'Tyagaraja', 'SankarAbharaNaM', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2718.shtml', '86c4a19546384d8b93bee91b33f3600ebbf9bea5d64ed250bcce89b6cb02aeac', 'Composition: paripaalaya daasharathE - shankaraabharaNam; Talam: tripuTa', 'tripuTa', 'paripAlaya dASarathE rAma mAM
paripAlaya dASarathE'),
        ('24c49b68-15a6-47ed-abea-0e1bf6a2616e'::uuid, 'talli taNDrulu', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2886.shtml', 'f7ff44a80fddb8d86465bf04c6e9a606818d176601eb65231acb9ee57d0a17bf', 'Composition: tali taNDrulu - balahamsa; Talam: aadi', 'aadi', '1 talli taNDrulu kala pEru kAni
ila nI sari daivamul(e)varE'),
        ('24e54ad6-b8d5-4e7e-b015-6cbcfa1ccf38'::uuid, 'Mokshamu Galadaa', 'Tyagaraja', 'Sāramati', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1291.shtml', '318837aaa69620f1bc22bcc29c17d72d3e3e1e42a5c7a7cf65ff03d6461e5fbe', 'Composition: mOkshhamu galadaa - saaramati; Talam: aadi', 'aadi', 'sAkshAtkAra nI 3 sad-bhakti sangIta jnAna vihInulaku (mOkshamu)'),
        ('254ba553-c1d3-4678-aa99-34f92acd44f9'::uuid, 'Vinaayakuni', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/vinayakuni-madhyamavathi.htm', '2bf7ecff34b60dd6df7066ec1dfedd47260d7778dda7a1ea15f4b978e82289ff', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'vinAyakuni valenu brOvavE ninu
vinA vElpul(e)varammA'),
        ('265850ff-accb-4515-a53d-0c05efdb151f'::uuid, 'Saranu Sarananucu', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2827.shtml', '3ae88a7380bc234ffe800f95008fe43e4762d2c757928bab6b5ec800120db9a6', 'Composition: sharaNu sharaNu - madyamaavati; Talam: aadi', 'aadi', 'SaraNu SaraN(a)nucu moraliDina nA
giramul(a)nni pariyAcakam(au)nA'),
        ('27f23797-2404-42d6-ab94-ca817fd6127d'::uuid, 'mAkElarA vicAramu', 'Tyagaraja', 'Ravi Chandrikā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2538.shtml', '2c25e4df81ca014ad66a882f511d86d61d838a529df9d34743a620eb83ef0426', 'Composition: maakElaraa vicaaramu - ravi candrikaa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'mAk(E)larA vicAramu
maruk(a)nna SrI rAma candra'),
        ('2837196d-84e8-4772-b5e1-db163e21a8af'::uuid, 'Vinataa Suta Vaahanudai', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2938.shtml', '3066660f38da5b516c80a3b480adce4c50a6f7c8aab78d830efcce865319730f', 'Composition: vinataa suta vaahanuDai - harikaambhOji; Talam: aadi', 'aadi', 'vinatA suta vAhanuDai
veDalenu kAnci varaduDu'),
        ('28935d89-dbfb-4d3b-8ce5-094c94136ab5'::uuid, 'virAja turaga', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2941.shtml', '96ffa71e1bafe4296844f2af52279a08f3ad8b88200e8df6805087f32b48052c', 'Composition: viraaja turaga - balahamsa; Talam: aadi', 'aadi', '1 virAja turaga rAja rAj(E)Svara
nirAmayuni jEyavE'),
        ('28f4d47f-3e15-44e9-b178-559f6cbd048b'::uuid, 'Kanulu Taakani', 'Tyagaraja', 'Kalyāna Vasantam', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2387.shtml', '2f91ceb6525f0dafaba4f23cdbd0ea7f9f38ad6b18871f3f3c2f0a3d4e01b4d1', 'Composition: kanulu taakani - kalyaaNa vasantam; Talam: roopakam', 'roopakam', 'kanulu tAkani para kAntala
manas(e)TulO rAma'),
        ('29542e1b-7e94-438c-ab5c-381853d8ee4e'::uuid, 'nApAli SrI rAma', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1488.shtml', '4849bbdc2578bc33c9cff55128d8d0cfeb8cc333f7a16ce6e96ebd1e01b27189', 'Composition: napaali shree raamaa - shankaraabharaNam; Talam: aadi', 'aadi', 'nApAli SrI rAma bhU-pAlaka 1 stOma
kApADa samayamu nI 2 pAdamul(I)rA'),
        ('2b65609e-9fbf-4232-baae-096cafdf4e79'::uuid, 'Endaro Mahaanubhaavulu', 'Tyagaraja', 'Sri', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/endaromahanubhavulu-new.htm', '6136455d55c15ac5e14c2bb49874d4cff5179f03007deba20cce1bc8630e261d', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'endarO mahAnubhAvul-
(a)ndariki vandanamu'),
        ('2c7ec18a-c429-402e-a8d6-ea963de31108'::uuid, 'Kula Birudunu', 'Tyagaraja', 'Deva Manohari', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2413.shtml', 'd20cabed63deb1cac70ff39359be16eb84c14b52a465c7bac1f03869108818f7', 'Composition: kula birudunu - dEva manOhari; Talam: roopakam', 'roopakam', 'kula birudunu brOcukommu rammu'),
        ('2d0ab787-2a6c-414b-868b-c0512661b2d7'::uuid, 'Nijamaitae Mundara', 'Tyagaraja', 'Bhairavi', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2659.shtml', 'ce5b09005f0820f120ce67c27f27e533328a46a7dd7b12735d597c94f14cbcbc', 'Composition: nijamaitE mundara - bhairavi; Talam: tripuTa', 'tripuTa', 'ajuDaina hari hayuDaina nA bhaktiyu (nija)'),
        ('2e539247-6fc1-4880-bbf0-5ad079d5d7bd'::uuid, 'Sri Rama Paadamaa', 'Tyagaraja', 'Amrta Vahini', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2848.shtml', '8dc1f27b71aaeb39e7924ff5b6f063b6ac2ea920ab07c94a8e03023c243e5232', 'Composition: shree raama paadamaa - amritavaahini; Talam: aadi', 'aadi', 'SrI rAma pAdamA nI kRpa cAlunE
cittAniki rAvE'),
        ('306f67e3-ca51-4cc0-908f-99474ee6d030'::uuid, 'Emaanaticchevo', 'Tyagaraja', 'Sahāna', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/emaanadicchevo.htm', 'ac95bdb901b4708342a0064a584d99a7bd84772d4ac2d9e95c6950d8e7245737', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'Em(A)nat(i)ccEvO Em(e)ncinAvO'),
        ('3411ea29-8485-47b2-9785-6171c6f8a396'::uuid, 'mA kulamuna', 'Tyagaraja', 'suraTi', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2533.shtml', 'd7088c687e420c84df95869174935561134bb006bf6e834c122e6a898f120f01', 'Composition: maa kulamuna - shuruTTi; Talam: tripuTa', 'tripuTa', 'mA kulamunak(i)ha param(o)sagina 1 nIku
mangaLaM Subha mangaLaM'),
        ('341feaef-7ba8-4621-b929-065ce4c82764'::uuid, 'SrIpaptE nI pada', 'Tyagaraja', 'Nāgaswarāvali', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2448.shtml', 'bd7b9ddafeeee8f3d941b60b09a10977b27b8cd0dad7ec070025429d0e58ecbc', 'Composition: shreepatE - naagaswaraavaLi; Talam: roopakam', 'roopakam', 'SrI-patE nI pada 1 cintanE jIvanamu'),
        ('34bdc909-e856-4190-b110-9ad2634b6c67'::uuid, 'Chetulaara', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2242.shtml', 'a8d5f4e200a6304d404db26114b4ca1a0a131c23f3c72d4d00c2a5a086ba457f', 'Composition: cEtulaara shrngaaramu - kharaharapriyaa; Talam: aadi', 'aadi', 'cEtulAra SRngAramu jEsi jUtunu SrI rAma'),
        ('356bceef-a192-4687-8185-7c594c9c3578'::uuid, 'Sundara Dasaratha', 'Tyagaraja', 'Kāpi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2874.shtml', '0776801eb22da14dea8feec41c50b0220abf25f2438decae0392fcdda78788ae', 'Composition: sundara dasharatha - kaapi; Talam: aadi', 'aadi', 'sundara daSaratha nandana
vandanam(o)narincedarA'),
        ('37249a7b-de16-40f8-a51e-6834bc6e3c62'::uuid, 'evarurA ninu vinA', 'Tyagaraja', 'Mohanam', 'Misra Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/evarura-mohanam.htm', '7f17d9c9d3af5125f98b4c1d855c27589728104fc45cd18dad3f75b53ae4c9c2', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Misra Chapu', 'evarurA ninu vinA gati mAku'),
        ('3747ba8d-2df1-4c7f-b272-20ba3c81a5d8'::uuid, 'Chera Raavademi', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2240.shtml', '49626f12fe0b02c1e78913f1d25b01d6619bc0331a9cf29b65c06aaed07947c1', 'Composition: cEra raavadEmiraa - reeti gowLa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'mEra kAdurA ika mahA mEru dhIra SrI kara (cEra)'),
        ('38a8fa0e-49e9-41af-9182-7d41a8b5a81b'::uuid, 'Rama Samayamu', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2775.shtml', '38a6ecb32d1afc31f48216371108ce92459e0eed5f6ad1a561ea6e50353ed3dc', 'Composition: raama samayamu - madyamaavati; Talam: aadi', 'aadi', 'rAma samayamu brOvarA nA pAli daivamA'),
        ('39793ab9-5a83-43b9-be64-4a009d09c037'::uuid, 'rAma daivamA', 'Tyagaraja', 'suraTi', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2749.shtml', '5ff4e6944a7751e8249f7809bfba72aa686143dc1b839298c357620c0d39e779', 'Composition: raama deivamaa - shuruTTi; Talam: roopakam', 'roopakam', 'rAma daivamA 1 rAka rAka lObhamA'),
        ('398b15ae-5419-49a9-9eda-4f565adb782d'::uuid, 'endukO bAga', 'Tyagaraja', 'Mohanam', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2289.shtml', '0c90272921c9de6c9c9e301ce15fcd644e34bfde27e9d2d8f42f01c6e598ffb1', 'Composition: endukO baaga teliyadu - mOhanam; Talam: aadi', 'aadi', 'endukO bAga teliyadu'),
        ('3b79777a-3ace-475d-8c0a-a6e5991cdd77'::uuid, 'Brocevaarevarae', 'Tyagaraja', 'Ranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1183.shtml', 'ee58e476d088c0f55305b8d2ee0eca1f7ead09bb7d217398d73603ac3499160e', 'Composition: brOcEvaarevarE - shree ranjani; Talam: aadi', 'aadi', 'brOcEvAr(e)varE raghu patI'),
        ('3be08780-f3f2-4ac2-acde-818f8438ed3e'::uuid, 'dEva rAma rAma', 'Tyagaraja', 'saurAshTraM', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2258.shtml', 'c20a6aded4834a1e972559614ccf7b7260b9e282de1fd00ee53f1f9e6206e4fa', 'Composition: dEva raama raama - sowraashTram; Talam: roopakam', 'roopakam', 'dEva rAma rAma mahAdEva rAma rAghuvara'),
        ('3d2fd5f4-a690-4ace-8ae7-df1fe2d23644'::uuid, 'manasu svAdhIna', 'Tyagaraja', 'SankarAbharaNaM', 'Misra Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/manasuswaddhina.htm', '25de83c9260363af2dc89348fda01c15c376501a7b20ce4e808a7993e7f777c7', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Misra Chapu', 'manasu svAdhInam(ai)na(y)A ghanuniki
mari mantra tantramul(E)la'),
        ('3d87f5dc-3db7-495f-8b79-e1ff88754323'::uuid, 'Upachaaramu Jesevaaru', 'Tyagaraja', 'Bhairavi', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/upacharamu.htm', '2e87bed2455c104a036b5879cedbbd7541c7e21709ce58425a058184f39c2e29', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'upacAramu jEsEvAr(u)nnAr(a)ni maravakurA'),
        ('3ed8918c-c99c-48b1-b0e9-7b0f06dc7850'::uuid, 'Bhuvini Dasudanae', 'Tyagaraja', 'Ranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2227.shtml', '2ba8f4029c33d60c777c51dc30ea8862f3c96f45ec6a9aab62128ff70b67f490', 'Composition: bhuvini daasuDanE - shree ranjani; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'bhuvini dAsuD(a)nE pErAsacE
bonkul(A)DitinA budha manO-hara'),
        ('3f2a06d0-efc0-4f57-9791-24178b0859d5'::uuid, 'jAnakI ramaNa', 'Tyagaraja', 'Shuddha Seemantini', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/janakiramana.htm', '186c6c93ed89aad17d6086287252512f2ce5d715de932e022a596abb0c157a10', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'jAnakI ramaNa bhakta pArijAta
pAhi sakala lOka SaraNa'),
        ('3ff7cbc9-e452-4da4-b06b-e520f683050b'::uuid, 'Sariyevvarae', 'Tyagaraja', 'Ranjani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2819.shtml', 'c4e2897cc07f43630d57ed0abf0e2c7863e617e0d53d69dabb5fc4fe4cfbc5fc', 'Composition: sari evvarE - shree ranjani; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sari(y)evvarE SrI jAnaki nI'),
        ('400ea94f-513e-4b4c-9fba-263856a8c4d8'::uuid, 'rAma SrI rAma lAli', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2777.shtml', '55d6883316e6a256cdef68643b64e441085211cbde0bb318f716eb8abac8bf62', 'Composition: raama shree raama laali (laali) - shankaraabharaNam; Talam: aadi', 'aadi', 'rAma SrI rAma lAli Ugucu ghana
SyAma nanu brOvu lAli (rAma)'),
        ('40792364-1527-4e70-804d-00e1323c621c'::uuid, 'Ramaabhirama Ramaneeya', 'Tyagaraja', 'Darbar', 'Misra Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/ramabhirama.htm', '10e81a8285d0f4b299a08d282f97be1aa2c0fd2389a66c14bd0f69a063c60feb', 'Notation header Talam: Misra Chapu; raga Durbar; pallavi Raamaabhi Raamaa Ramaniya Namaa', 'Misra Chapu', 'rAm(A)bhirAma ramaNIya nAma
1 sAmaja ripu bhIma sAkEta dhAma (rA)'),
        ('4215e9b5-17dd-4730-a140-55d48014b034'::uuid, 'Lalitae Sri Pravriddhae', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2527.shtml', 'ced9788555a6ca6cc78026c11259b7cbdf7fdf95ca46e9bf8b9c8ee341baa361', 'Composition: lalitE shree - bhairavi; Talam: aadi', 'aadi', 'lalitE 1 SrI pravRddhE SrImati
lAvaNya 2 nidhimati'),
        ('426f7403-9809-4704-b7e1-22e539b7acb3'::uuid, 'Elaraa Sri Krishnaa', 'Tyagaraja', 'Kāmbhoji', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1449.shtml', '92ea8fdf84e93e387150f84589ef5a8a8d55d5be5650e563f51ae3b5d1a98112', 'Composition: Elaraa shree - kaambhOji; Talam: roopakam', 'roopakam', 'ElarA SrI kRshNA nAtO calamu-
(y)ElarA kRshNA nIk(Ela)'),
        ('42d35ca5-4110-4fd8-bece-9ced6a37014e'::uuid, 'Rama Rama Rama Sita', 'Tyagaraja', 'Sāveri', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2007/04/thyagaraja-kriti-sri-rama-rama-rama.html', '', 'Opening composition description explicitly gives Saveri / Adi; matched against stored Latin pallavi', 'Adi', 'sriramaramaramasitahrjjaladhisoma'),
        ('44d9e846-50b1-43dd-bc7e-9f82994d953c'::uuid, 'Tulasi Bilva', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2900.shtml', '1b5c51302b9b3127c2b73d2b7e03e795cde5217dd1a4eda16ee7f30633213ab1', 'Composition: tuLasi bilva - kEdaara gowLa; Talam: aadi', 'aadi', 'tulasI bilva mallik(A)di
jalaja 1 sumamula pUjala kaikonavE'),
        ('44e32f1e-f9d6-4789-99e5-5f148bc97a82'::uuid, 'Paraaku Neekelaraa', 'Tyagaraja', 'Keeranāvali', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2710.shtml', 'a09263d86acde468216ca0b682284a7504bbb4985be0d5310279e0a5f2f486f6', 'Composition: paraaku nee - keeraNaavaLi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'parAku nIk(E)larA rAma'),
        ('451244d9-a0ee-43b3-b6aa-7a06cee44ce6'::uuid, 'sArasa nEtra', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-saarasa-netra-raga.html', 'b0d071ea167b47f8ddabe62b300ce36dbb23508f84ab67b6aa0bfa0e0b8f0747', 'Opening composition description and Latin pallavi; composer and raga verified', 'Adi', 'sArasa nEtr(A)pAra guNa
1 sAmaja Siksha 2 g(O)ddharaNa'),
        ('4629d110-e300-4557-b69d-a50aa9cdc0d8'::uuid, 'Baale Baalendu', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1395.shtml', 'fd01a9bae380e0edaa8985e97a67f8aaca9ee4b79b53a731451ddc0e4ca843c0', 'Composition: baale baalEndu - reeti gowLa; Talam: aadi', 'aadi', 'phAla lOcani SrI dharma saMvardhani 4 sakala lOka janani (bAle)'),
        ('46a8024b-ec47-4259-87ef-26bfb3e4b7be'::uuid, 'Ennaallu Nee Trova', 'Tyagaraja', 'Kāpi', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2298.shtml', '846d251c200f914c614a3b19aaa80ef3cfe7adda03a231958952014dff99b344', 'Composition: ennaaLLu nee trOva - kaapi; Talam: caapu', 'caapu', 'ennALLu nI 1 trOva jUtu rAma
Em(a)ni nE proddu trOtu'),
        ('4749735b-6ccb-4892-a504-04df5716bde0'::uuid, 'Sara Sara Samaraika', 'Tyagaraja', 'Kunthalavarāli', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2826.shtml', 'e00fc8facd4a31016d49167e0c2cc5e132936d0ee399470550ae310715690f6b', 'Composition: shara shara samaraika - kuntala varaaLi; Talam: aadi', 'aadi', 'Sara Sara samar(ai)ka SUra
2 Saradhi mada vidAra'),
        ('47f8faf6-b867-486c-baef-007293872b12'::uuid, 'O Jagannaatha', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2680.shtml', '5a3645459aa3c6441cf3924fb351354eefcb564a9aa8492b69f921252492fe79', 'Composition: O jagannaatha - kEdaara gowLa; Talam: aadi', 'aadi', 'O 1 jagan-nAthA(y)ani nE pilicitE
2 O(y)ani rA rAdA'),
        ('498f50fb-3632-498c-888d-2c04e78f771d'::uuid, 'Paahi Rama Ramayanucu', 'Tyagaraja', 'Kharaharapriyā', 'Tisra Ekam', 'karnatik.com', 'https://www.karnatik.com/c2702.shtml', '5e43fa8d7633612ccf0b30bb4bdaf6707fef283032e4a5fdc315bbb4c956f0f0', 'Composition: paahi raama raama - kharaharapriyaa; Talam: tishra laghu Eka; Kharaharapriya / Rupaka or Tisra Ekam (Divyanama)', 'tishra laghu Eka', 'pAhi rAma rAma(y)anucu bhajana sEyavE'),
        ('4998bbf0-54d1-4552-82bc-7c815c16c5e7'::uuid, 'Sri Ganapathini', 'Tyagaraja', 'saurAshTraM', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/sriganapathi.htm', 'a5629cb4df19e392667c504f414be5f86d118e75a6d9e16280f7400b6b6e7bf3', 'Talam header and Pallavi; manually verified spelling Sowrastram = saurAshTraM; composer and incipit matched', 'Adi (2 kalai )', 'SrI gaNa patini 1 sEvimpa rArE
Srita mAnavulArA'),
        ('49e73d43-23cb-427e-a526-21ae0a5d31b8'::uuid, 'mariyAda kAdurA', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2555.shtml', 'af99e356219735ed75db63356afe6f9b2f455e6d6d77dbeb3125b3d16c370668', 'Composition: mariyaada gaaduraa - shankaraabharaNam; Talam: aadi', 'aadi', 'mariyAda kAdurA'),
        ('4a53d422-1ce0-4895-a6d3-38f49d5df3a6'::uuid, 'Yochanaa Kamala', 'Tyagaraja', 'Darbar', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1064.shtml', '335c2ddf1574ede97fa83cf6414b724229c7e65d01049eb2da9573e2e9aaaad1', 'Composition: yOcanaa kamala - darbaar; Talam: aadi', 'aadi', 'sUcana teliyakan(o)rula yAcana sEtun(a)nucu nIku tOcenA'),
        ('4a69d9af-b815-406c-a475-23e708182795'::uuid, 'Elaavataara', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2274.shtml', '2eab9baa271dfb1228dca0485c20dfe043f2e9808a09874f86832d5c9236b525', 'Composition: Elaavataaram - mukhaari; Talam: aadi', 'aadi', 'El(A)vatAram(e)ttukoNTivi
Emi kAraNamu rAmuDai'),
        ('4b768a4c-4ef0-4cb4-a2e9-c8e76562063a'::uuid, 'Abhimaanamu Ledemi', 'Tyagaraja', 'Andhali', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c1357.shtml', 'd3473f4f93c3aa331098a9b59a9e601da834f09d0080d113e799861ff9bbb70d', 'Composition: abhimaanamu lEdimi - andhali; Talam: tripuTa', 'tripuTa', 'abhimAnamu lEd(E)mi nIv-
(a)bhinaya vacanamul(A)DEd(E)mi'),
        ('4c8ae919-199e-427e-a405-10afe38ef806'::uuid, 'Guru Leka', 'Tyagaraja', 'Gourimanohari', 'Khanda Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/guruleka.htm', '12480a326771acdcb91629e596cb8a3e5566a6d2331f2f575c103e9ec246ee81', 'Talam header and Pallavi; manually verified spelling Gowri Manohari = Gourimanohari; composer and incipit matched', 'Khanda Chapu', 'guru lEka(y)eTuvaNTi guNiki teliyaga pOdu'),
        ('4cae5d72-c291-465d-b117-242dff25e96c'::uuid, 'rAmA rAma rAma rAmAyani', 'Tyagaraja', 'Mohanam', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2771.shtml', '812c7519b69f2baf74dfdde5d7d6e939cf7afd0110524fffae5b0724d2193345', 'Composition: raama raama raama raama - mOhanam; Talam: caapu', 'caapu', '1 rAma rAma rAma rAmA(y)anin(a)nta
2 rAjapu jUp(E)larA O rAma'),
        ('4d49569a-cd31-444d-98b9-8be4ee2e1e91'::uuid, 'Vaarija Nayana-1', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2921.shtml', '26ec69503778841fef6e1d374f8b50f73954f048a13284ca8e7f9d44a6e0c243', 'Composition: vaarija nayana - kEdaara gowLa; Talam: aadi', 'aadi', 'vArija nayana nIvADanu nEnu
1 vAramu nannu 2 brOvu'),
        ('4e418fbf-5e06-4dc1-81dc-0f946c6de2ba'::uuid, 'Maa Ramachandruniki', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2534.shtml', 'd9baa0c4681e0bd74bf513bf40b06e87904a320e8135f70b74e18d84d769c24d', 'Composition: maa raamacandruniki - kEdaara gowLa; Talam: aadi', 'aadi', 'mA rAmacandruniki jaya mangaLam
ghOra bhava nIra nidhi tArakuniki mangaLam (mA)'),
        ('4e6c1010-d9a5-4461-b4d0-49c6b6e73b45'::uuid, 'Sri Janaki Manohari', 'Tyagaraja', 'Eeshamanohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2836.shtml', '6d5f607ac1e81d5b4441c42f2d51cce370cd57b014ee790f16a73f9aff28f495', 'Composition: shree jaanaki manOhara - eesamanOhari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'SrI jAnakI manOhara SrI rAghava hari'),
        ('4f0d4529-dc92-41e6-8532-edb311bddd4b'::uuid, 'Evarikai', 'Tyagaraja', 'Deva Manohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2323.shtml', '2b74940e23e6e9a27f82a43bf4829a95d8fda66bd61c4c0d60f721e744dd11d5', 'Composition: evarikai avataaram - dEva manOhari; Talam: aadi', 'aadi', 'evarkai avatAram(e)ttitivO
ipuDaina telupa(v)ayya 2 rAmayya (evarikai)'),
        ('4f90dd54-c76e-4cce-88f8-1c460cdb3566'::uuid, 'rAma sItA rAma', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2778.shtml', '9de0c4ec753f5a64e168d48f3b8e90f32e99ba3c02c728a77a184b1a2e42caf6', 'Composition: raama seetaaraama raama raaja - balahamsa; Talam: aadi', 'aadi', '1 rAma sItA rAma rAma rAma sItA rAma rAma'),
        ('4fa55259-3152-4f57-b528-52bcdf41b833'::uuid, 'vinanAsakoni', 'Tyagaraja', 'Pratāpavarāli', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2934.shtml', 'e8d7bb3520d4a5723f9ed883ba2532c705c3facfadef8794336903900381a9b8', 'Composition: vina naashakoni - prataapavaraaLi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'vinan(A)sakoni(y)unnAnurA viSva rUpuDa nE'),
        ('503b3c2f-cfc0-418c-a914-0f313291a9b4'::uuid, 'patiki hArati', 'Tyagaraja', 'suraTi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2726.shtml', '9815be38d5b7281cfa9605ba5b79525c9aa192f6ac9a1aeb26424e32b66f1815', 'Composition: patiki haarateerE - shuruTTi; Talam: aadi', 'aadi', '1 patiki hAratI rE sItA'),
        ('506dc30f-1708-4b28-b2e9-f90218b9f038'::uuid, 'nenaruncinAnu', 'Tyagaraja', 'Mālavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2598.shtml', '3ba483ac412c0d4cde77fd6bb322f486e88240803a49c72cc671b972714d7db3', 'Composition: nenaruncinaanu anniTiki - maaLavi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nenar(u)ncinAnu anniTiki
1 nidAnuD(a)ni nEnu nIdupai'),
        ('50772910-25eb-4778-bb6e-e6ebdc2459fc'::uuid, 'vara lIla gAna', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1422.shtml', '3dbf8d2464c777f20d549f7f3e9ae72b0373ab0aa368e0ec1d65e60e40d3b95d', 'Composition: varaleela gaanalOla - shankaraabharaNam; Talam: aadi', 'aadi', 'vara lIla gAna lOla sura pAla suguNa jAla
bharita nIla gaLa hRd-Alaya Sruti mUla su-
karuN(A)lavAla pAlay(A)Su mAM'),
        ('50942fd4-4ca3-421f-9555-a615685b5c43'::uuid, 'Peridi Ninu', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2519.shtml', 'd2a6f6805e5789d7e62252c3a2b574dc48b493205be818e91deb18e6d3696769', 'Composition: pEriDi ninu - kharaharapriyaa; Talam: aadi', 'aadi', 'vArini jUpavE SrI rAmayya (pEriDi)'),
        ('50b58873-0614-4f2e-95e7-b225a1151ffd'::uuid, 'Tappaganae', 'Tyagaraja', 'Suddha Bangāla', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2891.shtml', '1bc35cfe8e1502dc379468393a1c217d27a28531d697bfa973cdb7747cdf9cdb', 'Composition: tappaganE vaccunaa - sudda bangaaLa; Talam: roopakam', 'roopakam', 'tappaganE vaccunA
2 tanuvuku lampaTa nI kRpa'),
        ('52ace0b2-5d91-46b3-9c0c-add32c7f1766'::uuid, 'Vanaja Nayanudani', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2912.shtml', '8cc2e8d54e8507b2caa75d4f16c8de94d9e464f9b11a3f805f46ad5da1e17bfe', 'Composition: vanaja nayanuDani - kEdaara gowLa; Talam: aadi', 'aadi', 'vanaja nayanuD(a)ni valacitivO vAni
manasuna daya lEdE'),
        ('5603c15e-1264-46f6-a587-b07c80af900c'::uuid, 'Vinataa Suta Vaahana', 'Tyagaraja', 'Jayanthasena', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/vinatasuta.htm', '15f3ecc3c60339ec9b6364c98b204e26a5de8fbefe64aa1f279c238d1935630e', 'Notation header Talam: Adi; raga Jayantasena; pallavi Vinataa Suta Vaahana Sri Ramanaa', 'Adi', 'vinatA suta vAhana SrI ramaNa
manasAraga sEvinceda rAma'),
        ('5638a240-7a35-437e-b112-1cba581deef2'::uuid, 'Siggu Maali', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2863.shtml', 'c9281cd2ae7ba8b701c4766778436a1796a5e9f5a46f2636bd88787788707764', 'Composition: siggu maali - kEdaara gowLa; Talam: aadi', 'aadi', 'siggu mAli nA vale dharan(e)vvaru tiruga jAlar(a)yya'),
        ('57efd3b4-aee7-4124-91f5-a58848ad3177'::uuid, 'Ksheenamai', 'Tyagaraja', 'Mukhāri', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/ksheenamai.htm', '66675b5236edb11db72c7da84ab01637096256fe54b86473bc20af4dc52930d9', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi two kalai', 'kshINamai tiruga janmincE
2 siddhi mAnurA O manasA'),
        ('5833cc94-ff83-47a8-8008-90dd847de096'::uuid, 'Rama Ninnae Nammi', 'Tyagaraja', 'Huseni', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1515.shtml', '02815c714b1e8ca7ab4557ecf3208517caf27d4439739d4c1df90e6567e2f608', 'Composition: raamaa ninnE - husEni; Talam: aadi', 'aadi', 'rAmA ninnE namminAnu nijamuga sItA (rAmA)'),
        ('5835ba5e-e094-4771-bb97-0b2463498899'::uuid, 'Sri Raghuvara Sugunaalaya', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2844.shtml', 'be15d888cef149931ba27ca81f222b1f37a9b8a041de677193573f4f9fcd5654', 'Composition: shree raghuvara suguNaalaya - bhairavi; Talam: aadi', 'aadi', 'tarAna lEni parAkul(E)Tiki birAna nanu brOvaga rAdA vAdA'),
        ('59c2ff1b-cde6-423a-9864-db4521acdff2'::uuid, 'Evaraina Leraa', 'Tyagaraja', 'Siddhasena', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2321.shtml', '88ed149f6e1d76fbc5fffe02e89b1bb40dcfb5aba63dd594aecba1d40f1d4152', 'Composition: evaraina lEraa - siddhasEna; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'evaraina lErA peddalu
ilalOna dInula 1 brOva'),
        ('5ab61c20-7312-4ad2-aa80-a94319535990'::uuid, 'Rama Namam Bhajarae', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2755.shtml', 'd7b8da5c1c8229d128e8445da62bb6129db179e80e992af61882fb40154867f1', 'Composition: raama naamam - madyamaavati; Talam: aadi', 'aadi', 'rAma nAmaM bhajarE mAnasa'),
        ('5adc1d1f-8a3d-446d-83f9-49aa1d8151da'::uuid, 'Sri Raghuvara Aprameya', 'Tyagaraja', 'Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2841.shtml', 'c497f9fce6126480ba3ff502553eb64e5d025192f7212df73a42cc7e3d251d52', 'Composition: shree raghuvara - kaambhOji; Talam: aadi', 'aadi', 'SrI raghuvar(A)pramEya mAm-ava'),
        ('5dd36d8e-38c7-4270-a810-d0523181f59d'::uuid, 'Anaathudanu', 'Tyagaraja', 'Jingla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1383.shtml', 'cca0e9c19d1e73e239232d9cfbe4bf49fcad15904178527fe1b84347453a7c9d', 'Composition: anaathuDanu gaanu - jingala; Talam: aadi', 'aadi', 'anAthuDanu kAnu rAma nEn(anAathu)'),
        ('5e240d77-d99b-46f1-8c96-6fb183bf9734'::uuid, 'koluvamarE kada', 'Tyagaraja', 'Hanumatodi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/koluvamaregada.htm', '17bc1a754176f0d246e9166646c61e9ec752fd8c72ef3d9538cda370ffebd185', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai)', 'koluv(a)mare kadA kOdaNDa pANi'),
        ('5e7ba410-e01f-400a-a009-4efd245e4f1d'::uuid, 'sItA pati kAvavayya', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2866.shtml', '1014318eb737a1a26a8f422e4426e825082cdf986189698380d43fc69eba0646', 'Composition: seetaapati kaavavayya - shankaraabharaNam; Talam: aadi', 'aadi', 'sItA pati kAva(v)ayya'),
        ('5ef0c733-46d4-406f-9113-47a41c6a9fbf'::uuid, 'niravadhi sukhada', 'Tyagaraja', 'ravi candirka', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1086.shtml', 'ccade71215857c3c558089ec16a1844b9290eb84cc7297cc77130b9ff131175c', 'Composition: niravadhi sukhada - ravi candrikaa; Talam: aadi', 'aadi', 'niravadhi sukhada nirmala rUpa
nirjita 1 muni SApa'),
        ('60064fdb-4bb7-45ff-afd4-e0ace0677e29'::uuid, 'Rama Rama Rama Laali', 'Tyagaraja', 'Sahāna', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2768.shtml', '218b87498a010148b5d8a4cf0f4f9a9c21998962f22d4e227caa9a4852a5a48e', 'Composition: raama raama raama - sahaanaa; Talam: caapu', 'caapu', 'rAma rAma rAma lAli SrI rAma
rAma rAma lAvaNya lAli'),
        ('604bbad4-7a8c-4935-9467-bf7db2d850d0'::uuid, 'Paahi Kalyaana Rama', 'Tyagaraja', 'Kāpi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2691.shtml', '56addece19a849c166b3a3e4b38045ef521e53df5d0c32fea453784068f8a176', 'Composition: paahi kalyaaNa raama - kaapi; Talam: aadi; Kapi / Adi (Prahlada Bhakti Vijayam)', 'aadi', 'pAhi kalyANa rAma pAvana guNa rAma'),
        ('60b8e375-123e-4625-8b53-6764df3dddb2'::uuid, 'Nammina Vaarini', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2582.shtml', '10735c7e4c8e4637eae72cf3cf2b02e5e67a0fe4a8e3e6810526874d24a5832e', 'Composition: nammina vaarini - bhairavi; Talam: aadi', 'aadi', 'nammina vArini maracEdi
nyAyamA rAma'),
        ('60e61938-4873-4e58-8ad1-47ec0448c0d2'::uuid, 'Meevalla Guna Dosha', 'Tyagaraja', 'Kāpi', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c1120.shtml', '9b61556e7d2065a8a56aa9a299fa515c52e0cac2b43611843561f535b5a3be62', 'Composition: meevalla guNadOshham - kaapi; Talam: jhampa', 'jhampa', 'mIvalla guNa dOsham(E)mi SrI rAma'),
        ('61205a3a-0ef2-43be-8008-970f7c64267b'::uuid, 'gata mOhASrita', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2336.shtml', 'e152729d53f903d35928586e01e9ea45fed2f3d53a7df600b6314b4f238dfcf6', 'Composition: gata mOhaa - shankaraabharaNam; Talam: aadi', 'aadi', 'gata mOh(A)Srita pAl( 1 A)dbhuta sItA ramaNa'),
        ('62af415e-cd50-4a02-8d86-8a5f39c24bc5'::uuid, 'Rama Rama Nee Vaaramu', 'Tyagaraja', 'Anandabhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2766.shtml', '607cbaf36627be8e76c01d8e6a4f7220e2fa05a04a93ae764a49338cfadd6185', 'Composition: raama raama neevaaramu - aananda bhairavi; Talam: aadi', 'aadi', 'rAma rAma nIvAramu gAmA rAma sItA
rAma rAma sAdhu jana prEma rArA'),
        ('63a07e3d-51b8-43b4-bfbe-2a4b8f44b397'::uuid, 'paluka kaNDa', 'Tyagaraja', 'Navarasa kannada', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2707.shtml', '3713dc9c98ba2a38fc15e057e682c9f7748e68793ed282e7544bea0045ef4a85', 'Composition: paluku kaNDa - navarasa kannaDa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'paluku kaNDa cakkeranu kErunE
1 paNatulAra jUDarE'),
        ('63b423b1-fb00-4135-b44c-80bc6f381d51'::uuid, 'akhilANDESvari raksha mAM', 'Muthuswami Dikshitar', 'Dwijavanthi', 'Adi', 'ibiblio.org/guruguha', 'https://www.ibiblio.org/guruguha/mdeng.pdf', 'c6d962e850f521c2f7ab28a8f9f49f047f58cb302be4107b13298c18efaf8dfa', 'Printed/PDF page 17, raga/tala header and pallavi visually checked; P. P. Narayanaswami compilation, August 2007; Jujavanti/Dwijavanthi spelling; Adi agrees with full Shivkumar notation page despite its index listing Rupaka', 'ādi', 'akhilANDESvari raksha mAM
Agama sampradAya nipuNE SrI'),
        ('65f4c7f0-6a74-4f20-9046-3bdd2c26351b'::uuid, 'endukI calamu', 'Tyagaraja', 'SankarAbharaNaM', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2288.shtml', '0d0c1e9a4cc738bdd1301baf4a0e1268b4966e4aed99b8acfa8ccf88fdec4d38', 'Composition: endukee calamu - shankaraabharaNam; Talam: tripuTa', 'tripuTa', 'enduk(I) 1 calamu nEn(e)varitO telpudu'),
        ('6625f250-6e03-4ab2-950a-17ab43dc696b'::uuid, 'Raksha Pettarae', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2743.shtml', 'ee6b6223bea1a9e238e4b20072c94f6b34091fc1a430b16124ddbff785ebeda4', 'Composition: raksha bettarE - bhairavi; Talam: aadi', 'aadi', 'vaksha sthalamuna velayu lakshmI ramaNuniki 2 sAya (raksha)'),
        ('6627d303-b808-42c5-ac2b-bd2dd9595d26'::uuid, 'Emani Pogaduduraa', 'Tyagaraja', 'Veeravasantham', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c30956.shtml', '6f53425c6906641b83ba492ac24880e107830057be8f94678369ee97181bac2a', 'Composition: Emani pogaDudurA shrI rAma ni - vIravasanta; Talam: Adi', 'Adi', 'Emani pogaDudurA SrI rAma ninn(Emani)'),
        ('67f0e37a-e1df-4d91-bfe3-47fc310c7abe'::uuid, 'Pakkala Nilabadi', 'Tyagaraja', 'Kharaharapriyā', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c1700.shtml', 'e35a08274d543909c084b5490ca1839e5fac8074500c197390430c19615dead4', 'Composition: pakkala nilabaDi - kharaharapriyaa; Talam: caapu', 'caapu', 'pakkala nilabaDi kolicE muccata
bAga telpa rAdA'),
        ('6929bbfd-0c31-4f9b-8994-395b08b522fb'::uuid, 'Patti Viduva', 'Tyagaraja', 'Manjari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2728.shtml', '63bdb389f7db4616ad3edcd6084c923f191970a2f20af9088f2151aed02b2c9d', 'Composition: paTTi viDuva - manjari; Talam: aadi', 'aadi', 'paTTi viDuva rAdu nA 1 ceyi
paTTi viDuva 2 rAdu'),
        ('6a33383f-2c7f-4c78-b86a-d2455e486456'::uuid, 'Videmu Seyavae', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2930.shtml', 'a97a4605ae71a144f896abc939b7b0dfb51443a40ab8999775ba414a8efca34b', 'Composition: viDamu sEyavE - kharaharapriyaa; Talam: aadi', 'aadi', 'puDami tanaya cEti manci 4 maDupul(a)nucu talaci talaci (viDemu)'),
        ('6b81b280-71fa-46cf-8dfd-261dca578466'::uuid, 'parAmukham', 'Syama Sastri', 'Mechakalyāni', 'Triputa', 'syamakrishnavaibhavam.blogspot.com', 'https://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-paraamukham-raga.html', '67bec861964302b2f9e2df45ba29b3085ebe9f5c06c555081f89c2ff0fd9b7e9', 'Opening composition description: kalyANi (tALa tripuTa; stored pallavi matches source', 'tripuTa', 'parAmukham-En-ammA pArvati(y)ammA'),
        ('6c9d0ab2-70a2-4c6b-a0df-c4a925bf8ae9'::uuid, 'Naada Tanumanisam', 'Tyagaraja', 'Chittaranjani', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/nadathanumanisham.htm', 'aee752b86552475dd1e5095284a28a2dbfb10dd32e7addc78eca533551e467e2', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'nAda tanum-aniSam Sankaram
namAmi mE manasA SirasA'),
        ('6dfbf308-eb7a-408f-b4ed-ca9168670204'::uuid, 'Paritaapamu', 'Tyagaraja', 'Manohari', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2723.shtml', '3959f7111ad9cb17219b17d84175b0dc6a7132bc522be4e3c85d9352dcfa8415', 'Composition: paritaapamu gani - manOhari; Talam: roopakam', 'roopakam', 'paritApamu kani(y)ADina
palukula maracitivO nA (pari)'),
        ('6e50e23d-f17a-4ac7-82e6-c93b12b489d9'::uuid, 'akhilANDESvarO rakshatu', 'Muthuswami Dikshitar', 'Suddha Sāveri', 'Rupaka', 'ibiblio.org/guruguha', 'https://www.ibiblio.org/guruguha/mdeng.pdf', 'c6d962e850f521c2f7ab28a8f9f49f047f58cb302be4107b13298c18efaf8dfa', 'Printed/PDF page 19, raga/tala header and pallavi visually checked; P. P. Narayanaswami compilation, August 2007; source Suddhasaveri (1), exact stored incipit; no change to raga hierarchy or modern/traditional naming', 'rūpakam', 'akhilANDESvarO rakshatu mAM
hari hayAdi pUjitassatatam'),
        ('6f1b9810-3b06-4d1f-88fb-d1f030844691'::uuid, 'Inta Saukhyamani', 'Tyagaraja', 'Kāpi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1612.shtml', '8ad8dc37bc7c951234a479d08c34e46aa14c48dc0f1dde31cd7277d8fc0a285f', 'Composition: inta sowkhyamani nE - kaapi; Talam: aadi', 'aadi', 'dAnta sItA kAnta karuNA svAnta prEm(A)dulakE telusunu kAni'),
        ('72407711-358e-4764-a291-15c7fbbb90ad'::uuid, 'ramA ramaNa rArA', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2783.shtml', 'd42db12d60d70ba99f679bfaf2fdc50c5be27d75f7416391fe63d262ba73d68e', 'Composition: ramaaramaNa raaraa - shankaraabharaNam; Talam: aadi', 'aadi', '1 ramA ramaNa rArA O (ramA)'),
        ('73116a78-3416-4583-b4ff-7c9223110062'::uuid, 'mATi mATiki', 'Tyagaraja', 'Mohanam', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2557.shtml', '136692f0c59a41130c858713d0b246971269bb7e1a19c16e5201ff137295b463', 'Composition: maaTi maaTiki - mOhanam; Talam: caapu', 'caapu', 'mATi mATiki telpa valenA muni
mAnas(A)rcita caraNa rAmayya nItO'),
        ('73b44194-00c1-4753-8c8a-924d9fa3e18b'::uuid, 'svara rAga sudhA', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/swararagasudha.htm', '79df1d5ffbe46ad1b1c39a1e6b6685a894c0d4a560b5795d2795b390468c9b38', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai)', 'svara rAga sudhA rasa yuta bhakti
svarg( 1 A)pavargamurA O manasA'),
        ('73f878bd-d72c-4c56-9c1b-c6000006e4eb'::uuid, 'daya rAni', 'Tyagaraja', 'Mohanam', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2254.shtml', 'e26c0d2717cbbc0c7a5facd992d1c9a8716efd26351d3f96c21a75c0dbf13c37', 'Composition: dayaraanee dayaraanee - mOhanam; Talam: aadi', 'aadi', 'daya rAni daya rAni dASarathI rAma'),
        ('75093099-55f6-4e74-885b-25a5e961f4ae'::uuid, 'ika kAvalasinadEmi', 'Tyagaraja', 'Bālahamsa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2349.shtml', 'b9197def3cd4f615c415abc602bf00a2ed8ff4c93c763521ee11da42ef71d361', 'Composition: ika kaavalasinadEmi - baalahamsa; Talam: aadi', 'aadi', 'ika kAvalasinad(E)mi manasA
sukhamunan(u)NDavad(E)mi'),
        ('7570235e-b9cf-4060-a9cf-b400716ca3ae'::uuid, 'Indukaa Puttinchitivi', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2352.shtml', '957fe6f84cafa13999bb4e85bb688b13bd16b9462528c1ae03d2c1a0f2cdbd2d', 'Composition: indukaa puTTincitivi - bhairavi; Talam: aadi', 'aadi', 'induka puTTincitivi nann(indukA)'),
        ('76025d5f-aa93-41a2-a9c4-8169981a2f79'::uuid, 'Karunaa Jaladhi', 'Tyagaraja', 'kEdAra gauLa', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2396.shtml', '1b08c89697530848d0ef5396e011363d86c72200bd92424e7b1a34e761e12843', 'Composition: karuNaa jaladhee - kEdaara gowLa; Talam: caapu', 'caapu', 'karuNA jaladhi 1 dASarathi
2 kamanIya suguNa nidhi'),
        ('760be4bc-5b3a-430d-9969-4835c43aac74'::uuid, 'Evari Maata', 'Tyagaraja', 'Kāmbhoji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/evarimaata.htm', 'e47568571b83b3cbebc5c859d52701e5838bbae9bde173099f2609447e882ad6', 'Talam header and Pallavi; manually verified spelling Kambodhi = Kāmbhoji; composer and incipit matched', 'Adi (2 kalai)', 'evari mATa vinnAvO rAvO
indu lEvO 1 bhaLi bhaLi'),
        ('76706063-efdd-49a7-b091-4817afed5b57'::uuid, 'Sri Narada Muni', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2443.shtml', 'f9e0e38f62a79aec6bc19bf84d6242e388501c6cdafe36b5157b7186b2c86849', 'Composition: shree naarada gururaaya - bhairavi; Talam: aadi', 'aadi', 'SrI nArada muni guru 1 rAya kaNTi-m(E) nATi tapamO guru rAya'),
        ('76af1dff-ea85-4687-8238-27af5ac9dbc6'::uuid, 'Mucchata', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2565.shtml', '5734a215e448a6bfaf093bb96a9bdc9e9e82a222943fee6709650e26b0e93247', 'Composition: muccaTa brahmaadulaku - madyamaavati; Talam: aadi', 'aadi', 'muccaTa brahm(A)dulaku dorakunA
1 muditalAra jUtAmu rArE'),
        ('77d3a9de-411c-4c6b-b285-026d19d0bf27'::uuid, 'Sogasugaa Mridanga', 'Tyagaraja', 'Ranjani', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2870.shtml', 'd9133c668f3d92766723ee856507f621746cba884690d5d3b9fdabbb19389ee0', 'Composition: sogasugaa mridanga - shree ranjani; Talam: roopakam', 'roopakam', 'sogasugA mRdanga tALamu jata kUrci ninu
sokka jEyu dhIruD(e)vvaDO'),
        ('77e3a868-f451-4028-bc8f-ff9fb696ef15'::uuid, 'Nee Bhajana', 'Tyagaraja', 'Nāyaki', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2645.shtml', 'eed964eb0adefd7929c133e861925260c024ddd7e5dcbe5fac17f3111f4be8e6', 'Composition: nee bhajana - naayaki; Talam: aadi', 'aadi', 'nI 1 bhajana gAna rasikula
nEn(e)ndu kAnarA rAma'),
        ('78b86c5a-7c6e-4797-a4ab-0be561979065'::uuid, 'Vandanamu', 'Tyagaraja', 'Sahāna', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1450.shtml', 'a341f74ae013ebb5bf180ba4b0415a12e84b791055adf7551e65bc94ff7f06a0', 'Composition: vandanamu raghunandana - sahaanaa; Talam: aadi', 'aadi', 'vandanamu raghu nandana sEtu
bandhana bhakta 1 candana rAma'),
        ('7914b699-b204-42b1-9057-72f4ff5db6d4'::uuid, 'Mahita Pravrddha', 'Tyagaraja', 'Kāmbhoji', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2537.shtml', 'aeb9290e2b60e5b515455f599a0f8a7aa2e23667fc91b892826368e6e8ef0063', 'Composition: mahita pravrddha - kaambhOji; Talam: caapu', 'caapu', 'mahita pravRddha SrImati
guha gaNa pati janani'),
        ('79c5ac50-423e-4f35-81e0-dd68e289c63f'::uuid, 'bhakti biccam', 'Tyagaraja', 'SankarAbharaNaM', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/bhakthibiccha.htm', 'f8f39e0b5706f194e0fa10d96a3179b893791cd72d3783a1cf4cb687819aeeaf', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', '1 bhakti 2 biccam(i)yyavE
bhAvukamagu 3 sAtvIka (bha)'),
        ('7a163c9f-1b45-410c-858e-6388391da0f4'::uuid, 'Ela Teliya Lero', 'Tyagaraja', 'Darbar', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2273.shtml', '39f20133dbac3caacc0ee6d91d0bf03b980c3c58a06767a28315d3f041f4101a', 'Composition: Ela teliyalErO - darbaar; Talam: caapu', 'caapu', 'bAla SaS(A)nka 3 kal(A)lankRta nuta nIla varNa suguN(A)laya'),
        ('7ab08ebb-7d65-48c7-93cb-152d9491283b'::uuid, 'Vaarija Nayana-2', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2921.shtml', '26ec69503778841fef6e1d374f8b50f73954f048a13284ca8e7f9d44a6e0c243', 'Composition: vaarija nayana - kEdaara gowLa; Talam: aadi', 'aadi', 'vArija nayana nIvADanu nEnu
vAramu nannu brOvumu'),
        ('7ac1ec39-56e0-4c84-9ea1-3e55dc7569b2'::uuid, 'O rAma O rAma', 'Tyagaraja', 'Ārabhi', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-o-rama-o-rama-raga.html', 'a2351df694a3e63e3111827e63cb582646c3704a846b8766e5c3e1a70da8cd07', 'Opening composition description and Latin pallavi; composer and raga verified', 'Adi', 'O rAma O rAma OMkAra dhAma
O rAma O rAma onarincu prEma'),
        ('7ad8d15d-8260-46b7-bc02-2ef448f61842'::uuid, 'Badalika Teera', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1393.shtml', 'b772cefbad53b57a74b792a223469e1f5e3d24adba210d0bc2c6244114504d29', 'Composition: baDalika deera - reeti gowLa; Talam: aadi', 'aadi', 'baDalika tIra pavvaLincavE'),
        ('7ae3db13-a2b8-4ab5-a944-30906c1b6fed'::uuid, 'innALLu daya', 'Tyagaraja', 'nArAyaNa gauLa', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2358.shtml', '8652c3c9be28046a10242ee463596a8dd657f995c1244761a3cf18cbe9c701ff', 'Composition: inn aaLLu - naaraayaNa gowLa; Talam: caapu', 'caapu', 'innALLu 1 daya rAk(u)nna vainam(E)mi
ipuDaina telupa(v)ayya'),
        ('7c4ff154-ff0d-4a4a-b261-6d9a5122b0bc'::uuid, 'Edi Nee Baahu Bala', 'Tyagaraja', 'Darbar', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2270.shtml', '8f89ab87ae1cbae26d6abe39cbb38950005bc010ad13a3dd197043971bfb93f5', 'Composition: Edi nee baahu - darbaar; Talam: aadi', 'aadi', 'karamuna 1 merayu Sara cApamu Akali kona lEdA'),
        ('7e410ccb-66d7-48f4-adc2-2102b12681ad'::uuid, 'Laali Laalayya', 'Tyagaraja', 'kEdAra gauLa', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2524.shtml', '02ddc86ef85eae9cceb8cabca1f336c828c76c0af279e33ed758cf9238569128', 'Composition: laali laalayya - kEdaara gowLa; Talam: jhampa', 'jhampa', 'lAli lAlayya lAli'),
        ('80528ba0-42c8-4b50-b65f-b1771996f18b'::uuid, 'Nija Marmamula', 'Tyagaraja', 'Umābharanam', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2658.shtml', 'c3fc29220b0653f4ac744b374c8d6500aa0cfca464f3a25cef9c5feac33ddaf8', 'Composition: nija marmamulanu - umaabharaNam; Talam: aadi', 'aadi', 'nija marmamulanu telisina vArini
nIv(a)layincEd( 1 E)makO 2 rAma'),
        ('8176b9b9-321e-4d81-adb9-f2549f32e2ae'::uuid, 'Sri Rama Sri Rama Sri Manoharamaa', 'Tyagaraja', 'Sahāna', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2856.shtml', '53f7bf95487959531ccf6378a853f4cc74139a4fb274c73c220a44479fafa459', 'Composition: shree raama shree raama shree - sahaanaa; Talam: aadi', 'aadi', 'SrI rAma SrI rAma SrI manO-haramA'),
        ('823cf8d7-f2e9-4a9d-bedd-506dffd550c1'::uuid, 'Aada Modi Galadae', 'Tyagaraja', 'Chārukesi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1359.shtml', '459e07c9189696926047c08d63c729a919d424cf21c6a7384f56900ca3e354ab', 'Composition: aaDamODi galadE - caarukEshi; Talam: aadi', 'aadi', 'ADa mODi 1 galadE rAm(a)yya mATal-(ADa)'),
        ('827483ca-f61e-453e-b719-dba2f7eaf2db'::uuid, 'Sukhiyevaro', 'Tyagaraja', 'Kanadā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1339.shtml', 'fd5f75c3f9d7fbc4c2ea0de55e6ea0cc1f31337d049b74716c3f7e1af92cc775', 'Composition: sukhi evarO - kaanaDaa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sukhi(y)evarO rAma nAma sukhi(y)evarO'),
        ('82dd8598-5a91-4ffc-b26b-e4800a18d4b9'::uuid, 'Endundi Vedalitivo', 'Tyagaraja', 'Darbar', 'Misra Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/endundi.htm', '13c503c96233e3acd7e3bdfcdc7bc38d6fe7309874cbc6b437a91e0ab286dee4', 'Talam header and Pallavi; manually verified spelling Durbar = Darbar; composer and incipit matched', 'Misra Chapu', 'enduNDi veDalitivO E(v)UrO nE teliya
ipuDaina telupa(v)ayya'),
        ('848fe8eb-0b6e-423d-9b37-f0ce62ceffa5'::uuid, 'rAma rAma rAma rAma', 'Tyagaraja', 'cencuruTi', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c31327.shtml', '69afabd53f86d360216ff222525a3255de3f51699d33b6b0818441a94bbaedbc', 'Composition: rAma rAma rAma rArA - cenjuruTi; Talam: rUpaka', 'rUpaka', 'rAma rAma rAma rAma rArA sItA rAma'),
        ('85833460-8549-4669-819e-b03c923b0b9d'::uuid, 'Rama Raghu Kula', 'Tyagaraja', 'Kāpi', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2764.shtml', '7f0ca9b86c635dc413e5f00bba465396f62f317f558deb63fa55d656e6f7a3cd', 'Composition: raama raghukula - kaapi; Talam: roopakam', 'roopakam', 'rAma raghu kula jala nidhi
sOma lOk(A)bhirAma'),
        ('8788331c-a2c8-48a2-98d3-dac53ea56935'::uuid, 'nanu pAlimpa', 'Tyagaraja', 'Mohanam', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/nannupAlimpa.pdf', 'c80f7623ebd9744aa4a74d547c28387ed04f2ed0393e39c7cd8fad1defe93e68', 'PDF title Nannu PAlimpa - Mohanam - Adi (2 kalai) - Tyagaraja; HTML notation URL returns 404', 'Adi (2 kalai)', 'nanu pAlimpa naDaci vaccitivO
nA prANa 1 nAtha'),
        ('87f6024e-4c43-4fca-a81d-8a192925843b'::uuid, 'Evaricchiriraa', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2322.shtml', 'c6fd5834a611092cdcdf95632cfefe535262c0255e30973495568a5ab7a5d183', 'Composition: evaricciriraa - madyamaavati; Talam: aadi', 'aadi', 'evar(i)ccirirA Sara 1 cApamulu nIk-
(i)na kul(A)bdhi candra'),
        ('882c4c90-15d7-4b65-b52f-48515600bb63'::uuid, 'kadaluvADu', 'Tyagaraja', 'nArAyaNa gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2378.shtml', '4af956a000d82577de68df177901c494278c69e1b4d720f80d3a9ba6ef60cc20', 'Composition: kadalE vaaDu gaadE - naaraayaNa gowLa; Talam: aadi', 'aadi', '1 kadaluvADu kADE rAmuDu
kathal(e)nnO kalavADE'),
        ('887bed65-9c5c-4fca-a7f8-4ac576cd1ee3'::uuid, 'Sujana Jeevana', 'Tyagaraja', 'Kamās', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/sujanajeevana.htm', '93fc142c662bac5147530c40a6bc84b4a9cf9dd3556f8d6ea98cfff5494e4044', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'sujana jIvana rAma suguNa 1 bhUshaNa rAma'),
        ('89ada848-7385-437c-956f-7ae855ceeb39'::uuid, 'Samukhaana Nilva', 'Tyagaraja', 'Kokilavarāli', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2516.shtml', 'dece94e66981651d42390567d36383b2e1ea44a176cb3e2aecdccd53de8d1d63', 'Composition: samukhaana nilva - kOkilavaraaLi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'samukhAna nilva kalgunA kamal(A)nana'),
        ('8a3e248e-3e40-4eaf-9c77-8f2e4bec6a70'::uuid, 'Kali Narulaku', 'Tyagaraja', 'Kunthalavarāli', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2383.shtml', '426648d46c8d245cf39127a48cf320016944d9b1e1b33cb7c0c0d04fad4f2737', 'Composition: kalinarulaku - kuntala varaaLi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'kali narulaku mahimalu
telipi(y)Emi phalam(a)na lEdA'),
        ('8b489e84-f7a4-4453-a698-6e992d1c8ff1'::uuid, 'ipuDaina nanu', 'Tyagaraja', 'Ārabhi', 'Misra Capu', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-ipudaina-nanu-raga.html', 'b2f7f2e596e52a505d1e680da4f5510734c56d49c773b3033e85d6cec2fd4e18', 'Opening composition description and Latin pallavi; composer and raga verified', 'miSra cApu', '1 ipuDaina nanu talacinArA svAmi
2 kRpaku pAtruDan(a)ni kIrtincinArA'),
        ('8c33d9b4-30bf-4e2e-a847-84926136a4d6'::uuid, 'Varada Raja', 'Tyagaraja', 'Swarabhooshani', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2916.shtml', '172c56a84700b02bf84cbc56988205cf661d2faaadb6c35811ece94efde1b593', 'Composition: varadaraaja ninnu - swarabhooshani; Talam: roopakam', 'roopakam', 'varada rAja ninu kOri
vacciti mrokkErA'),
        ('8c4d0b7b-9488-4225-8b97-f8549283d929'::uuid, 'Kaarubaaru', 'Tyagaraja', 'Mukhāri', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/karubaru.htm', '1aacc67223d6ada1a408784fcd0370ce43de39faa08d68362c02eb4be22c13b7', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai )', 'kArubAru sEyuvAru
1 galarE nIvale sAkEta nagarini'),
        ('8c55c921-a589-49e0-bc1d-546105d51451'::uuid, 'manasu vishaya', 'Tyagaraja', 'nATa kuranji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2546.shtml', '52cf1ed064a1d834e6bdf54a9eb5c9fb5965b342de404570419a11dc4b914599', 'Composition: manasu vishhaya - naaTTai kurinji; Talam: aadi', 'aadi', '1 manasu vishaya naTa viTulak(o)sangitE
mA rAmuni kRpa kalugunO manasA'),
        ('8c967ef0-a727-44c6-a4a6-cd4ece4abf83'::uuid, 'Nitya Roopa', 'Tyagaraja', 'Kāpi', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2670.shtml', '0e931c59436d40a733d172508d010348b055975a0b0545d5ab5b9111af3f08d1', 'Composition: nitya roopa - darbaar; Talam: roopakam', 'roopakam', 'nitya rUpa evari pANDityam(E)mi naDucurA'),
        ('8d1452e5-ff2d-4bb3-998e-3f892fcfc1d8'::uuid, 'Naati Maata', 'Tyagaraja', 'Devakriya', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2593.shtml', 'd7bc9b0362e144ac13e728ac37c9dada63f541ffb7ef6a4924ca062b61a5af40', 'Composition: naaTi maaTa - dEvakriya; Talam: aadi', 'aadi', 'mATi mATiki nApai 2 mannana jEyucu ETiki yOcana I bhAgyamu'),
        ('8dbbd152-4b44-4f65-9ac1-12e859634911'::uuid, 'Endu Kaugalinturaa', 'Tyagaraja', 'Shuddha Desi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2287.shtml', 'fdb5109fd98084f03c18abd95b73ad019e0b10d9f24148c65c05c36fbc6870f9', 'Composition: endu kowgalinturaa - sudda dEsi; Talam: aadi', 'aadi', 'endu 1 kaugalinturA ninn-
(e)ntani varNinturA ninn(endu)'),
        ('8e3b3314-6895-48e7-b5fc-f82d6f2e06b0'::uuid, 'Manavinaalakincha', 'Tyagaraja', 'Nalinakānthi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/manavyala.htm', '272b4e1d7c2192f703440de28502df70873c3c2838f340d7fa743cd8a64a7ea4', 'Notation header Talam: dEshAdi, stored as Adi; raga Nalinakanti; pallavi manavyalakinca radate', 'dEshAdi', 'manavin(A)lakinca 2 rAdaTE
marmam(e)lla telpedanE manasA'),
        ('8ea31119-0764-4ab4-8f99-92264ba34d3f'::uuid, 'varadA navanItASa', 'Tyagaraja', 'rAga panjaraM', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2915.shtml', 'daa77a275e4ce3647481cc745c3e32180a1569a9c148f28d5eee44aa5f7c9a09', 'Composition: varada navaneeta - raagapanjaramu; Talam: tripuTa', 'tripuTa', 'varadA 1 navanIt(A)Sa pAhi
vara dAnava mada 2 nASa Ehi'),
        ('8f054e71-b43a-45c7-8d46-550d6bff56d0'::uuid, 'Vara Sikhi Vaahana', 'Tyagaraja', 'Supradipama', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2919.shtml', '92738bd273a9f6125a88828dfd1bac1497b0997ad0871b496c6269af02415da3', 'Composition: varashikhi vaahana - supradeepam; Talam: aadi', 'aadi', 'vara Sikhi vAhana vArija lOcana
kuru Sam tanu jita 1 kusuma Sar(A)yuta'),
        ('8f631948-3af9-471c-9ca7-499daad07c76'::uuid, 'bhajana sEyu mArgamu', 'Tyagaraja', 'Nārāyani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1399.shtml', '337272de4bb7cef7763a4eaa1390d0b492627f29503684821a6c0dc645a3bcf7', 'Composition: bhajana sEyu margamunu - naaraayaNi; Talam: aadi', 'aadi', 'bhajana sEyu mArgamunu jUpavE parama
bhAgavata bhAgadhEya sad-(bhajana)'),
        ('902b62c9-a743-45e9-b206-62e56a3624f5'::uuid, 'ramincuvArevarurA', 'Tyagaraja', 'Suposhini', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1724.shtml', 'da34642794597dc9429cf7733c4a114f54130b94a6220afe9b9cad0c3c948701', 'Composition: raamincu vaarevaruraa - supOshini; Talam: roopakam', 'roopakam', 'ramincuvAr(e)varurA
ragh(U)ttamA ninu vinA'),
        ('906547be-a4ff-41c2-9953-ea7284b7899f'::uuid, 'Enduku Nirdaya', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/endukunirdaya.htm', '45daf206c819fdb5b8e0e0f6091fda105bcda080e50d37602a37b805c0b9eb8f', 'Talam header and Pallavi; manually verified spelling Hari Kambodhi = Harikāmbhōji; composer and incipit matched', 'Adi', 'enduku nirdaya evar(u)nnArurA'),
        ('907ed608-a27c-447b-9f71-a4e75af7eda4'::uuid, 'Kaligiyunte Kadaa', 'Tyagaraja', 'Kīravāṇi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/kaligiyunte-new.htm', 'bd9aa71179677d89befd488794d7bda85b9169b0fb5fdfdd63f1a5c389cd216a', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai)', 'kaligiyuNTE kadA kalgunu
kAmita phala dAyaka'),
        ('90e3cbc4-3907-4c15-baac-732f8f232479'::uuid, 'SAntamu lEka', 'Tyagaraja', 'sAma', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2825.shtml', 'afd06fa495dd93be272dc64104ca67030ca8c05dbcd200ee7142581446c2baa8', 'Composition: shaantamu lEka - shyaamaa; Talam: aadi', 'aadi', 'SAntamu lEka saukhyamu lEdu
sArasa daLa nayana'),
        ('91213a3a-2fd2-4770-8b40-9c683dbf8539'::uuid, 'Mari Mari Ninnae', 'Tyagaraja', 'Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2553.shtml', 'fa3210b75f85da22eebc87a23e07d0170184e9fede78586df4016bb119576fc4', 'Composition: mari mari ninnE - kaambhOji; Talam: aadi', 'aadi', 'mari mari ninnE moraliDa nI
manasuna daya rAdu'),
        ('91bb583a-c037-44c6-b078-af6d214be16e'::uuid, 'vErevvarE gati', 'Tyagaraja', 'suraTi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2928.shtml', '27fe93a3eb4843f1d71ca5e1a2b64ebbf8cbb0cf2ccf11c864e5bb66b4351f46', 'Composition: vErevvarE gati - shuruTTi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'vEr(e)vvarE gati 1 vEmArulaku sItA pati'),
        ('93c3fa2f-7a78-4b85-a56c-3a6d33dd1aa9'::uuid, 'Enta Raani', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/entaranitana.htm', 'ce149b6399faafdc9d996430f76e4548245e124ae9391dc7fd294f12b0e732c0', 'Talam header and Pallavi; manually verified spelling Hari Kambodhi = Harikāmbhōji; composer and incipit matched', 'Adi', 'enta rAni tanak(e)nta pOni nI
2 centa 3 viDuva jAla SrI rAma'),
        ('940e158a-ef4d-4ed5-bd0d-a9d1db08e564'::uuid, 'Nee Daya Kalgute', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2651.shtml', 'e02291d63e861360907351c2fde715f7035a0c368e62a85e5cdb5e2476f61356', 'Composition: nee daya galguTE - reeti gowLa; Talam: aadi', 'aadi', 'nI daya kalguTE bhAgyam(a)ni
nijamugan(E)la tOcadO'),
        ('947faba5-85b3-40ff-8d98-0c2a2eea9edc'::uuid, 'Paripaalaya Paripaalaya', 'Tyagaraja', 'Reethigowla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2720.shtml', 'a391b80c99f1cffe95055738271a4300eedac5c994ef1a315394eb664d440d27', 'Composition: paripaalaya paripaalaya - reeti gowLa; Talam: aadi', 'aadi', 'tanuvE nIk(a)nuvaina sadanamaurA raghu nAtha (pari)'),
        ('95d12cdb-7527-4fb7-b405-9bce7fe30ae6'::uuid, 'Manasu Nilpa', 'Tyagaraja', 'Abhogi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2451.shtml', '1f6549cab348e2d917c68c1e63db8f9d23b5aa0d4a2b033c73aa2e80fd477a1c', 'Composition: manasu nilpa - aabhOgi; Talam: aadi', 'aadi', 'manasu nilpa Sakti lEka pOtE
madhura ghaNTa virula 1 pUj(E)mi jEyunu'),
        ('96dda707-dc3a-4422-879c-026ff2156df4'::uuid, 'Emani Vegintunae', 'Tyagaraja', 'Huseni', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2280.shtml', '81e832ec544d9baccf3f700475453b52b9182001b51ad7f615ace44bffe65944', 'Composition: Emani vEgintunE - husEni; Talam: aadi', 'aadi', 'Emani 1 vEgintunE SrI rAma rAma'),
        ('97d3a36f-e933-4e17-81da-47907fb1a4fe'::uuid, 'Rama Neeyeda', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1445.shtml', '570ef4bd84bf18ccdca268b2ee0237314db13907c9b9b434e9acd9358b8a6389', 'Composition: raama neeyeDa - kharaharapriyaa; Talam: madhyaadi', 'madhyaadi', 'kAmini vEsha dhAriki sAdhvI naDatal-(E)maina telusunA A rIti'),
        ('99896a62-e18f-4879-a71a-f051027af8e5'::uuid, 'nA moralanu', 'Tyagaraja', 'Ārabhi', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-na-moralanu-raga.html', '3edd3ebc740279b239af10e4ea75bce82c8ed30f9c55e31eb370cc1d873c58d2', 'Opening composition description and Latin pallavi; composer and raga verified', 'Adi', 'nA moralanu vini EmaravalenA
pAmara manujulalO O rAma'),
        ('9bab5692-0510-485b-a8b2-774563963747'::uuid, 'ninnE nera namminAnu', 'Tyagaraja', 'Ārabhi', 'Misra Capu', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-ninne-nera-namminanu.html', '252e14df289b821d218b3f8aa1ed0656c89115b78fef670a9f974e2df8ea61f0', 'Opening composition description and Latin pallavi; composer and raga verified', 'miSra cApu', 'ninnE nera namminAnu
nIraj(A)ksha nanu brOvumu'),
        ('9ccd7f15-0aac-4df4-8057-c9868e7a3fad'::uuid, 'O Rama Rama Sarvonnata', 'Tyagaraja', 'Nāgagāndhāri', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2683.shtml', 'fda291121baaf4e10e0716e6494857a1943418b491e3ca1f7477844f1a4837b9', 'Composition: O raama raama - naagagaandhaari; Talam: caapu', 'caapu', 'vEda Siramul(e)llan(A)daraNatO nIvE daivam(a)ni nammaga nammi'),
        ('9d3cb035-32c2-4c5c-96fc-64cdd92320ea'::uuid, 'rAma ninu vinA', 'Tyagaraja', 'SankarAbharaNaM', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2759.shtml', '12e79994665e992375fb025dfc757fb6402fd11ce11635bb2d56d045f4a8aaaf', 'Composition: raama ninnu vinaa - shankaraabharaNam; Talam: roopakam', 'roopakam', 'rAma ninu vinA nanu
rakshimpan(o)rula kAna'),
        ('9d588414-4b01-4651-a98a-dd30b3b0dae8'::uuid, 'Rama Kathaa', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/ramakathasudha.htm', '0ccdc03fbd89da520b679777a9286755e32fff770af62836fc1d24394516b2b9', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai)', 'rAma kathA sudhA rasa pAnam(o)ka
rAjyamu jEsunE'),
        ('9e06c13c-2bbb-47a7-b580-1de7365754cc'::uuid, 'heccarikagA', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c1290.shtml', 'c25bc12653eaf64ab4ccbed86254efa06ff1e30bf6abddbc3cc5c0c9168d6b05', 'Composition: heccarikagaa - yadukula kaambhOji; Talam: jhampa', 'jhampa', 'heccarikagA rArA hE rAma candra
heccarikagA rArA hE suguNa sAndra'),
        ('9e76c325-2670-4a57-a575-492b6b68d9f0'::uuid, 'Raghu Nandana Raghu Nandana', 'Tyagaraja', 'kEdAra gauLa', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2736.shtml', 'f06ec54b0c1382bf5217855e1a7076b421020599aa5818c3406072d1608430ed', 'Composition: raghunandana raghunandana - kEdaara gowLa; Talam: roopakam', 'roopakam', 'raghu nandana raghu nandana raghu nandana rAma'),
        ('9e77e997-acfc-4e43-b743-52c0164dbfce'::uuid, 'Abhimaanamennadu', 'Tyagaraja', 'Kunjari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1356.shtml', 'f7bace63702cf57dda90ae788c2363e506a9a1f315a5db2212a8819fc4ad0877', 'Composition: abhimaanamennaDu galguraa - vivardhani; Talam: aadi', 'aadi', 'abhimAnam(e)nnaDu kalgurA
anAthuDaina nAdupai nIku'),
        ('9fe198f5-d71b-46a6-a5f7-14245b32d29d'::uuid, 'Nenaruncharaa Naapaini', 'Tyagaraja', 'Simhavāhini', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2597.shtml', 'dc77f18d783be6c93154fc99f1d4a4aaebbe4765fc488456ebe153eafa8cad02', 'Composition: nenaruncaraa naapaini - simhavaahini; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nenar(u)ncarA nApaini cAla
nI dAsuDanu kAdA SrI rAma'),
        ('a123a98d-dce2-4e9e-b2d2-d7627d33b034'::uuid, 'koniyADE', 'Tyagaraja', 'Kokiladhwani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2406.shtml', '407901fdb8c485bd28dc78990f97ceb04218ac77ecf8d2f791e6f057e1e6c90c', 'Composition: koniyaaDEDu - kOkiladhwani; Talam: aadi', 'aadi', '1 koniyADE nAyeDa 2 daya velaku-
koniyADEvu sumI rAma ninu (koni)'),
        ('a3f21cde-4cc7-444c-a493-69704c02e447'::uuid, 'Saamaja Vara Gamana', 'Tyagaraja', 'Hindolam', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/samajavaragamana.htm', '73bc3881acf022f23441c5f346201c8cdaae7b8258fd165b4744edd64c9b2a35', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'sAmaja vara gamana sAdhu hRt-
sAras(A)bja pAla kAl(A)tIta vikhyAta'),
        ('a42c4225-9283-4261-aa28-20f535ee7f89'::uuid, 'Chinna Naade', 'Tyagaraja', 'Kalānidhi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2243.shtml', '2969ebc7639c31e55b420b48d54f087567bf8e9debb63b9698ecce408295ba3f', 'Composition: cinna naaDE - kalaanidhi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'cinna nADE nA 1 ceyi paTTitivE'),
        ('a461999c-05bc-4910-8b2c-afde25719ea5'::uuid, 'Rama Bhakti', 'Tyagaraja', 'Suddha Bangāla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1066.shtml', '6cf980e7d64968ffed73bf8ef0ac92975bd2392068fe75354f915d701cbb9d1f', 'Composition: raama bhakti saamraajyam - sudda bangaaLa; Talam: aadi', 'aadi', 'rAma bhakti sAmrAjyam(E)
mAnavulak(a)bbEnO manasA'),
        ('a4e7ca76-bbc4-4bc5-bbb4-bc83bc17e805'::uuid, 'Vinataa Suta Raaraa', 'Tyagaraja', 'Huseni', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2936.shtml', '8040c2c53af089557967f0f2705a933a6db82ae8e7408d2f6c457d712442a49d', 'Composition: vinataa suta raaraa - husEni; Talam: aadi', 'aadi', 'vinatA suta rArA nA vinuti 1 gaikonarA'),
        ('a5078f61-fd1b-43ac-9404-4d3d3d9db598'::uuid, 'kRpa jUcuTaku', 'Tyagaraja', 'Chāyatārangini', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2409.shtml', 'e2bae851cab70b5423d53b69ae38ffee265ee62e35245e7c399b3d829ab07e8a', 'Composition: krpa joocuTaku - caayataarangini; Talam: aadi', 'aadi', 'kRpa jUcuTaku vELarA rAma'),
        ('a5535815-c812-4a87-b99b-60e2ff956c64'::uuid, 'Raghu Nandana Raaja', 'Tyagaraja', 'Shuddha Desi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2737.shtml', '0489fa2cb3ade02d0e847955f8f4a4fba94e74402fb1924fb416a1224feae427', 'Composition: raghunandana raajamOhana - sudda dEsi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'raghu nandana 1 rAja mOhana
ramiyimpavE nA manasuna'),
        ('a5d69bd6-cb2f-40e6-9007-75e1c57259e6'::uuid, 'Rama Nee Samaanamu', 'Tyagaraja', 'Kharaharapriyā', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/ramaneesamana.htm', '98c2418875a9047db4e7789dc3fe69020dbbf0227baf3fb80b3884b204a4a79e', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'rAma nI samAnam(e)varu
raghu vamS(O)ddhAraka'),
        ('a6f42c55-bb6b-449d-94f9-cfa032bcb288'::uuid, 'Vara Raaga Laya', 'Tyagaraja', 'Cencukambhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2914.shtml', 'ddc79d6fc71ce96e668fc63ac0be6948fe9771ac10bcd5fc063a4e9444a63ba3', 'Composition: vara raaga - shencukaambhOji; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'vara rAga layajnulu tAm(a)nucu
1 vadarEr(a)yya'),
        ('a70de925-9076-47ab-b473-0b8a5056ff40'::uuid, 'sArvabhauma', 'Tyagaraja', 'rAga panjaraM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2409.shtml', 'e2bae851cab70b5423d53b69ae38ffee265ee62e35245e7c399b3d829ab07e8a', 'Composition: saarvabhowma saakETa - raagapanjaramu; Talam: aadi', 'aadi', 'sArvabhauma sAkEta rAma
manasAra palka rAdA dEvatA (sArva)'),
        ('a72f45b9-a0d0-48ae-81b5-ea33308cdeb6'::uuid, 'akhilANDESvaryai namastE', 'Muthuswami Dikshitar', 'Ārabhi', 'Adi', 'ibiblio.org/guruguha', 'https://www.ibiblio.org/guruguha/mdeng.pdf', 'c6d962e850f521c2f7ab28a8f9f49f047f58cb302be4107b13298c18efaf8dfa', 'Printed/PDF page 18, raga/tala header and pallavi visually checked; P. P. Narayanaswami compilation, August 2007', 'adi', 'akhilANDESvaryai namastE
aNimAdi siddhISvaryai namastE'),
        ('a78ecf02-3598-4160-b868-5130b5d56bf0'::uuid, 'entanucu sairintunu', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2305.shtml', 'c69ab17af714ad3737d7665aaf0e57b35f81da9b50797950d352ea9dbdb23a78', 'Composition: entanucu sairintunu - yadukula kaambhOji; Talam: aadi', 'aadi', 'entanucu sairintunu sItA
kAntu daya rAdu'),
        ('a9971f1e-8715-4ff9-8ad4-e62079e74abe'::uuid, 'nannu kanna talli', 'Tyagaraja', 'kEsari - sindhu kannaDa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2585.shtml', '05b818289a278bf2d36c4c51cdaed3eb7a46be8857b4e6baf3572768ac4e58f5', 'Composition: nannukanna talli - kEsari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', '1 nannu kanna talli nA bhAgyamA
nArAyaNi 2 dharmAmbikE'),
        ('a99ff3b7-2904-4145-9abf-da6ecfdac695'::uuid, 'buddhi rAdu', 'Tyagaraja', 'SankarAbharaNaM', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2229.shtml', '6b9e6e7de14fbb153a9c40f9e532852f749fb9c599620991037ced91d2e1d979', 'Composition: buddhi raadu - shankaraabharaNam; Talam: jhampa', 'jhampa', 'buddhi rAdu buddhi rAdu peddala 1 suddulu vinaka'),
        ('a9c3d9ba-c8e5-4237-95e9-33b92854e64a'::uuid, 'Krpaalavaala', 'Tyagaraja', 'Nādavarangini', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2321.shtml', '88ed149f6e1d76fbc5fffe02e89b1bb40dcfb5aba63dd594aecba1d40f1d4152', 'Composition: nrpaalavaala kalaadhara - naadavarangini; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'kRp(A)lavAla kalA dhara SEkhara
kRt(A)bhivandana SrI rAma'),
        ('aac223fb-6a9b-4f2f-81aa-3327b74937fa'::uuid, 'celimini jalajAkshu', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2238.shtml', '958f48c391fe8cd1082f3c6d4c606d160a3121202f55e82d6486fbb510f645e0', 'Composition: celimini jalajaakshhu - yadukula kaambhOji; Talam: aadi', 'aadi', 'celimini jalaj(A)kshu kaNTE
ceppar(a)yyA mIru'),
        ('ab2cb69a-ad62-4c7a-9f12-0a742d367bfe'::uuid, 'Raga Ratna', 'Tyagaraja', 'Reethigowla', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1077.shtml', 'a7034b8eead58d7c83801a17781947cec1a3cb8000a282bf046a9cf5d0fca6c9', 'Composition: raaga ratna maalikace - reeti gowLa; Talam: roopakam', 'roopakam', 'rAga ratna mAlikacE ranjillun(a)Ta hari Sata'),
        ('ac6521f4-0f36-44ce-815d-62c2d876f5ed'::uuid, 'Undedi Ramudu', 'Tyagaraja', 'Harikāmbhōji', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1082.shtml', '9a4f9a2dea28118d3443a5927fd7ef54728c83b759751a3827a6f25f79c93a2e', 'Composition: unDEdi raamuDu - harikaambhOji; Talam: roopakam', 'roopakam', 'uNDEdi rAmuD(o)kaDu
Uraka ceDi pOku manasA'),
        ('ae1996ea-2791-42ba-b927-89fc137549dd'::uuid, 'Raghupatae Rama', 'Tyagaraja', 'Sahāna', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2738.shtml', '523314aa58049eb463f4fec9bde51f427425f0f8918079d560ec015e77d05b40', 'Composition: raghupatE raama - sahaanaa; Talam: roopakam', 'roopakam', 'raghu patE rAma rAkshasa bhIma'),
        ('ae1ab85e-1041-4783-81a3-a4eba9e527ed'::uuid, 'Dharmaatma', 'Tyagaraja', 'kEdAra gauLa', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2262.shtml', 'ad32db07b0edda700c387bdbed092cc585cfec2bd5668820662aaf06cfe7cde5', 'Composition: dharmaatma nannipuDu - kEdaara gowLa; Talam: jhampa', 'jhampa', 'dharm(A)tma nann(i)puDu daya jUDavE(y)ana
marmamuna palukunadi mancidO 2'),
        ('ae7718b1-6674-47be-beb0-f50b9ed908ec'::uuid, 'Ivaraku jUcinadi', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2368.shtml', 'e60db37345f052f843ff91df93988404bcf5a564994e353ebd41c885b0acd512', 'Composition: eevaraku joocinadi - shankaraabharaNam; Talam: aadi', 'aadi', 'Ivaraku jUcinadi cAladA inkan(A) rItiyA'),
        ('aee9ddbb-ae17-4ffb-bf95-a9d11cec6b2f'::uuid, 'cAla kalla', 'Tyagaraja', 'Ārabhi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/salakalla.htm', '3caafb9027ec44be52f2082fc70d4ec29e8abadae51a7066e48ea444a4ea5f49', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'cAla kallal(A)Dukonna saukhyam(E)mirA'),
        ('af06814d-3082-4e48-9ee7-f141d551b0d7'::uuid, 'Yajnaadulu', 'Tyagaraja', 'Jayamanohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2943.shtml', '4a39e13348dc5a687b29dee8080359c653099e3cdd1e37541558c26c4a26638e', 'Composition: yajnaadulu sukhamanu - jayamanOhari; Talam: aadi', 'aadi', 'yajn(A)dulu sukham(a)nuvAriki samul-
(a)jnAlu galarA O manasA'),
        ('af1822f3-138e-4d69-a92c-d46b7961cd4a'::uuid, 'Sri Ramya Chitta', 'Tyagaraja', 'Jayamanohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2858.shtml', 'e9a78329ba7faa397e85bed414018d408c1948df54a56214d34f5f8ab7d90dcd', 'Composition: shree ramya cittaalankaara - jayamanOhari; Talam: aadi', 'aadi', 'SrI ramya citt(A)lankAra svarUpa brOvumu'),
        ('af577b3c-4ac4-4bfd-974e-96583cdb2614'::uuid, 'Kanugonu', 'Tyagaraja', 'Nāyaki', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/kanugonu-nayaki.htm', '3d2511ff4b43b8c7aa16c60362c69b8831c608f5501f4683ba134c447c2f2725', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'kanugonu saukhyamu
kamalajuk(ai)na kalgunA'),
        ('af5bf75e-750d-4f43-a8ef-38dee94c34f3'::uuid, 'brOva bhAramA', 'Tyagaraja', 'Bahudāri', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/brovabarama.htm', '396e0447b4465c68652a77efbe834842d5daef7a57b9d1a1d3d2c55cf86d08aa', 'Notation header Talam: (Desh)Adi; raga Bahudari; pallavi Brova Bharamaa Raghuraama', '(Desh)Adi', 'brOva bhAramA raghu rAma
bhuvanam(e)lla nIvai nann(o)kani (brOva)'),
        ('b0ae2fec-57ab-4d7f-ae31-2ce53d529aab'::uuid, 'nannu brOvakanu', 'Tyagaraja', 'SankarAbharaNaM', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2586.shtml', '234e590f386b24416e4817c452d39c49e0aba6bda52b35f522772c116ba4c84c', 'Composition: nanu brOvakanu - shankaraabharaNam; Talam: tripuTa', 'tripuTa', 'nannu brOvakanu viDavanurA rAma'),
        ('b0b1dfb2-b596-4645-b9aa-47082992be82'::uuid, 'eduTa nilicitE', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/edutanilachite.htm', '55762e2dbeab2b2a242034c243d7aa97945a136e6bbcac595f90e7d211f96f5a', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi � ( 2 kalai )', 'eduTa nilicitE nIdu 1 sommul(E)mi pOvurA'),
        ('b175d4b9-f889-4906-b2a4-10646c17d56d'::uuid, 'Raaraa Phani Sayana', 'Tyagaraja', 'Harikāmbhōji', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2789.shtml', '93b4f00c1519e08093664fab0e5121af691a185e13d1a7b1d2bb6eb2ad86580d', 'Composition: raaraa phaNishayana - harikaambhOji; Talam: roopakam', 'roopakam', 'rArA phaNi Sayana ravi jaladhija nayana
rAkA SaSi vadana ramaNIy(A)paghana'),
        ('b2626e28-503e-49cd-b5f6-6b8ab9cbd054'::uuid, 'Enaati Nomu', 'Tyagaraja', 'Bhairavi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/enatinomu.htm', 'da7d11313de7a959f5f29cc4446c1eaf70ed1dbe44ddfb9734c350a4851f6339', 'Notation header Talam: Adi; raga Bhairavi; source pallavi Enaati Nomu Phalamo. Corroborated against stored Latin charanam opening.', 'Adi', 'nEnu kOrina kOrkal(e)llanu nEDu tanaku neravErenu'),
        ('b3486e9f-fca1-4e77-bc15-11bddc79d381'::uuid, 'amba ninu', 'Tyagaraja', 'Ārabhi', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-amba-ninu-raga-arabhi.html', 'ffd9bb9ac4b0d4941287b13fcbfdbecdc8cba00d1d8655fc781cf2fd168c98a5', 'Opening composition description and Latin pallavi; composer and raga verified', 'Adi', 'amba ninu nammitin(a)NTE
nIk(a)numAnam(E)mamma'),
        ('b47eeeff-377e-4f6e-92ee-85af5ba16c1a'::uuid, 'Naama Kusuma', 'Tyagaraja', 'Sri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2580.shtml', '02345ddadd32cf5ec273ce7109ba0be8a766c4497efc12f9af72c004b4477776', 'Composition: naama kusumamulacE - shree; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nAma kusumamulacE pUjincE
nara janmamE janmanu manasA'),
        ('b536b933-9088-4ca0-928e-2618ce2fae57'::uuid, 'Sarasa Saama Daana', 'Tyagaraja', 'Kāpi Nārāyani', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/sarasasamadana.htm', 'd3b8f838b65032c7e6a56478f4bf2446fae75754ded5464519d040168117507c', 'Notation header Talam: Deshadi, stored as Adi; raga Kapi Narayani; pallavi Sarasa Saama Daana', 'Deshadi', 'sarasa 1 sAma dAna bhEda daNDa catura
sATi daivam(e)varE brOvavE'),
        ('b6de335c-33da-46d8-9993-7f57a9b7a910'::uuid, 'Rama Kothanda Rama', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2753.shtml', 'fba7a6766fb27394bdfa0bb2f07b215d4ecdb75d3eed48402facdb7147b42a12', 'Composition: raama kOdaNDaraama raama - bhairavi; Talam: aadi', 'aadi', 'rAma kOdaNDa rAma rAma kalyANa rAma 1'),
        ('b7e2a6c8-fcc6-4fa9-9f53-038f3816b06c'::uuid, 'Daya Leni', 'Tyagaraja', 'Nāyaki', 'Khanda Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/dayaleni.htm', '9295dd2302e8d0262dbec4b9fb8d334ebd90a1b85f8335c2c1a72b3ae3a82e98', 'Notation header Talam: Khanda Chapu (Jhampa); raga Naayaki; pallavi daya lEni bratukemi; index alternative Khanda Ekam or Misra Jhampa not used', 'Khanda Chapu ( Jhampa )', 'daya lEni bratuk(E)mi
1 daSaratha rAma nI'),
        ('b8b35e19-e62b-4508-9204-486ab0763307'::uuid, 'ADavAramella', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c1372.shtml', '25e453ac603912e6ea05fbcd594cece31fcb28e5d1051878901f91b810dd3757', 'Composition: aaDavaara mella gooDi - yadukula kaambhOji; Talam: tripuTa', 'tripuTa', 'ADavAram(e)lla kUDi 1 manam-
(A)DudAmu harini vEDi'),
        ('b95c07ee-f897-49ea-9f7b-c37be42335ba'::uuid, 'Chalamelaraa', 'Tyagaraja', 'mArgahindOLaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2232.shtml', '6c7e47d7567fc85e7c6dcd2a429aa8f8b11e8574e70212b3e536c5f67137566a', 'Composition: calamElaraa - maargahindOLam; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'calam(E)larA sAkEta rAma'),
        ('b9af5088-9906-47c4-bec2-4ea59e57f75b'::uuid, 'mOhana rAma', 'Tyagaraja', 'Mohanam', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/mOhanarAma.htm', 'fc0ce00db17363d8d080ab0e9ea9f2b9f0fb12eafb111aeb9f82f598bc6d896a', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi (2 kalai)', '1 mOhana rAma mukha jita sOma
mudduga palkumA'),
        ('ba2ec6a9-1804-48f1-a379-0fc505078892'::uuid, 'pAhi rAma candra', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2700.shtml', 'aebe18e882126f4f67872215dbf6d5b835810bd017cf06bf0dcbb0547cd69e5d', 'Composition: paahi raamacandra raaghava - yadukula kaambhOji; Talam: aadi', 'aadi', 'pAhi rAma candra rAghava 1 harE mAm
pAhi rAma candra rAghava'),
        ('bbe4c6a3-5a76-4902-abf9-32d547c04d49'::uuid, 'rAmacandra nI daya', 'Tyagaraja', 'suraTi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2780.shtml', '5af098a3db46307ea6ddf63137b7713f966405dd86d4c12c45a873f9bbfa1d81', 'Composition: raamacandra nee daya - shuruTTi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'rAmacandra nI daya rAma(y)Ela rAd(a)ya'),
        ('bbf7e456-a0ca-4e71-a854-3bec3998b428'::uuid, 'angArakaM ASrayAmyahaM', 'Muthuswami Dikshitar', 'suraTi', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/angaraka.htm', '9dd4bc041cd58749caf2101259c94da2b319a5d351cf4ad2803bcae2c56bf013', 'Talam header and Pallavi; manually verified spelling Surutti = suraTi; composer and incipit matched', 'Rupakam', 'angArakaM ASrayAmyahaM
vinatASrita jana mandAraM

[Madhyama Kala Sahitya]
mangaLa vAraM bhUmi kumAraM vAraM vAram'),
        ('bd7dec39-7fb5-45e6-b905-5e887fc294f8'::uuid, 'Toli Ne Jesina', 'Tyagaraja', 'Suddha Bangāla', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2898.shtml', 'f39de27b0d13c905b89a2fd8be4051f0f1d2fa56ed8ef4836529c4c6f5572572', 'Composition: toli nE jEsina - sudda bangaaLa; Talam: aadi', 'aadi', 'toli nE jEsina pUjA phalamu
telisenu nApAli daivamA'),
        ('bdbcc661-264b-4943-9c46-5c824056a239'::uuid, 'O rAjIvAksha', 'Tyagaraja', 'Ārabhi', 'Misra Capu', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-o-rajivaksha-raga.html', 'a72c0d80539a080f1e1d812eadd9acc1f2668eb5e2efd91118650b07f7a48bb6', 'Opening composition description and Latin pallavi; composer and raga verified', 'miSra cApu', 'O rAjIv(A)ksha 1 Ora jUpulu
jUcedav(E)rA nE nIku vErA'),
        ('c001d672-4240-4cc1-86c1-24c9cab2b435'::uuid, 'Ora Joopu', 'Tyagaraja', 'kannaDa gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1475.shtml', 'b080306dd0202cf3c9caef98c6bfd9fd92dc488b9fc528c4f35fa1b8d0954895', 'Composition: Orajoopu choocEDi nyaayamaa - kannaDagowLa; Talam: aadi', 'aadi', 'Ora jUpu jUcEdi nyAyamA
O ragh(U)ttama nIvaNTi vAniki'),
        ('c2350f5a-4019-453e-9ac4-82d8fe95cbf9'::uuid, 'Indukaayee Tanuvunu Penchinadi', 'Tyagaraja', 'Mukhāri', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c31033.shtml', '61f852fc67be7a283b134695be00bac8404a4dd48fc1c8823f29f4b7a74f4a86', 'Composition: induka I tanuvunu - mukhAri; Talam: cApu', 'cApu', 'indukA(y)I tanuvunu pencinadi(ndukA)'),
        ('c262739c-e7cc-492d-b248-3775fec24bfe'::uuid, 'O Ranga Saayi', 'Tyagaraja', 'Kāmbhoji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/orangashaayi.htm', '8f0b486512481464619fe7f5b23175c7b7717366445600afa5ebb5e03b535622', 'Talam header and Pallavi; manually verified spelling Kambodhi = Kāmbhoji; composer and incipit matched', 'Adi (2 kalai)', 'O ranga 1 SAyi 2 pilicitE
3 O(y)anucu rA rAdA'),
        ('c2c9522c-5669-40d5-9dc8-ea5a21279303'::uuid, 'Alakalallalaadaga', 'Tyagaraja', 'Madhyamāvathi', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/alakalladaga.htm', 'bfd11ab37248081bd7cf306446716d69042579730d75292907c2a806f0cb37f4', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Rupakam', 'alakal(a)llalADaga kani(y)-
A rAN-muni(y)eTu pongenO'),
        ('c7603975-aaf9-4379-8434-59af93a50da3'::uuid, 'Urakae Kalgunaa', 'Tyagaraja', 'Sahāna', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2905.shtml', 'd571dc422d8b6bb76c771c7c0dcba040fb42f8a61df6888565d5d03fbce474d4', 'Composition: oorakE galguna - sahaanaa; Talam: caapu', 'caapu', 'UrakE kalgunA rAmuni bhakti'),
        ('c857fcff-1b60-4cdf-8132-179ad49d4959'::uuid, 'enduku daya rAdu', 'Tyagaraja', 'Hanumatodi', 'Triputa', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/10/thyagaraja-kriti-enduku-daya-radu-raga.html', 'b999249684a4d9262154854938cbb26f645b20b54431b9bcbb5fec84ab514015', 'Opening composition description and Latin pallavi; composer and raga verified; Todi is the registered alias for Hanumatodi; Triputa subtype not inferred', 'tripuTa', 'enduku daya rAdurA SrI rAmacandra nIk(enduku)'),
        ('c876a731-58e5-4d9e-89ab-62f4a353e683'::uuid, 'Neevanti Daivamu', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2672.shtml', 'cd9d76deaf51676f7a374ceef548c39e39764d8093649e4a34e9bcf2f9b5ae0c', 'Composition: neevaNTi deivamu - bhairavi; Talam: aadi', 'aadi', 'nIvaNTi daivamu nE kAna
nIraj(A)ksha SrI rAmayya'),
        ('c8f46289-6807-4fbf-aed0-a5b82f3a2ff6'::uuid, 'itara daivamulu', 'Tyagaraja', 'Chāyatārangini', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2367.shtml', 'd91606c147c1ccaec47c60c3d53ef95e59d5cf7dd5d39079a89607645972af0e', 'Composition: itara daivamula - caayataarangini; Talam: roopakam', 'roopakam', 'itara daivamula vallan-
(i)lanu saukhyamA rAma'),
        ('cbf2b349-35dc-48eb-aa89-74ffd3464421'::uuid, 'Emi nEramu', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2284.shtml', '74ee27c8a0071d54ed04808d4010390aa1b74473d8ff5bb6bf993064a5de525b', 'Composition: Emi nEramu - shankaraabharaNam; Talam: aadi', 'aadi', 'Emi nEramu nannu brOva
enta bhAramu 1 nAvalla (Emi)'),
        ('cd626987-39ee-4517-b13d-2a53d14244f5'::uuid, 'Paripalaya Mam', 'Tyagaraja', 'Darbar', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2719.shtml', '608e406384956fc8d5e3e02a5b6c9e345e3af52236a301cc30cdd5a12565c2a2', 'Composition: paripaalaya maam - darbaar; Talam: tripuTa', 'tripuTa', 'paripAlaya mAm kOdaNDa pANE
1 pAvan(A)Srita 2 cintAmaNE'),
        ('cd6fdf67-8092-49f8-9a35-61851937370d'::uuid, 'Laavanya Rama', 'Tyagaraja', 'Purna Shadjam', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2528.shtml', 'fc9f19e6574e5574f2cb5b1f9874a4a99e4de034e153faba761bff9e6f8b1221', 'Composition: laavaNya raama - poornashadjam; Talam: roopakam', 'roopakam', 'lAvaNya rAma 1 kanulAra jUDavE ati (lAvaNya)'),
        ('ce57d2f1-4b5c-41a9-9034-6130eb0d20f3'::uuid, 'Talachinantanae', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2885.shtml', '6f6dda217b9bb092b6f720482d4841a9a3dedaac4cdf725129e8fefc88fdef3c', 'Composition: talaci nantanE - mukhaari; Talam: aadi', 'aadi', 'talacin(a)ntanE nA tanuv(E)mO 1 jhall(a)nerA'),
        ('ce8ecb05-26e2-4115-ad6a-88f841790c3a'::uuid, 'Raghu Veera Rana', 'Tyagaraja', 'Huseni', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2740.shtml', '35f1cbfc2206ec9c41a849cae1106c8a2f0293a146ec687922ff16ebfe333949', 'Composition: raghuveera raNadheera - husEni; Talam: roopakam', 'roopakam', 'raghu vIra raNa dhIra
rArA rAja kumAra'),
        ('cee0b420-4914-4914-9318-01b6d52d1238'::uuid, 'Adigi Sukhamu', 'Tyagaraja', 'Madhyamāvathi', 'Misra Capu', 'shivkumar.org', 'https://www.shivkumar.org/music/adigi-sukhamu-madhyamavathi.htm', 'ae2fbf7706df8791b07818571e7b523eb18426b3fbba4eefa72938d34cdabe3d', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Misra Chapu', 'aDigi sukhamul(e)vvar(a)nubhavincirirA
Adi mUlamA rAma'),
        ('cf004928-e512-49a3-badf-e69382fec877'::uuid, 'Mundu Venuka', 'Tyagaraja', 'Darbar', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/munduvenuka-durbar.htm', '22ccae882593676f7abe8883562e944d614a44e1ae5b7ddcf026e8fa2a41a581', 'Talam header and Pallavi; manually verified spelling Durbar = Darbar; composer and incipit matched', 'Adi', 'mundu venuka(y)iru pakkala tODai
1 mura 2 khara hara rArA'),
        ('cf5d54a5-dcc8-4b4f-a2fe-257c3a1f9f53'::uuid, 'Eti Yochanalu', 'Tyagaraja', 'Keeranāvali', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/etiyochana.htm', '5b19899ac0babd0e4f7170f955932306782c04132acc56aa079c545fab00a43a', 'Notation header Talam: Deshadi, stored as Adi; raga Kiranavali; pallavi ETi yOcanalu jEsEvurA', 'Deshadi', 'ETi 1 yOcanalu jEsEvurA
eduru palkuvAr(e)varu lErurA'),
        ('cf6e4421-14d9-4412-97cd-14c4500eb678'::uuid, 'Padavi Nee', 'Tyagaraja', 'Salaka Bhairavi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/padavini.htm', 'bc61948832ae19d24ba5c2d51b0cb6be0a150af7947d0e128008e68d8c584bc5', 'Talam header and Pallavi; manually verified spelling Salaga Bhairavi = Salaka Bhairavi; composer and incipit matched; source explicitly says Adi (Deshadi), not a bare Deshadi inference', 'Adi (Deshadi)', 'padavi nI 1 sad-bhaktiyu kalguTE'),
        ('cfd4399f-f56b-46de-ae87-805d470f9801'::uuid, 'Ksheera Saagara Vihaara', 'Tyagaraja', 'Anandabhairavi', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2412.shtml', '041d568df458e3995b81d0084e0627554c8f1ecee462c23d04c2b9048842d575', 'Composition: ksheerasaagara vihaara - aananda bhairavi; Talam: jhampa', 'jhampa', 'kshIra sAgara vihAra aparimita
ghOra pAtaka vidAra
krUra jana gaNa vidUra nigama
sancAra sundara SarIra'),
        ('d1152836-f0ae-44cf-b271-46ab504c30cc'::uuid, 'Manasu Loni Marmamu', 'Tyagaraja', 'Hindolam', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1116.shtml', 'eeff4168d748f0acf88454d114f07230274c18fda7478b0855212aab422ea5dc', 'Composition: manasulOni marmamu - sudda hindOLam (varam); Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'manasu lOni 1 marmamunu telusukO
mAna rakshaka marakat(A)nga nA (manasu)'),
        ('d2e28055-6ebd-4502-bc9c-e39fdabafd06'::uuid, 'ranga nAyaka', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2785.shtml', '4f8bc46349a62c3d363e5657311658c19e1f2c60f54bd17762c8dbb9669db165', 'Composition: ranganaayaka rakshimpumayya - shankaraabharaNam; Talam: aadi', 'aadi', 'ranga nAyaka rakshimpum(a)yya
racca sEya(n)Ela 1 raghu vaMSa nAtha'),
        ('d400db6d-6e56-4e51-a275-a9fb7e0883fe'::uuid, 'Enta Nerchina', 'Tyagaraja', 'Suddha Dhanyāsi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1092.shtml', '8197985d8ae26919e4e7c43c7efb4c23d26313192c65448103b2849b05753220', 'Composition: enta nErcina - udhaya ravi candrikaa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'enta nErcina 1 enta jUcina
enta vAralaina 2 kAnta dAsulE'),
        ('d4579215-90be-47f8-8410-44020f815417'::uuid, 'SRngArincukoni', 'Tyagaraja', 'suraTi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2862.shtml', '9fff62ba9777b8742d444cae89dbff71e8b89a1f8473299ef4b802b248549467', 'Composition: shringaarincukoni - shuruTTi; Talam: aadi', 'aadi', 'SRngArincukoni veDaliri SrI kRshNunitOnu'),
        ('d479e704-921d-4583-89af-e8fcb94bd85b'::uuid, 'Evarani Nirnayinchiri', 'Tyagaraja', 'Devamrta Varshini', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1505.shtml', '8f4f6887493d80c288dda76a2cec1fcab3d4789af782cf6753d328fccb8744b1', 'Composition: evarani - dEvaamrutavarshani; Talam: Adi', 'Adi', 'evarani nirNayincirirA ninn-
(e)Tl(A)rAdhincirirA 1 nara varul(evarani)'),
        ('d4a411e4-f029-46c9-8aad-699e2d235cae'::uuid, 'Raga Sudhaa', 'Tyagaraja', 'Andolikā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2735.shtml', '02e26a6f56354a811dd4ee54a5722a99f0db3d3c4ff350ae8916a7979f81b69c', 'Composition: raaga sudhaarasa - aandOLikaa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'rAga sudhA rasa pAnamu jEsi
1 ranjillavE 2 O manasA'),
        ('d6da96c9-8628-4cd4-b079-3fa04883fcba'::uuid, 'parAmukhamEla', 'Tyagaraja', 'suraTi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2717.shtml', '548e65ca06b584397c476b5c95a2394ca2e1bef2a9a4ae48577a55d1792a5f74', 'Composition: paraamukham Elaraa - shuruTTi; Talam: aadi', 'aadi', '1 parAmukham(E)larA rAmayya'),
        ('d7383d27-873a-4a6c-9add-3018cd4af058'::uuid, 'Sita Pati Naa Manasuna', 'Tyagaraja', 'Kamās', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2867.shtml', '156685b865aabaadd2aeeed6324d750aeda05dd11812942d5fed88c38aa3dd53', 'Composition: seetaapati naa - kamaas; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sItA patI nA manasuna
siddhAntam(a)ni(y)unnAnurA'),
        ('d750cc52-5801-419e-b789-0c9c1ec346c4'::uuid, 'kuvalaya daLa', 'Tyagaraja', 'nATa kuranji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2414.shtml', '0de0243745a313d9fd1c722599e65983f110be875fbe6edbf23cbb52da475220', 'Composition: kuvalayadaLa - naaTTai kurinji; Talam: aadi', 'aadi', 'kuvalaya daLa nayana brOvavE
kunda 1 kuDmala radana'),
        ('d844e1e2-5f46-4eb1-97a1-35b4ff52f883'::uuid, 'Kamalaapta Kula', 'Tyagaraja', 'bRndAvana sAranga', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2386.shtml', '0237c26785e69b960e5914a639be617cb25ccf2a89fa04b7c507a6dac2e4ab3c', 'Composition: kamalaapta kula - brindaavana saaranga; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'kamal(A)pta kula kalaS(A)bdhi candra
kAva(v)ayya nannu karuNA samudra'),
        ('d8e009c1-0e0e-48e2-b48f-72bfdf483d32'::uuid, 'daya sEyavayya', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2255.shtml', '4c3a4b669197bd2129b6969281a6d772badb444ab9218eb7c46e58c96ec0b7b8', 'Composition: daya sEyavayyaa - yadukula kaambhOji; Talam: aadi', 'aadi', 'daya sEya(v)ayyA sadaya rAma candra'),
        ('d9b7b93f-9c9f-47f8-94e0-23d6c05c1c75'::uuid, 'Aanandamaananda', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1381.shtml', 'fff3cf818feb689373af85479e58033a1dfacf7ec6654299160be4d06965f6e5', 'Composition: aanandamaanandamaayenu - bhairavi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'SrI rAma nE dhanyuDaitini Ananda nIradhilOn(I)danaitini rAma'),
        ('d9f78754-2edc-4509-af4d-3f6104437b0a'::uuid, 'Muripemu', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2571.shtml', '32a8d6c86525cd3a004edd11a75557609ede825b7fb386c9facc751cd471bb33', 'Composition: muripEmu galigegadaa - mukhaari; Talam: aadi', 'aadi', 'muripemu kalige kadA rAma san-
muni nuta kari varada SrI rAma'),
        ('da47bd3e-75ff-4227-8d7f-174aa3bb0d87'::uuid, 'Sangita Sastra', 'Tyagaraja', 'Mukhāri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2810.shtml', '3901deba0d10b7b6567e16770088f2dcbd7a4600e5b10ce873680cdd997159f2', 'Composition: sangeeta shaastra - saalagabhairavi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'sangIta SAstra jnAnamu
1 sArUpya saukhyadamE manasA'),
        ('dc5fbe4f-b710-4128-bbf9-c40ec1f71157'::uuid, 'Rama Nannu Brova', 'Tyagaraja', 'Harikāmbhōji', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/ramanannubrova.htm', '063af2c506f6f2af961ecb4b54dc800a6a158593c7ad64fa44fe3f7beeb900c0', 'Talam header and Pallavi; manually verified spelling Hari Kambodhi = Harikāmbhōji; composer and incipit matched; Tyagaraja attribution corroborated by charanam mudra and linked catalogue lyric source', 'Rupakam', 'rAma nannu brOva
1 rAv(E)makO lOk(A)bhi(rAma)'),
        ('dc82e4eb-1e97-42d0-b505-432013bcaca1'::uuid, 'Nijamuga Nee', 'Tyagaraja', 'Sahāna', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2660.shtml', 'efc40f8c985ae7e627f321e8fc3e9b87bacf4193d57581afe926b44c00056391', 'Composition: nijamuga nee - sahaanaa; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nijamuga nI 1 mahima telpa lEru'),
        ('dd9e3656-85ac-4ba9-b9ed-a2d9c4a5e317'::uuid, 'Chani Todi Tevae', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2236.shtml', 'd66383173fd6b06211b641dae3f6dd863cd42f07caa0231f3ebf2cd476ece55f', 'Composition: cani tODi tEvE - harikaambhOji; Talam: aadi', 'aadi', 'cani 1 tODi tEvE O manasA'),
        ('ddcdd2df-804c-4c4d-a2ac-57f1c0c71275'::uuid, 'Raamuni Maravakavae', 'Tyagaraja', 'kEdAra gauLa', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2784.shtml', '91905db61c30173cc16f7e671de8f372e3359c5fc1f206abc1e8a1f503395ed6', 'Composition: raamuni maravakavE - kEdaara gowLa; Talam: aadi', 'aadi', 'rAmuni maravakavE O manasA'),
        ('de0d61d6-3ab2-4fc0-8023-bf7dda4778a9'::uuid, 'Aparaadhamula Maanpi', 'Tyagaraja', 'Darbar', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c1386.shtml', 'd21033671035dce30355a66c9096e10f590c206da3dcf95df95343cebd0e7406', 'Composition: aparaadhamulamaanpi - darbaar; Talam: jhampa', 'jhampa', 'aparAdhamula mAnpi(y)AdukO(v)ayya'),
        ('de598d7d-0eef-43f7-82f9-b9ff9ed743a9'::uuid, 'Kanna Tandri', 'Tyagaraja', 'Deva Manohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2389.shtml', '87ee765b8990765f79aceefe6906b57476b7eb34d2dd9958fa660d1814e71135', 'Composition: kanna taNDri naapai - dEva manOhari; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'kanna taNDri nApai
karuNa mAnakE gAsi tALanE'),
        ('deab3382-1a62-4554-aade-c66780944c6f'::uuid, 'darSanamu sEya', 'Tyagaraja', 'nArAyaNa gauLa', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2249.shtml', '85c09974bb0240552a0be5bf59f8e934e81b68885e9a2e74ece1091ba21ce60c', 'Composition: darshanamu sEya - naaraayaNa gowLa; Talam: jhampa', 'jhampa', 'darSanamu sEya nA taramA'),
        ('df4ef29d-2fd9-42b8-b984-b0035e62bdf7'::uuid, 'Bhaja Ramam', 'Tyagaraja', 'Huseni', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1400.shtml', '273022487b50327a95108d504121464622c21f353b4d7aa7f4a76f75ccc021be', 'Composition: bhaja raamam satatam - husEni; Talam: aadi', 'aadi', 'bhaja rAmaM satataM mAnasa'),
        ('dff636dc-d88e-48a3-a88a-f08b9dfc0154'::uuid, 'bhava nuta', 'Tyagaraja', 'Mohanam', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/bhavanuta.htm', 'dc5d2ab2a01469f92bc1ac0cc66435d74461cad35716afaadcc37ca54a3e43a1', 'Notation page header Talam: Adi and title line Mohanam Adi; pallavi matches exactly; site index lists Rupakam and was not used', 'Adi', 'bhava nuta nA hRdayamuna ramimpumu baDalika tIra'),
        ('dffbb6d7-4eb4-4c89-8124-396b951cae93'::uuid, 'rArA mAyiNTidAka', 'Tyagaraja', 'Asāveri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2787.shtml', 'ddd8976fdb92fe4665c80455ba332410cc707713367d9d038d30d02b41e96b84', 'Composition: raaraa maayiNTi daaka - asaavEri; Talam: aadi', 'aadi', 'rArA mA(y) 1 iNTidAka raghu-
vIra sukumAra mrokkedarA'),
        ('e0151a7a-e0d9-413d-b07a-4c1b2854971f'::uuid, 'sundarESvaruni', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-sundaresvaruni-raga.html', 'cd335653b76be2007c381480b8b0eb2b4dc55cd69725ba3444b41fdf04f80e43', 'Opening composition description and Latin pallavi; composer and raga verified', 'Adi', 'sundarESvaruni jUci
surula jUDa manasu vaccunA'),
        ('e0600de7-2027-4961-87be-cdfb8fe78357'::uuid, 'Oka Maata', 'Tyagaraja', 'Harikāmbhōji', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2687.shtml', '739845f964b9f826818e9c931fb244e78ccece1e890c4857638f96909f6ae6fa', 'Composition: oka maaTa - harikaambhOji; Talam: roopakam; Harikambhoji / Rupaka (Divyanama/Utsava)', 'roopakam', 'oka mATa 2 oka bANamu
3 oka patnI vratuDE manasA'),
        ('e0723c64-9c2b-4b5c-a5b4-6cc4bb597524'::uuid, 'Anyaayamu', 'Tyagaraja', 'Kāpi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1360.shtml', '9bf5a021f337661e6ab565555715fba144c5bc68f0b68146ae6df67ed64dd6f2', 'Composition: anyaayamu sEyakuraa - kaapi; Talam: aadi', 'aadi', 'anyAyamu sEyakurA rAma 1 nann-
(a)nyunigA jUDakurA nAyeDa rAma'),
        ('e17ac587-44d1-4e1a-82c1-ffda7bc0b3f3'::uuid, 'Sarvantaryaami', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2823.shtml', '85bdea1f86643320a9ab4c93f1c43ff39f46ab9c5cb93aa04882fe48aa248dc3', 'Composition: sarvaantaryaami nee - bhairavi; Talam: aadi', 'aadi', 'sarv(A)ntaryAmi nIv(a)nE
sAmrAjyamu nijamE rAma'),
        ('e20e4215-c9ef-4f2a-b7d2-28c5aa43a1bb'::uuid, 'Nagu Momu Gala Vaani', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1704.shtml', 'cecdd452ee0e24a5e085b34ce780bedcae5a095b8b0cfdae7acba2750437cec7', 'Composition: nagumOmu galavaani - madyamaavati; Talam: aadi', 'aadi', 'nagu mOmu gala vAni nA manO-haruni
jagam(E)lu SUruni jAnakI varuni'),
        ('e2f166b0-c936-4b29-ab16-22fe89701283'::uuid, 'Yuktamu Kaadu', 'Tyagaraja', 'Sri', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2801.shtml', '977f8085acc49c0ffcf9687b5daa0d329e9c3416324537c38c9db949f3ace711', 'Composition: yuktamu gaadu - shree; Talam: aadi', 'aadi', 'yuktamu kAdu nanu 1 rakshincakan(u)NDEdi rAma'),
        ('e5f5a5ac-dd47-4b23-9d18-99d09ea95266'::uuid, 'Paahi Mam Sri Ramachandra', 'Tyagaraja', 'Kāpi', 'Jhampa', 'karnatik.com', 'https://www.karnatik.com/c2694.shtml', '031ee456a33c33e7fdb6257276873cf9eb189d630ecbe45e864878f43387a1cd', 'Composition: paahi maam shree - kaapi; Talam: jhampa', 'jhampa', 'akkaratO pAdamulaku mrokkiti Ela parAku (pAhi)'),
        ('e630e8f7-f8da-45b3-8e8b-fce0f91a74e9'::uuid, 'Kori Sevimpa', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2407.shtml', 'af7a351240ea6e6014bdcb5b5919a3f0141eb663450f3ac8f7c52e9d8f802e38', 'Composition: kOri sEvimparaarE - kharaharapriyaa; Talam: aadi', 'aadi', 'kOri sEvimpa rArE kOrkal(I)DEra'),
        ('e722f5ff-3066-43d1-9848-a926454745d4'::uuid, 'Atade Dhanyudu', 'Tyagaraja', 'Kāpi', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c1390.shtml', '45628f8e2d6fe7b57243b715942dadc0f7d1c102551ebf89dfaa863fa12ffdfc', 'Composition: ataDE dhanyuduraa - kaapi; Talam: caapu', 'caapu', 'ataDE dhanyuDurA O manasA'),
        ('e72a140c-7ead-4721-bf46-2693f3b460ed'::uuid, 'E tAvuna nErcitivO', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2313.shtml', '81adbf515c8995f38f109ca9de93b3371e4a8e8c9afed035847dd297c7867b88', 'Composition: Etaavuna nErcitivO - yadukula kaambhOji; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'E tAvuna nErcitivO rAma
1 enduk(i)nta gAsi'),
        ('e73049d2-c17d-4a3a-9a18-7a895449d74b'::uuid, 'Entani Ne', 'Tyagaraja', 'Mukhāri', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/entanine.htm', 'abc2ffe6ecc4818a6b8615dfbb7c7864f4ab68db1b2c7360b5232b8bc0b69f64', 'Notation page header Ragam Mukhari (22nd mela janya), Talam Rupakam, Composer Tyagaraja, Pallavi Entani Ne Varnintunu Shabari Bhaagyam; Shivkumar index entry agrees on Mukhari and Rupakam', 'Rupakam', 'entani nE varNintunu SabarI bhAgyam(entani)'),
        ('e7af76fc-eddf-4b85-829f-3e56a07a9acc'::uuid, 'Manasaa Sri Ramachandruni', 'Tyagaraja', 'Eeshamanohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2543.shtml', '11adda51e26ab906a39e47887fe667bcbedb18bb6084943086282e5679a7acbf', 'Composition: manasaa shree raamacandruni - eesamanOhari; Talam: aadi', 'aadi', 'manasA SrI rAma candruni
1 maravakE EmarakE O (manasA)'),
        ('e7d895a3-ab2c-46d9-84c8-d7fbaaa71896'::uuid, 'Upachaaramulanu Chekona', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2904.shtml', '658cc821691f7a35841998ecb12bf0c22848a7af42ac59d521ed612655caf6c7', 'Composition: upacaaramulanu - bhairavi; Talam: aadi', 'aadi', 'upacAramulanu cEkona(v)ayya
1 uraga rAja Sayana'),
        ('e8c3ba4e-a639-43e4-83b8-34411027cb41'::uuid, 'Giripai Nelakonna', 'Tyagaraja', 'Sahāna', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1122.shtml', 'a736d124924f0386e3929608592173c4a16994f66fe930f3ed11359c91f1501c', 'Composition: giripai nelakonna - sahaanaa; Talam: aadi', 'aadi', 'giripai nelakonna rAmuni
guri tappaka kaNTi'),
        ('e8de604f-1086-4bb5-bb70-cc9b7b21828e'::uuid, 'gItArthamu', 'Tyagaraja', 'suraTi', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/gitarthamu.htm', '32db5cbd4f794181c2e91867cc26405987671806c9140f9b346062efcefd7e5f', 'Talam header and Pallavi; manually verified spelling Surutti = suraTi; composer and incipit matched', 'Adi', '1 gIt(A)rthamu 2 sangIt(A)nandamun-
( 3 I) tAvuna jUDarA O manasA'),
        ('e90dc863-7cae-4986-a557-1711f7f98bd1'::uuid, 'Dina Mani Vamsa', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/dinamanivamsha.htm', '98bcb8e5b7a20336b0aa4c858de39c00c060e6c1bc01944c8071dd9b80f8d944', 'Talam header and Pallavi; manually verified spelling Hari Kambodhi = Harikāmbhōji; composer and incipit matched', 'Adi', 'dina maNi vaMSa tilaka lAvaNya
dIna SaraNya'),
        ('e9358be5-57d8-43f8-bb82-e8477f851b57'::uuid, 'Natha Brovavae', 'Tyagaraja', 'Bhairavi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2592.shtml', '6a51858867dff428e7c26cf30de966c81fb4e4d3f8fb1baf09ab4eed661c4909', 'Composition: naatha brOvavE - bhairavi; Talam: aadi', 'aadi', 'nAtha brOvavE raghu nAtha brOvavE'),
        ('ec1fb06f-6d95-4689-9ae6-3a9cf0999d34'::uuid, 'Sri Maanini', 'Tyagaraja', 'Purna Shadjam', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2837.shtml', 'b867abdaef913759573865b747b7b866060737804196b56520edb3237279b6e5', 'Composition: shree maanini - poornashadjam; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'SrI 1 mAninI manOhara
cira kAlam(ai)na 2 mATa(y)okaTirA
vEmAru palka jAlarA'),
        ('edb0fece-4478-4bc9-84c1-6f3ffd2c3b2b'::uuid, 'rAma Eva daivataM', 'Tyagaraja', 'Bālahamsa', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2750.shtml', '4d89d53840f3fae998af952277ac2d929608d8b673dc7801b26fc81ab6aff660', 'Composition: raama Eva deivatam - balahamsa; Talam: roopakam', 'roopakam', 'rAma Eva daivataM raghu kula tilakO mE'),
        ('eef82f61-9e42-4514-8142-e77b2cd0b68e'::uuid, 'vEda vAkyamani', 'Tyagaraja', 'Mohanam', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2926.shtml', '521e565aa0f344d0f6d3a024035421b95347f5a68ba17f971efdd225699399e0', 'Composition: vEdavaakyamani - mOhanam; Talam: tripuTa', 'tripuTa', '1 vEda vAkyam(a)ni(y)enciri(y)I
2 veladul(e)lla sammatinciri'),
        ('f0fd6665-d378-48fd-8772-6610a997c34d'::uuid, 'nAdOpAsana', 'Tyagaraja', 'Begada', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/nadopasana.htm', '5f19cb2dd57737736b840b03f1ee6aee8dc6564b0a7332f11685b1ca1073216b', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', '1 nAd(O)pAsanacE Sankara
nArAyaNa vidhulu velasiri O manasA'),
        ('f1a32daa-973d-48df-8bef-b3ee93fe17fa'::uuid, 'Rama Lobhamela', 'Tyagaraja', 'Darbar', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2754.shtml', '616718ebdae77f4ce9a3483f9d801fbdb0e48af16ef0219d1c2ad7ae73a0b35f', 'Composition: raama lObhamEla - darbaar; Talam: aadi', 'aadi', 'rAma lObham(E)la nanu
rakshincupaTla nIk(i)nta SrI (rAma)'),
        ('f287410c-39e9-495d-9aab-419a63ecc2ed'::uuid, 'Nee Muddu Momu', 'Tyagaraja', 'Kamalā Manohari', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2654.shtml', '030306f9ea4df0c1686409a59f643151845f493b70b2ddf142db496cb4a7d7e9', 'Composition: nee muddu mOmu - kamalaa manOhari; Talam: aadi', 'aadi', 'nI muddu mOmu jUpavE'),
        ('f2990794-8758-460b-96fd-abd3fbd0e589'::uuid, 'Deva Sri Tapasteertha', 'Tyagaraja', 'Madhyamāvathi', 'Triputa', 'karnatik.com', 'https://www.karnatik.com/c2259.shtml', 'adc2131510e5f246091bf2b6c7a647280de23eba927b9688ecc9f10c68ea19f3', 'Composition: dEva shree tapasteerthapura - madyamaavati; Talam: tripuTa', 'tripuTa', 'dEva SrI 1 tapastIrtha pura nivAsa
dEhi bhaktim-adhunA'),
        ('f49769af-f30b-4345-8282-da36516062d9'::uuid, 'Raanidi Raadu', 'Tyagaraja', 'Manirangu', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2786.shtml', '50e08a189ad183eb6cf0317aa9a6762129e8e81d3b893cc6535d017ad88715e4', 'Composition: raanidhi raadu - maNirangu; Talam: aadi', 'aadi', 'rAnidi rAdu sur(A)surulak(ai)na'),
        ('f5081739-3f8e-4427-90c7-becb3b1eeba9'::uuid, 'sItA kalyANa', 'Tyagaraja', 'SankarAbharaNaM', 'Khanda Capu', 'thyagaraja-vaibhavam.blogspot.com', 'https://thyagaraja-vaibhavam.blogspot.com/2008/04/thyagaraja-kriti-sita-kalyana.html', '027c199b5c9f619dbd243f35e54bb699ef3fd02d24307b33f6ec6e922b5388b5', 'Opening composition description and Latin pallavi; composer and raga verified; source explicitly permits Sankarabharanam or Kuranji; stored Sankarabharanam retained', 'khaNDa cApu', 'sItA kalyANa vaibhOgamE
rAma kalyANa vaibhOgamE'),
        ('f5b3f945-9362-4b7b-945e-8d920e6b66c9'::uuid, 'gandhamu puyyarugA', 'Tyagaraja', 'Punnagavarali', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2333.shtml', '838566b8439043570c784339734b212772b6f45a7e6389d06985547301513eac', 'Composition: gandhamu puyyarugaa - punnaagavaraaLi; Talam: aadi', 'aadi', '1 gandhamu puyyarugA 2 pannIru gandhamu puyyarugA'),
        ('f5e86fd2-05eb-4c88-99c4-7ef9214618a0'::uuid, 'Naada Loludai', 'Tyagaraja', 'Kalyāna Vasantam', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c1611.shtml', 'c1c9aba66b7228c8cdc4380ff384d967c804ee8f70e18637db9acf91b460f103', 'Composition: naadalOluDai - kalyaaNa vasantam; Talam: roopakam', 'roopakam', 'svAdu phala prada sapta svara rAga nicaya sahita (nAda)'),
        ('f6acd658-e5b4-44d8-b642-b1e74d95898a'::uuid, 'Mitri Bhaagyamae', 'Tyagaraja', 'Kharaharapriyā', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/mitribhagyame.htm', '2e8f0e75e936060636d69d8c3cbb1060d165b4f5111fee1de1f2d817cb4e2144', 'Notation page Talam: Adi; raga Kharaharapriya; source pallavi mitri bhAgyamE. Index lists Rupakam and was not used. Corroborated against stored Latin anupallavi opening.', 'Adi', 'citra ratna-maya 3 SEsha talpam(a)ndu sItA patini 4 unici(y)Ucu sau(mitri)'),
        ('f6fe6cd6-1724-491f-9268-2fe4e4a4024d'::uuid, 'Phanipati Sayi', 'Tyagaraja', 'Jhankāradhvani', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1820.shtml', '40acbbe0394254c94c33d679310f1c3101621ffa9d63be45b38c2ebc83f38884', 'Composition: phaNipati shaayi - jhankaaradhwani; Talam: aadi', 'aadi', 'phaNi pati 1 SAyi mAm pAtu
2 pAlit(A)bdhi 1pAyi'),
        ('f8b7f2f6-8f23-485b-bf8d-acc343a368ee'::uuid, 'E vidhamulanaina', 'Tyagaraja', 'SankarAbharaNaM', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2331.shtml', 'b391a85c82455bea1513226f0aaf3540f3516da99c10cf2193c3ccbf0a135d89', 'Composition: Evidhamulanaina - shankaraabharaNam; Talam: aadi', 'aadi', 'E vidhamulan(ai)na kAni nann-
( 1 E)lukona manasu rAdA rAma'),
        ('f8fd0f11-c316-40c8-b9d1-25e334dbddd6'::uuid, 'nI dayacE rAma', 'Tyagaraja', 'Yadukula Kāmbhoji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2653.shtml', '710ec0716e26e5fbe78c3eef64a2fa3a1dda8a5e12897ed86ab86ba8fe8f76c4', 'Composition: nee dayacE - yadukula kaambhOji; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'nI dayacE rAma nity(A)nanduD(ai)ti'),
        ('f92c6be0-2781-4d84-9670-541b32447bcc'::uuid, 'Dehi Tava Pada', 'Tyagaraja', 'Sahāna', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/dehitavapada-sahana.htm', '753d6e3904131eaf17a1ba1adb07250a2b89ca9dc37881de30fb65c2c8dc582d', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', 'dEhi tava pada bhaktim
vaidEhi patita pAvani mE sadA'),
        ('f960fdb0-3d6a-4a26-8d93-8af37750f722'::uuid, 'eTulaina bhakti', 'Tyagaraja', 'sAma', 'Misra Capu', 'karnatik.com', 'https://www.karnatik.com/c2320.shtml', '536a8c22dfda4be367b6db07fa2df13e1fd33bcf9a8ea2e2c3193385cd4c8099', 'Composition: eTulaina bhakti - shyaamaa; Talam: caapu', 'caapu', 'eTulaina bhakti vaccuTakE yatnamu sEyavE'),
        ('f9bb7a8a-cedd-4490-b7fb-ed2408d6a5d5'::uuid, 'Laali Laaliyani', 'Tyagaraja', 'Harikāmbhōji', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2525.shtml', 'a64ebc5913a315ca2a375534dceef9611dd93620cbf7f02b6fb54186d76d0b4a', 'Composition: laali laaliyani - harikaambhOji; Talam: aadi', 'aadi', 'lAli lAli(y)ani(y)UcErA 1 vana
mAli mAlimitO jUcErA'),
        ('fa07ba19-5745-48bf-a872-27feea461225'::uuid, 'Nenendu Vetukuduraa', 'Tyagaraja', 'Karnātaka Behāg', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2599.shtml', 'e486369753870562d3a124e384d721bc7e0dce937e28874413eb334fdc497224', 'Composition: nEnendu vEtukuduraa - harikaambhOji; Talam: aadi', 'aadi', 'nEn(e)ndu vetukudurA hari'),
        ('fa581380-0a7d-44a5-ac3f-35fcd907ea61'::uuid, 'Sarva Loka Dayaanidhae', 'Tyagaraja', 'Huseni', 'Tisra Ekam', 'karnatik.com', 'https://www.karnatik.com/c2822.shtml', '2b587c89c563b8ce66e6601836ed2b5d8223c751d00336343c8bdedfe5738ecd', 'Composition: sarvalOka dayaanidhE - husEni; Talam: tishra laghu; Huseni / Tisra Laghu Ekam or Adi (Divyanama)', 'tishra laghu', 'sarva lOka dayA nidhE sArvabhauma dASarathE'),
        ('fa5f98b6-119e-40eb-b50c-637f0284abaf'::uuid, 'Garuda Gamana', 'Tyagaraja', 'Gourimanohari', 'Rupaka', 'karnatik.com', 'https://www.karnatik.com/c2335.shtml', '44a937b44346b596a7046e593e8632354d2870cfcb2853dd0087c86dc9ad66a9', 'Composition: garuDa gamana vaasudEva - gowri manOhari; Talam: roopakam', 'roopakam', 'garuDa gamana vAsudEva
karunatOnu brOvu nannu'),
        ('fbbfd918-c7d6-4f72-a1aa-f27ef5065892'::uuid, 'Kalugunaa', 'Tyagaraja', 'Poornalalita', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2384.shtml', '1c0c8c2697e177ece1c2b727daea2f863f29ec1048bf53b576a0ea28741b5030', 'Composition: kalugunaa pada - poornalalitaa; Talam: aadi', 'aadi', 'palumAru jUcucu brahmAnanduDai paragE bhakt(A)grEsara tanaku'),
        ('fc6630fc-1e9f-4b15-9530-f5f3371f8ef3'::uuid, 'Menu Joochi', 'Tyagaraja', 'Sarasāngi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c2559.shtml', '0233b9640abbb2539d2a5fbfa7102d85a0e38412907ee48f818e71dbe9eb19db', 'Composition: mEnu jooci - sarasaangi; Talam: Adi (Deshadi)', 'Adi (Deshadi)', 'mEnu jUci mOsa pOkavE manasA
lOni 1 jADal(I)lAgu kAdA'),
        ('fcd026e0-0056-48c3-af9f-a8a55c400077'::uuid, 'Venkatesa', 'Tyagaraja', 'Madhyamāvathi', 'Adi', 'karnatik.com', 'https://www.karnatik.com/c1346.shtml', '5a3559bbae7725297bcabf8a57fe0ef9429c5aeacf9b25cdc52a98b144bf75af', 'Composition: venkaTEshaa ninu - madyamaavati; Talam: aadi', 'aadi', 'vEnkaTESa ninu sEvimpanu padi
vEla 2 kanulu kAvalen(a)yya'),
        ('fdbda5e6-0080-4b9d-84bf-6378a9cbd9bb'::uuid, 'Venu Gaana Loluni', 'Tyagaraja', 'kEdAra gauLa', 'Rupaka', 'shivkumar.org', 'https://www.shivkumar.org/music/venuganalola.htm', 'd0105c2a8ad12d30482a2c2f07917ded187cf963a77949860ee12acfaf6c379c', 'Talam header and Pallavi; manually verified spelling Kedaragowlai = kEdAra gauLa; composer and incipit matched', 'Rupakam', 'vENu gAna lOluni kana vEyi kannulu 1 kAvalenE'),
        ('fe316a27-7978-4a95-a260-83977c69bf73'::uuid, 'rAmA ninu nammina', 'Tyagaraja', 'Mohanam', 'Adi', 'shivkumar.org', 'https://www.shivkumar.org/music/ramaninnunammina.htm', '2d298fbdb098175ffbf00c5bbe928be6e8697de0e587f726d98e7756961d389b', 'Composition header: Talam; Pallavi; composer and raga checked against catalogue', 'Adi', '1 rAmA 2 ninu nammina vAramu
kAmA sakala lOk(A)bhi(rAma)');

    FOR r IN SELECT * FROM tmp_tala_backfill LOOP
        SELECT * INTO before_row FROM krithis WHERE id = r.krithi_id FOR UPDATE;
        IF NOT FOUND THEN CONTINUE; END IF;

        -- Strict identity invariant 1: Title must match
        IF before_row.title <> r.title THEN
            RAISE EXCEPTION 'TRACK-144: composition title mismatch on % (expected %, found %)',
                r.krithi_id, r.title, before_row.title;
        END IF;

        -- Strict identity invariant 2: Composer must match
        IF NOT EXISTS (
            SELECT 1 FROM composers c
            WHERE c.id = before_row.composer_id AND c.name = r.composer
        ) THEN
            RAISE EXCEPTION 'TRACK-144: composition composer mismatch on % (expected %)',
                r.krithi_id, r.composer;
        END IF;

        -- Strict identity invariant 3: Primary raga must match
        IF NOT EXISTS (
            SELECT 1 FROM ragas rg
            WHERE rg.id = before_row.primary_raga_id AND rg.name = r.raga
        ) THEN
            RAISE EXCEPTION 'TRACK-144: composition raga mismatch on % (expected %)',
                r.krithi_id, r.raga;
        END IF;

        -- Strict identity invariant 4: Stored Latin lyrics must match expected incipit
        IF NOT EXISTS (
            SELECT 1 FROM krithi_lyric_variants v
            LEFT JOIN krithi_lyric_sections ls ON ls.lyric_variant_id = v.id
            LEFT JOIN krithi_sections s ON s.id = ls.section_id AND s.krithi_id = v.krithi_id
            WHERE v.krithi_id = r.krithi_id AND v.script = 'latin'
              AND (
                  (ls.text IS NOT NULL AND regexp_replace(lower(ls.text), '[^a-z]', '', 'g') LIKE '%' || regexp_replace(lower(r.expected_pallavi), '[^a-z]', '', 'g') || '%')
                  OR
                  (v.lyrics IS NOT NULL AND regexp_replace(lower(v.lyrics), '[^a-z]', '', 'g') LIKE '%' || regexp_replace(lower(r.expected_pallavi), '[^a-z]', '', 'g') || '%')
              )
        ) THEN
            RAISE EXCEPTION 'TRACK-144: stored Latin incipit does not match for % (expected %)',
                r.krithi_id, r.expected_pallavi;
        END IF;

        SELECT id INTO STRICT target_tala_id FROM talas WHERE name = r.proposed_tala;
        SELECT id INTO STRICT source_id FROM import_sources WHERE name = r.source_name;

        -- Verify and apply tala assignment if not already set
        IF before_row.tala_id <> target_tala_id THEN
            IF before_row.tala_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM talas WHERE id = before_row.tala_id AND name = 'Unknown') THEN
                RAISE EXCEPTION 'TRACK-144: intervening tala edit on %', r.krithi_id;
            END IF;

            UPDATE krithis SET tala_id = target_tala_id, updated_at = clock_timestamp()
            WHERE id = r.krithi_id RETURNING * INTO after_row;

            INSERT INTO audit_log(entity_table, entity_id, action, diff, metadata)
            VALUES ('krithis', r.krithi_id, 'UPDATE',
                jsonb_build_object('before', to_jsonb(before_row), 'after', to_jsonb(after_row)),
                jsonb_build_object(
                    'track', 'TRACK-144',
                    'source', r.source_name,
                    'source_url', r.source_url,
                    'source_locator', r.source_locator,
                    'raw_tala', r.raw_tala,
                    'proposed_tala', r.proposed_tala,
                    'method', 'corpus_tala_backfill'
                ));

            IF NOT EXISTS (
                SELECT 1 FROM krithi_source_evidence
                WHERE krithi_id = r.krithi_id AND import_source_id = source_id AND source_url = r.source_url
            ) THEN
                INSERT INTO krithi_source_evidence(
                    krithi_id, import_source_id, source_url, source_format, extraction_method,
                    checksum, contributed_fields, raw_extraction
                ) VALUES (
                    r.krithi_id, source_id, r.source_url,
                    CASE WHEN r.source_url LIKE '%.pdf' THEN 'PDF' ELSE 'HTML' END,
                    'MANUAL',
                    COALESCE(NULLIF(r.source_checksum, ''), md5(r.source_url)),
                    ARRAY['tala'],
                    jsonb_build_object(
                        'track', 'TRACK-144',
                        'source', r.source_name,
                        'raw_tala', r.raw_tala,
                        'proposed_tala', r.proposed_tala,
                        'source_locator', r.source_locator
                    )
                ) RETURNING * INTO evidence_row;

                INSERT INTO audit_log(entity_table, entity_id, action, diff, metadata)
                VALUES ('krithi_source_evidence', evidence_row.id, 'CREATE',
                    jsonb_build_object('after', to_jsonb(evidence_row)), '{"track":"TRACK-144"}'::jsonb);
            END IF;
        END IF;
    END LOOP;

    --------------------------------------------------------------------------
    -- Part 4: Invalidate Stale Document Embeddings
    --------------------------------------------------------------------------
    -- V63 prematurely synchronized document_embeddings.content_hash to search_documents.content_hash
    -- after updating indexed_content ([Tala: Unknown] -> [Tala: <name>]), without regenerating vectors.
    -- Reset content_hash on all affected document_embeddings so the refresh checker detects them.
    UPDATE document_embeddings de
    SET content_hash = 'STALE_TRACK_144_NEEDS_REBUILD'
    FROM search_documents sd
    WHERE de.document_id = sd.id
      AND sd.krithi_id IN (SELECT krithi_id FROM tmp_tala_backfill);

END $track144_v64$;
