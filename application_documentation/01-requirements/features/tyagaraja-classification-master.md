| Metadata | Value |
|:---|:---|
| **Status** | Draft — classification skeleton |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-10-01 |
| **Author** | Sangita Grantha Team |

# Tyagaraja classification master

---

Generated from the TRACK-147 structured master. Claims in this document are unreviewed. None are approved for seeding.

Related track: [TRACK-147](../../../conductor/tracks/TRACK-147-tyagaraja-musicological-classification.md). Structured master: [track147-tyagaraja-master.json](../../../database/data/track147-tyagaraja-master.json).

> [!IMPORTANT]
> Eight catalogue identities are collisions. They are listed in the collision register and are not classification subjects. Their source rows are the subjects.

## Accounting

Snapshot date: 2026-10-01. Composer id `b1bdccd8-8cb1-4a14-a9e6-7ffc843cd7b6`.

| Unit | Count |
|:---|---:|
| Catalogue identities | 675 |
| Source rows | 684 |
| Single-source subjects | 667 |
| Collision subjects | 17 |
| Collision identities, not classified | 8 |
| Classification subjects | 684 |

## Collision register

Each row is one source page. The catalogue title is the current collapsed identity.

| Catalogue title | Source setting | Subject | Import status |
|:---|:---|:---|:---|
| SrI nArasiMha | phala ranjani / Unknown | tyagaraja:import:40e2d47b-a0c5-4e44-b301-8cdd05b9589d | mapped |
| SrI nArasiMha | bilahari / Isra Capu | tyagaraja:import:733ec39f-27a2-4f8e-a703-e88196a5ab09 | mapped |
| Sri Rama Jaya Rama | Madhyamavati / Unknown | tyagaraja:import:1f73ab63-0278-4b61-85ce-3fbacbce1ba0 | mapped |
| Sri Rama Jaya Rama | varALi / Isra Capu | tyagaraja:import:3916fad4-5ef9-43ba-af21-5df0f233566c | mapped |
| Rama Rama Rama Sita | Huseni / Unknown | tyagaraja:import:3115095a-36f9-4999-b32c-d8a063268cd9 | mapped |
| Rama Rama Rama Sita | sAvEri / Adi | tyagaraja:import:3946f1d2-2a27-4591-b504-5e334d51b53e | mapped |
| paripUrNa kAma | hamsa bhramari / Adi | tyagaraja:import:0082d54a-2341-48c0-9b54-d7039232b77c | approved |
| paripUrNa kAma | pUri kalyANi / Adi | tyagaraja:import:4707048d-f06e-46f0-9202-3b985cee65b2 | approved |
| rAma sItA rAma | SankarAbharaNaM / Unknown | tyagaraja:import:1daabb05-6208-44a6-89c0-dc48be8fb187 | approved |
| rAma sItA rAma | balahamsa / Unknown | tyagaraja:import:ab6c4e31-d6a7-474d-ac72-e30adb14ecaf | mapped |
| jaya mangaLaM | ghaNTA / Jhampa | tyagaraja:import:5d70a340-0137-4dfd-baa8-596ccaebf89f | mapped |
| jaya mangaLaM | mOhanaM / Unknown | tyagaraja:import:6eb15e4d-fcd0-45e1-9edd-969928695354 | mapped |
| jaya mangaLaM | nAda nAmakriya / Adi | tyagaraja:import:b26f89a5-7924-43e8-8ee6-c50d0a8bb6f1 | mapped |
| Sri Narada Muni | Bhairavi / Unknown | tyagaraja:import:84ffdbb8-6845-4747-9127-b0a46cf65630 | mapped |
| Sri Narada Muni | pantuvarALi / Rupakam | tyagaraja:import:c702d09d-b5a5-4b16-82ac-5fc01e265dd9 | mapped |
| pAhi rAma candra | SankarAbharaNaM / Unknown | tyagaraja:import:3943b77a-d7b2-4f7f-8715-03ba0e5e2eb7 | approved |
| pAhi rAma candra | yadukula kAmbhOji / Unknown | tyagaraja:import:604a9623-f1b0-4efc-9486-ea415b219870 | in_review |

## Source inventory

`database/for_import/Thyagaraja-Krithi-For-Import.csv` (SHA-256 `2effb1483e352efa55e5d9fec6c5381a8c79091fcc565d2ab51bf13ab9f77998`) has 687 rows. Each row is matched to a subject by exact URL. Row numbers are locators, not identities.

| Disposition | CSV rows |
|:---|---:|
| classification_subject | 684 |
| cross_composer_mapping | 2 |
| not_imported | 1 |

Rows that are not Tyagaraja classification subjects:

| Row | CSV title | CSV raga | Disposition | Mapped identity | Note |
|---:|:---|:---|:---|:---|:---|
| 241 | karuNa jUDavamma | tODi | cross_composer_mapping | Syama Sastri `cf983d2a-154e-4903-9670-6ea1a1172149` | Import row (raw tODi / Adi, status mapped) points at the Syama Sastri identity karuNa jUDavammA, stored in Varāli. Composer and raga disagree. TRACK-146 flags the same witness. Not a merge decision. |
| 557 | sItA nAyaka | rIti gauLa | not_imported |  | No imported_krithis row has this source_key in the 2026-10-01 census. Import outcome unknown. |
| 587 | SrI gaNa nAthaM | kanakAngi | cross_composer_mapping | Muthuswami Dikshitar `e8fdd062-5beb-43fd-906d-51afe0df3b50` | Import row (raw kanakAngi / Adi, status mapped) points at the Dikshitar identity SrI gaNa nAthaM, stored in Eeshamanohari. Composer and raga disagree. Not a merge decision. |

## External gap

This figure is not a list of missing compositions. No titles are invented for it.

| Label | Reported count | Status |
|:---|---:|:---|
| track093_target_692 | 692 | unresolved_external |

## Unresolved register

- 8 collapsed identities stay `collision_not_classified`.
- 3 CSV rows are outside the Tyagaraja catalogue. Their composer and identity stay unresolved.
- Deity, geography, theme, ritual, and dramatic claims are `unreviewed` on every subject.
- 5 Ghana-raga Pancharatnam group claims are automated suggestions from the title-heuristic tag. They use Aradhana performance order and are not approved memberships.
- Kovur, Tiruvottiyur, Lalgudi, Srirangam, Utsava Sampradaya, Divyanama, Nauka Charitram, and Prahlada Bhakti Vijayam are not assigned.
- 27 identities have no pallavi section and 4 have no charanam. Lyric text was not invented.
- 11 subjects preserve a `desadi` source tala stored as Adi.

## Concordance

| Subject | Kind | Title | Canonical raga | Canonical tala | Raw raga | Raw tala | Status | Audit |
|:---|:---|:---|:---|:---|:---|:---|:---|:---|
| tyagaraja:import:0082d54a-2341-48c0-9b54-d7039232b77c | witness | paripUrNa kAma | pUri kalyANi | Adi | hamsa bhramari | Adi | approved |  |
| tyagaraja:import:1daabb05-6208-44a6-89c0-dc48be8fb187 | witness | rAma sItA rAma | Bālahamsa | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:import:1f73ab63-0278-4b61-85ce-3fbacbce1ba0 | witness | Sri Rama Jaya Rama | Madhyamāvathi | Adi | Madhyamavati | Unknown | mapped | missing_pallavi |
| tyagaraja:import:3115095a-36f9-4999-b32c-d8a063268cd9 | witness | Rama Rama Rama Sita | Sāveri | Adi | Huseni | Unknown | mapped |  |
| tyagaraja:import:3916fad4-5ef9-43ba-af21-5df0f233566c | witness | Sri Rama Jaya Rama | Madhyamāvathi | Adi | varALi | Isra Capu | mapped | missing_pallavi |
| tyagaraja:import:3943b77a-d7b2-4f7f-8715-03ba0e5e2eb7 | witness | pAhi rAma candra | Yadukula Kāmbhoji | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:import:3946f1d2-2a27-4591-b504-5e334d51b53e | witness | Rama Rama Rama Sita | Sāveri | Adi | sAvEri | Adi | mapped |  |
| tyagaraja:import:40e2d47b-a0c5-4e44-b301-8cdd05b9589d | witness | SrI nArasiMha | Phalaranjani | Adi | phala ranjani | Unknown | mapped |  |
| tyagaraja:import:4707048d-f06e-46f0-9202-3b985cee65b2 | witness | paripUrNa kAma | pUri kalyANi | Adi | pUri kalyANi | Adi | approved |  |
| tyagaraja:import:5d70a340-0137-4dfd-baa8-596ccaebf89f | witness | jaya mangaLaM | Nadanamakriya | Adi | ghaNTA | Jhampa | mapped |  |
| tyagaraja:import:604a9623-f1b0-4efc-9486-ea415b219870 | witness | pAhi rAma candra | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | in_review |  |
| tyagaraja:import:6eb15e4d-fcd0-45e1-9edd-969928695354 | witness | jaya mangaLaM | Nadanamakriya | Adi | mOhanaM | Unknown | mapped |  |
| tyagaraja:import:733ec39f-27a2-4f8e-a703-e88196a5ab09 | witness | SrI nArasiMha | Phalaranjani | Adi | bilahari | Isra Capu | mapped |  |
| tyagaraja:import:84ffdbb8-6845-4747-9127-b0a46cf65630 | witness | Sri Narada Muni | Bhairavi | Adi | Bhairavi | Unknown | mapped | missing_pallavi |
| tyagaraja:import:ab6c4e31-d6a7-474d-ac72-e30adb14ecaf | witness | rAma sItA rAma | Bālahamsa | Adi | balahamsa | Unknown | mapped |  |
| tyagaraja:import:b26f89a5-7924-43e8-8ee6-c50d0a8bb6f1 | witness | jaya mangaLaM | Nadanamakriya | Adi | nAda nAmakriya | Adi | mapped |  |
| tyagaraja:import:c702d09d-b5a5-4b16-82ac-5fc01e265dd9 | witness | Sri Narada Muni | Bhairavi | Adi | pantuvarALi | Rupakam | mapped | missing_pallavi |
| tyagaraja:krithi:0040c0f8-7f32-44f7-85ef-0eb0ec2e05f1 | identity | nADADina mATa | Jana Ranjani | Misra Capu | jana ranjani | Isra Capu | approved |  |
| tyagaraja:krithi:005bc2ed-d9c5-4133-9071-df929a5e25f9 | identity | hari hari nIyokka | Punnagavarali | Adi | punnAga varALi | Adi | approved |  |
| tyagaraja:krithi:00d7ec02-dd0c-423b-8862-3fe5e24923e2 | identity | rAma rAma rAma mAM pAhi | yamunA kalyANi | Adi | yamunA kalyANi | Adi | approved |  |
| tyagaraja:krithi:01166b8b-a60b-4eac-9895-1d3feae3f94f | identity | rAma rAma gOvinda | saurAshTraM | Adi | saurAshTraM | Unknown | approved |  |
| tyagaraja:krithi:02eb0b19-f09f-4fb5-8877-c7366bf0a59c | identity | mAmava satataM | Jaganmohini | Adi | jaganmOhini | Adi | approved |  |
| tyagaraja:krithi:03341b3f-7e95-42ed-a669-1efd9cbd91d1 | identity | vara nArada | Vijayashree | Adi | vijaya SrI | Adi | approved |  |
| tyagaraja:krithi:04ab08e7-0967-4a18-a070-0778232fb3dc | identity | vishNu vAhanuDu | SankarAbharaNaM | Rupaka | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:04d55f5a-b278-469c-998e-d2dfc02f9939 | identity | entanucu varNintunE | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:04d710ce-ff2a-4319-a057-2f45f5f3d2a4 | identity | vAsudEvayani | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:055d1261-b0bd-477f-a231-b69780ab60c9 | identity | Koluvaiyunnaadae | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:0582b4fa-d55a-4756-9428-566fc120e361 | identity | SrI raghu vara dASarathE | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:05868726-8f74-4dbc-99c9-379a982df9f8 | identity | tatvameruga | Garudadhvani | Rupaka | garuDa dhvani | Rupakam | approved |  |
| tyagaraja:krithi:0595d4fd-e943-450d-b3cd-12bc11dcaf7a | identity | Nee Bhakti | Jayamanohari | Misra Capu | Jaya Manohari | Unknown | approved |  |
| tyagaraja:krithi:06a450bb-c569-4409-aa2a-973ea4575797 | identity | rAma nIvE kAni | Nārāyani | Adi | nArAyaNi | Unknown | approved |  |
| tyagaraja:krithi:06f0c5bd-c58f-43bd-aa22-4efacbbc6351 | identity | Saraseeruhaanana | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:0718d8a2-6b19-4a0f-a56b-967679017098 | identity | intakanna telpa | Sāveri | Capu | sAvEri | Capu | approved |  |
| tyagaraja:krithi:0745ba3f-6cd8-4c9a-b4b6-092fa6620960 | identity | bAgAyenayya | Chandrajyothi | Adi | candra jyOti | Adi | approved |  |
| tyagaraja:krithi:0762a13c-331d-4d0a-8837-28b8f94865db | identity | Nannu Brova Neekinta | Abhogi | Adi | Abhogi | Unknown | approved |  |
| tyagaraja:krithi:076dd213-8b5d-4f79-bf15-6ab19effa363 | identity | mAnasa sancararE | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:0779ebfd-09f1-41be-b241-a7d333dc6dd3 | identity | para lOka bhayamu | Mandāri | Adi | mandAri | Adi | approved |  |
| tyagaraja:krithi:07df20a5-a4c7-418f-8f29-1d9253fc111f | identity | kanugoNTini | Bilahari | Adi | bilahari | Desadi | mapped | missing_charanam |
| tyagaraja:krithi:081c6116-428c-4143-aae7-f1b0a562280d | identity | maravakarA | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:0879def4-4c65-4d11-9377-6be31e34d0cb | identity | Raaraa Seetha Ramani | Hindolavasanta | Rupaka | Hindola Vasanta | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:08d27bad-2d7b-463f-a661-0c54c760ace2 | identity | cEsinadella | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:09216653-9ebd-4df0-82db-c374e18068d7 | identity | bhajana parulakEla | suraTi | Rupaka | suraTi | Unknown | approved |  |
| tyagaraja:krithi:09266c45-7c28-4dba-921e-42d95393d6e8 | identity | enta vEDukondu | Saraswathi Manohari | Adi | sarasvati manOhari | Unknown | approved |  |
| tyagaraja:krithi:0931f98a-64fa-403f-967c-941677395832 | identity | karuNa jUDavayya | Sāranga | Adi | sAranga | Adi | approved |  |
| tyagaraja:krithi:09fc370e-ec19-482a-9519-41b0d903125c | identity | valla kAdanaka | SankarAbharaNaM | Rupaka | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:0a7dd82c-98da-4aa8-87b2-004d6d15cf4b | identity | bhajarE raghuvIraM | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:0aae2382-ec72-4d8f-90e1-62aba112b37a | identity | Naadupai | Madhyamāvathi | Khanda Capu | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:0b16335f-76ce-40f6-8404-36ca524bc9c3 | identity | Nadachi Nadachi | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:0b317a6b-ddb4-4e32-a960-c003f1b243b2 | identity | SObhillu sapta svara | Jaganmohini | Rupaka | jaganmOhini | Rupakam | approved |  |
| tyagaraja:krithi:0b5f8096-d9e5-4ee2-b849-4bed529ae9de | identity | sarasIruha nayanE | amRta varshiNi | Adi | amRta varshiNi | Adi | approved |  |
| tyagaraja:krithi:0b84442c-d3eb-4765-be36-d9b98ecf8a96 | identity | ennaga manasuku | Neelāmbari | Adi | nIlAmbari | Adi | approved |  |
| tyagaraja:krithi:0bd37b74-295a-4dda-aa04-b3448f89b4a7 | identity | Sogasu Jooda | kannaDa gauLa | Rupaka | Kannada Gaula | Unknown | approved |  |
| tyagaraja:krithi:0c4d94e7-3ccd-4546-a153-7e2138792820 | identity | aDugu varamula | Ārabhi | Misra Capu | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:0c6cadaa-bab0-4224-af70-571fdaab646a | identity | eTula brOtuvO | Chakravākam | Triputa | cakravAkaM | Triputa | approved |  |
| tyagaraja:krithi:0c91baf8-8070-4144-bb5b-f54c93974c07 | identity | karuNA samudra | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:0d1fe89d-3fdf-408e-aa22-3e667614637e | identity | enduku peddala | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:0d9065ca-f1ce-4426-87f1-a02c2145ba33 | identity | Maa Jaanaki | Kāmbhoji | Adi | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:0d9a0e4a-7265-4683-bf40-ab2c1596cb88 | identity | marakata maNi | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:0db0b6cc-0fcf-4076-b654-3cf019a4a4b4 | identity | Siva Siva | Panthuvarāli | Adi | pantuvarALi | Adi | mapped |  |
| tyagaraja:krithi:0df490a3-9675-4f43-b370-3c0b42ee933f | identity | uyyAlalUgavayya | Neelāmbari | Khanda Capu | nIlAmbari | Khanda Capu | approved |  |
| tyagaraja:krithi:0e2cb747-b347-49a2-a477-da67dc559557 | identity | Mridu Bhaashana | Maruvadhanyāsi | Adi | Maruva Dhanyasi | Unknown | approved |  |
| tyagaraja:krithi:0e4f787a-94a0-42e3-be60-da868e838de4 | identity | daNDamu | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:0ebeaa05-5481-4300-ac39-70986688efed | identity | Chentanae Sadaa | Kunthalavarāli | Adi | Kuntala Varali | Unknown | approved |  |
| tyagaraja:krithi:0f06796a-2f9a-489b-9b41-18a659c30754 | identity | Jo Jo Rama | Reethigowla | Adi | Riti Gaula | Unknown | approved |  |
| tyagaraja:krithi:0f39f77c-d5b8-467a-915a-3df2e4e5fadf | identity | ennaDu jUtunO | Kalāvati | Adi | kalAvati | Adi | approved |  |
| tyagaraja:krithi:1100e4b9-0f07-4e9c-996b-e5aedf9444a3 | identity | Tanayandae Prema | Bhairavi | Triputa | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:110eac88-12e8-4638-a35a-f753fb39010e | identity | Evarunnaaru Brova | Mālavashree | Adi | Malava Sri | Unknown | approved |  |
| tyagaraja:krithi:1156c31c-e939-4643-b650-9faa062bb33e | identity | Chakkani Raja | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:11aca4c2-2d69-44b0-93c8-5000619555c2 | identity | inka yOcanaitE | Ghanta | Adi | ghaNTA | Adi | approved |  |
| tyagaraja:krithi:11b3833e-634a-4018-b159-485576ccc8c2 | identity | aparAdhamulanOrva | rasALi-vanALi-vanAvaLi | Adi | rasALi-vanALi-vanAvaLi | Adi | approved |  |
| tyagaraja:krithi:125e543a-cb8c-462a-9b51-fe981e8e4337 | identity | pAlintuvO | Kāntāmani | Adi | kAntAmaNi | Adi | approved |  |
| tyagaraja:krithi:1270d6f4-f7c3-4714-8dfe-19932d2a58d0 | identity | kAru vElpulu | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:12ab2d34-a5d6-4ba6-9923-92cf53c8ce3a | identity | balamu kulamu | Sāveri | Rupaka | sAvEri | Rupakam | approved |  |
| tyagaraja:krithi:14973eb2-3f3a-49b9-ac25-728a110ffa46 | identity | Sanaatana | Phalamanjari | Adi | Phala Manjari | Unknown | approved |  |
| tyagaraja:krithi:14c51c11-fc2e-4bc4-bd10-519bcd48d00f | identity | nI nAma rUpamulaku | saurAshTraM | Adi | saurAshTraM | Adi | approved |  |
| tyagaraja:krithi:156cb2fc-cea9-41eb-b3a9-79378063f6f0 | identity | idi samayamurA | chAyA nATa | Adi | chAyA nATa | Adi | approved |  |
| tyagaraja:krithi:1585860b-29ed-4b34-904e-ea2c45dc5434 | identity | innALLu nannEli | Ghanta | Capu | ghaNTA | Capu | approved |  |
| tyagaraja:krithi:15f7defb-f208-4eea-b4ca-9cc671862c6a | identity | toli nEnu jEsina | Kokiladhwani | Adi | kOkila dhvani | Unknown | approved |  |
| tyagaraja:krithi:16d66530-977b-4535-ac28-eb80c8b301a9 | identity | lIlagAnu jUcu | divya maNi-dundubhi | Adi | divya maNi-dundubhi | Adi | approved |  |
| tyagaraja:krithi:17cf2d35-e811-4f63-8b25-71d17e25e27d | identity | Manasaa Sri Ramuni | Māraranjani | Adi | Mara Ranjani | Unknown | approved |  |
| tyagaraja:krithi:17e53b19-6e57-4c5b-b3c6-05cf6af2f3ff | identity | Maaru Palkaga | Ranjani | Adi | Sri Ranjani | Unknown | approved |  |
| tyagaraja:krithi:1818bb71-e19c-499e-924a-0e6fbcc9d937 | identity | nAyeDa vancana | Nabhomani | Adi | nabhOmaNi | Adi | approved |  |
| tyagaraja:krithi:18202aae-9d3c-41e4-96fd-80914610cabf | identity | ninu vinA nA madi | Navarasa kannada | Rupaka | navarasa kannaDa | Unknown | approved |  |
| tyagaraja:krithi:189bc0d1-e96b-4e26-9de8-ff55f51321a3 | identity | Ennaallu Tirigedi | Mālavashree | Adi | Malava Sri | Unknown | approved |  |
| tyagaraja:krithi:18ace057-6496-48db-904d-e88db708a1c6 | identity | nI pada pankaja | Begada | Adi | bEgaDa | Adi | approved |  |
| tyagaraja:krithi:18ae493e-7724-40a7-b7c1-f1d861202b93 | identity | Inkaa Daya | nArAyaNa gauLa | Adi | Narayana Gaula | Unknown | approved |  |
| tyagaraja:krithi:195994c2-534a-4950-be10-ce9ba7a7f0cf | identity | pAhi paramAtma | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:198cf9c3-a2f0-4345-b0a7-c0be4f2f8d99 | identity | prArabdham | Swarāvali | Jhampa | svarAvaLi | Unknown | approved |  |
| tyagaraja:krithi:1a5f7193-3f44-4170-b510-d13b78ad9390 | identity | Neeke Teliyaka | Anandabhairavi | Adi | Ananda Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:1a9e1fa0-2319-4a2b-afa2-311d2f7324b2 | identity | samayamu EmarakE | kalgaDa | Adi | kalgaDa | Adi | approved |  |
| tyagaraja:krithi:1b751137-9e78-4a63-9036-2f648bf2dcec | identity | adi kAdu bhajana | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:1b7baaeb-ee79-408c-b860-3a4c72e127b3 | identity | Chootaamu Raareyee Vedkanu | Kāpi | Misra Capu | Kapi | Unknown | approved |  |
| tyagaraja:krithi:1b7be621-ec0f-4cf8-b6ba-3be43fafbc8c | identity | rAmAbhirAma raghurAma | Sāveri | Jhampa | sAvEri | Jhampa | mapped |  |
| tyagaraja:krithi:1b89544a-16fb-4433-93c8-b3840dbfe10a | identity | Nalina Lochana | Madhyamāvathi | Misra Capu | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:1b936da8-9b2a-4cc1-af40-00d40ec0d8c8 | identity | SambhO mahA dEva | Panthuvarāli | Rupaka | pantuvarALi | Rupakam | approved |  |
| tyagaraja:krithi:1c1e58ca-f6d3-4a4d-981f-f4025d8b5d7b | identity | pAhi pAhi dIna bandhO | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:1cc1eb20-bad0-45d7-8688-b4d32f6edea7 | identity | Dvaitamu Sukhamaa | Reethigowla | Adi | Riti Gaula | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:1cc88aff-8c5f-4d2d-b8ad-32d122bbcab8 | identity | Tanayuni Brova | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:1ccbd525-96aa-4e90-a999-ff7b2ec7ce7f | identity | daSaratha nandana | Asāveri | Adi | asAvEri | Adi | approved |  |
| tyagaraja:krithi:1d2d8060-66e8-48e4-933b-62a7ff17cb28 | identity | Rama Paahi | Kāpi | Misra Capu | Kapi | Unknown | approved |  |
| tyagaraja:krithi:1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4 | identity | Ivasudha Neevanti | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:1e8717c9-7316-4de5-9ed4-5a026a9722bc | identity | ninu bAsi | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:1eb54ff8-6e91-4195-aa3b-e11360ecdfdb | identity | enta bhAgyamu | Sāranga | Adi | sAranga | Adi | mapped |  |
| tyagaraja:krithi:1ebf6bb3-f892-417d-aecd-cf8011706d3f | identity | mummUrtulu | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:1ef669fc-6fa2-446e-97de-1e9127778eb0 | identity | SrI rAma raghu rAma | Yadukula Kāmbhoji | Jhampa | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:1f4904e8-42a2-43df-958d-a2c2ebdfb9ca | identity | giri rAja sutA | Bangāla | Adi | bangALa | Desadi | approved |  |
| tyagaraja:krithi:1f53b74e-3fa8-4f0a-b0f6-ccde12faf39d | identity | SambhO Siva | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:1f930f28-ac22-4e5a-a219-37ec6bd88e94 | identity | Nannu Vidichi | Reethigowla | Misra Capu | Riti Gaula | Unknown | approved |  |
| tyagaraja:krithi:1fa2297e-2e46-4341-bfc7-faf18f161a78 | identity | Narada Guru Svami | Darbar | Adi | Darbar | Unknown | approved |  |
| tyagaraja:krithi:208081e1-0c9c-44f1-90bf-ae4b337238b0 | identity | Nagu Momu Kana Leni | Abheri | Adi | Abheri | Unknown | approved |  |
| tyagaraja:krithi:209b89f7-44a8-4d47-a939-c70b8db7e919 | identity | Manavini Vinumaa | Jayanārāyani | Adi | Jaya Narayani | Unknown | approved |  |
| tyagaraja:krithi:211ce953-0b8c-4d7a-a8d2-0415d5cfc72e | identity | unna tAvuna | Ghanta | Capu | ghaNTA | Capu | approved |  |
| tyagaraja:krithi:2192caff-da11-4a23-81ad-3036d3cdd173 | identity | sundari nI divya | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:21d2c1a9-f3f0-437c-82b6-d6a91fcd0543 | identity | ninnanavalasina | Mechakalyāni | Misra Capu | kalyANi | Isra Capu | approved |  |
| tyagaraja:krithi:21e5206c-591b-42cc-81cf-0f59f78939b4 | identity | Chinthisthunnaadae | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:21f76636-f967-49e3-a299-ea95d6cb906a | identity | parulanu vEDanu | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:22ad2fcd-21d3-4a11-9236-6a651049674f | identity | Paahi Parama Dayaalo | Kāpi | Adi | Kapi | Unknown | approved |  |
| tyagaraja:krithi:23a58ec7-e5c1-46f5-a459-303300cf5002 | identity | paripAlaya dASarathE | SankarAbharaNaM | Triputa | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:24850293-86b2-4e38-a786-91d36dd79354 | identity | A daya SrI raghu vara | Āhiri | Adi | Ahiri | Adi | approved |  |
| tyagaraja:krithi:24c49b68-15a6-47ed-abea-0e1bf6a2616e | identity | talli taNDrulu | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:24e54ad6-b8d5-4e7e-b015-6cbcfa1ccf38 | identity | Mokshamu Galadaa | Sāramati | Adi | Saramati | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:24e91683-f7ee-4a88-87a6-f9013ae5a626 | identity | allakallOlamu | saurAshTraM | Adi | saurAshTraM | Adi | mapped |  |
| tyagaraja:krithi:254ba553-c1d3-4678-aa99-34f92acd44f9 | identity | Vinaayakuni | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:2550fc36-84a5-4f93-8b30-ccf33224ff39 | identity | nIvADa nE gAna | Sāranga | Jhampa | sAranga | Jhampa | approved |  |
| tyagaraja:krithi:2634061d-05f7-43c4-850f-b222ce1e3d7f | identity | evaru teliyanu poyyedaru | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:265850ff-accb-4515-a53d-0c05efdb151f | identity | Saranu Sarananucu | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:26fe209d-d385-416f-8c88-e007f0f2380a | identity | SrI raghu vara karuNAkara | Devagāndhāri | Adi | dEva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:271a1479-c84d-4c22-af76-040f659fce2b | identity | gati nIvani | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:27c515fc-4a3f-4928-87af-da95d769570d | identity | mOsa pOku | gauLipantu | Adi | gauLipantu | Adi | approved |  |
| tyagaraja:krithi:27f23797-2404-42d6-ab94-ca817fd6127d | identity | mAkElarA vicAramu | Ravi Chandrikā | Adi | ravi candrika | Unknown | approved |  |
| tyagaraja:krithi:27f8e742-e72a-4f8c-afc6-52dd4378bd6a | identity | aTla palukuduvu | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:280e5aa5-1617-439a-888b-7e5dd752d0bb | identity | ninnE nera | Panthuvarāli | Rupaka | pantuvarALi | Rupakam | approved |  |
| tyagaraja:krithi:2837196d-84e8-4772-b5e1-db163e21a8af | identity | Vinataa Suta Vaahanudai | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:28935d89-dbfb-4d3b-8ce5-094c94136ab5 | identity | virAja turaga | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:28ad469d-6389-46d8-862a-d7fb5a79e3a6 | identity | nI daya rAdA | Vasantabhairavi | Rupaka | vasanta bhairavi | Rupakam | approved |  |
| tyagaraja:krithi:28f4d47f-3e15-44e9-b178-559f6cbd048b | identity | Kanulu Taakani | Kalyāna Vasantam | Rupaka | Kalyana Vasanta | Unknown | approved |  |
| tyagaraja:krithi:2905868f-4b50-4303-b6f0-f44d24e6b3ba | identity | sAkEta nikEtana | Kannada | Rupaka | kannaDa | Rupakam | approved |  |
| tyagaraja:krithi:29542e1b-7e94-438c-ab5c-381853d8ee4e | identity | nApAli SrI rAma | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:2973dc12-38d4-432d-add6-68d573b4b9d0 | identity | bhava sannuta | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:299dd78b-38ae-4146-baaa-b76374d8e76e | identity | intanucu varNimpa | Gundakriya | Adi | guNDakriya | Adi | approved |  |
| tyagaraja:krithi:2a547e61-f7ef-490e-90c5-f1a5443bd52f | identity | parAku jEsina | Jujahuli | Adi | jujAhuli | Adi | approved |  |
| tyagaraja:krithi:2a88986a-0b3e-4cb8-8cad-5d144aa62f7f | identity | bRndAvana lOla | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:2ae151d0-4293-45ca-ac0b-ed97cf7bfe3d | identity | idi nIku mEra | Punnagavarali | Adi | punnAga varALi | Adi | approved |  |
| tyagaraja:krithi:2b65609e-9fbf-4232-baae-096cafdf4e79 | identity | Endaro Mahaanubhaavulu | Sri | Adi | Sri Ragam | Unknown | approved |  |
| tyagaraja:krithi:2bfd07c0-6310-4199-b91c-5c45088c3a35 | identity | muddu mOmu | Sūryakāntam | Adi | sUrya kAntaM | Adi | approved |  |
| tyagaraja:krithi:2c08a5d7-d828-459d-adf8-ceac8baec957 | identity | suguNamulE | Chakravākam | Rupaka | cakravAkaM | Rupakam | approved |  |
| tyagaraja:krithi:2c1b9506-be59-4666-810b-172d3b3a3734 | identity | cUtAmu rArE | Ārabhi | Rupaka | Arabhi | Rupakam | approved |  |
| tyagaraja:krithi:2c318386-f377-416c-972d-41506897e0b5 | identity | SrI rAma dAsa dAsOhaM | Dhanyāsi | Capu | dhanyAsi | Capu | approved |  |
| tyagaraja:krithi:2c7ec18a-c429-402e-a8d6-ea963de31108 | identity | Kula Birudunu | Deva Manohari | Rupaka | Deva Manohari | Unknown | approved |  |
| tyagaraja:krithi:2d0ab787-2a6c-414b-868b-c0512661b2d7 | identity | Nijamaitae Mundara | Bhairavi | Triputa | Bhairavi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:2d20f97d-a762-4b59-b399-5012d1c6d301 | identity | SaSi vadana | Chandrajyothi | Adi | candra jyOti | Adi | approved |  |
| tyagaraja:krithi:2d40a745-53c0-4325-a7ce-7882158aef5d | identity | mEru samAna dhIra | mAyA mALavagauLa | Adi | mAyA mALava gauLa | Adi | approved |  |
| tyagaraja:krithi:2e115182-dcc2-4a41-962c-8381821a7489 | identity | jagadAnanda kAraka | Nāṭṭai | Adi | nATa | Adi | approved |  |
| tyagaraja:krithi:2e22c27b-4b03-4431-970e-af5b963d4082 | identity | rArA raghuvIra | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:2e539247-6fc1-4880-bbf0-5ad079d5d7bd | identity | Sri Rama Paadamaa | Amrta Vahini | Adi | Amrta Vahini | Unknown | approved |  |
| tyagaraja:krithi:2f4496f6-a4f8-49ba-9051-004d950f8948 | identity | Ehi tri-jagadISa | Sāranga | Misra Capu | sAranga | Isra Capu | approved |  |
| tyagaraja:krithi:306f67e3-ca51-4cc0-908f-99474ee6d030 | identity | Emaanaticchevo | Sahāna | Rupaka | Sahana | Unknown | approved |  |
| tyagaraja:krithi:31927e17-df05-415c-b697-c8e09110d352 | identity | inta bhAgyamani | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:31e84a65-246d-4825-a1d3-8099f020d26f | identity | bhajarE bhaja | Kannada | Misra Capu | kannaDa | Isra Capu | approved |  |
| tyagaraja:krithi:3353f8ab-7b79-4f76-9d4b-988b3e04874b | identity | namO namO rAghavAya | dESiya tODi | Rupaka | dESiya tODi | Rupakam | approved |  |
| tyagaraja:krithi:33822898-08a7-437e-8734-df26ba0622d4 | identity | sari jEsi vEduka | Teevravāhini | Adi | tIvra vAhini | Adi | approved |  |
| tyagaraja:krithi:3411ea29-8485-47b2-9785-6171c6f8a396 | identity | mA kulamuna | suraTi | Triputa | suraTi | Unknown | approved |  |
| tyagaraja:krithi:341feaef-7ba8-4621-b929-065ce4c82764 | identity | SrIpaptE nI pada | Nāgaswarāvali | Rupaka | nAga svarAvaLi | Unknown | approved |  |
| tyagaraja:krithi:3432b7e1-7cc2-4d0d-8afd-5afcc60a22af | identity | tana mIdanE | Bhooshāvali | Adi | bhUshAvaLi | Adi | approved |  |
| tyagaraja:krithi:34379c8b-3252-4567-971b-4ed61653471d | identity | karmamE balavanta | Sāveri | Capu | sAvEri | Capu | approved |  |
| tyagaraja:krithi:34773412-c158-44eb-b0b6-2a8552562fc9 | identity | nE pogaDakuNTE | dESiya tODi | Jhampa | dESiya tODi | Jhampa | approved |  |
| tyagaraja:krithi:34bdc909-e856-4190-b110-9ad2634b6c67 | identity | Chetulaara | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:34c3f5a8-8e1d-4e68-b308-1b9b21064d34 | identity | palukavEmi patita pAvana | Ārabhi | Rupaka | Arabhi | Rupakam | approved |  |
| tyagaraja:krithi:34ee92f6-8f4e-428a-8822-9ca37913349e | identity | SrI rAma SrI rAma | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:356bceef-a192-4687-8185-7c594c9c3578 | identity | Sundara Dasaratha | Kāpi | Adi | Kapi | Unknown | approved |  |
| tyagaraja:krithi:3687463a-34b0-4b34-8837-777a357d7a3f | identity | rAma bANa | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:37249a7b-de16-40f8-a51e-6834bc6e3c62 | identity | evarurA ninu vinA | Mohanam | Misra Capu | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:3747ba8d-2df1-4c7f-b272-20ba3c81a5d8 | identity | Chera Raavademi | Reethigowla | Adi | Riti Gaula | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:38759c7d-178c-41ee-add6-6fb6bfa447ff | identity | sItA vara | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:38a8fa0e-49e9-41af-9182-7d41a8b5a81b | identity | Rama Samayamu | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:38a9e30f-5da1-4149-bf2a-57e5908fab87 | identity | palukavEmi nA daivamA | Poornachandrika | Adi | pUrNa candrika | Adi | approved |  |
| tyagaraja:krithi:39793ab9-5a83-43b9-be64-4a009d09c037 | identity | rAma daivamA | suraTi | Rupaka | suraTi | Unknown | approved |  |
| tyagaraja:krithi:398b15ae-5419-49a9-9eda-4f565adb782d | identity | endukO bAga | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:3996431c-54d4-4877-825d-c5ffb23b6e20 | identity | vAsu dEva vara guNa | Bilahari | Catusra Ekam | bilahari | Ekam | approved |  |
| tyagaraja:krithi:3b79777a-3ace-475d-8c0a-a6e5991cdd77 | identity | Brocevaarevarae | Ranjani | Adi | Sri Ranjani | Unknown | approved |  |
| tyagaraja:krithi:3be08780-f3f2-4ac2-acde-818f8438ed3e | identity | dEva rAma rAma | saurAshTraM | Rupaka | saurAshTraM | Unknown | approved |  |
| tyagaraja:krithi:3c7ee141-416e-40b1-85d0-1c61bf5d6eca | identity | nIkevari bOdhana | Suddha Sāveri | Adi | Suddha sAvEri | Adi | approved |  |
| tyagaraja:krithi:3c94b2b6-27fa-430e-8c67-8211f0428a6d | identity | pAhi ramA ramaNa | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:3d2fd5f4-a690-4ace-8ae7-df1fe2d23644 | identity | manasu svAdhIna | SankarAbharaNaM | Misra Capu | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:3d87f5dc-3db7-495f-8b79-e1ff88754323 | identity | Upachaaramu Jesevaaru | Bhairavi | Rupaka | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:3dfe8560-76b2-45bd-bbe3-d3f822c709e8 | identity | callarE rAmacandrunipai | Āhiri | Capu | Ahiri | Capu | approved |  |
| tyagaraja:krithi:3e1da6ee-2454-45d0-bbe4-c095f7e5d6df | identity | nammaka nE mOsa | Asāveri | Rupaka | asAvEri | Rupakam | approved |  |
| tyagaraja:krithi:3e4a4599-8d90-4857-8eb4-8cad30fd1a90 | identity | kaTTu jEsinAvu | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:3ed8918c-c99c-48b1-b0e9-7b0f06dc7850 | identity | Bhuvini Dasudanae | Ranjani | Adi | Sri Ranjani | Unknown | approved |  |
| tyagaraja:krithi:3f2a06d0-efc0-4f57-9791-24178b0859d5 | identity | jAnakI ramaNa | Shuddha Seemantini | Adi | Suddha sImantini | Unknown | approved |  |
| tyagaraja:krithi:3f42c38b-8e30-422c-8c0d-ecf62fd52c0a | identity | innALLa vale | Hanumatodi | Misra Capu | tODi | Isra Capu | approved |  |
| tyagaraja:krithi:3f6e3166-2845-4d8d-a8eb-a9ce08624b05 | identity | munupE teliyaka | Bangāla | Adi | bangALa | Desadi | mapped | missing_charanam |
| tyagaraja:krithi:3ff7cbc9-e452-4da4-b06b-e520f683050b | identity | Sariyevvarae | Ranjani | Adi | Sri Ranjani | Unknown | approved |  |
| tyagaraja:krithi:400ea94f-513e-4b4c-9fba-263856a8c4d8 | identity | rAma SrI rAma lAli | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:40781929-d277-40e1-86aa-13d7aca393cf | identity | eTlA dorigitivO | Vasanthā | Adi | vasanta | Adi | approved |  |
| tyagaraja:krithi:40792364-1527-4e70-804d-00e1323c621c | identity | Ramaabhirama Ramaneeya | Darbar | Misra Capu | Darbar | Unknown | approved |  |
| tyagaraja:krithi:415cdb7f-0aec-4d27-a71c-c29d047b4981 | identity | sarivArilOna | Bhinnashadjam | Adi | bhinna shaDjaM | Adi | approved |  |
| tyagaraja:krithi:418cb879-644a-4d62-afec-05de7dcb8b78 | identity | munnu rAvaNa | Hanumatodi | Jhampa | tODi | Jhampa | approved |  |
| tyagaraja:krithi:4215e9b5-17dd-4730-a140-55d48014b034 | identity | Lalitae Sri Pravriddhae | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:426f7403-9809-4704-b7e1-22e539b7acb3 | identity | Elaraa Sri Krishnaa | Kāmbhoji | Rupaka | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:42c81e5a-4af5-4b5f-8538-81af000f2329 | identity | sandEhamunu | Rāmapriyā | Adi | rAma priya | Adi | approved |  |
| tyagaraja:krithi:44d9e846-50b1-43dd-bc7e-9f82994d953c | identity | Tulasi Bilva | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:44e32f1e-f9d6-4789-99e5-5f148bc97a82 | identity | Paraaku Neekelaraa | Keeranāvali | Adi | Kiranavali | Unknown | approved |  |
| tyagaraja:krithi:451244d9-a0ee-43b3-b6aa-7a06cee44ce6 | identity | sArasa nEtra | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:4601ab2f-a5c0-4e59-a19f-852d974c9c94 | identity | EmEmO teliyaka | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:4629d110-e300-4557-b69d-a50aa9cdc0d8 | identity | Baale Baalendu | Reethigowla | Adi | Riti Gaula | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:463943a7-924f-40c4-8f71-335093644ce6 | identity | nIvaNTi daivamu | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:46a8024b-ec47-4259-87ef-26bfb3e4b7be | identity | Ennaallu Nee Trova | Kāpi | Misra Capu | Kapi | Unknown | approved |  |
| tyagaraja:krithi:4749735b-6ccb-4892-a504-04df5716bde0 | identity | Sara Sara Samaraika | Kunthalavarāli | Adi | Kuntala Varali | Unknown | approved |  |
| tyagaraja:krithi:477ec205-abda-480e-9101-df377a6f8e1f | identity | dinamE sudinamu | Latāngi | Misra Capu | latAngi | Isra Capu | approved |  |
| tyagaraja:krithi:47f8faf6-b867-486c-baef-007293872b12 | identity | O Jagannaatha | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:48708093-afc0-4d70-8b10-7696f8175380 | identity | tanavAri tanamu | Begada | Adi | bEgaDa | Desadi | approved |  |
| tyagaraja:krithi:4954ca8e-a122-4d71-b862-213d2f21f552 | identity | jnAnamosaga rAdA | pUri kalyANi | Rupaka | pUri kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:498f50fb-3632-498c-888d-2c04e78f771d | identity | Paahi Rama Ramayanucu | Kharaharapriyā | Tisra Ekam | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:4998bbf0-54d1-4552-82bc-7c815c16c5e7 | identity | Sri Ganapathini | saurAshTraM | Adi | Saurashtram | Unknown | approved |  |
| tyagaraja:krithi:49e73d43-23cb-427e-a526-21ae0a5d31b8 | identity | mariyAda kAdurA | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:4a2f41e9-41fa-44eb-9d64-f42375f541b8 | identity | sadA madini | Gambheeravani | Adi | gambhIra vANi | Adi | approved |  |
| tyagaraja:krithi:4a53d422-1ce0-4895-a6d3-38f49d5df3a6 | identity | Yochanaa Kamala | Darbar | Adi | Darbar | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:4a69d9af-b815-406c-a475-23e708182795 | identity | Elaavataara | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:4b768a4c-4ef0-4cb4-a2e9-c8e76562063a | identity | Abhimaanamu Ledemi | Andhali | Triputa | Andali | Unknown | approved |  |
| tyagaraja:krithi:4c8ae919-199e-427e-a405-10afe38ef806 | identity | Guru Leka | Gourimanohari | Khanda Capu | Gauri Manohari | Unknown | approved |  |
| tyagaraja:krithi:4cae5d72-c291-465d-b117-242dff25e96c | identity | rAmA rAma rAma rAmAyani | Mohanam | Misra Capu | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:4d49569a-cd31-444d-98b9-8be4ee2e1e91 | identity | Vaarija Nayana-1 | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:4e418fbf-5e06-4dc1-81dc-0f946c6de2ba | identity | Maa Ramachandruniki | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:4e6c1010-d9a5-4461-b4d0-49c6b6e73b45 | identity | Sri Janaki Manohari | Eeshamanohari | Adi | Isa Manohari | Unknown | approved |  |
| tyagaraja:krithi:4e9eac68-ee8d-4da8-8c75-7131e956f112 | identity | SrI kAnta nIyeDa | Bhavapriyā | Adi | bhava priya | Adi | approved |  |
| tyagaraja:krithi:4eb527cc-5ea7-4ff5-a6f8-364b3e599613 | identity | nEramA rAma | saurAshTraM | Adi | saurAshTraM | Adi | approved |  |
| tyagaraja:krithi:4f0d4529-dc92-41e6-8532-edb311bddd4b | identity | Evarikai | Deva Manohari | Adi | Deva Manohari | Unknown | approved |  |
| tyagaraja:krithi:4fa55259-3152-4f57-b528-52bcdf41b833 | identity | vinanAsakoni | Pratāpavarāli | Adi | pratApa varALi | Unknown | approved |  |
| tyagaraja:krithi:501d0f9a-35b4-4ae6-a31e-042f70e0b0fe | identity | SrI rAma rAma rAma | Neelāmbari | Khanda Capu | nIlAmbari | Khanda Capu | approved |  |
| tyagaraja:krithi:503b3c2f-cfc0-418c-a914-0f313291a9b4 | identity | patiki hArati | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:506dc30f-1708-4b28-b2e9-f90218b9f038 | identity | nenaruncinAnu | Mālavi | Adi | mAlavi | Unknown | approved |  |
| tyagaraja:krithi:50772910-25eb-4778-bb6e-e6ebdc2459fc | identity | vara lIla gAna | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:5088493f-452d-4768-b189-d8db42a92b40 | identity | nI cittamu nA | Vijayavasantham | Adi | vijaya vasanta | Adi | approved |  |
| tyagaraja:krithi:50942fd4-4ca3-421f-9555-a615685b5c43 | identity | Peridi Ninu | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:50b58873-0614-4f2e-95e7-b225a1151ffd | identity | Tappaganae | Suddha Bangāla | Rupaka | Suddha Bangala | Unknown | approved |  |
| tyagaraja:krithi:50e60677-4457-41f1-93c4-8511155e43b9 | identity | satta lEni | Nāganandini | Adi | nAgA nandini | Adi | approved |  |
| tyagaraja:krithi:51c1a4e1-6aec-4a72-8724-43bebcfff79a | identity | samayamu telisi | Asāveri | Capu | asAvEri | Capu | approved |  |
| tyagaraja:krithi:52ace0b2-5d91-46b3-9c0c-add32c7f1766 | identity | Vanaja Nayanudani | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:53f0f17b-9706-4612-b67a-34a6ed48306d | identity | inta tAmasamaitE | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:54d126e8-c68b-4853-be2e-81a7349d5e5e | identity | kamala bhavuDu | Mechakalyāni | Misra Capu | kalyANi | Isra Capu | approved |  |
| tyagaraja:krithi:550a03c3-991c-435f-b1b5-439c16e98183 | identity | ISa pAhi | Mechakalyāni | Rupaka | kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:5547eb46-0408-4f54-8bb5-983120d4e09c | identity | daya jUcuTakidi | Ganavaridhi | Adi | gAna vAridhi | Adi | approved |  |
| tyagaraja:krithi:5602cd3e-70d2-4214-a292-baa93d13e03b | identity | Emi dOva | Sāranga | Adi | sAranga | Adi | approved |  |
| tyagaraja:krithi:5603c15e-1264-46f6-a587-b07c80af900c | identity | Vinataa Suta Vaahana | Jayanthasena | Adi | Jayanta Sena | Unknown | approved |  |
| tyagaraja:krithi:561c1792-bd6a-4969-a503-8dd829f7de50 | identity | raghu nAyaka | Hamsadhwani | Adi | hamsa dhvani | Desadi | approved |  |
| tyagaraja:krithi:5638a240-7a35-437e-b112-1cba581deef2 | identity | Siggu Maali | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:563d4335-68f1-486b-8d9a-2bd4ad16f534 | identity | mATADavEmi | Neelāmbari | Adi | nIlAmbari | Adi | approved |  |
| tyagaraja:krithi:5757c1bb-3822-48b5-bb2c-651f55d0e2ba | identity | E tAvunarA | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:57b48f4b-dc54-496f-a5e3-79d6d794a3c8 | identity | Emani mATADitivO | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:57efd3b4-aee7-4124-91f5-a58848ad3177 | identity | Ksheenamai | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:5833cc94-ff83-47a8-8008-90dd847de096 | identity | Rama Ninnae Nammi | Huseni | Adi | Huseni | Unknown | approved |  |
| tyagaraja:krithi:5835ba5e-e094-4771-bb97-0b2463498899 | identity | Sri Raghuvara Sugunaalaya | Bhairavi | Adi | Bhairavi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:586cd3e5-ba41-4693-9495-379acce3fd91 | identity | varAlandukommani | Gurjari | Adi | ghurjari | Adi | approved |  |
| tyagaraja:krithi:59c2ff1b-cde6-423a-9864-db4521acdff2 | identity | Evaraina Leraa | Siddhasena | Adi | Siddha Sena | Unknown | approved |  |
| tyagaraja:krithi:5a0f3caf-3e75-4957-924c-733a757bae98 | identity | para lOka sAdhanamE | pUri kalyANi | Adi | pUri kalyANi | Adi | approved |  |
| tyagaraja:krithi:5ab61c20-7312-4ad2-aa80-a94319535990 | identity | Rama Namam Bhajarae | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:5adc1d1f-8a3d-446d-83f9-49aa1d8151da | identity | Sri Raghuvara Aprameya | Kāmbhoji | Adi | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:5aed6f36-6823-4538-85aa-9f694ef3160f | identity | eTla kanugondunO | Ghanta | Capu | ghaNTA | Capu | approved |  |
| tyagaraja:krithi:5afcc2aa-5106-48e2-a718-f27160892df5 | identity | nIvErA kula dhanamu | Begada | Misra Capu | bEgaDa | Isra Capu | approved |  |
| tyagaraja:krithi:5b980151-152a-42e4-b1a7-acbe7344f9d8 | identity | nE mora peTTitE | Rūpavati | Triputa | rUpavati | Triputa | approved |  |
| tyagaraja:krithi:5c0186a2-1cfe-43ae-ba94-559f0a876259 | identity | ceDE buddhi | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:5d531213-817e-4cb7-8bed-0f6e5306f6a3 | identity | mariyAda kAdayya | Bhairavam | Adi | bhairavaM | Adi | approved |  |
| tyagaraja:krithi:5dd36d8e-38c7-4270-a810-d0523181f59d | identity | Anaathudanu | Jingla | Adi | Jingla | Unknown | approved |  |
| tyagaraja:krithi:5ddc1beb-66f3-4585-a283-743ae3bbcf11 | identity | nI sari sATi | Hemavati | Adi | hEmavati | Adi | approved |  |
| tyagaraja:krithi:5e240d77-d99b-46f1-8c96-6fb183bf9734 | identity | koluvamarE kada | Hanumatodi | Adi | tODi | Unknown | approved |  |
| tyagaraja:krithi:5e362e9a-6b53-48de-818c-59844db7819a | identity | SrI janaka tanayE | Kalākānti | Adi | kalakaNThi | Adi | approved |  |
| tyagaraja:krithi:5e53b603-eb35-474f-8372-807702423ae7 | identity | pUla pAnpu | Āhiri | Adi | Ahiri | Adi | approved |  |
| tyagaraja:krithi:5e7ba410-e01f-400a-a009-4efd245e4f1d | identity | sItA pati kAvavayya | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:5e8cdd8c-65bf-4708-9a76-bfa20383bf1c | identity | vidhi SakrAdulaku | yamunA kalyANi | Rupaka | yamunA kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:5ef0c733-46d4-406f-9113-47a41c6a9fbf | identity | niravadhi sukhada | ravi candirka | Adi | ravi candirka | Unknown | approved |  |
| tyagaraja:krithi:5f669a6d-ae60-4c45-81e7-e83291ba6861 | identity | dari dApu lEka | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:60064fdb-4bb7-45ff-afd4-e0ace0677e29 | identity | Rama Rama Rama Laali | Sahāna | Misra Capu | Sahana | Unknown | approved |  |
| tyagaraja:krithi:604bbad4-7a8c-4935-9467-bf7db2d850d0 | identity | Paahi Kalyaana Rama | Kāpi | Adi | Kapi | Unknown | approved |  |
| tyagaraja:krithi:60b8e375-123e-4625-8b53-6764df3dddb2 | identity | Nammina Vaarini | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:60e61938-4873-4e58-8ad1-47ec0448c0d2 | identity | Meevalla Guna Dosha | Kāpi | Jhampa | Kapi | Unknown | approved |  |
| tyagaraja:krithi:61205a3a-0ef2-43be-8008-970f7c64267b | identity | gata mOhASrita | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:61d6612e-3f50-4111-b905-747663b7486c | identity | evaru teliya poyyEru | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:62af415e-cd50-4a02-8d86-8a5f39c24bc5 | identity | Rama Rama Nee Vaaramu | Anandabhairavi | Adi | Ananda Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:62bad2d2-cf4b-4694-b2dc-5b9a1c8e7aee | identity | evvarE rAmayya | Gāngeyabhuśani | Adi | gAngEya bhUshaNi | Adi | approved |  |
| tyagaraja:krithi:63a07e3d-51b8-43b4-bfbe-2a4b8f44b397 | identity | paluka kaNDa | Navarasa kannada | Adi | navarasa kannaDa | Unknown | approved |  |
| tyagaraja:krithi:63c9ca63-d545-476e-ad9a-a5ed471f1072 | identity | raghu vara nannu | Panthuvarāli | Adi | pantuvarALi | Adi | approved |  |
| tyagaraja:krithi:641897b8-ca27-43e0-a4c2-970699e833e8 | identity | lOkAvana catura | Begada | Adi | bEgaDa | Adi | approved |  |
| tyagaraja:krithi:64520f4d-bdef-4f3c-9395-19be699c756c | identity | vidulaku mrokkeda | mAyA mALavagauLa | Adi | mAyA mALava gauLa | Adi | approved |  |
| tyagaraja:krithi:647549cb-584d-4008-9c82-49b60268b9f4 | identity | orulanADu | Suddha Sāveri | Adi | Suddha sAvEri | Adi | approved |  |
| tyagaraja:krithi:64e3ccdf-ec93-422b-969c-6ccfd2f3e43c | identity | ramA ramaNa bhAramA | Vasantabhairavi | Adi | vasanta bhairavi | Adi | approved |  |
| tyagaraja:krithi:65f4c7f0-6a74-4f20-9046-3bdd2c26351b | identity | endukI calamu | SankarAbharaNaM | Triputa | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:6625f250-6e03-4ab2-950a-17ab43dc696b | identity | Raksha Pettarae | Bhairavi | Adi | Bhairavi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:6627d303-b808-42c5-ac2b-bd2dd9595d26 | identity | Emani Pogaduduraa | Veeravasantham | Adi | Vira Vasanta | Unknown | approved |  |
| tyagaraja:krithi:673807ed-af4c-4334-827d-da1083587b88 | identity | ninnE bhajana | Nāṭṭai | Adi | nATa | Adi | approved |  |
| tyagaraja:krithi:67f0e37a-e1df-4d91-bfe3-47fc310c7abe | identity | Pakkala Nilabadi | Kharaharapriyā | Misra Capu | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:68f17162-1900-424a-9900-5baac4dda46b | identity | Marugelaraa | Jayanthashrī | Adi | Jayanta Sri | Unknown | approved |  |
| tyagaraja:krithi:69014557-ad45-4ce7-bbf3-f99adce3c556 | identity | Emani nera nammu | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:6929bbfd-0c31-4f9b-8994-395b08b522fb | identity | Patti Viduva | Manjari | Adi | Manjari | Unknown | approved |  |
| tyagaraja:krithi:69dc224b-64d7-476b-99dd-a0f2ecb9b9b4 | identity | parA Sakti manupa | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:6a33383f-2c7f-4c78-b86a-d2455e486456 | identity | Videmu Seyavae | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:6c2fc35a-9ff3-4d04-9c51-fa4f03532744 | identity | nAda sudhA rasam | Ārabhi | Rupaka | Arabhi | Rupakam | approved |  |
| tyagaraja:krithi:6c9d0ab2-70a2-4c6b-a0df-c4a925bf8ae9 | identity | Naada Tanumanisam | Chittaranjani | Adi | Citta Ranjani | Unknown | approved |  |
| tyagaraja:krithi:6cb08ce2-279f-4088-84ef-6c7e84dbd30d | identity | sAramE kAni | Panthuvarāli | Misra Capu | pantuvarALi | Isra Capu | approved |  |
| tyagaraja:krithi:6db8faab-0307-4ec1-aeba-debdcdf3de00 | identity | oka pAri jUDaga | Kalāvati | Adi | kalAvati | Adi | approved |  |
| tyagaraja:krithi:6dfbf308-eb7a-408f-b4ed-ca9168670204 | identity | Paritaapamu | Manohari | Rupaka | Manohari | Unknown | approved |  |
| tyagaraja:krithi:6e54dc31-79d5-4368-b0c9-bd3ab6f9ceb7 | identity | Emi jEsitEnEmi | Hanumatodi | Misra Capu | tODi | Isra Capu | approved |  |
| tyagaraja:krithi:6f1b9810-3b06-4d1f-88fb-d1f030844691 | identity | Inta Saukhyamani | Kāpi | Adi | Kapi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:6fc8ec78-f85f-4c50-b228-8ffc67e8f42a | identity | pAlaya SrI raghu vara | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:6fd7860c-0e5d-428f-af9f-cd02c3ad2da8 | identity | sAdhincenE | Ārabhi | Adi | Arabhi | Adi | mapped |  |
| tyagaraja:krithi:706f4761-8454-4dd6-a111-db02cb4ede3f | identity | Ela nI daya rAdu | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:712c208e-5fa2-40be-8fac-9981eabdae15 | identity | SrIpa priya | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:72407711-358e-4764-a291-15c7fbbb90ad | identity | ramA ramaNa rArA | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | mapped |  |
| tyagaraja:krithi:7253c9f5-6903-4ad3-b63f-1eb6b9f01052 | identity | SrI rAma rAma jagadAtma | Poornachandrika | Khanda Capu | pUrNa candrika | Khanda Capu | approved |  |
| tyagaraja:krithi:73116a78-3416-4583-b4ff-7c9223110062 | identity | mATi mATiki | Mohanam | Misra Capu | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:733e0e9d-e729-4df1-8097-b9c27968cfb8 | identity | hari dAsulu | yamunA kalyANi | Adi | yamunA kalyANi | Adi | approved |  |
| tyagaraja:krithi:73866e63-6c04-4cdc-8fac-b068e353d351 | identity | mElu mElu rAma nAma | saurAshTraM | Adi | saurAshTraM | Adi | approved |  |
| tyagaraja:krithi:73b44194-00c1-4753-8c8a-924d9fa3e18b | identity | svara rAga sudhA | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:73f878bd-d72c-4c56-9c1b-c6000006e4eb | identity | daya rAni | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:744d9aaa-7563-4bf4-9b5b-a7926568288f | identity | vADErA daivamu | Panthuvarāli | Adi | pantuvarALi | Adi | approved |  |
| tyagaraja:krithi:74d4a92d-a364-4bad-b814-8ae50fd2deb8 | identity | dhyAnamE ganga | Dhanyāsi | Adi | dhanyAsi | Adi | approved |  |
| tyagaraja:krithi:74f7c50c-ea71-429f-898e-66e289d514e6 | identity | dEvi SrI tuLasamma | mAyA mALavagauLa | Adi | mAyA mALava gauLa | Adi | approved |  |
| tyagaraja:krithi:75093099-55f6-4e74-885b-25a5e961f4ae | identity | ika kAvalasinadEmi | Bālahamsa | Adi | balahamsa | Unknown | approved |  |
| tyagaraja:krithi:756f894a-790a-4e3b-9fe1-a01600723782 | identity | mElukO dayA nidhi | saurAshTraM | Rupaka | saurAshTraM | Rupakam | approved |  |
| tyagaraja:krithi:7570235e-b9cf-4060-a9cf-b400716ca3ae | identity | Indukaa Puttinchitivi | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:76025d5f-aa93-41a2-a9c4-8169981a2f79 | identity | Karunaa Jaladhi | kEdAra gauLa | Misra Capu | Kedara Gaula | Unknown | mapped |  |
| tyagaraja:krithi:760be4bc-5b3a-430d-9969-4835c43aac74 | identity | Evari Maata | Kāmbhoji | Adi | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:76af1dff-ea85-4687-8238-27af5ac9dbc6 | identity | Mucchata | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:7762af58-19eb-4bd6-a6ce-d17a54dd7a73 | identity | sAri veDalina | Asāveri | Adi | asAvEri | Adi | approved |  |
| tyagaraja:krithi:77d3a9de-411c-4c6b-b285-026d19d0bf27 | identity | Sogasugaa Mridanga | Ranjani | Rupaka | Sri Ranjani | Unknown | approved |  |
| tyagaraja:krithi:77e3a868-f451-4028-bc8f-ff9fb696ef15 | identity | Nee Bhajana | Nāyaki | Adi | Nayaki | Unknown | approved |  |
| tyagaraja:krithi:78b86c5a-7c6e-4797-a4ab-0be561979065 | identity | Vandanamu | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:78eda9c2-9ed0-403d-ab7e-953b713519d8 | identity | vinavE O manasA | Vivardhani | Rupaka | vivardhani | Rupakam | approved |  |
| tyagaraja:krithi:7914b699-b204-42b1-9057-72f4ff5db6d4 | identity | Mahita Pravrddha | Kāmbhoji | Misra Capu | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:79c5ac50-423e-4f35-81e0-dd68e289c63f | identity | bhakti biccam | SankarAbharaNaM | Rupaka | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:79f487a2-9d16-4ba8-8a5d-1a1e3ae3b2b7 | identity | enta pApinaiti | gauLipantu | Capu | gauLipantu | Capu | approved |  |
| tyagaraja:krithi:7a163c9f-1b45-410c-858e-6388391da0f4 | identity | Ela Teliya Lero | Darbar | Misra Capu | Darbar | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:7a6c4270-9f6a-44f7-b41c-40c47914af0e | identity | ninnADanEla | Kannada | Adi | kannaDa | Adi | approved |  |
| tyagaraja:krithi:7ab08ebb-7d65-48c7-93cb-152d9491283b | identity | Vaarija Nayana-2 | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:7ac1ec39-56e0-4c84-9ea1-3e55dc7569b2 | identity | O rAma O rAma | Ārabhi | Adi | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:7ad8d15d-8260-46b7-bc02-2ef448f61842 | identity | Badalika Teera | Reethigowla | Adi | Riti Gaula | Unknown | approved |  |
| tyagaraja:krithi:7ae3db13-a2b8-4ab5-a944-30906c1b6fed | identity | innALLu daya | nArAyaNa gauLa | Misra Capu | nArAyaNa gauLa | Unknown | approved |  |
| tyagaraja:krithi:7bb627c0-4204-4c39-89db-b92a858b70f6 | identity | sAmiki sari | Begada | Rupaka | bEgaDa | Rupakam | approved |  |
| tyagaraja:krithi:7c0a3afe-bdb1-4777-945a-31948041fabb | identity | callaga nAtO | Vegavāhini | Adi | vEga vAhini | Adi | approved |  |
| tyagaraja:krithi:7c4ff154-ff0d-4a4a-b261-6d9a5122b0bc | identity | Edi Nee Baahu Bala | Darbar | Adi | Darbar | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:7d556895-a35c-4b48-b922-e2815fa492e9 | identity | O ramA ramaNa | Kedaram | Adi | kEdAraM | Adi | approved |  |
| tyagaraja:krithi:7d7ffdc6-fe9f-4864-a630-a418679f4753 | identity | bhajana sEya rAdA | Atāna | Rupaka | aThANa | Rupakam | approved |  |
| tyagaraja:krithi:7e410ccb-66d7-48f4-adc2-2102b12681ad | identity | Laali Laalayya | kEdAra gauLa | Jhampa | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:7e954839-2267-43a1-8d6b-9872b7cb2a04 | identity | SrI raghu kulamandu | Hamsadhwani | Adi | hamsa dhvani | Desadi | approved |  |
| tyagaraja:krithi:7ea89f3a-86c9-4e20-9a5a-98effa850ce6 | identity | nA jIvAdhAra | Bilahari | Adi | bilahari | Adi | approved |  |
| tyagaraja:krithi:7fa033e9-6780-4a14-ab66-095c4fcd15ac | identity | E pApamu jEsitirA | Atāna | Misra Capu | aThANa | Isra Capu | approved |  |
| tyagaraja:krithi:80528ba0-42c8-4b50-b65f-b1771996f18b | identity | Nija Marmamula | Umābharanam | Adi | Umabharanam | Unknown | approved |  |
| tyagaraja:krithi:815e13a3-f865-467a-bd13-9c1dcaa85631 | identity | dASarathE dayA | kOila priya | Adi | kOila priya | Adi | approved |  |
| tyagaraja:krithi:8176b9b9-321e-4d81-adb9-f2549f32e2ae | identity | Sri Rama Sri Rama Sri Manoharamaa | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:823bc4b9-1fbc-47d0-bf88-cd84dcc69de0 | identity | SObhAnE SObhAnE | Panthuvarāli | Adi | pantuvarALi | Adi | approved |  |
| tyagaraja:krithi:823cf8d7-f2e9-4a9d-bedd-506dffd550c1 | identity | Aada Modi Galadae | Chārukesi | Adi | Carukesi | Unknown | approved |  |
| tyagaraja:krithi:824e344a-8e70-4017-8857-68af76099b64 | identity | nIvE kanneDa | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:827483ca-f61e-453e-b719-dba2f7eaf2db | identity | Sukhiyevaro | Kanadā | Adi | Kanada | Unknown | approved |  |
| tyagaraja:krithi:82b9fd7a-bf48-472e-8971-c509c3e6f0fc | identity | ETi janmamidi | Varāli | Misra Capu | varALi | Isra Capu | approved |  |
| tyagaraja:krithi:82dd8598-5a91-4ffc-b26b-e4800a18d4b9 | identity | Endundi Vedalitivo | Darbar | Misra Capu | Darbar | Unknown | approved |  |
| tyagaraja:krithi:8300ca9e-eca0-4d3a-8a93-0c54c3715baf | identity | nammi vaccina | Mechakalyāni | Rupaka | kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:831f63cc-5e89-4b18-947e-7f59e5310503 | identity | rAma nIpai tanaku | Kedaram | Adi | kEdAraM | Adi | approved |  |
| tyagaraja:krithi:83575f33-e1cc-41a5-bcc5-097d66d09b56 | identity | anurAgamu lEni | Saraswathi | Rupaka | sarasvati | Rupakam | approved |  |
| tyagaraja:krithi:8359d42b-c00b-444c-9988-a124387f728f | identity | appa rAma bhakti | Panthuvarāli | Rupaka | pantuvarALi | Rupakam | approved |  |
| tyagaraja:krithi:83a16bba-77a5-44fc-8517-26df2dc77268 | identity | proddu poyyenu | Hanumatodi | Capu | tODi | Capu | approved |  |
| tyagaraja:krithi:83bde171-ad79-460a-a9c7-131a44227f35 | identity | veDalenu kOdaNDa pANi | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:83d3429e-a1d0-43b6-b307-020f21d64ac2 | identity | nI dAsAnudAsuDu | hamIr kalyANi | Adi | hamIr kalyANi | Adi | approved |  |
| tyagaraja:krithi:841e36dd-f1ee-462a-a677-f485cc14c75c | identity | dharanu nI sari | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:848fe8eb-0b6e-423d-9b37-f0ce62ceffa5 | identity | rAma rAma rAma rAma | cencuruTi | Rupaka | cencuruTi | Unknown | approved |  |
| tyagaraja:krithi:850aec01-f57c-48d8-a7a3-fa26b7d4e7af | identity | dIna janAvana | Bhoopālam | Adi | bhUpALaM - bhauLi | Adi | approved |  |
| tyagaraja:krithi:85159a71-61ef-4634-a80a-f2366876bcc0 | identity | lakshaNamulu | Suddha Sāveri | Adi | Suddha sAvEri | Adi | approved |  |
| tyagaraja:krithi:85833460-8549-4669-819e-b03c923b0b9d | identity | Rama Raghu Kula | Kāpi | Rupaka | Kapi | Unknown | approved |  |
| tyagaraja:krithi:85cfedb0-6913-4602-a969-c73fe754f6eb | identity | I mEnu kaligina | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:86b2b208-507e-41a5-bb2e-00cf6363c1f8 | identity | ennaDO rakshincitE | saurAshTraM | Adi | saurAshTraM | Adi | approved |  |
| tyagaraja:krithi:8788331c-a2c8-48a2-98d3-dac53ea56935 | identity | nanu pAlimpa | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:87f6024e-4c43-4fca-a81d-8a192925843b | identity | Evaricchiriraa | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:882c4c90-15d7-4b65-b52f-48515600bb63 | identity | kadaluvADu | nArAyaNa gauLa | Adi | nArAyaNa gauLa | Unknown | approved |  |
| tyagaraja:krithi:882f7bab-9a00-41ef-9c4c-d96cec3d2a23 | identity | Ananda sAgara | Garudadhvani | Adi | garuDa dhvani | Desadi | approved |  |
| tyagaraja:krithi:887bed65-9c5c-4fca-a7f8-4ac576cd1ee3 | identity | Sujana Jeevana | Kamās | Rupaka | Kamas | Unknown | approved |  |
| tyagaraja:krithi:889b13ef-8794-4e41-978d-3fc3ce84d8f4 | identity | indukEmi | Varāli | Misra Capu | varALi | Isra Capu | approved |  |
| tyagaraja:krithi:88b63b1b-d4af-4635-b839-293d69e0fb16 | identity | nIvE kAni | Bilahari | Misra Capu | bilahari | Isra Capu | approved |  |
| tyagaraja:krithi:89410ceb-c9e9-466b-a539-58f01a3027c1 | identity | pariyAcakamA | Vanaspati | Rupaka | vanaspati | Rupakam | approved |  |
| tyagaraja:krithi:89ada848-7385-437c-956f-7ae855ceeb39 | identity | Samukhaana Nilva | Kokilavarāli | Adi | Kokila Varali | Unknown | approved |  |
| tyagaraja:krithi:89ed80d6-5286-4706-9542-b8109e31010f | identity | karuNA jaladhE | Nadanamakriya | Adi | nAda nAmakriya | Adi | approved |  |
| tyagaraja:krithi:89fb546a-46d3-4795-a0ca-0f366ac3f41c | identity | janakajA samEta | Asāveri | Rupaka | asAvEri | Rupakam | approved |  |
| tyagaraja:krithi:8a3e248e-3e40-4eaf-9c77-8f2e4bec6a70 | identity | Kali Narulaku | Kunthalavarāli | Adi | Kuntala Varali | Unknown | approved |  |
| tyagaraja:krithi:8a4f25e2-7015-4458-a604-ff800a1a034d | identity | anupama guNa | Atāna | Khanda Capu | aThANa | Khanda Capu | approved |  |
| tyagaraja:krithi:8ac85b44-5e45-447e-ae8d-6c45bd46d24a | identity | endukO nI manasu | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:8b16692d-8110-406d-a3e6-110d9db253b8 | identity | kOri vaccitinayya | Bilahari | Adi | bilahari | Adi | approved |  |
| tyagaraja:krithi:8b489e84-f7a4-4453-a698-6e992d1c8ff1 | identity | ipuDaina nanu | Ārabhi | Misra Capu | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:8c33d9b4-30bf-4e2e-a847-84926136a4d6 | identity | Varada Raja | Swarabhooshani | Rupaka | Svara Bhushani | Unknown | approved |  |
| tyagaraja:krithi:8c4d0b7b-9488-4225-8b97-f8549283d929 | identity | Kaarubaaru | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:8c55c921-a589-49e0-bc1d-546105d51451 | identity | manasu vishaya | nATa kuranji | Adi | nATa kuranji | Unknown | approved |  |
| tyagaraja:krithi:8c967ef0-a727-44c6-a4a6-cd4ece4abf83 | identity | Nitya Roopa | Kāpi | Rupaka | Kapi | Unknown | approved |  |
| tyagaraja:krithi:8d1452e5-ff2d-4bb3-998e-3f892fcfc1d8 | identity | Naati Maata | Devakriya | Adi | Deva Kriya | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:8dbbd152-4b44-4f65-9ac1-12e859634911 | identity | Endu Kaugalinturaa | Shuddha Desi | Adi | Suddha Desi | Unknown | approved |  |
| tyagaraja:krithi:8e1c22da-611b-473d-90c8-daf52c3433e6 | identity | pAhi mAM harE | saurAshTraM | Rupaka | saurAshTraM | Rupakam | approved |  |
| tyagaraja:krithi:8e3b3314-6895-48e7-b5fc-f82d6f2e06b0 | identity | Manavinaalakincha | Nalinakānthi | Adi | Nalina Kanti | Unknown | approved |  |
| tyagaraja:krithi:8ea31119-0764-4ab4-8f99-92264ba34d3f | identity | varadA navanItASa | rAga panjaraM | Triputa | rAga panjaraM | Unknown | approved |  |
| tyagaraja:krithi:8f054e71-b43a-45c7-8d46-550d6bff56d0 | identity | Vara Sikhi Vaahana | Supradipama | Adi | Supradipama | Unknown | approved |  |
| tyagaraja:krithi:8f109d67-f928-4258-bbe1-2793ede30bd3 | identity | karuNayElAgaNTE | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:8f631948-3af9-471c-9ca7-499daad07c76 | identity | bhajana sEyu mArgamu | Nārāyani | Adi | nArAyaNi | Unknown | approved |  |
| tyagaraja:krithi:902b62c9-a743-45e9-b206-62e56a3624f5 | identity | ramincuvArevarurA | Suposhini | Rupaka | supOshiNi | Unknown | approved |  |
| tyagaraja:krithi:906547be-a4ff-41c2-9953-ea7284b7899f | identity | Enduku Nirdaya | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:907ed608-a27c-447b-9f71-a4e75af7eda4 | identity | Kaligiyunte Kadaa | Kīravāṇi | Adi | Kiravani | Unknown | approved |  |
| tyagaraja:krithi:90d7b1fa-3d56-460a-a9b1-7b54730cd98a | identity | varamaina nEtrOtsava | paraju | Capu | paraju | Capu | approved |  |
| tyagaraja:krithi:90e3cbc4-3907-4c15-baac-732f8f232479 | identity | SAntamu lEka | sAma | Adi | sAma | Unknown | approved |  |
| tyagaraja:krithi:90ee1155-699d-45eb-a708-86752114e23f | identity | gAna mUrtE | Gānamūrti | Adi | gAna mUrti | Adi | approved |  |
| tyagaraja:krithi:91213a3a-2fd2-4770-8b40-9c683dbf8539 | identity | Mari Mari Ninnae | Kāmbhoji | Adi | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:9190e3d4-c031-4481-9aba-97c6ac87460b | identity | mAmava raghurAma | Sāranga | Rupaka | sAranga | Rupakam | approved |  |
| tyagaraja:krithi:91aa1a5e-66c5-49ee-a5e8-8603809925d2 | identity | tappi bratiki | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:91bb583a-c037-44c6-b078-af6d214be16e | identity | vErevvarE gati | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:925bf438-13a3-40ff-9a74-f2ecd23fb0b7 | identity | amma rAvamma | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:93a47f0f-00ee-4562-9abb-f3339a42ad8d | identity | vAcAmagOcaramE | Kaikavashi | Adi | kaikavaSi | Adi | approved |  |
| tyagaraja:krithi:93c3fa2f-7a78-4b85-a56c-3a6d33dd1aa9 | identity | Enta Raani | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:940e158a-ef4d-4ed5-bd0d-a9d1db08e564 | identity | Nee Daya Kalgute | Reethigowla | Adi | Riti Gaula | Unknown | approved |  |
| tyagaraja:krithi:947faba5-85b3-40ff-8d98-0c2a2eea9edc | identity | Paripaalaya Paripaalaya | Reethigowla | Adi | Riti Gaula | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:95d12cdb-7527-4fb7-b405-9bce7fe30ae6 | identity | Manasu Nilpa | Abhogi | Adi | Abhogi | Unknown | approved |  |
| tyagaraja:krithi:96dda707-dc3a-4422-879c-026ff2156df4 | identity | Emani Vegintunae | Huseni | Adi | Huseni | Unknown | approved |  |
| tyagaraja:krithi:96e1ae65-0fd5-4ea8-a604-1b3718daf949 | identity | ghuma ghuma | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:97d3a36f-e933-4e17-81da-47907fb1a4fe | identity | Rama Neeyeda | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:99896a62-e18f-4879-a71a-f051027af8e5 | identity | nA moralanu | Ārabhi | Adi | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:9a54e49d-2092-41cd-b705-b3ffc23fb219 | identity | sAkshi lEdanucu | Bangāla | Adi | bangALa | Desadi | approved |  |
| tyagaraja:krithi:9b732200-36d9-4854-b058-5d28e2090335 | identity | vaccunu hari | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:9b755232-dca7-4ddf-92f0-1b298ddb53a1 | identity | lEkanA ninnu | Asāveri | Adi | asAvEri | Adi | approved |  |
| tyagaraja:krithi:9b867d71-cc42-4dab-b712-ff8af6848c39 | identity | lAliyUgavE | Neelāmbari | Rupaka | nIlAmbari | Rupakam | approved |  |
| tyagaraja:krithi:9bab5692-0510-485b-a8b2-774563963747 | identity | ninnE nera namminAnu | Ārabhi | Misra Capu | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:9bb5ac84-38de-4d8d-a413-3a86e64601bb | identity | teliya lEru rAma | Dhenukā | Adi | dhEnuka | Adi | approved |  |
| tyagaraja:krithi:9be09efe-40d0-4d73-bbbf-7dd0f12200c6 | identity | sudhA mAdhurya | Sindhu Rāmakriya | Adi | sindhu rAmakriya | Adi | approved |  |
| tyagaraja:krithi:9c3072cf-e1d8-4206-ab48-978b32ad71af | identity | jAnaki nAyaka | Dhanyāsi | Adi | dhanyAsi | Adi | approved |  |
| tyagaraja:krithi:9c764bd2-a66c-4de8-bb35-9a8f07a0797e | identity | sangIta jnAnamu | Dhanyāsi | Adi | dhanyAsi | Adi | approved |  |
| tyagaraja:krithi:9c89d2d5-c211-4790-8577-90bfccbf697e | identity | sundari ninnu | Ārabhi | Misra Capu | Arabhi | Isra Capu | approved |  |
| tyagaraja:krithi:9ca70f2a-dac2-4490-8918-bbe0633eef09 | identity | sundara tara dEhaM | Panthuvarāli | Adi | pantuvarALi | Adi | approved |  |
| tyagaraja:krithi:9cc07b0e-d05c-4c48-809d-e29f56119d93 | identity | sItA manOhara | Ramāmanohari | Adi | rAma manOhari | Adi | approved |  |
| tyagaraja:krithi:9ccd7f15-0aac-4df4-8057-c9868e7a3fad | identity | O Rama Rama Sarvonnata | Nāgagāndhāri | Misra Capu | Naga Gandhari | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:9d1cf5c5-460a-418c-8327-15a4bca0ee32 | identity | rAma nIvAdukonduvO | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:9d3cb035-32c2-4c5c-96fc-64cdd92320ea | identity | rAma ninu vinA | SankarAbharaNaM | Rupaka | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:9d588414-4b01-4651-a98a-dd30b3b0dae8 | identity | Rama Kathaa | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:9e06c13c-2bbb-47a7-b580-1de7365754cc | identity | heccarikagA | Yadukula Kāmbhoji | Jhampa | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:9e417346-a8b0-4332-8b03-0593967138df | identity | evaritO nE telpudu | Mānavati | Adi | mAnavati | Adi | approved |  |
| tyagaraja:krithi:9e76c325-2670-4a57-a575-492b6b68d9f0 | identity | Raghu Nandana Raghu Nandana | kEdAra gauLa | Rupaka | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:9e77e997-acfc-4e43-b743-52c0164dbfce | identity | Abhimaanamennadu | Kunjari | Adi | Kunjari | Unknown | approved |  |
| tyagaraja:krithi:9e8781fa-61be-4dd7-a791-7d27c3ce6813 | identity | prANa nAtha | Śūlini | Adi | SUlini | Adi | approved |  |
| tyagaraja:krithi:9f2497ab-aab5-4d9b-85cf-0d8dc5a0cbf7 | identity | nidhi cAla sukhamA | Mechakalyāni | Misra Capu | kalyANi | Isra Capu | approved |  |
| tyagaraja:krithi:9f397b10-c37b-4e44-84a3-878c82f70374 | identity | rAkA SaSi vadana | Takka | Adi | Takka | Adi | approved |  |
| tyagaraja:krithi:9fe198f5-d71b-46a6-a5f7-14245b32d29d | identity | Nenaruncharaa Naapaini | Simhavāhini | Adi | Simha Vahini | Unknown | approved |  |
| tyagaraja:krithi:9fe40f47-7453-4930-9257-9a0386751cd6 | identity | SrI rAma candra | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:a018529d-c3a4-4b90-808b-b2168626750f | identity | pAhi rAma dUta | Vasantavarāli | Rupaka | vasanta varALi | Rupakam | approved |  |
| tyagaraja:krithi:a0737775-4a51-4d82-a5f2-fe87969ffed2 | identity | kAsiccEdE | gauLipantu | Adi | gauLipantu | Adi | approved |  |
| tyagaraja:krithi:a117fff3-40e1-463d-83c9-3d601aa53d44 | identity | EmandunE vicitramunu | Shreemani | Adi | SrImaNi | Adi | approved |  |
| tyagaraja:krithi:a123a98d-dce2-4e9e-b2d2-d7627d33b034 | identity | koniyADE | Kokiladhwani | Adi | kOkila dhvani | Unknown | approved |  |
| tyagaraja:krithi:a170a743-39a8-4546-ae2d-954265d6ee51 | identity | perugu pAlu | Ghanta | Adi | ghaNTA | Adi | approved |  |
| tyagaraja:krithi:a3ca381c-38df-4855-98ee-a776f51c6e44 | identity | Sri Narada Nada | Kanadā | Rupaka | Kanada | Unknown | approved |  |
| tyagaraja:krithi:a3f21cde-4cc7-444c-a493-69704c02e447 | identity | Saamaja Vara Gamana | Hindolam | Adi | Hindolam | Unknown | approved |  |
| tyagaraja:krithi:a42c4225-9283-4261-aa28-20f535ee7f89 | identity | Chinna Naade | Kalānidhi | Adi | Kala Nidhi | Unknown | approved |  |
| tyagaraja:krithi:a461999c-05bc-4910-8b2c-afde25719ea5 | identity | Rama Bhakti | Suddha Bangāla | Adi | Suddha Bangala | Unknown | approved |  |
| tyagaraja:krithi:a4bfcc4e-ee7d-42a3-807c-cb77e40a36ac | identity | rArA nannElukOrA | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:a4e7ca76-bbc4-4bc5-bbb4-bc83bc17e805 | identity | Vinataa Suta Raaraa | Huseni | Adi | Huseni | Unknown | approved |  |
| tyagaraja:krithi:a5078f61-fd1b-43ac-9404-4d3d3d9db598 | identity | kRpa jUcuTaku | Chāyatārangini | Adi | chAyA tarangiNi | Unknown | approved |  |
| tyagaraja:krithi:a525487e-f906-4e61-a600-3a2694b96bbb | identity | rAma rAma rAmacandra | Ghanta | Jhampa | ghaNTA | Jhampa | approved |  |
| tyagaraja:krithi:a5535815-c812-4a87-b99b-60e2ff956c64 | identity | Raghu Nandana Raaja | Shuddha Desi | Adi | Suddha Desi | Unknown | approved |  |
| tyagaraja:krithi:a5d69bd6-cb2f-40e6-9007-75e1c57259e6 | identity | Rama Nee Samaanamu | Kharaharapriyā | Rupaka | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:a671b559-0861-4f9f-83aa-f0cfdf6be981 | identity | manasA mana | Vardhani | Rupaka | vardhani | Rupakam | approved |  |
| tyagaraja:krithi:a6f42c55-bb6b-449d-94f9-cfa032bcb288 | identity | Vara Raaga Laya | Cencukambhoji | Adi | Cencukambhoji | Unknown | approved |  |
| tyagaraja:krithi:a70de925-9076-47ab-b473-0b8a5056ff40 | identity | sArvabhauma | rAga panjaraM | Adi | rAga panjaraM | Unknown | approved |  |
| tyagaraja:krithi:a78ecf02-3598-4160-b868-5130b5d56bf0 | identity | entanucu sairintunu | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:a85a5a00-0b27-4b07-bbda-3e1f176ea81c | identity | durmArga cara | Ranjani | Rupaka | ranjani | Rupakam | approved |  |
| tyagaraja:krithi:a9971f1e-8715-4ff9-8ad4-e62079e74abe | identity | nannu kanna talli | kEsari - sindhu kannaDa | Adi | kEsari - sindhu kannaDa | Unknown | approved |  |
| tyagaraja:krithi:a99ff3b7-2904-4145-9abf-da6ecfdac695 | identity | buddhi rAdu | SankarAbharaNaM | Jhampa | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:a9c3d9ba-c8e5-4237-95e9-33b92854e64a | identity | Krpaalavaala | Nādavarangini | Adi | Nada Varangini | Unknown | approved |  |
| tyagaraja:krithi:aa8402c5-457f-4eff-ba6f-fe68c32dca54 | identity | tanalOnE dhyAninci | Devagāndhāri | Adi | deva gAndhAri | Adi | mapped |  |
| tyagaraja:krithi:aac223fb-6a9b-4f2f-81aa-3327b74937fa | identity | celimini jalajAkshu | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:aaef830f-30d3-43ea-b97d-b697088f7df8 | identity | nA morAlakimpa | Devagāndhāri | Rupaka | deva gAndhAri | Rupakam | approved |  |
| tyagaraja:krithi:ab2cb69a-ad62-4c7a-9f12-0a742d367bfe | identity | Raga Ratna | Reethigowla | Rupaka | Riti Gaula | Unknown | approved |  |
| tyagaraja:krithi:ab6cdbca-83dc-4ecd-94f2-17db72701237 | identity | vinayamunanu | saurAshTraM | Capu | saurAshTraM | Capu | approved |  |
| tyagaraja:krithi:ac5c15a8-1f75-4d06-aca7-a2fbda94660a | identity | mApAla velasi | Asāveri | Adi | asAvEri | Adi | approved |  |
| tyagaraja:krithi:ac6521f4-0f36-44ce-815d-62c2d876f5ed | identity | Undedi Ramudu | Harikāmbhōji | Rupaka | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:ae1996ea-2791-42ba-b927-89fc137549dd | identity | Raghupatae Rama | Sahāna | Rupaka | Sahana | Unknown | approved |  |
| tyagaraja:krithi:ae1ab85e-1041-4783-81a3-a4eba9e527ed | identity | Dharmaatma | kEdAra gauLa | Jhampa | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:ae7718b1-6674-47be-beb0-f50b9ed908ec | identity | Ivaraku jUcinadi | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | mapped |  |
| tyagaraja:krithi:aee9ddbb-ae17-4ffb-bf95-a9d11cec6b2f | identity | cAla kalla | Ārabhi | Adi | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:af06814d-3082-4e48-9ee7-f141d551b0d7 | identity | Yajnaadulu | Jayamanohari | Adi | Jaya Manohari | Unknown | approved |  |
| tyagaraja:krithi:af1822f3-138e-4d69-a92c-d46b7961cd4a | identity | Sri Ramya Chitta | Jayamanohari | Adi | Jaya Manohari | Unknown | approved |  |
| tyagaraja:krithi:af577b3c-4ac4-4bfd-974e-96583cdb2614 | identity | Kanugonu | Nāyaki | Rupaka | Nayaki | Unknown | approved |  |
| tyagaraja:krithi:af5bf75e-750d-4f43-a8ef-38dee94c34f3 | identity | brOva bhAramA | Bahudāri | Adi | bahudAri | Unknown | approved |  |
| tyagaraja:krithi:af5f710e-5b8f-47d6-8035-03d86306d0ad | identity | dorakunAyiTuvaNTi | Bilahari | Adi | bilahari | Adi | mapped |  |
| tyagaraja:krithi:b025bb22-3195-4d29-afe8-fe51b82a04f0 | identity | kanna talli | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:b08bbe00-3159-4746-8d25-6204e3f47538 | identity | kaddanu vAriki | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:b0ae2fec-57ab-4d7f-ae31-2ce53d529aab | identity | nannu brOvakanu | SankarAbharaNaM | Triputa | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:b0b1dfb2-b596-4645-b9aa-47082992be82 | identity | eduTa nilicitE | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:b0f93eb6-3016-47ee-ba01-de508d3281eb | identity | paramAtmuDu | Vāgadhīśvari | Adi | vAgadhISvari | Adi | approved |  |
| tyagaraja:krithi:b175d4b9-f889-4906-b2a4-10646c17d56d | identity | Raaraa Phani Sayana | Harikāmbhōji | Rupaka | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:b179b900-1473-432a-b948-cafce8f98428 | identity | dorakunAyani | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:b1b80b45-404e-45dc-8994-7bc51d114780 | identity | toli janmamuna | Bilahari | Khanda Capu | bilahari | Khanda Capu | approved |  |
| tyagaraja:krithi:b2626e28-503e-49cd-b5f6-6b8ab9cbd054 | identity | Enaati Nomu | Bhairavi | Adi | Bhairavi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:b3486e9f-fca1-4e77-bc15-11bddc79d381 | identity | amba ninu | Ārabhi | Adi | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:b4029df0-a209-470b-ba56-3d64fa8aebc4 | identity | duDuku gala | Gowla | Adi | gauLa | Adi | approved | missing_charanam |
| tyagaraja:krithi:b47eeeff-377e-4f6e-92ee-85af5ba16c1a | identity | Naama Kusuma | Sri | Adi | Sri Ragam | Unknown | approved |  |
| tyagaraja:krithi:b4c7cd33-23fc-43f7-a875-b0f55546b9e2 | identity | aTu kArAdani | Manoranjani | Adi | manO ranjani | Adi | approved |  |
| tyagaraja:krithi:b536b933-9088-4ca0-928e-2618ce2fae57 | identity | Sarasa Saama Daana | Kāpi Nārāyani | Adi | Kapi Narayani | Unknown | approved |  |
| tyagaraja:krithi:b5bc235e-0c6c-4b5b-9453-9b63fadb9dc4 | identity | sandEhamuyElarA | Mechakalyāni | Rupaka | kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:b5e6e60a-ee65-4283-ab81-7c6f67211ea8 | identity | kaNTa jUDumi | Vāchaspati | Adi | vAcaspati | Adi | mapped |  |
| tyagaraja:krithi:b5ee5ff1-23e7-43fc-bce4-b6ca181ea8e6 | identity | dEvAdi dEva | Sindhu Rāmakriya | Adi | sindhu rAmakriya | Adi | approved |  |
| tyagaraja:krithi:b6001002-db9b-4741-8240-88e88be6297d | identity | endu bAyarA daya | Dhanyāsi | Rupaka | dhanyAsi | Rupakam | approved |  |
| tyagaraja:krithi:b6a09287-09a1-4929-a8db-12f1638c2c89 | identity | viDa jAladurA | Jana Ranjani | Adi | jana ranjani | Adi | approved |  |
| tyagaraja:krithi:b6de335c-33da-46d8-9993-7f57a9b7a910 | identity | Rama Kothanda Rama | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:b7e2a6c8-fcc6-4fa9-9f53-038f3816b06c | identity | Daya Leni | Nāyaki | Khanda Capu | Nayaki | Unknown | approved |  |
| tyagaraja:krithi:b8b35e19-e62b-4508-9204-486ab0763307 | identity | ADavAramella | Yadukula Kāmbhoji | Triputa | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:b8f7ee40-0310-44f4-8c93-cf99bba5f80f | identity | cAlu cAlu nI yuktulu | Sāveri | Capu | sAvEri | Capu | approved |  |
| tyagaraja:krithi:b9063ef4-7e13-4609-9085-ad2d2134f70b | identity | sItamma mAyamma | Vasanthā | Rupaka | vasanta | Rupakam | approved |  |
| tyagaraja:krithi:b95c07ee-f897-49ea-9f7b-c37be42335ba | identity | Chalamelaraa | mArgahindOLaM | Adi | Marga Hindolam | Unknown | approved |  |
| tyagaraja:krithi:b9af5088-9906-47c4-bec2-4ea59e57f75b | identity | mOhana rAma | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:bad7d739-4c2d-443d-a38e-02efe78777d7 | identity | gaTTigAnu | Begada | Rupaka | bEgaDa | Rupakam | approved |  |
| tyagaraja:krithi:bbc8382b-a344-47c2-8b4d-fb831a541230 | identity | SrI rAma rAmASritulamu | Sāveri | Capu | sAvEri | Capu | approved |  |
| tyagaraja:krithi:bbe4c6a3-5a76-4902-abf9-32d547c04d49 | identity | rAmacandra nI daya | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:bc6352e6-e448-44a0-ac41-68e185fd77d2 | identity | nIvu brOva valenamma | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:bd738a97-9839-4e9f-acf6-237fb574d6c9 | identity | dAcukOvalenA | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:bd7d546b-61b3-485e-91c0-0082bafe41e3 | identity | E nOmu nOcitimO | Punnagavarali | Rupaka | punnAga varALi | Rupakam | approved |  |
| tyagaraja:krithi:bd7dec39-7fb5-45e6-b905-5e887fc294f8 | identity | Toli Ne Jesina | Suddha Bangāla | Adi | Suddha Bangala | Unknown | approved |  |
| tyagaraja:krithi:bd9294ff-1fee-469e-9671-96da3ef2b4ea | identity | mAra vairi | Nāsikābhūśaṇi | Adi | nAsikA bhUshaNi | Adi | approved |  |
| tyagaraja:krithi:bd9c039b-d92d-4a05-8f0d-d9082a834b1e | identity | rAma rAma kRshNa | gauLipantu | Adi | gauLipantu | Adi | approved |  |
| tyagaraja:krithi:bdbcc661-264b-4943-9c46-5c824056a239 | identity | O rAjIvAksha | Ārabhi | Misra Capu | Arabhi | Unknown | approved |  |
| tyagaraja:krithi:be43d382-8fba-4cff-b60f-67bf7e36307e | identity | kalaSa vArdhijAM | Ratnāngi | Adi | ratnAngi | Adi | approved |  |
| tyagaraja:krithi:beba8e1c-18ff-43bb-8848-7b2051304fb5 | identity | nIku tanaku | Begada | Misra Capu | bEgaDa | Isra Capu | approved |  |
| tyagaraja:krithi:bef7d3cf-7c08-4bae-97a2-06e923ca217f | identity | kRshNA mAkEmi | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:bf294950-e310-47b0-a178-efe225225aac | identity | nata jana | Simhendramadhyamam | Rupaka | simhEndra madhyamaM | Rupakam | approved |  |
| tyagaraja:krithi:bf785ecf-eb8b-4b25-95db-313b0d0a8904 | identity | SivE pAhi mAM | Mechakalyāni | Adi | kalyANi | Adi | approved |  |
| tyagaraja:krithi:c001d672-4240-4cc1-86c1-24c9cab2b435 | identity | Ora Joopu | kannaDa gauLa | Adi | Kannada Gaula | Unknown | approved |  |
| tyagaraja:krithi:c1361d3c-78dd-4361-a575-3d9e2336b64a | identity | anduNDakanE | Panthuvarāli | Misra Capu | pantuvarALi | Isra Capu | approved |  |
| tyagaraja:krithi:c17494d8-1d3b-4da8-8de7-43c2c33c32b6 | identity | kalala nErcina | Deepakam | Adi | dIpakaM | Adi | approved |  |
| tyagaraja:krithi:c1be6e4b-82aa-4447-beae-9355696067d7 | identity | mAnamu lEdA | hamIr kalyANi | Adi | hamIr kalyANi | Adi | approved |  |
| tyagaraja:krithi:c2350f5a-4019-453e-9ac4-82d8fe95cbf9 | identity | Indukaayee Tanuvunu Penchinadi | Mukhāri | Misra Capu | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:c262739c-e7cc-492d-b248-3775fec24bfe | identity | O Ranga Saayi | Kāmbhoji | Adi | Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:c296bbc5-8cdb-4769-84c4-5383948e7342 | identity | tera tIyaga | gauLipantu | Adi | gauLipantu | Adi | approved |  |
| tyagaraja:krithi:c2a4541f-1bb4-41bc-bfe1-89890ed9bd20 | identity | SrI tuLasamma | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:c2c9522c-5669-40d5-9dc8-ea5a21279303 | identity | Alakalallalaadaga | Madhyamāvathi | Rupaka | Madhyamavati | Unknown | mapped |  |
| tyagaraja:krithi:c5aee074-7a6b-4fd3-83ea-19fd6d9083eb | identity | rAju veDale | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:c5c8fbe2-e19e-4005-b41e-685c35027234 | identity | kOTi nadulu | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:c7603975-aaf9-4379-8434-59af93a50da3 | identity | Urakae Kalgunaa | Sahāna | Misra Capu | Sahana | Unknown | approved |  |
| tyagaraja:krithi:c857fcff-1b60-4cdf-8132-179ad49d4959 | identity | enduku daya rAdu | Hanumatodi | Triputa | tODi | Unknown | approved |  |
| tyagaraja:krithi:c876a731-58e5-4d9e-89ab-62f4a353e683 | identity | Neevanti Daivamu | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:c8b35259-088a-488a-a79d-4e905ad7b0eb | identity | cUDarE celulAra | Panthuvarāli | Misra Capu | pantuvarALi | Isra Capu | approved |  |
| tyagaraja:krithi:c8f46289-6807-4fbf-aed0-a5b82f3a2ff6 | identity | itara daivamulu | Chāyatārangini | Rupaka | chAyA tarangiNi | Unknown | approved |  |
| tyagaraja:krithi:c91fb560-8678-44ac-b964-d8518a05cdf2 | identity | bhaktuni cAritramu | Begada | Adi | bEgaDa | Adi | approved |  |
| tyagaraja:krithi:c97eab13-9938-4cc5-9cf4-4a398501a33d | identity | vAridhi nIku | Hanumatodi | Capu | tODi | Capu | approved |  |
| tyagaraja:krithi:cb1fb445-00e6-4495-805d-291236649acd | identity | evaru manaku | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:cbf2b349-35dc-48eb-aa89-74ffd3464421 | identity | Emi nEramu | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:cc45b4cc-c3ca-4a69-b3c5-efdeeecb8f85 | identity | ODanu jaripE | Sāranga | Adi | sAranga | Adi | approved |  |
| tyagaraja:krithi:ccae28a8-47dc-439c-bab1-f37be1d01255 | identity | nIdu caraNamulE | Simhendramadhyamam | Misra Capu | simhEndra madhyamaM | Isra Capu | approved |  |
| tyagaraja:krithi:cd283f28-6a3e-4d45-9db7-b885bee8dc60 | identity | sundari nannindarilO | Begada | Rupaka | bEgaDa | Rupakam | approved |  |
| tyagaraja:krithi:cd626987-39ee-4517-b13d-2a53d14244f5 | identity | Paripalaya Mam | Darbar | Triputa | Darbar | Unknown | approved |  |
| tyagaraja:krithi:cd6fdf67-8092-49f8-9a35-61851937370d | identity | Laavanya Rama | Purna Shadjam | Rupaka | Purna Shadjam | Unknown | approved |  |
| tyagaraja:krithi:ce57d2f1-4b5c-41a9-9034-6130eb0d20f3 | identity | Talachinantanae | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:ce8ecb05-26e2-4115-ad6a-88f841790c3a | identity | Raghu Veera Rana | Huseni | Rupaka | Huseni | Unknown | approved |  |
| tyagaraja:krithi:cedd6d75-244b-47c9-958e-726445428a99 | identity | dArini telusukoNTi | Suddha Sāveri | Adi | Suddha sAvEri | Adi | approved |  |
| tyagaraja:krithi:cee0b420-4914-4914-9318-01b6d52d1238 | identity | Adigi Sukhamu | Madhyamāvathi | Misra Capu | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:cf004928-e512-49a3-badf-e69382fec877 | identity | Mundu Venuka | Darbar | Adi | Darbar | Unknown | approved |  |
| tyagaraja:krithi:cf46edfe-080f-4931-bf5d-7145e618a3bd | identity | rAma rAma rAma nApai | Mechakalyāni | Misra Capu | kalyANi | Isra Capu | approved |  |
| tyagaraja:krithi:cf5d54a5-dcc8-4b4f-a2fe-257c3a1f9f53 | identity | Eti Yochanalu | Keeranāvali | Adi | Kiranavali | Unknown | approved |  |
| tyagaraja:krithi:cf6e4421-14d9-4412-97cd-14c4500eb678 | identity | Padavi Nee | Salaka Bhairavi | Adi | Salaka Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:cfd4399f-f56b-46de-ae87-805d470f9801 | identity | Ksheera Saagara Vihaara | Anandabhairavi | Jhampa | Ananda Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:cfdee002-d055-47ad-a85d-741034fa023a | identity | patiki mangaLa | Ārabhi | Adi | Arabhi | Adi | approved |  |
| tyagaraja:krithi:d02d50ef-a6be-45aa-9b91-f69a712a5d0c | identity | nArada gAna | Atāna | Rupaka | aThANa | Rupakam | approved |  |
| tyagaraja:krithi:d0d6a110-b908-4dff-9e07-a0d521502f6a | identity | vaddayuNDEdE | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:d1152836-f0ae-44cf-b271-46ab504c30cc | identity | Manasu Loni Marmamu | Hindolam | Adi | Hindolam | Unknown | approved |  |
| tyagaraja:krithi:d1d8b96f-fc20-48c1-a92f-e1b2b8a91496 | identity | E dAri sancarinturA | Shruthiranjani | Adi | Sruti ranjani | Adi | approved |  |
| tyagaraja:krithi:d1fd3851-f596-40f2-a069-5f27e814ca08 | identity | indukAyI tanuvunu | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:d2e28055-6ebd-4502-bc9c-e39fdabafd06 | identity | ranga nAyaka | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:d400db6d-6e56-4e51-a275-a9fb7e0883fe | identity | Enta Nerchina | Suddha Dhanyāsi | Adi | Suddha Dhanyasi | Unknown | approved |  |
| tyagaraja:krithi:d40b776c-a855-4fa0-99f1-3a664a016c56 | identity | ennALLUrakE | Śubhapantuvarāḷi | Misra Capu | Subha pantuvarALi | Isra Capu | approved |  |
| tyagaraja:krithi:d4579215-90be-47f8-8410-44020f815417 | identity | SRngArincukoni | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:d479e704-921d-4583-89af-e8fcb94bd85b | identity | Evarani Nirnayinchiri | Devamrta Varshini | Adi | Devamrta Varshini | Unknown | approved |  |
| tyagaraja:krithi:d4a411e4-f029-46c9-8aad-699e2d235cae | identity | Raga Sudhaa | Andolikā | Adi | Andolika | Unknown | approved |  |
| tyagaraja:krithi:d5076e9b-05cb-4fed-bb41-de1b09b5f4a8 | identity | SyAma sundarAnga | Dhanyāsi | Rupaka | dhanyAsi | Rupakam | approved |  |
| tyagaraja:krithi:d508d190-8532-4ecc-8939-ee7684d093c1 | identity | intakanna Ananda | Bilahari | Rupaka | bilahari | Rupakam | approved |  |
| tyagaraja:krithi:d537afd4-27de-4888-8805-27ca35786ff2 | identity | eTula kApADuduvO | Āhiri | Triputa | Ahiri | Triputa | approved |  |
| tyagaraja:krithi:d59984e7-fabc-4536-b5a2-60e550277626 | identity | kana kana rucirA | Varāli | Adi | varALi | Ad | approved |  |
| tyagaraja:krithi:d6cafbcb-889b-4c4b-a4a1-d7c32fe5344e | identity | kAla haraNa | Suddha Sāveri | Rupaka | Suddha sAvEri | Rupakam | approved |  |
| tyagaraja:krithi:d6da96c9-8628-4cd4-b079-3fa04883fcba | identity | parAmukhamEla | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:d7383d27-873a-4a6c-9add-3018cd4af058 | identity | Sita Pati Naa Manasuna | Kamās | Adi | Kamas | Unknown | approved |  |
| tyagaraja:krithi:d746595f-9362-42d2-983c-712345f880d4 | identity | nIkE daya rAka | Neelāmbari | Misra Capu | nIlAmbari | Isra Capu | approved |  |
| tyagaraja:krithi:d750cc52-5801-419e-b789-0c9c1ec346c4 | identity | kuvalaya daLa | nATa kuranji | Adi | nATa kuranji | Unknown | approved |  |
| tyagaraja:krithi:d77e7ccf-6bb0-4796-bf3a-96f5b615017b | identity | nI daya rAvale | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:d821cbd3-b67d-4b70-ad29-31920f66eb13 | identity | nArAyaNa hari | yamunA kalyANi | Adi | yamunA kalyANi | Adi | approved |  |
| tyagaraja:krithi:d844e1e2-5f46-4eb1-97a1-35b4ff52f883 | identity | Kamalaapta Kula | bRndAvana sAranga | Adi | Brndavana Saranga | Unknown | approved |  |
| tyagaraja:krithi:d8e009c1-0e0e-48e2-b48f-72bfdf483d32 | identity | daya sEyavayya | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:d9b7b93f-9c9f-47f8-94e0-23d6c05c1c75 | identity | Aanandamaananda | Bhairavi | Adi | Bhairavi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:d9f78754-2edc-4509-af4d-3f6104437b0a | identity | Muripemu | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:da47bd3e-75ff-4227-8d7f-174aa3bb0d87 | identity | Sangita Sastra | Mukhāri | Adi | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:da7f37d0-ac3c-42f4-ba35-85bae4134678 | identity | lEmi telpa | Navanītam | Adi | navanItaM | Adi | approved |  |
| tyagaraja:krithi:db5c9fda-a13f-450d-985e-5e335cd6280a | identity | mahima taggincukO | Riśabhapriyā | Adi | rishabha priya | Adi | approved |  |
| tyagaraja:krithi:dbc42cee-6859-4196-90ae-16e05f9a06d1 | identity | parama pAvana | Panthuvarāli | Misra Capu | pantuvarALi | Isra Capu | approved |  |
| tyagaraja:krithi:dbcefbe4-de22-4830-b9be-468a632dbc8f | identity | rAma nAmamu janma | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:dbe88431-22bd-44ec-8cd8-f1282b64e468 | identity | gAravimpa rAdA | Ghanta | Rupaka | ghaNTA | Rupakam | approved |  |
| tyagaraja:krithi:dc5fbe4f-b710-4128-bbf9-c40ec1f71157 | identity | Rama Nannu Brova | Harikāmbhōji | Rupaka | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:dc82e4eb-1e97-42d0-b505-432013bcaca1 | identity | Nijamuga Nee | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:dd9e3656-85ac-4ba9-b9ed-a2d9c4a5e317 | identity | Chani Todi Tevae | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:ddc9018a-3774-4a41-b7c8-7e3b25ef9874 | identity | samsArulaitE | Sāveri | Adi | sAvEri | Adi | approved |  |
| tyagaraja:krithi:ddcdd2df-804c-4c4d-a2ac-57f1c0c71275 | identity | Raamuni Maravakavae | kEdAra gauLa | Adi | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:de0d61d6-3ab2-4fc0-8023-bf7dda4778a9 | identity | Aparaadhamula Maanpi | Darbar | Jhampa | Darbar | Unknown | approved |  |
| tyagaraja:krithi:de598d7d-0eef-43f7-82f9-b9ff9ed743a9 | identity | Kanna Tandri | Deva Manohari | Adi | Deva Manohari | Unknown | approved |  |
| tyagaraja:krithi:deab3382-1a62-4554-aade-c66780944c6f | identity | darSanamu sEya | nArAyaNa gauLa | Jhampa | nArAyaNa gauLa | Unknown | approved |  |
| tyagaraja:krithi:df23bae3-d13f-4b59-91dd-493c39c4eee2 | identity | rE mAnasa | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:df442f18-93e6-4e36-bd2d-60d09acb2d78 | identity | E rAmuni nammitinO | Vakuḷābharaṇam | Adi | vakuLAbharaNaM | Adi | approved |  |
| tyagaraja:krithi:df4ef29d-2fd9-42b8-b984-b0035e62bdf7 | identity | Bhaja Ramam | Huseni | Adi | Huseni | Unknown | approved |  |
| tyagaraja:krithi:df731af2-9892-4e46-a3f4-f773dacf50af | identity | jaya jaya SrI raghurAma | Mangalakaishiki | Adi | mangaLa kaisiki | Adi | approved |  |
| tyagaraja:krithi:dff636dc-d88e-48a3-a88a-f08b9dfc0154 | identity | bhava nuta | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:dffbb6d7-4eb4-4c89-8124-396b951cae93 | identity | rArA mAyiNTidAka | Asāveri | Adi | asAvEri | Unknown | approved |  |
| tyagaraja:krithi:e0151a7a-e0d9-413d-b07a-4c1b2854971f | identity | sundarESvaruni | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:e020bc69-086e-47b1-be97-d3d8294f0379 | identity | tuLasi jagajjanani | Sāveri | Rupaka | sAvEri | Rupakam | approved |  |
| tyagaraja:krithi:e0600de7-2027-4961-87be-cdfb8fe78357 | identity | Oka Maata | Harikāmbhōji | Rupaka | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:e0723c64-9c2b-4b5c-a5b4-6cc4bb597524 | identity | Anyaayamu | Kāpi | Adi | Kapi | Unknown | approved |  |
| tyagaraja:krithi:e144c7fb-3320-4168-9b65-bc120afa3a7d | identity | mElukOvayya | Bowli | Jhampa | bhauLi | Jhampa | approved |  |
| tyagaraja:krithi:e17ac587-44d1-4e1a-82c1-ffda7bc0b3f3 | identity | Sarvantaryaami | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:e20e4215-c9ef-4f2a-b7d2-28c5aa43a1bb | identity | Nagu Momu Gala Vaani | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:e2f166b0-c936-4b29-ab16-22fe89701283 | identity | Yuktamu Kaadu | Sri | Adi | Sri Ragam | Unknown | approved |  |
| tyagaraja:krithi:e3f7b286-f9a0-4a4e-9f4f-ef8dd67ad35a | identity | E panikO | Asāveri | Adi | asAvEri | Adi | approved |  |
| tyagaraja:krithi:e48f4721-e331-4e3d-84c0-3e1064bd1ce8 | identity | telisi rAma | Poornachandrika | Adi | pUrNa candrika | Adi | approved |  |
| tyagaraja:krithi:e52f31c0-cf9f-49f3-84c9-abdd055ba15f | identity | sarasIruha nayana | Bilahari | Misra Capu | bilahari | Isra Capu | approved |  |
| tyagaraja:krithi:e54a2564-61d0-4c0e-96e2-ecd02b517e1d | identity | endu dAginADO | Hanumatodi | Misra Capu | tODi | Isra Capu | approved |  |
| tyagaraja:krithi:e561edc8-f58f-4a1e-acec-968495fec3db | identity | enta muddO | Bindhumālini | Adi | bindu mAlini | Adi | approved |  |
| tyagaraja:krithi:e5f5a5ac-dd47-4b23-9d18-99d09ea95266 | identity | Paahi Mam Sri Ramachandra | Kāpi | Jhampa | Kapi | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:e630e8f7-f8da-45b3-8e8b-fce0f91a74e9 | identity | Kori Sevimpa | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved |  |
| tyagaraja:krithi:e722f5ff-3066-43d1-9848-a926454745d4 | identity | Atade Dhanyudu | Kāpi | Misra Capu | Kapi | Unknown | approved |  |
| tyagaraja:krithi:e72a140c-7ead-4721-bf46-2693f3b460ed | identity | E tAvuna nErcitivO | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:e73049d2-c17d-4a3a-9a18-7a895449d74b | identity | Entani Ne | Mukhāri | Rupaka | Mukhari | Unknown | approved |  |
| tyagaraja:krithi:e7af76fc-eddf-4b85-829f-3e56a07a9acc | identity | Manasaa Sri Ramachandruni | Eeshamanohari | Adi | Isa Manohari | Unknown | approved |  |
| tyagaraja:krithi:e7b6ac75-839f-4122-82a2-dfc748eabab6 | identity | hariyanuvAri | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:e7d895a3-ab2c-46d9-84c8-d7fbaaa71896 | identity | Upachaaramulanu Chekona | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:e897a8c6-ef38-411f-8229-0b6f2ad13e3c | identity | koluvaiyunnADE | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:e8ac1d32-c7c9-42e3-9157-e4d06c301a43 | identity | tIrunA nA lOni | Sāveri | Jhampa | sAvEri | Jhampa | approved |  |
| tyagaraja:krithi:e8c3ba4e-a639-43e4-83b8-34411027cb41 | identity | Giripai Nelakonna | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:e8c46f2f-e996-4fa2-b978-c8e0d843da11 | identity | rUkalu padi vElu | dESiya tODi | Adi | dESiya tODi | Adi | approved |  |
| tyagaraja:krithi:e8de604f-1086-4bb5-bb70-cc9b7b21828e | identity | gItArthamu | suraTi | Adi | suraTi | Unknown | approved |  |
| tyagaraja:krithi:e90dc863-7cae-4986-a557-1711f7f98bd1 | identity | Dina Mani Vamsa | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:e931e5ec-b7c7-4404-acf9-a2b468b156d7 | identity | madilOna yOcana | Kolahalam | Adi | kOlAhalaM | Desadi | mapped | missing_charanam |
| tyagaraja:krithi:e9358be5-57d8-43f8-bb82-e8477f851b57 | identity | Natha Brovavae | Bhairavi | Adi | Bhairavi | Unknown | approved |  |
| tyagaraja:krithi:eb516533-8464-40c3-8c7c-86dcbce93129 | identity | manasA eTulOrtunE | Malayamārutam | Rupaka | malaya mArutaM | Rupakam | approved |  |
| tyagaraja:krithi:ec1fb06f-6d95-4689-9ae6-3a9cf0999d34 | identity | Sri Maanini | Purna Shadjam | Adi | Purna Shadjam | Unknown | approved |  |
| tyagaraja:krithi:ed2395c0-fb30-482d-b293-95c3a690da5a | identity | E varamaDugudu | Mechakalyāni | Rupaka | kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:ed95ad2a-f86e-45ff-ada3-bb472b2a418e | identity | kaDa tEra rAdA | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:edb0fece-4478-4bc9-84c1-6f3ffd2c3b2b | identity | rAma Eva daivataM | Bālahamsa | Rupaka | balahamsa | Unknown | mapped |  |
| tyagaraja:krithi:eef82f61-9e42-4514-8142-e77b2cd0b68e | identity | vEda vAkyamani | Mohanam | Triputa | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:ef184876-c808-401a-bb3d-b8321d265334 | identity | vaddanE vAru | Śanmukhapriyā | Adi | shaNmukha priya | Adi | approved |  |
| tyagaraja:krithi:efd71f41-5405-402d-af29-c8fc6ddb6848 | identity | tuLasi daLamulacE | mAyA mALavagauLa | Rupaka | mAyA mALava gauLa | Rupakam | approved |  |
| tyagaraja:krithi:f0fd6665-d378-48fd-8772-6610a997c34d | identity | nAdOpAsana | Begada | Adi | bEgaDa | Unknown | approved |  |
| tyagaraja:krithi:f1097b68-6d86-4a3c-8f98-08707295554e | identity | vina rAdA | Devagāndhāri | Adi | deva gAndhAri | Desadi | mapped |  |
| tyagaraja:krithi:f1a32daa-973d-48df-8bef-b3ee93fe17fa | identity | Rama Lobhamela | Darbar | Adi | Darbar | Unknown | approved |  |
| tyagaraja:krithi:f2651795-4c95-40e8-8734-418fe059b2e3 | identity | pAhi kalyANa sundara | Punnagavarali | Capu | punnAga varALi | Capu | approved |  |
| tyagaraja:krithi:f287410c-39e9-495d-9aab-419a63ecc2ed | identity | Nee Muddu Momu | Kamalā Manohari | Adi | Kamala Manohari | Unknown | approved |  |
| tyagaraja:krithi:f2990794-8758-460b-96fd-abd3fbd0e589 | identity | Deva Sri Tapasteertha | Madhyamāvathi | Triputa | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:f37cec70-10b7-493a-8068-a305c502528e | identity | ilalO praNatArti | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:f49769af-f30b-4345-8282-da36516062d9 | identity | Raanidi Raadu | Manirangu | Adi | Manirangu | Unknown | mapped |  |
| tyagaraja:krithi:f4f086b2-d63d-45c0-9f69-dd8b609e85bf | identity | sAgaruNDu | yamunA kalyANi | Rupaka | yamunA kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:f5081739-3f8e-4427-90c7-becb3b1eeba9 | identity | sItA kalyANa | SankarAbharaNaM | Khanda Capu | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:f5b3f945-9362-4b7b-945e-8d920e6b66c9 | identity | gandhamu puyyarugA | Punnagavarali | Adi | punnAga varALi | Unknown | approved |  |
| tyagaraja:krithi:f5e86fd2-05eb-4c88-99c4-7ef9214618a0 | identity | Naada Loludai | Kalyāna Vasantam | Rupaka | Kalyana Vasanta | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:f60c8d2f-7ac2-4ee8-9512-dec61c9cd6e0 | identity | bhajana sEyavE | Mechakalyāni | Rupaka | kalyANi | Rupakam | approved |  |
| tyagaraja:krithi:f6acd658-e5b4-44d8-b642-b1e74d95898a | identity | Mitri Bhaagyamae | Kharaharapriyā | Adi | Kharahara Priya | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:f6fe6cd6-1724-491f-9268-2fe4e4a4024d | identity | Phanipati Sayi | Jhankāradhvani | Adi | Jhankara Dhvani | Unknown | approved |  |
| tyagaraja:krithi:f78b4391-b29d-4602-9733-a7fee6c96b29 | identity | ninu vinA sukhamu | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:f885a354-b961-400f-ac3f-df5fee309d0c | identity | smaraNE sukhamu | Jana Ranjani | Adi | jana ranjani | Desadi | approved |  |
| tyagaraja:krithi:f8b7f2f6-8f23-485b-bf8d-acc343a368ee | identity | E vidhamulanaina | SankarAbharaNaM | Adi | SankarAbharaNaM | Unknown | approved |  |
| tyagaraja:krithi:f8bbbeb4-ca30-4de9-a9f6-6be0d8b95753 | identity | maracE vADanA | Kedaram | Adi | kEdAraM | Adi | approved |  |
| tyagaraja:krithi:f8fd0f11-c316-40c8-b9d1-25e334dbddd6 | identity | nI dayacE rAma | Yadukula Kāmbhoji | Adi | yadukula kAmbhOji | Unknown | approved |  |
| tyagaraja:krithi:f92c6be0-2781-4d84-9670-541b32447bcc | identity | Dehi Tava Pada | Sahāna | Adi | Sahana | Unknown | approved |  |
| tyagaraja:krithi:f92e657b-fbd3-413b-84d6-333777eed436 | identity | kshIra sAgara Sayana | Devagāndhāri | Adi | deva gAndhAri | Adi | approved |  |
| tyagaraja:krithi:f960fdb0-3d6a-4a26-8d93-8af37750f722 | identity | eTulaina bhakti | sAma | Misra Capu | sAma | Unknown | approved |  |
| tyagaraja:krithi:f9bb7a8a-cedd-4490-b7fb-ed2408d6a5d5 | identity | Laali Laaliyani | Harikāmbhōji | Adi | Hari Kambhoji | Unknown | approved |  |
| tyagaraja:krithi:fa07ba19-5745-48bf-a872-27feea461225 | identity | Nenendu Vetukuduraa | Karnātaka Behāg | Adi | Karnataka Behag | Unknown | approved |  |
| tyagaraja:krithi:fa538997-21c3-431c-b60a-687571bd18c8 | identity | amma dharma saMvardhani | Atāna | Adi | aThANa | Adi | approved |  |
| tyagaraja:krithi:fa581380-0a7d-44a5-ac3f-35fcd907ea61 | identity | Sarva Loka Dayaanidhae | Huseni | Tisra Ekam | Huseni | Unknown | approved |  |
| tyagaraja:krithi:fa5f98b6-119e-40eb-b50c-637f0284abaf | identity | Garuda Gamana | Gourimanohari | Rupaka | Gauri Manohari | Unknown | approved |  |
| tyagaraja:krithi:fbbf74f2-3429-4c8c-be94-8e088034968b | identity | tava dAsOhaM | Punnagavarali | Adi | punnAga varALi | Adi | approved |  |
| tyagaraja:krithi:fbbfd918-c7d6-4f72-a1aa-f27ef5065892 | identity | Kalugunaa | Poornalalita | Adi | Purna Lalita | Unknown | approved | missing_pallavi |
| tyagaraja:krithi:fc6630fc-1e9f-4b15-9530-f5f3371f8ef3 | identity | Menu Joochi | Sarasāngi | Adi | Sarasangi | Unknown | approved |  |
| tyagaraja:krithi:fc683216-fb66-4f4d-b349-b1fad0d07359 | identity | jaya jaya sItArAM | Sāveri | Capu | sAvEri | Capu | approved |  |
| tyagaraja:krithi:fcd026e0-0056-48c3-af9f-a8a55c400077 | identity | Venkatesa | Madhyamāvathi | Adi | Madhyamavati | Unknown | approved |  |
| tyagaraja:krithi:fd23c17f-b737-4b36-ac87-37c55157c9e5 | identity | graha balamEmi | Revagupti | Adi | rEvagupti | Adi | approved |  |
| tyagaraja:krithi:fd8d6b24-7414-467c-9e7f-1db5b40f0081 | identity | nI cittamu niScalamu | Dhanyāsi | Capu | dhanyAsi | Capu | approved |  |
| tyagaraja:krithi:fdb8a81c-1f1a-4963-ac13-28fa71d3f6a4 | identity | baNTu rIti | Hamsanādam | Adi | hamsa nAdaM | Adi | approved |  |
| tyagaraja:krithi:fdbda5e6-0080-4b9d-84bf-6378a9cbd9bb | identity | Venu Gaana Loluni | kEdAra gauLa | Rupaka | Kedara Gaula | Unknown | approved |  |
| tyagaraja:krithi:fdde206b-d725-41ac-a159-c7d0818916e4 | identity | AragimpavE | Hanumatodi | Rupaka | tODi | Rupakam | approved |  |
| tyagaraja:krithi:fe316a27-7978-4a95-a260-83977c69bf73 | identity | rAmA ninu nammina | Mohanam | Adi | mOhanaM | Unknown | approved |  |
| tyagaraja:krithi:ff680a45-cadf-4566-b7f7-a5c74e513eda | identity | nOrEmi SrI rAma | Varāli | Adi | varALi | Adi | approved |  |
| tyagaraja:krithi:ffb09968-4a54-4bd2-a32b-d326b72f0967 | identity | rAmAbhirAma manasu | Dhanyāsi | Adi | dhanyAsi | Adi | approved |  |
| tyagaraja:krithi:ffb45270-30b7-4431-b902-7aebca467ac0 | identity | dASarathI nI RNamu | Hanumatodi | Adi | tODi | Adi | approved |  |
| tyagaraja:krithi:ffd84ae2-f4cd-4b4e-b36b-6e6769072734 | identity | idE bhAgyamu | Kannada | Misra Capu | kannaDa | Isra Capu | approved |  |
| tyagaraja:krithi:fff09e5d-b370-41d1-98db-4905b7902722 | identity | rAmaM bhajEhaM | Sāveri | Adi | sAvEri | Adi | mapped | missing_pallavi |
