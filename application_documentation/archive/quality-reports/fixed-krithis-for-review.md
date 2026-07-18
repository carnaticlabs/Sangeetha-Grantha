# Fixed Krithis — Section-Issues Cleanup (for review)

Generated 2026-07-16. Automated fix of the Curator "Section Issues" queue via the
Latin-as-template section split (all writes audit-logged as `UPDATE_LYRIC_VARIANT_SECTIONS`).

- **Krithis touched:** 509
- **Lyric variants fixed:** 2462 (distinct)  ·  audit-log rows: 2467
- **Queue:** 2945 → 484 section issues remaining

Each row: a krithi whose non-template variants were split from one oversized section into
their proper sections, matching the Latin/English template's structure. `langs` = which
language variants were fixed; `secs` = canonical section count now enforced on each.

| # | Composer | Title | Langs fixed | Variants | Secs | Krithi ID |
|---|----------|-------|-------------|:-------:|:----:|-----------|
| 1 | Tyagaraja | A daya SrI raghu vara | kn,ml,sa,te | 4 | 3 | `24850293-86b2-4e38-a786-91d36dd79354` |
| 2 | Tyagaraja | ADavAramella | kn,ml,sa,ta,te | 5 | 10 | `b8b35e19-e62b-4508-9204-486ab0763307` |
| 3 | Tyagaraja | Aada Modi Galadae | en,kn,ml,sa,ta,te | 6 | 3 | `823cf8d7-f2e9-4a9d-bedd-506dffd550c1` |
| 4 | Tyagaraja | Abhimaanamennadu | kn,ml,sa,ta,te | 5 | 3 | `9e77e997-acfc-4e43-b743-52c0164dbfce` |
| 5 | Tyagaraja | Abhimaanamu Ledemi | kn,ml,sa,ta,te | 5 | 4 | `4b768a4c-4ef0-4cb4-a2e9-c8e76562063a` |
| 6 | Tyagaraja | Adigi Sukhamu | kn,ml,sa,ta,te | 5 | 6 | `cee0b420-4914-4914-9318-01b6d52d1238` |
| 7 | Tyagaraja | Ananda sAgara | kn,ml,sa,te | 4 | 3 | `882f7bab-9a00-41ef-9c4c-d96cec3d2a23` |
| 8 | Tyagaraja | AragimpavE | kn,ml,sa,ta,te | 5 | 3 | `fdde206b-d725-41ac-a159-c7d0818916e4` |
| 9 | Tyagaraja | Bhuvini Dasudanae | kn,ml,sa,ta,te | 5 | 3 | `3ed8918c-c99c-48b1-b0e9-7b0f06dc7850` |
| 10 | Tyagaraja | Chani Todi Tevae | kn,ml,sa,ta,te | 5 | 3 | `dd9e3656-85ac-4ba9-b9ed-a2d9c4a5e317` |
| 11 | Tyagaraja | Chentanae Sadaa | kn,ml,sa,ta,te | 5 | 3 | `0ebeaa05-5481-4300-ac39-70986688efed` |
| 12 | Tyagaraja | Daya Leni | kn,ml,sa,ta,te | 5 | 3 | `b7e2a6c8-fcc6-4fa9-9f53-038f3816b06c` |
| 13 | Tyagaraja | Dehi Tava Pada | kn,ml,sa,ta,te | 5 | 5 | `f92c6be0-2781-4d84-9670-541b32447bcc` |
| 14 | Tyagaraja | Deva Sri Tapasteertha | kn,ml,sa,ta,te | 5 | 5 | `f2990794-8758-460b-96fd-abd3fbd0e589` |
| 15 | Tyagaraja | Dina Mani Vamsa | kn,ml,sa,ta,te | 5 | 3 | `e90dc863-7cae-4986-a557-1711f7f98bd1` |
| 16 | Tyagaraja | E dAri sancarinturA | kn,ml,sa,te | 4 | 3 | `d1d8b96f-fc20-48c1-a92f-e1b2b8a91496` |
| 17 | Tyagaraja | E nOmu nOcitimO | kn,ml,sa,te | 4 | 6 | `bd7d546b-61b3-485e-91c0-0082bafe41e3` |
| 18 | Tyagaraja | E panikO | kn,ml,sa,ta,te | 5 | 3 | `e3f7b286-f9a0-4a4e-9f4f-ef8dd67ad35a` |
| 19 | Tyagaraja | E rAmuni nammitinO | kn,ml,sa,ta,te | 5 | 5 | `df442f18-93e6-4e36-bd2d-60d09acb2d78` |
| 20 | Tyagaraja | E tAvuna nErcitivO | kn,ml,sa,te | 4 | 3 | `e72a140c-7ead-4721-bf46-2693f3b460ed` |
| 21 | Tyagaraja | E tAvunarA | kn,ml,sa,ta,te | 5 | 3 | `5757c1bb-3822-48b5-bb2c-651f55d0e2ba` |
| 22 | Tyagaraja | E vidhamulanaina | kn,ml,sa,te | 4 | 5 | `f8b7f2f6-8f23-485b-bf8d-acc343a368ee` |
| 23 | Tyagaraja | ETi janmamidi | kn,ml,sa,te | 4 | 6 | `82b9fd7a-bf48-472e-8971-c509c3e6f0fc` |
| 24 | Tyagaraja | Ehi tri-jagadISa | kn,ml,sa,ta,te | 5 | 5 | `2f4496f6-a4f8-49ba-9051-004d950f8948` |
| 25 | Tyagaraja | Ela nI daya rAdu | kn,ml,sa,te | 4 | 5 | `706f4761-8454-4dd6-a111-db02cb4ede3f` |
| 26 | Tyagaraja | Elaavataara | kn,ml,sa,ta,te | 5 | 3 | `4a69d9af-b815-406c-a475-23e708182795` |
| 27 | Tyagaraja | EmEmO teliyaka | kn,ml,sa,ta,te | 5 | 7 | `4601ab2f-a5c0-4e59-a19f-852d974c9c94` |
| 28 | Tyagaraja | EmandunE vicitramunu | kn,ml,sa,te | 4 | 3 | `a117fff3-40e1-463d-83c9-3d601aa53d44` |
| 29 | Tyagaraja | Emani Pogaduduraa | kn,ml,sa,ta,te | 5 | 3 | `6627d303-b808-42c5-ac2b-bd2dd9595d26` |
| 30 | Tyagaraja | Emani mATADitivO | kn,ml,sa,ta,te | 5 | 3 | `57b48f4b-dc54-496f-a5e3-79d6d794a3c8` |
| 31 | Tyagaraja | Emani nera nammu | kn,ml,sa,ta,te | 5 | 9 | `69014557-ad45-4ce7-bbf3-f99adce3c556` |
| 32 | Tyagaraja | Emi dOva | kn,ml,sa,ta,te | 5 | 3 | `5602cd3e-70d2-4214-a292-baa93d13e03b` |
| 33 | Tyagaraja | Emi jEsitEnEmi | kn,ml,sa,te | 4 | 7 | `6e54dc31-79d5-4368-b0c9-bd3ab6f9ceb7` |
| 34 | Tyagaraja | Emi nEramu | kn,ml,sa,ta,te | 5 | 3 | `cbf2b349-35dc-48eb-aa89-74ffd3464421` |
| 35 | Tyagaraja | Enduku Nirdaya | kn,ml,sa,ta,te | 5 | 7 | `906547be-a4ff-41c2-9953-ea7284b7899f` |
| 36 | Tyagaraja | Ennaallu Tirigedi | kn,ml,sa,ta,te | 5 | 6 | `189bc0d1-e96b-4e26-9de8-ff55f51321a3` |
| 37 | Tyagaraja | Entani Ne | kn,ml,sa,te | 4 | 3 | `e73049d2-c17d-4a3a-9a18-7a895449d74b` |
| 38 | Tyagaraja | Evaraina Leraa | kn,ml,sa,ta,te | 5 | 3 | `59c2ff1b-cde6-423a-9864-db4521acdff2` |
| 39 | Tyagaraja | Evari Maata | kn,ml,sa,ta,te | 5 | 3 | `760be4bc-5b3a-430d-9969-4835c43aac74` |
| 40 | Tyagaraja | Evaricchiriraa | kn,ml,sa,ta,te | 5 | 3 | `87f6024e-4c43-4fca-a81d-8a192925843b` |
| 41 | Tyagaraja | Evarunnaaru Brova | kn,ml,sa,ta,te | 5 | 3 | `110eac88-12e8-4638-a35a-f753fb39010e` |
| 42 | Tyagaraja | Garuda Gamana | kn,ml,sa,ta,te | 5 | 3 | `fa5f98b6-119e-40eb-b50c-637f0284abaf` |
| 43 | Tyagaraja | Guru Leka | kn,ml,sa,ta,te | 5 | 3 | `4c8ae919-199e-427e-a405-10afe38ef806` |
| 44 | Tyagaraja | I mEnu kaligina | kn,ml,sa,ta,te | 5 | 6 | `85cfedb0-6913-4602-a969-c73fe754f6eb` |
| 45 | Tyagaraja | ISa pAhi | kn,ml,sa,te | 4 | 5 | `550a03c3-991c-435f-b1b5-439c16e98183` |
| 46 | Tyagaraja | Indukaayee Tanuvunu Penchinadi | kn,ml,sa,ta,te | 5 | 8 | `c2350f5a-4019-453e-9ac4-82d8fe95cbf9` |
| 47 | Tyagaraja | Inkaa Daya | kn,ml,sa,ta,te | 5 | 5 | `18ae493e-7724-40a7-b7c1-f1d861202b93` |
| 48 | Tyagaraja | Ivaraku jUcinadi | ml,sa,te | 3 | 5 | `ae7718b1-6674-47be-beb0-f50b9ed908ec` |
| 49 | Tyagaraja | Ivasudha Neevanti | kn,ml,sa,ta,te | 5 | 3 | `1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4` |
| 50 | Tyagaraja | Kali Narulaku | kn,ml,sa,ta,te | 5 | 3 | `8a3e248e-3e40-4eaf-9c77-8f2e4bec6a70` |
| 51 | Tyagaraja | Kanna Tandri | kn,ml,sa,ta,te | 5 | 3 | `de598d7d-0eef-43f7-82f9-b9ff9ed743a9` |
| 52 | Tyagaraja | Kanugonu | kn,ml,sa,ta,te | 5 | 3 | `af577b3c-4ac4-4bfd-974e-96583cdb2614` |
| 53 | Tyagaraja | Karunaa Jaladhi | ml,sa,te | 3 | 5 | `76025d5f-aa93-41a2-a9c4-8169981a2f79` |
| 54 | Tyagaraja | Kula Birudunu | kn,ml,sa,ta,te | 5 | 3 | `2c7ec18a-c429-402e-a8d6-ea963de31108` |
| 55 | Tyagaraja | Laali Laaliyani | kn,ml,sa,ta,te | 5 | 5 | `f9bb7a8a-cedd-4490-b7fb-ed2408d6a5d5` |
| 56 | Tyagaraja | Laavanya Rama | kn,ml,sa,ta,te | 5 | 3 | `cd6fdf67-8092-49f8-9a35-61851937370d` |
| 57 | Tyagaraja | Maa Jaanaki | kn,ml,sa,ta,te | 5 | 3 | `0d9065ca-f1ce-4426-87f1-a02c2145ba33` |
| 58 | Tyagaraja | Maa Ramachandruniki | kn,ml,sa,ta,te | 5 | 5 | `4e418fbf-5e06-4dc1-81dc-0f946c6de2ba` |
| 59 | Tyagaraja | Maaru Palkaga | kn,ml,sa,ta,te | 5 | 3 | `17e53b19-6e57-4c5b-b3c6-05cf6af2f3ff` |
| 60 | Tyagaraja | Manasaa Sri Ramachandruni | kn,ml,sa,ta,te | 5 | 3 | `e7af76fc-eddf-4b85-829f-3e56a07a9acc` |
| 61 | Tyagaraja | Manasaa Sri Ramuni | kn,ml,sa,ta,te | 5 | 3 | `17cf2d35-e811-4f63-8b25-71d17e25e27d` |
| 62 | Tyagaraja | Mari Mari Ninnae | kn,ml,sa,ta,te | 5 | 3 | `91213a3a-2fd2-4770-8b40-9c683dbf8539` |
| 63 | Tyagaraja | Menu Joochi | kn,ml,sa,ta,te | 5 | 3 | `fc6630fc-1e9f-4b15-9530-f5f3371f8ef3` |
| 64 | Tyagaraja | Mucchata | kn,ml,sa,te | 4 | 5 | `76af1dff-ea85-4687-8238-27af5ac9dbc6` |
| 65 | Tyagaraja | Muripemu | kn,ml,sa,te | 4 | 5 | `d9f78754-2edc-4509-af4d-3f6104437b0a` |
| 66 | Tyagaraja | Naada Tanumanisam | kn,ml,sa,ta,te | 5 | 3 | `6c9d0ab2-70a2-4c6b-a0df-c4a925bf8ae9` |
| 67 | Tyagaraja | Naadupai | kn,ml,sa,te | 4 | 6 | `0aae2382-ec72-4d8f-90e1-62aba112b37a` |
| 68 | Tyagaraja | Naama Kusuma | kn,ml,sa,ta,te | 5 | 3 | `b47eeeff-377e-4f6e-92ee-85af5ba16c1a` |
| 69 | Tyagaraja | Nagu Momu Gala Vaani | kn,ml,sa,ta,te | 5 | 5 | `e20e4215-c9ef-4f2a-b7d2-28c5aa43a1bb` |
| 70 | Tyagaraja | Nalina Lochana | kn,ml,sa,te | 4 | 5 | `1b89544a-16fb-4433-93c8-b3840dbfe10a` |
| 71 | Tyagaraja | Nee Bhajana | kn,ml,sa,ta,te | 5 | 3 | `77e3a868-f451-4028-bc8f-ff9fb696ef15` |
| 72 | Tyagaraja | Nee Muddu Momu | kn,ml,sa,ta,te | 5 | 5 | `f287410c-39e9-495d-9aab-419a63ecc2ed` |
| 73 | Tyagaraja | Nenaruncharaa Naapaini | kn,ml,sa,ta,te | 5 | 3 | `9fe198f5-d71b-46a6-a5f7-14245b32d29d` |
| 74 | Tyagaraja | Nenendu Vetukuduraa | kn,ml,sa,ta,te | 5 | 3 | `fa07ba19-5745-48bf-a872-27feea461225` |
| 75 | Tyagaraja | Nija Marmamula | kn,ml,sa,ta,te | 5 | 3 | `80528ba0-42c8-4b50-b65f-b1771996f18b` |
| 76 | Tyagaraja | Nijamuga Nee | kn,ml,sa,ta,te | 5 | 3 | `dc82e4eb-1e97-42d0-b505-432013bcaca1` |
| 77 | Tyagaraja | O Ranga Saayi | kn,ml,sa,ta,te | 5 | 3 | `c262739c-e7cc-492d-b248-3775fec24bfe` |
| 78 | Tyagaraja | O rAjIvAksha | kn,ml,sa,ta,te | 5 | 5 | `bdbcc661-264b-4943-9c46-5c824056a239` |
| 79 | Tyagaraja | O rAma O rAma | kn,ml,sa,te | 4 | 11 | `7ac1ec39-56e0-4c84-9ea1-3e55dc7569b2` |
| 80 | Tyagaraja | O ramA ramaNa | kn,ml,sa,ta,te | 5 | 10 | `7d556895-a35c-4b48-b922-e2815fa492e9` |
| 81 | Tyagaraja | ODanu jaripE | kn,ml,sa,ta,te | 5 | 5 | `cc45b4cc-c3ca-4a69-b3c5-efdeeecb8f85` |
| 82 | Tyagaraja | Padavi Nee | kn,ml,sa,ta,te | 5 | 6 | `cf6e4421-14d9-4412-97cd-14c4500eb678` |
| 83 | Tyagaraja | Paritaapamu | kn,ml,sa,ta,te | 5 | 3 | `6dfbf308-eb7a-408f-b4ed-ca9168670204` |
| 84 | Tyagaraja | Patti Viduva | kn,ml,sa,ta,te | 5 | 3 | `6929bbfd-0c31-4f9b-8994-395b08b522fb` |
| 85 | Tyagaraja | Raaraa Phani Sayana | kn,ml,sa,ta,te | 5 | 5 | `b175d4b9-f889-4906-b2a4-10646c17d56d` |
| 86 | Tyagaraja | Raga Sudhaa | kn,ml,sa,ta,te | 5 | 3 | `d4a411e4-f029-46c9-8aad-699e2d235cae` |
| 87 | Tyagaraja | Raghu Nandana Raghu Nandana | kn,ml,sa,ta,te | 5 | 14 | `9e76c325-2670-4a57-a575-492b6b68d9f0` |
| 88 | Tyagaraja | Raghu Veera Rana | kn,ml,sa,ta,te | 5 | 3 | `ce8ecb05-26e2-4115-ad6a-88f841790c3a` |
| 89 | Tyagaraja | Raghupatae Rama | kn,ml,sa,ta,te | 5 | 5 | `ae1996ea-2791-42ba-b927-89fc137549dd` |
| 90 | Tyagaraja | Rama Bhakti | kn,ml,sa,ta,te | 5 | 3 | `a461999c-05bc-4910-8b2c-afde25719ea5` |
| 91 | Tyagaraja | Rama Kathaa | kn,ml,sa,ta,te | 5 | 3 | `9d588414-4b01-4651-a98a-dd30b3b0dae8` |
| 92 | Tyagaraja | Rama Lobhamela | kn,ml,sa,ta,te | 5 | 5 | `f1a32daa-973d-48df-8bef-b3ee93fe17fa` |
| 93 | Tyagaraja | Rama Namam Bhajarae | kn,ml,sa,ta,te | 5 | 8 | `5ab61c20-7312-4ad2-aa80-a94319535990` |
| 94 | Tyagaraja | Rama Nannu Brova | kn,ml,sa,ta,te | 5 | 3 | `dc5fbe4f-b710-4128-bbf9-c40ec1f71157` |
| 95 | Tyagaraja | Rama Samayamu | kn,ml,sa,te | 4 | 5 | `38a8fa0e-49e9-41af-9182-7d41a8b5a81b` |
| 96 | Tyagaraja | SAntamu lEka | kn,ml,sa,ta,te | 5 | 6 | `90e3cbc4-3907-4c15-baac-732f8f232479` |
| 97 | Tyagaraja | SObhAnE SObhAnE | kn,ml,sa,ta,te | 5 | 5 | `823bc4b9-1fbc-47d0-bf88-cd84dcc69de0` |
| 98 | Tyagaraja | SObhillu sapta svara | kn,ml,sa,ta,te | 5 | 3 | `0b317a6b-ddb4-4e32-a960-c003f1b243b2` |
| 99 | Tyagaraja | SRngArincukoni | kn,ml,sa,te | 4 | 7 | `d4579215-90be-47f8-8410-44020f815417` |
| 100 | Tyagaraja | SaSi vadana | kn,ml,sa,ta,te | 5 | 3 | `2d20f97d-a762-4b59-b399-5012d1c6d301` |
| 101 | Tyagaraja | SambhO Siva | kn,ml,sa,ta,te | 5 | 9 | `1f53b74e-3fa8-4f0a-b0f6-ccde12faf39d` |
| 102 | Tyagaraja | SambhO mahA dEva | kn,ml,sa,ta,te | 5 | 3 | `1b936da8-9b2a-4cc1-af40-00d40ec0d8c8` |
| 103 | Tyagaraja | Sanaatana | kn,ml,sa,ta,te | 5 | 3 | `14973eb2-3f3a-49b9-ac25-728a110ffa46` |
| 104 | Tyagaraja | Sangita Sastra | kn,ml,sa,ta,te | 5 | 3 | `da47bd3e-75ff-4227-8d7f-174aa3bb0d87` |
| 105 | Tyagaraja | Sarasa Saama Daana | kn,ml,sa,ta,te | 5 | 3 | `b536b933-9088-4ca0-928e-2618ce2fae57` |
| 106 | Tyagaraja | Saraseeruhaanana | kn,ml,sa,ta,te | 5 | 3 | `06f0c5bd-c58f-43bd-aa22-4efacbbc6351` |
| 107 | Tyagaraja | Sariyevvarae | kn,ml,sa,ta,te | 5 | 3 | `3ff7cbc9-e452-4da4-b06b-e520f683050b` |
| 108 | Tyagaraja | Sarva Loka Dayaanidhae | kn,ml,sa,ta,te | 5 | 9 | `fa581380-0a7d-44a5-ac3f-35fcd907ea61` |
| 109 | Tyagaraja | Siggu Maali | kn,ml,sa,te | 4 | 5 | `5638a240-7a35-437e-b112-1cba581deef2` |
| 110 | Tyagaraja | Sita Pati Naa Manasuna | kn,ml,sa,ta,te | 5 | 3 | `d7383d27-873a-4a6c-9add-3018cd4af058` |
| 111 | Tyagaraja | SivE pAhi mAM | kn,ml,sa,ta,te | 5 | 5 | `bf785ecf-eb8b-4b25-95db-313b0d0a8904` |
| 112 | Tyagaraja | Sogasugaa Mridanga | kn,ml,sa,ta,te | 5 | 3 | `77d3a9de-411c-4c6b-b285-026d19d0bf27` |
| 113 | Tyagaraja | SrI janaka tanayE | kn,ml,sa,ta,te | 5 | 3 | `5e362e9a-6b53-48de-818c-59844db7819a` |
| 114 | Tyagaraja | SrI kAnta nIyeDa | kn,ml,sa,ta,te | 5 | 3 | `4e9eac68-ee8d-4da8-8c75-7131e956f112` |
| 115 | Tyagaraja | SrI rAma SrI rAma | kn,ml,sa,ta,te | 5 | 13 | `34ee92f6-8f4e-428a-8822-9ca37913349e` |
| 116 | Tyagaraja | SrI rAma candra | kn,ml,sa,ta,te | 5 | 10 | `9fe40f47-7453-4930-9257-9a0386751cd6` |
| 117 | Tyagaraja | SrI rAma dAsa dAsOhaM | kn,ml,sa,te | 4 | 12 | `2c318386-f377-416c-972d-41506897e0b5` |
| 118 | Tyagaraja | SrI rAma rAmASritulamu | kn,te | 2 | 13 | `bbc8382b-a344-47c2-8b4d-fb831a541230` |
| 119 | Tyagaraja | SrI rAma rAma jagadAtma | kn,ml,sa,ta,te | 5 | 7 | `7253c9f5-6903-4ad3-b63f-1eb6b9f01052` |
| 120 | Tyagaraja | SrI rAma rAma rAma | kn,ml,sa,ta,te | 5 | 6 | `501d0f9a-35b4-4ae6-a31e-042f70e0b0fe` |
| 121 | Tyagaraja | SrI raghu kulamandu | kn,ml,sa,ta,te | 5 | 3 | `7e954839-2267-43a1-8d6b-9872b7cb2a04` |
| 122 | Tyagaraja | SrI raghu vara dASarathE | kn,ml,sa,te | 4 | 12 | `0582b4fa-d55a-4756-9428-566fc120e361` |
| 123 | Tyagaraja | SrI raghu vara karuNAkara | kn,ml,ta,te | 4 | 5 | `26fe209d-d385-416f-8c88-e007f0f2380a` |
| 124 | Tyagaraja | SrI tuLasamma | kn,ml,sa,ta,te | 5 | 4 | `c2a4541f-1bb4-41bc-bfe1-89890ed9bd20` |
| 125 | Tyagaraja | SrIpa priya | kn,ml,sa,ta,te | 5 | 3 | `712c208e-5fa2-40be-8fac-9981eabdae15` |
| 126 | Tyagaraja | SrIpaptE nI pada | kn,ml,sa,ta,te | 5 | 3 | `341feaef-7ba8-4621-b929-065ce4c82764` |
| 127 | Tyagaraja | Sri Janaki Manohari | kn,ml,sa,ta,te | 5 | 3 | `4e6c1010-d9a5-4461-b4d0-49c6b6e73b45` |
| 128 | Tyagaraja | Sri Maanini | kn,ml,sa,ta,te | 5 | 3 | `ec1fb06f-6d95-4689-9ae6-3a9cf0999d34` |
| 129 | Tyagaraja | Sujana Jeevana | kn,ml,sa,ta,te | 5 | 3 | `887bed65-9c5c-4fca-a7f8-4ac576cd1ee3` |
| 130 | Tyagaraja | SyAma sundarAnga | kn,ml,sa,ta,te | 5 | 3 | `d5076e9b-05cb-4fed-bb41-de1b09b5f4a8` |
| 131 | Tyagaraja | Talachinantanae | kn,ml,sa,ta,te | 5 | 5 | `ce57d2f1-4b5c-41a9-9034-6130eb0d20f3` |
| 132 | Tyagaraja | Toli Ne Jesina | kn,ml,sa,ta,te | 5 | 3 | `bd7dec39-7fb5-45e6-b905-5e887fc294f8` |
| 133 | Tyagaraja | Tulasi Bilva | kn,ml,sa,ta,te | 5 | 3 | `44d9e846-50b1-43dd-bc7e-9f82994d953c` |
| 134 | Tyagaraja | Vaarija Nayana-1 | kn,ml,sa,ta,te | 5 | 5 | `4d49569a-cd31-444d-98b9-8be4ee2e1e91` |
| 135 | Tyagaraja | Vaarija Nayana-2 | kn,ml,sa,ta,te | 5 | 6 | `7ab08ebb-7d65-48c7-93cb-152d9491283b` |
| 136 | Tyagaraja | Vanaja Nayanudani | kn,ml,sa,ta,te | 5 | 3 | `52ace0b2-5d91-46b3-9c0c-add32c7f1766` |
| 137 | Tyagaraja | Vandanamu | kn,ml,sa,ta,te | 5 | 10 | `78b86c5a-7c6e-4797-a4ab-0be561979065` |
| 138 | Tyagaraja | Vara Raaga Laya | kn,ml,sa,ta,te | 5 | 3 | `a6f42c55-bb6b-449d-94f9-cfa032bcb288` |
| 139 | Tyagaraja | Varada Raja | kn,ml,sa,ta,te | 5 | 3 | `8c33d9b4-30bf-4e2e-a847-84926136a4d6` |
| 140 | Tyagaraja | Venu Gaana Loluni | kn,ml,sa,ta,te | 5 | 3 | `fdbda5e6-0080-4b9d-84bf-6378a9cbd9bb` |
| 141 | Tyagaraja | Vinaayakuni | kn,ml,sa,ta,te | 5 | 5 | `254ba553-c1d3-4678-aa99-34f92acd44f9` |
| 142 | Tyagaraja | Vinataa Suta Raaraa | kn,ml,sa,ta,te | 5 | 5 | `a4e7ca76-bbc4-4bc5-bbb4-bc83bc17e805` |
| 143 | Tyagaraja | Vinataa Suta Vaahanudai | kn,ml,sa,ta,te | 5 | 3 | `2837196d-84e8-4772-b5e1-db163e21a8af` |
| 144 | Tyagaraja | Yuktamu Kaadu | kn,ml,sa,te | 4 | 5 | `e2f166b0-c936-4b29-ab16-22fe89701283` |
| 145 | Tyagaraja | aDugu varamula | kn,ml,sa,ta,te | 5 | 5 | `0c4d94e7-3ccd-4546-a153-7e2138792820` |
| 146 | Tyagaraja | aTla palukuduvu | kn,ml,sa,te | 4 | 3 | `27f8e742-e72a-4f8c-afc6-52dd4378bd6a` |
| 147 | Tyagaraja | aTu kArAdani | kn,ml,sa,ta,te | 5 | 3 | `b4c7cd33-23fc-43f7-a875-b0f55546b9e2` |
| 148 | Tyagaraja | adi kAdu bhajana | kn,ml,sa,ta,te | 5 | 3 | `1b751137-9e78-4a63-9036-2f648bf2dcec` |
| 149 | Tyagaraja | amba ninu | kn,ml,sa,ta,te | 5 | 5 | `b3486e9f-fca1-4e77-bc15-11bddc79d381` |
| 150 | Tyagaraja | amma dharma saMvardhani | kn,ml,sa,te | 4 | 5 | `fa538997-21c3-431c-b60a-687571bd18c8` |
| 151 | Tyagaraja | amma rAvamma | kn,ml,sa,ta,te | 5 | 3 | `925bf438-13a3-40ff-9a74-f2ecd23fb0b7` |
| 152 | Tyagaraja | anduNDakanE | kn,ml,sa,ta,te | 5 | 5 | `c1361d3c-78dd-4361-a575-3d9e2336b64a` |
| 153 | Tyagaraja | anupama guNa | kn,ml,sa,ta,te | 5 | 6 | `8a4f25e2-7015-4458-a604-ff800a1a034d` |
| 154 | Tyagaraja | anurAgamu lEni | kn,ml,sa,ta,te | 5 | 3 | `83575f33-e1cc-41a5-bcc5-097d66d09b56` |
| 155 | Tyagaraja | aparAdhamulanOrva | kn,ml,sa,ta,te | 5 | 3 | `11b3833e-634a-4018-b159-485576ccc8c2` |
| 156 | Tyagaraja | appa rAma bhakti | kn,ml,sa,ta,te | 5 | 5 | `8359d42b-c00b-444c-9988-a124387f728f` |
| 157 | Tyagaraja | bAgAyenayya | kn,ml,sa,ta,te | 5 | 3 | `0745ba3f-6cd8-4c9a-b4b6-092fa6620960` |
| 158 | Tyagaraja | bRndAvana lOla | kn,ml,sa,ta,te | 5 | 3 | `2a88986a-0b3e-4cb8-8cad-5d144aa62f7f` |
| 159 | Tyagaraja | baNTu rIti | kn,ml,sa,ta,te | 5 | 3 | `fdb8a81c-1f1a-4963-ac13-28fa71d3f6a4` |
| 160 | Tyagaraja | balamu kulamu | kn,ml,sa,ta,te | 5 | 6 | `12ab2d34-a5d6-4ba6-9923-92cf53c8ce3a` |
| 161 | Tyagaraja | bhajana parulakEla | kn,ml,sa,ta,te | 5 | 3 | `09216653-9ebd-4df0-82db-c374e18068d7` |
| 162 | Tyagaraja | bhajana sEya rAdA | kn,ml,sa,ta,te | 5 | 5 | `7d7ffdc6-fe9f-4864-a630-a418679f4753` |
| 163 | Tyagaraja | bhajana sEyavE | kn,ml,sa,ta,te | 5 | 3 | `f60c8d2f-7ac2-4ee8-9512-dec61c9cd6e0` |
| 164 | Tyagaraja | bhajana sEyu mArgamu | kn,ml,sa,ta,te | 5 | 3 | `8f631948-3af9-471c-9ca7-499daad07c76` |
| 165 | Tyagaraja | bhajarE bhaja | kn,ml,sa,ta,te | 5 | 5 | `31e84a65-246d-4825-a1d3-8099f020d26f` |
| 166 | Tyagaraja | bhajarE raghuvIraM | kn,ml,sa,ta,te | 5 | 11 | `0a7dd82c-98da-4aa8-87b2-004d6d15cf4b` |
| 167 | Tyagaraja | bhakti biccam | kn,ml,sa,ta,te | 5 | 3 | `79c5ac50-423e-4f35-81e0-dd68e289c63f` |
| 168 | Tyagaraja | bhaktuni cAritramu | kn,ml,sa,te | 4 | 5 | `c91fb560-8678-44ac-b964-d8518a05cdf2` |
| 169 | Tyagaraja | bhava nuta | kn,ml,sa,ta,te | 5 | 5 | `dff636dc-d88e-48a3-a88a-f08b9dfc0154` |
| 170 | Tyagaraja | bhava sannuta | kn,ml,sa,ta,te | 5 | 6 | `2973dc12-38d4-432d-add6-68d573b4b9d0` |
| 171 | Tyagaraja | brOva bhAramA | kn,ml,sa,ta,te | 5 | 3 | `af5bf75e-750d-4f43-a8ef-38dee94c34f3` |
| 172 | Tyagaraja | buddhi rAdu | kn,ml,sa,ta,te | 5 | 5 | `a99ff3b7-2904-4145-9abf-da6ecfdac695` |
| 173 | Tyagaraja | cAla kalla | kn,ml,sa,ta,te | 5 | 3 | `aee9ddbb-ae17-4ffb-bf95-a9d11cec6b2f` |
| 174 | Tyagaraja | cAlu cAlu nI yuktulu | kn,ml,sa,ta,te | 5 | 5 | `b8f7ee40-0310-44f4-8c93-cf99bba5f80f` |
| 175 | Tyagaraja | cEsinadella | kn,ml,sa,ta,te | 5 | 5 | `08d27bad-2d7b-463f-a661-0c54c760ace2` |
| 176 | Tyagaraja | cUtAmu rArE | kn,ml,sa,ta,te | 5 | 5 | `2c1b9506-be59-4666-810b-172d3b3a3734` |
| 177 | Tyagaraja | callaga nAtO | kn,ml,sa,ta,te | 5 | 3 | `7c0a3afe-bdb1-4777-945a-31948041fabb` |
| 178 | Tyagaraja | callarE rAmacandrunipai | kn,ml,sa,ta,te | 5 | 7 | `3dfe8560-76b2-45bd-bbe3-d3f822c709e8` |
| 179 | Tyagaraja | ceDE buddhi | kn,ml,sa,ta,te | 5 | 3 | `5c0186a2-1cfe-43ae-ba94-559f0a876259` |
| 180 | Tyagaraja | celimini jalajAkshu | kn,ml,sa,ta,te | 5 | 7 | `aac223fb-6a9b-4f2f-81aa-3327b74937fa` |
| 181 | Tyagaraja | dASarathE dayA | kn,ml,sa,ta,te | 5 | 3 | `815e13a3-f865-467a-bd13-9c1dcaa85631` |
| 182 | Tyagaraja | dASarathI nI RNamu | kn,ml,sa,ta,te | 5 | 3 | `ffb45270-30b7-4431-b902-7aebca467ac0` |
| 183 | Tyagaraja | dAcukOvalenA | kn,ml,sa,ta,te | 5 | 5 | `bd738a97-9839-4e9f-acf6-237fb574d6c9` |
| 184 | Tyagaraja | dArini telusukoNTi | kn,ml,sa,te | 4 | 5 | `cedd6d75-244b-47c9-958e-726445428a99` |
| 185 | Tyagaraja | dEvAdi dEva | kn,ml,sa,ta,te | 5 | 3 | `b5ee5ff1-23e7-43fc-bce4-b6ca181ea8e6` |
| 186 | Tyagaraja | dEva rAma rAma | kn,ml,sa,ta,te | 5 | 3 | `3be08780-f3f2-4ac2-acde-818f8438ed3e` |
| 187 | Tyagaraja | dEvi SrI tuLasamma | kn,ml,sa,ta,te | 5 | 3 | `74f7c50c-ea71-429f-898e-66e289d514e6` |
| 188 | Tyagaraja | daNDamu | kn,ml,sa,ta,te | 5 | 3 | `0e4f787a-94a0-42e3-be60-da868e838de4` |
| 189 | Tyagaraja | daSaratha nandana | kn,ml,sa,ta,te | 5 | 8 | `1ccbd525-96aa-4e90-a999-ff7b2ec7ce7f` |
| 190 | Tyagaraja | darSanamu sEya | kn,ml,sa,ta,te | 5 | 5 | `deab3382-1a62-4554-aade-c66780944c6f` |
| 191 | Tyagaraja | dari dApu lEka | kn,ml,sa,ta,te | 5 | 3 | `5f669a6d-ae60-4c45-81e7-e83291ba6861` |
| 192 | Tyagaraja | daya jUcuTakidi | kn,ml,sa,ta,te | 5 | 3 | `5547eb46-0408-4f54-8bb5-983120d4e09c` |
| 193 | Tyagaraja | daya rAni | kn,ml,sa,ta,te | 5 | 12 | `73f878bd-d72c-4c56-9c1b-c6000006e4eb` |
| 194 | Tyagaraja | daya sEyavayya | kn,ml,sa,ta,te | 5 | 7 | `d8e009c1-0e0e-48e2-b48f-72bfdf483d32` |
| 195 | Tyagaraja | dharanu nI sari | kn,ml,sa,ta,te | 5 | 7 | `841e36dd-f1ee-462a-a677-f485cc14c75c` |
| 196 | Tyagaraja | dhyAnamE ganga | kn,ml,sa,ta,te | 5 | 3 | `74d4a92d-a364-4bad-b814-8ae50fd2deb8` |
| 197 | Tyagaraja | dinamE sudinamu | kn,ml,sa,ta,te | 5 | 3 | `477ec205-abda-480e-9101-df377a6f8e1f` |
| 198 | Tyagaraja | dorakunAyani | kn,ml,sa,ta,te | 5 | 5 | `b179b900-1473-432a-b948-cafce8f98428` |
| 199 | Tyagaraja | duDuku gala | kn,ml,sa,ta,te | 5 | 2 | `b4029df0-a209-470b-ba56-3d64fa8aebc4` |
| 200 | Tyagaraja | durmArga cara | kn,ml,sa,ta,te | 5 | 3 | `a85a5a00-0b27-4b07-bbda-3e1f176ea81c` |
| 201 | Tyagaraja | eTlA dorigitivO | kn,ml,sa,ta,te | 5 | 3 | `40781929-d277-40e1-86aa-13d7aca393cf` |
| 202 | Tyagaraja | eTla kanugondunO | kn,ml,sa,ta,te | 5 | 6 | `5aed6f36-6823-4538-85aa-9f694ef3160f` |
| 203 | Tyagaraja | eTula brOtuvO | kn,ml,sa,ta,te | 5 | 3 | `0c6cadaa-bab0-4224-af70-571fdaab646a` |
| 204 | Tyagaraja | eTula kApADuduvO | kn,ml,sa,ta,te | 5 | 5 | `d537afd4-27de-4888-8805-27ca35786ff2` |
| 205 | Tyagaraja | eTulaina bhakti | kn,ml,sa,ta,te | 5 | 5 | `f960fdb0-3d6a-4a26-8d93-8af37750f722` |
| 206 | Tyagaraja | eduTa nilicitE | kn,ml,sa,ta,te | 5 | 5 | `b0b1dfb2-b596-4645-b9aa-47082992be82` |
| 207 | Tyagaraja | endu bAyarA daya | kn,ml,sa,ta,te | 5 | 5 | `b6001002-db9b-4741-8240-88e88be6297d` |
| 208 | Tyagaraja | endu dAginADO | kn,ml,sa,ta,te | 5 | 5 | `e54a2564-61d0-4c0e-96e2-ecd02b517e1d` |
| 209 | Tyagaraja | endukI calamu | kn,ml,sa,te | 4 | 5 | `65f4c7f0-6a74-4f20-9046-3bdd2c26351b` |
| 210 | Tyagaraja | endukO bAga | kn,ml,sa,te | 4 | 5 | `398b15ae-5419-49a9-9eda-4f565adb782d` |
| 211 | Tyagaraja | endukO nI manasu | kn,ml,sa,te | 4 | 5 | `8ac85b44-5e45-447e-ae8d-6c45bd46d24a` |
| 212 | Tyagaraja | enduku daya rAdu | kn,ml,sa,ta,te | 5 | 5 | `c857fcff-1b60-4cdf-8132-179ad49d4959` |
| 213 | Tyagaraja | enduku peddala | kn,ml,sa,ta,te | 5 | 3 | `0d1fe89d-3fdf-408e-aa22-3e667614637e` |
| 214 | Tyagaraja | ennALLUrakE | kn,ml,sa,ta,te | 5 | 3 | `d40b776c-a855-4fa0-99f1-3a664a016c56` |
| 215 | Tyagaraja | ennaDO rakshincitE | kn,ml,sa,te | 4 | 5 | `86b2b208-507e-41a5-bb2e-00cf6363c1f8` |
| 216 | Tyagaraja | ennaDu jUtunO | kn,ml,sa,te | 4 | 3 | `0f39f77c-d5b8-467a-915a-3df2e4e5fadf` |
| 217 | Tyagaraja | ennaga manasuku | kn,ml,sa,ta,te | 5 | 4 | `0b84442c-d3eb-4765-be36-d9b98ecf8a96` |
| 218 | Tyagaraja | enta muddO | kn,ml,sa,ta,te | 5 | 3 | `e561edc8-f58f-4a1e-acec-968495fec3db` |
| 219 | Tyagaraja | enta pApinaiti | kn,ml,sa,te | 4 | 6 | `79f487a2-9d16-4ba8-8a5d-1a1e3ae3b2b7` |
| 220 | Tyagaraja | enta vEDukondu | kn,ml,sa,te | 4 | 3 | `09266c45-7c28-4dba-921e-42d95393d6e8` |
| 221 | Tyagaraja | entanucu sairintunu | kn,ml,sa,te | 4 | 5 | `a78ecf02-3598-4160-b868-5130b5d56bf0` |
| 222 | Tyagaraja | entanucu varNintunE | kn,ml,sa,ta,te | 5 | 5 | `04d55f5a-b278-469c-998e-d2dfc02f9939` |
| 223 | Tyagaraja | evaritO nE telpudu | kn,ml,sa,ta,te | 5 | 3 | `9e417346-a8b0-4332-8b03-0593967138df` |
| 224 | Tyagaraja | evaru manaku | kn,ml,sa,ta,te | 5 | 3 | `cb1fb445-00e6-4495-805d-291236649acd` |
| 225 | Tyagaraja | evaru teliya poyyEru | kn,ml,sa,te | 4 | 5 | `61d6612e-3f50-4111-b905-747663b7486c` |
| 226 | Tyagaraja | evarurA ninu vinA | kn,ml,sa,ta,te | 5 | 6 | `37249a7b-de16-40f8-a51e-6834bc6e3c62` |
| 227 | Tyagaraja | evvarE rAmayya | kn,ml,sa,te | 4 | 3 | `62bad2d2-cf4b-4694-b2dc-5b9a1c8e7aee` |
| 228 | Tyagaraja | gAna mUrtE | kn,ml,sa,ta,te | 5 | 3 | `90ee1155-699d-45eb-a708-86752114e23f` |
| 229 | Tyagaraja | gAravimpa rAdA | kn,ml,sa,ta,te | 5 | 5 | `dbe88431-22bd-44ec-8cd8-f1282b64e468` |
| 230 | Tyagaraja | gItArthamu | kn,ml,sa,ta,te | 5 | 3 | `e8de604f-1086-4bb5-bb70-cc9b7b21828e` |
| 231 | Tyagaraja | gaTTigAnu | kn,ml,sa,ta,te | 5 | 5 | `bad7d739-4c2d-443d-a38e-02efe78777d7` |
| 232 | Tyagaraja | gandhamu puyyarugA | kn,ml,sa,ta,te | 5 | 6 | `f5b3f945-9362-4b7b-945e-8d920e6b66c9` |
| 233 | Tyagaraja | gata mOhASrita | kn,ml,sa,ta,te | 5 | 8 | `61205a3a-0ef2-43be-8008-970f7c64267b` |
| 234 | Tyagaraja | gati nIvani | kn,ml,sa,te | 4 | 5 | `271a1479-c84d-4c22-af76-040f659fce2b` |
| 235 | Tyagaraja | ghuma ghuma | kn,ml,sa,ta,te | 5 | 5 | `96e1ae65-0fd5-4ea8-a604-1b3718daf949` |
| 236 | Tyagaraja | giri rAja sutA | kn,ml,sa,ta,te | 5 | 3 | `1f4904e8-42a2-43df-958d-a2c2ebdfb9ca` |
| 237 | Tyagaraja | graha balamEmi | kn,ml,sa,ta,te | 5 | 3 | `fd23c17f-b737-4b36-ac87-37c55157c9e5` |
| 238 | Tyagaraja | hari dAsulu | kn,ml,sa,ta,te | 5 | 7 | `733e0e9d-e729-4df1-8097-b9c27968cfb8` |
| 239 | Tyagaraja | hari hari nIyokka | kn,ml,sa,te | 4 | 4 | `005bc2ed-d9c5-4133-9071-df929a5e25f9` |
| 240 | Tyagaraja | hariyanuvAri | kn,ml,sa,ta,te | 5 | 5 | `e7b6ac75-839f-4122-82a2-dfc748eabab6` |
| 241 | Tyagaraja | heccarikagA | kn,ml,sa,ta,te | 5 | 5 | `9e06c13c-2bbb-47a7-b580-1de7365754cc` |
| 242 | Tyagaraja | idE bhAgyamu | kn,ml,sa,te | 4 | 5 | `ffd84ae2-f4cd-4b4e-b36b-6e6769072734` |
| 243 | Tyagaraja | idi nIku mEra | kn,ml,sa,ta,te | 5 | 5 | `2ae151d0-4293-45ca-ac0b-ed97cf7bfe3d` |
| 244 | Tyagaraja | idi samayamurA | kn,ml,sa,ta,te | 5 | 3 | `156cb2fc-cea9-41eb-b3a9-79378063f6f0` |
| 245 | Tyagaraja | ika kAvalasinadEmi | kn,ml,sa,ta,te | 5 | 5 | `75093099-55f6-4e74-885b-25a5e961f4ae` |
| 246 | Tyagaraja | ilalO praNatArti | kn,ml,sa,ta,te | 5 | 3 | `f37cec70-10b7-493a-8068-a305c502528e` |
| 247 | Tyagaraja | indukAyI tanuvunu | kn,ml,sa,te | 4 | 5 | `d1fd3851-f596-40f2-a069-5f27e814ca08` |
| 248 | Tyagaraja | indukEmi | kn,ml,sa,ta,te | 5 | 10 | `889b13ef-8794-4e41-978d-3fc3ce84d8f4` |
| 249 | Tyagaraja | inka yOcanaitE | kn,ml,sa,te | 4 | 5 | `11aca4c2-2d69-44b0-93c8-5000619555c2` |
| 250 | Tyagaraja | innALLu daya | kn,ml,sa,te | 4 | 5 | `7ae3db13-a2b8-4ab5-a944-30906c1b6fed` |
| 251 | Tyagaraja | innALLu nannEli | kn,ml,sa,te | 4 | 6 | `1585860b-29ed-4b34-904e-ea2c45dc5434` |
| 252 | Tyagaraja | inta bhAgyamani | kn,ml,sa,ta,te | 5 | 5 | `31927e17-df05-415c-b697-c8e09110d352` |
| 253 | Tyagaraja | inta tAmasamaitE | kn,ml,sa,ta,te | 5 | 5 | `53f0f17b-9706-4612-b67a-34a6ed48306d` |
| 254 | Tyagaraja | intakanna Ananda | kn,ml,sa,ta,te | 5 | 5 | `d508d190-8532-4ecc-8939-ee7684d093c1` |
| 255 | Tyagaraja | intakanna telpa | kn,ml,sa,ta,te | 5 | 7 | `0718d8a2-6b19-4a0f-a56b-967679017098` |
| 256 | Tyagaraja | intanucu varNimpa | kn,ml,sa,te | 4 | 5 | `299dd78b-38ae-4146-baaa-b76374d8e76e` |
| 257 | Tyagaraja | ipuDaina nanu | kn,ml,sa,ta,te | 5 | 5 | `8b489e84-f7a4-4453-a698-6e992d1c8ff1` |
| 258 | Tyagaraja | itara daivamulu | kn,ml,sa,te | 4 | 3 | `c8f46289-6807-4fbf-aed0-a5b82f3a2ff6` |
| 259 | Tyagaraja | jAnakI ramaNa | kn,ml,sa,te | 4 | 3 | `3f2a06d0-efc0-4f57-9791-24178b0859d5` |
| 260 | Tyagaraja | jAnaki nAyaka | kn,ml,sa,ta,te | 5 | 7 | `9c3072cf-e1d8-4206-ab48-978b32ad71af` |
| 261 | Tyagaraja | janakajA samEta | kn,ml,sa,ta,te | 5 | 5 | `89fb546a-46d3-4795-a0ca-0f366ac3f41c` |
| 262 | Tyagaraja | jaya jaya sItArAM | kn,ml,sa,ta,te | 5 | 8 | `fc683216-fb66-4f4d-b349-b1fad0d07359` |
| 263 | Tyagaraja | jnAnamosaga rAdA | kn,ml,sa,ta,te | 5 | 3 | `4954ca8e-a122-4d71-b862-213d2f21f552` |
| 264 | Tyagaraja | kAla haraNa | kn,ml,sa,ta,te | 5 | 5 | `d6cafbcb-889b-4c4b-a4a1-d7c32fe5344e` |
| 265 | Tyagaraja | kAru vElpulu | kn,ml,sa,ta,te | 5 | 5 | `1270d6f4-f7c3-4714-8dfe-19932d2a58d0` |
| 266 | Tyagaraja | kAsiccEdE | kn,ml,sa,ta,te | 5 | 3 | `a0737775-4a51-4d82-a5f2-fe87969ffed2` |
| 267 | Tyagaraja | kOTi nadulu | kn,ml,sa,ta,te | 5 | 3 | `c5c8fbe2-e19e-4005-b41e-685c35027234` |
| 268 | Tyagaraja | kOri vaccitinayya | kn,ml,sa,ta,te | 5 | 5 | `8b16692d-8110-406d-a3e6-110d9db253b8` |
| 269 | Tyagaraja | kRpa jUcuTaku | kn,ml,sa,ta,te | 5 | 3 | `a5078f61-fd1b-43ac-9404-4d3d3d9db598` |
| 270 | Tyagaraja | kRshNA mAkEmi | kn,ml,sa,ta,te | 5 | 8 | `bef7d3cf-7c08-4bae-97a2-06e923ca217f` |
| 271 | Tyagaraja | kaDa tEra rAdA | kn,ml,sa,ta,te | 5 | 3 | `ed95ad2a-f86e-45ff-ada3-bb472b2a418e` |
| 272 | Tyagaraja | kaTTu jEsinAvu | kn,ml,sa,ta,te | 5 | 3 | `3e4a4599-8d90-4857-8eb4-8cad30fd1a90` |
| 273 | Tyagaraja | kadaluvADu | kn,ml,sa,ta,te | 5 | 3 | `882c4c90-15d7-4b65-b52f-48515600bb63` |
| 274 | Tyagaraja | kaddanu vAriki | kn,ml,sa,ta,te | 5 | 3 | `b08bbe00-3159-4746-8d25-6204e3f47538` |
| 275 | Tyagaraja | kalaSa vArdhijAM | kn,ml,sa,ta,te | 5 | 3 | `be43d382-8fba-4cff-b60f-67bf7e36307e` |
| 276 | Tyagaraja | kalala nErcina | kn,ml,sa,ta,te | 5 | 3 | `c17494d8-1d3b-4da8-8de7-43c2c33c32b6` |
| 277 | Tyagaraja | kamala bhavuDu | kn,ml,sa,ta,te | 5 | 5 | `54d126e8-c68b-4853-be2e-81a7349d5e5e` |
| 278 | Tyagaraja | kanna talli | kn,ml,sa,te | 4 | 5 | `b025bb22-3195-4d29-afe8-fe51b82a04f0` |
| 279 | Tyagaraja | karmamE balavanta | kn,ml,sa,ta,te | 5 | 5 | `34379c8b-3252-4567-971b-4ed61653471d` |
| 280 | Tyagaraja | karuNA jaladhE | kn,ml,sa,ta,te | 5 | 10 | `89ed80d6-5286-4706-9542-b8109e31010f` |
| 281 | Tyagaraja | karuNA samudra | kn,ml,sa,ta,te | 5 | 3 | `0c91baf8-8070-4144-bb5b-f54c93974c07` |
| 282 | Tyagaraja | karuNa jUDavayya | kn,ml,sa,ta,te | 5 | 3 | `0931f98a-64fa-403f-967c-941677395832` |
| 283 | Tyagaraja | karuNayElAgaNTE | kn,ml,sa,ta,te | 5 | 7 | `8f109d67-f928-4258-bbe1-2793ede30bd3` |
| 284 | Tyagaraja | koluvaiyunnADE | kn,ml,sa,te | 4 | 5 | `e897a8c6-ef38-411f-8229-0b6f2ad13e3c` |
| 285 | Tyagaraja | koluvamarE kada | kn,ml,sa,ta,te | 5 | 5 | `5e240d77-d99b-46f1-8c96-6fb183bf9734` |
| 286 | Tyagaraja | koniyADE | kn,ml,sa,ta,te | 5 | 3 | `a123a98d-dce2-4e9e-b2d2-d7627d33b034` |
| 287 | Tyagaraja | kshIra sAgara Sayana | kn,ml,sa,ta,te | 5 | 3 | `f92e657b-fbd3-413b-84d6-333777eed436` |
| 288 | Tyagaraja | kuvalaya daLa | kn,ml,sa,ta,te | 5 | 9 | `d750cc52-5801-419e-b789-0c9c1ec346c4` |
| 289 | Tyagaraja | lAliyUgavE | kn,ml,sa,ta,te | 5 | 3 | `9b867d71-cc42-4dab-b712-ff8af6848c39` |
| 290 | Tyagaraja | lEkanA ninnu | kn,ml,sa,ta,te | 5 | 5 | `9b755232-dca7-4ddf-92f0-1b298ddb53a1` |
| 291 | Tyagaraja | lEmi telpa | kn,ml,sa,ta,te | 5 | 3 | `da7f37d0-ac3c-42f4-ba35-85bae4134678` |
| 292 | Tyagaraja | lIlagAnu jUcu | kn,ml,sa,ta,te | 5 | 3 | `16d66530-977b-4535-ac28-eb80c8b301a9` |
| 293 | Tyagaraja | lOkAvana catura | kn,ml,sa,ta,te | 5 | 5 | `641897b8-ca27-43e0-a4c2-970699e833e8` |
| 294 | Tyagaraja | lakshaNamulu | kn,ml,sa,ta,te | 5 | 3 | `85159a71-61ef-4634-a80a-f2366876bcc0` |
| 295 | Tyagaraja | mA kulamuna | kn,ml,sa,ta,te | 5 | 5 | `3411ea29-8485-47b2-9785-6171c6f8a396` |
| 296 | Tyagaraja | mATADavEmi | kn,ml,sa,ta,te | 5 | 3 | `563d4335-68f1-486b-8d9a-2bd4ad16f534` |
| 297 | Tyagaraja | mATi mATiki | kn,ml,sa,ta,te | 5 | 5 | `73116a78-3416-4583-b4ff-7c9223110062` |
| 298 | Tyagaraja | mAkElarA vicAramu | kn,ml,sa,ta,te | 5 | 3 | `27f23797-2404-42d6-ab94-ca817fd6127d` |
| 299 | Tyagaraja | mAmava raghurAma | kn,ml,sa,ta,te | 5 | 8 | `9190e3d4-c031-4481-9aba-97c6ac87460b` |
| 300 | Tyagaraja | mAmava satataM | kn,ml,sa,ta,te | 5 | 3 | `02eb0b19-f09f-4fb5-8877-c7366bf0a59c` |
| 301 | Tyagaraja | mAnamu lEdA | kn,ml,sa,te | 4 | 3 | `c1be6e4b-82aa-4447-beae-9355696067d7` |
| 302 | Tyagaraja | mAnasa sancararE | kn,ml,sa,te | 4 | 11 | `076dd213-8b5d-4f79-bf15-6ab19effa363` |
| 303 | Tyagaraja | mApAla velasi | kn,ml,sa,ta,te | 5 | 5 | `ac5c15a8-1f75-4d06-aca7-a2fbda94660a` |
| 304 | Tyagaraja | mAra vairi | kn,ml,sa,ta,te | 5 | 3 | `bd9294ff-1fee-469e-9671-96da3ef2b4ea` |
| 305 | Tyagaraja | mElukO dayA nidhi | kn,ml,sa,ta,te | 5 | 5 | `756f894a-790a-4e3b-9fe1-a01600723782` |
| 306 | Tyagaraja | mElukOvayya | kn,ml,sa,ta,te | 5 | 4 | `e144c7fb-3320-4168-9b65-bc120afa3a7d` |
| 307 | Tyagaraja | mEru samAna dhIra | kn,ml,sa,ta,te | 5 | 3 | `2d40a745-53c0-4325-a7ce-7882158aef5d` |
| 308 | Tyagaraja | mOhana rAma | kn,ml,sa,ta,te | 5 | 3 | `b9af5088-9906-47c4-bec2-4ea59e57f75b` |
| 309 | Tyagaraja | mOsa pOku | kn,ml,sa,ta,te | 5 | 5 | `27c515fc-4a3f-4928-87af-da95d769570d` |
| 310 | Tyagaraja | mahima taggincukO | kn,ml,sa,ta,te | 5 | 3 | `db5c9fda-a13f-450d-985e-5e335cd6280a` |
| 311 | Tyagaraja | manasA eTulOrtunE | kn,ml,sa,ta,te | 5 | 3 | `eb516533-8464-40c3-8c7c-86dcbce93129` |
| 312 | Tyagaraja | manasA mana | kn,ml,sa,ta,te | 5 | 3 | `a671b559-0861-4f9f-83aa-f0cfdf6be981` |
| 313 | Tyagaraja | manasu svAdhIna | kn,ml,sa,ta,te | 5 | 4 | `3d2fd5f4-a690-4ace-8ae7-df1fe2d23644` |
| 314 | Tyagaraja | manasu vishaya | kn,ml,sa,ta,te | 5 | 3 | `8c55c921-a589-49e0-bc1d-546105d51451` |
| 315 | Tyagaraja | maracE vADanA | kn,ml,sa,ta,te | 5 | 3 | `f8bbbeb4-ca30-4de9-a9f6-6be0d8b95753` |
| 316 | Tyagaraja | marakata maNi | kn,ml,sa,ta,te | 5 | 3 | `0d9a0e4a-7265-4683-bf40-ab2c1596cb88` |
| 317 | Tyagaraja | maravakarA | kn,ml,sa,ta,te | 5 | 7 | `081c6116-428c-4143-aae7-f1b0a562280d` |
| 318 | Tyagaraja | mariyAda kAdayya | kn,ml,sa,ta,te | 5 | 3 | `5d531213-817e-4cb7-8bed-0f6e5306f6a3` |
| 319 | Tyagaraja | mariyAda kAdurA | kn,ml,sa,ta,te | 5 | 3 | `49e73d43-23cb-427e-a526-21ae0a5d31b8` |
| 320 | Tyagaraja | muddu mOmu | kn,ml,sa,ta,te | 5 | 3 | `2bfd07c0-6310-4199-b91c-5c45088c3a35` |
| 321 | Tyagaraja | mummUrtulu | kn,ml,sa,ta,te | 5 | 3 | `1ebf6bb3-f892-417d-aecd-cf8011706d3f` |
| 322 | Tyagaraja | munnu rAvaNa | kn,ml,sa,te | 4 | 6 | `418cb879-644a-4d62-afec-05de7dcb8b78` |
| 323 | Tyagaraja | nA jIvAdhAra | kn,ml,sa,ta,te | 5 | 3 | `7ea89f3a-86c9-4e20-9a5a-98effa850ce6` |
| 324 | Tyagaraja | nA morAlakimpa | kn,ml,sa,ta,te | 5 | 5 | `aaef830f-30d3-43ea-b97d-b697088f7df8` |
| 325 | Tyagaraja | nA moralanu | kn,ml,sa,ta,te | 5 | 4 | `99896a62-e18f-4879-a71a-f051027af8e5` |
| 326 | Tyagaraja | nADADina mATa | kn,ml,sa,ta,te | 5 | 3 | `0040c0f8-7f32-44f7-85ef-0eb0ec2e05f1` |
| 327 | Tyagaraja | nAdOpAsana | kn,ml,sa,ta,te | 5 | 3 | `f0fd6665-d378-48fd-8772-6610a997c34d` |
| 328 | Tyagaraja | nAda sudhA rasam | kn,ml,sa,ta,te | 5 | 3 | `6c2fc35a-9ff3-4d04-9c51-fa4f03532744` |
| 329 | Tyagaraja | nApAli SrI rAma | kn,ml,sa,ta,te | 5 | 7 | `29542e1b-7e94-438c-ab5c-381853d8ee4e` |
| 330 | Tyagaraja | nArAyaNa hari | kn,te | 2 | 13 | `d821cbd3-b67d-4b70-ad29-31920f66eb13` |
| 331 | Tyagaraja | nArada gAna | kn,ml,sa,ta,te | 5 | 3 | `d02d50ef-a6be-45aa-9b91-f69a712a5d0c` |
| 332 | Tyagaraja | nAyeDa vancana | kn,ml,sa,ta,te | 5 | 3 | `1818bb71-e19c-499e-924a-0e6fbcc9d937` |
| 333 | Tyagaraja | nE mora peTTitE | kn,ml,sa,ta,te | 5 | 3 | `5b980151-152a-42e4-b1a7-acbe7344f9d8` |
| 334 | Tyagaraja | nE pogaDakuNTE | kn,ml,sa,ta,te | 5 | 3 | `34773412-c158-44eb-b0b6-2a8552562fc9` |
| 335 | Tyagaraja | nEramA rAma | kn,ml,sa,ta,te | 5 | 3 | `4eb527cc-5ea7-4ff5-a6f8-364b3e599613` |
| 336 | Tyagaraja | nI cittamu nA | kn,ml,sa,ta,te | 5 | 3 | `5088493f-452d-4768-b189-d8db42a92b40` |
| 337 | Tyagaraja | nI cittamu niScalamu | kn,ml,sa,ta,te | 5 | 3 | `fd8d6b24-7414-467c-9e7f-1db5b40f0081` |
| 338 | Tyagaraja | nI dAsAnudAsuDu | kn,ml,sa,ta,te | 5 | 4 | `83d3429e-a1d0-43b6-b307-020f21d64ac2` |
| 339 | Tyagaraja | nI daya rAdA | kn,ml,sa,ta,te | 5 | 5 | `28ad469d-6389-46d8-862a-d7fb5a79e3a6` |
| 340 | Tyagaraja | nI daya rAvale | kn,ml,sa,ta,te | 5 | 5 | `d77e7ccf-6bb0-4796-bf3a-96f5b615017b` |
| 341 | Tyagaraja | nI dayacE rAma | kn,ml,sa,ta,te | 5 | 3 | `f8fd0f11-c316-40c8-b9d1-25e334dbddd6` |
| 342 | Tyagaraja | nI nAma rUpamulaku | kn,ml,sa,ta,te | 5 | 7 | `14c51c11-fc2e-4bc4-bd10-519bcd48d00f` |
| 343 | Tyagaraja | nI pada pankaja | kn,ml,sa,ta,te | 5 | 5 | `18ace057-6496-48db-904d-e88db708a1c6` |
| 344 | Tyagaraja | nI sari sATi | kn,ml,sa,ta,te | 5 | 3 | `5ddc1beb-66f3-4585-a283-743ae3bbcf11` |
| 345 | Tyagaraja | nIdu caraNamulE | kn,ml,sa,ta,te | 5 | 3 | `ccae28a8-47dc-439c-bab1-f37be1d01255` |
| 346 | Tyagaraja | nIkE daya rAka | kn,ml,sa,ta,te | 5 | 5 | `d746595f-9362-42d2-983c-712345f880d4` |
| 347 | Tyagaraja | nIkevari bOdhana | kn,ml,sa,ta,te | 5 | 3 | `3c7ee141-416e-40b1-85d0-1c61bf5d6eca` |
| 348 | Tyagaraja | nIku tanaku | kn,ml,sa,ta,te | 5 | 5 | `beba8e1c-18ff-43bb-8848-7b2051304fb5` |
| 349 | Tyagaraja | nIvADa nE gAna | kn,ml,sa,ta,te | 5 | 3 | `2550fc36-84a5-4f93-8b30-ccf33224ff39` |
| 350 | Tyagaraja | nIvE kAni | kn,ml,sa,ta,te | 5 | 5 | `88b63b1b-d4af-4635-b839-293d69e0fb16` |
| 351 | Tyagaraja | nIvE kanneDa | kn,ml,sa,ta,te | 5 | 5 | `824e344a-8e70-4017-8857-68af76099b64` |
| 352 | Tyagaraja | nIvErA kula dhanamu | kn,ml,sa,ta,te | 5 | 5 | `5afcc2aa-5106-48e2-a718-f27160892df5` |
| 353 | Tyagaraja | nIvaNTi daivamu | kn,ml,sa,te | 4 | 5 | `463943a7-924f-40c4-8f71-335093644ce6` |
| 354 | Tyagaraja | nIvu brOva valenamma | kn,ml,sa,te | 4 | 5 | `bc6352e6-e448-44a0-ac41-68e185fd77d2` |
| 355 | Tyagaraja | nOrEmi SrI rAma | kn,ml,sa,ta,te | 5 | 3 | `ff680a45-cadf-4566-b7f7-a5c74e513eda` |
| 356 | Tyagaraja | namO namO rAghavAya | kn,ml,sa,ta,te | 5 | 9 | `3353f8ab-7b79-4f76-9d4b-988b3e04874b` |
| 357 | Tyagaraja | nammaka nE mOsa | kn,ml,sa,ta,te | 5 | 7 | `3e1da6ee-2454-45d0-bbe4-c095f7e5d6df` |
| 358 | Tyagaraja | nammi vaccina | kn,ml,sa,ta,te | 5 | 3 | `8300ca9e-eca0-4d3a-8a93-0c54c3715baf` |
| 359 | Tyagaraja | nannu brOvakanu | kn,ml,sa,ta,te | 5 | 7 | `b0ae2fec-57ab-4d7f-ae31-2ce53d529aab` |
| 360 | Tyagaraja | nannu kanna talli | kn,ml,sa,ta,te | 5 | 3 | `a9971f1e-8715-4ff9-8ad4-e62079e74abe` |
| 361 | Tyagaraja | nanu pAlimpa | kn,ml,sa,ta,te | 5 | 3 | `8788331c-a2c8-48a2-98d3-dac53ea56935` |
| 362 | Tyagaraja | nata jana | kn,ml,sa,ta,te | 5 | 3 | `bf294950-e310-47b0-a178-efe225225aac` |
| 363 | Tyagaraja | nenaruncinAnu | kn,ml,sa,ta,te | 5 | 3 | `506dc30f-1708-4b28-b2e9-f90218b9f038` |
| 364 | Tyagaraja | nidhi cAla sukhamA | kn,ml,sa,ta,te | 5 | 3 | `9f2497ab-aab5-4d9b-85cf-0d8dc5a0cbf7` |
| 365 | Tyagaraja | ninnADanEla | kn,ml,sa,ta,te | 5 | 5 | `7a6c4270-9f6a-44f7-b41c-40c47914af0e` |
| 366 | Tyagaraja | ninnE bhajana | kn,ml,sa,ta,te | 5 | 3 | `673807ed-af4c-4334-827d-da1083587b88` |
| 367 | Tyagaraja | ninnE nera | kn,ml,sa,ta,te | 5 | 5 | `280e5aa5-1617-439a-888b-7e5dd752d0bb` |
| 368 | Tyagaraja | ninnE nera namminAnu | kn,ml,sa,te | 4 | 6 | `9bab5692-0510-485b-a8b2-774563963747` |
| 369 | Tyagaraja | ninnanavalasina | kn,ml,sa,te | 4 | 7 | `21d2c1a9-f3f0-437c-82b6-d6a91fcd0543` |
| 370 | Tyagaraja | ninu bAsi | kn,ml,sa,ta,te | 5 | 3 | `1e8717c9-7316-4de5-9ed4-5a026a9722bc` |
| 371 | Tyagaraja | ninu vinA nA madi | kn,ml,sa,ta,te | 5 | 5 | `18202aae-9d3c-41e4-96fd-80914610cabf` |
| 372 | Tyagaraja | ninu vinA sukhamu | kn,ml,sa,ta,te | 5 | 5 | `f78b4391-b29d-4602-9733-a7fee6c96b29` |
| 373 | Tyagaraja | niravadhi sukhada | kn,ml,sa,ta,te | 5 | 3 | `5ef0c733-46d4-406f-9113-47a41c6a9fbf` |
| 374 | Tyagaraja | oka pAri jUDaga | kn,ml,sa,ta,te | 5 | 3 | `6db8faab-0307-4ec1-aeba-debdcdf3de00` |
| 375 | Tyagaraja | orulanADu | kn,ml,sa,ta,te | 5 | 5 | `647549cb-584d-4008-9c82-49b60268b9f4` |
| 376 | Tyagaraja | pAhi kalyANa sundara | kn,ml,sa,te | 4 | 13 | `f2651795-4c95-40e8-8734-418fe059b2e3` |
| 377 | Tyagaraja | pAhi mAM harE | kn,ml,sa,ta,te | 5 | 7 | `8e1c22da-611b-473d-90c8-daf52c3433e6` |
| 378 | Tyagaraja | pAhi pAhi dIna bandhO | kn,ml,sa,ta,te | 5 | 9 | `1c1e58ca-f6d3-4a4d-981f-f4025d8b5d7b` |
| 379 | Tyagaraja | pAhi paramAtma | kn,ml,sa,ta,te | 5 | 9 | `195994c2-534a-4950-be10-ce9ba7a7f0cf` |
| 380 | Tyagaraja | pAhi rAma dUta | kn,ml,sa,ta,te | 5 | 6 | `a018529d-c3a4-4b90-808b-b2168626750f` |
| 381 | Tyagaraja | pAhi ramA ramaNa | kn,ml,sa,ta,te | 5 | 9 | `3c94b2b6-27fa-430e-8c67-8211f0428a6d` |
| 382 | Tyagaraja | pAlaya SrI raghu vara | kn,ml,sa,ta,te | 5 | 8 | `6fc8ec78-f85f-4c50-b228-8ffc67e8f42a` |
| 383 | Tyagaraja | pAlintuvO | kn,ml,sa,ta,te | 5 | 3 | `125e543a-cb8c-462a-9b51-fe981e8e4337` |
| 384 | Tyagaraja | pUla pAnpu | kn,ml,sa,ta,te | 5 | 5 | `5e53b603-eb35-474f-8372-807702423ae7` |
| 385 | Tyagaraja | paluka kaNDa | kn,ml,sa,ta,te | 5 | 3 | `63a07e3d-51b8-43b4-bfbe-2a4b8f44b397` |
| 386 | Tyagaraja | palukavEmi nA daivamA | kn,ml,sa,ta,te | 5 | 3 | `38a9e30f-5da1-4149-bf2a-57e5908fab87` |
| 387 | Tyagaraja | palukavEmi patita pAvana | kn,ml,sa,ta,te | 5 | 11 | `34c3f5a8-8e1d-4e68-b308-1b9b21064d34` |
| 388 | Tyagaraja | parA Sakti manupa | kn,ml,sa,ta,te | 5 | 5 | `69dc224b-64d7-476b-99dd-a0f2ecb9b9b4` |
| 389 | Tyagaraja | parAku jEsina | kn,ml,sa,ta,te | 5 | 3 | `2a547e61-f7ef-490e-90c5-f1a5443bd52f` |
| 390 | Tyagaraja | parAmukhamEla | kn,ml,sa,ta,te | 5 | 3 | `d6da96c9-8628-4cd4-b079-3fa04883fcba` |
| 391 | Tyagaraja | para lOka bhayamu | kn,ml,sa,ta,te | 5 | 3 | `0779ebfd-09f1-41be-b241-a7d333dc6dd3` |
| 392 | Tyagaraja | para lOka sAdhanamE | kn,ml,sa,ta,te | 5 | 3 | `5a0f3caf-3e75-4957-924c-733a757bae98` |
| 393 | Tyagaraja | paramAtmuDu | kn,ml,sa,ta,te | 5 | 3 | `b0f93eb6-3016-47ee-ba01-de508d3281eb` |
| 394 | Tyagaraja | parama pAvana | kn,ml,sa,ta,te | 5 | 10 | `dbc42cee-6859-4196-90ae-16e05f9a06d1` |
| 395 | Tyagaraja | paripAlaya dASarathE | kn,ml,sa,ta,te | 5 | 8 | `23a58ec7-e5c1-46f5-a459-303300cf5002` |
| 396 | Tyagaraja | paripUrNa kAma | kn,ml,sa,ta,te | 5 | 3 | `4abda836-1e15-4b0b-a7a7-58035712c55a` |
| 397 | Tyagaraja | pariyAcakamA | kn,ml,sa,ta,te | 5 | 3 | `89410ceb-c9e9-466b-a539-58f01a3027c1` |
| 398 | Tyagaraja | parulanu vEDanu | kn,ml,sa,ta,te | 5 | 3 | `21f76636-f967-49e3-a299-ea95d6cb906a` |
| 399 | Tyagaraja | patiki hArati | kn,ml,sa,ta,te | 5 | 5 | `503b3c2f-cfc0-418c-a914-0f313291a9b4` |
| 400 | Tyagaraja | patiki mangaLa | kn,ml,sa,ta,te | 5 | 3 | `cfdee002-d055-47ad-a85d-741034fa023a` |
| 401 | Tyagaraja | perugu pAlu | kn,ml,sa,te | 4 | 8 | `a170a743-39a8-4546-ae2d-954265d6ee51` |
| 402 | Tyagaraja | prANa nAtha | kn,ml,sa,ta,te | 5 | 3 | `9e8781fa-61be-4dd7-a791-7d27c3ce6813` |
| 403 | Tyagaraja | prArabdham | kn,ml,sa,ta,te | 5 | 3 | `198cf9c3-a2f0-4345-b0a7-c0be4f2f8d99` |
| 404 | Tyagaraja | proddu poyyenu | kn,ml,sa,te | 4 | 5 | `83a16bba-77a5-44fc-8517-26df2dc77268` |
| 405 | Tyagaraja | rAju veDale | kn,ml,sa,ta,te | 5 | 3 | `c5aee074-7a6b-4fd3-83ea-19fd6d9083eb` |
| 406 | Tyagaraja | rAkA SaSi vadana | kn,ml,sa,ta,te | 5 | 5 | `9f397b10-c37b-4e44-84a3-878c82f70374` |
| 407 | Tyagaraja | rAmA ninu nammina | kn,ml,sa,te | 4 | 5 | `fe316a27-7978-4a95-a260-83977c69bf73` |
| 408 | Tyagaraja | rAmA rAma rAma rAmAyani | kn,ml,sa,te | 4 | 11 | `4cae5d72-c291-465d-b117-242dff25e96c` |
| 409 | Tyagaraja | rAmAbhirAma manasu | kn,ml,sa,ta,te | 5 | 3 | `ffb09968-4a54-4bd2-a32b-d326b72f0967` |
| 410 | Tyagaraja | rAma SrI rAma lAli | kn,ml,sa,ta,te | 5 | 4 | `400ea94f-513e-4b4c-9fba-263856a8c4d8` |
| 411 | Tyagaraja | rAma bANa | kn,ml,sa,ta,te | 5 | 3 | `3687463a-34b0-4b34-8837-777a357d7a3f` |
| 412 | Tyagaraja | rAma daivamA | kn,ml,sa,ta,te | 5 | 5 | `39793ab9-5a83-43b9-be64-4a009d09c037` |
| 413 | Tyagaraja | rAma nAmamu janma | kn,ml,sa,ta,te | 5 | 3 | `dbcefbe4-de22-4830-b9be-468a632dbc8f` |
| 414 | Tyagaraja | rAma nIpai tanaku | kn,ml,sa,ta,te | 5 | 5 | `831f63cc-5e89-4b18-947e-7f59e5310503` |
| 415 | Tyagaraja | rAma nIvAdukonduvO | kn,ml,sa,te | 4 | 5 | `9d1cf5c5-460a-418c-8327-15a4bca0ee32` |
| 416 | Tyagaraja | rAma nIvE kAni | kn,ml,sa,ta,te | 5 | 5 | `06a450bb-c569-4409-aa2a-973ea4575797` |
| 417 | Tyagaraja | rAma ninu vinA | kn,ml,sa,ta,te | 5 | 6 | `9d3cb035-32c2-4c5c-96fc-64cdd92320ea` |
| 418 | Tyagaraja | rAma rAma gOvinda | kn,ml,sa,ta,te | 5 | 9 | `01166b8b-a60b-4eac-9895-1d3feae3f94f` |
| 419 | Tyagaraja | rAma rAma kRshNa | kn,ml,sa,ta,te | 5 | 11 | `bd9c039b-d92d-4a05-8f0d-d9082a834b1e` |
| 420 | Tyagaraja | rAma rAma rAma mAM pAhi | kn,ml,sa,ta,te | 5 | 9 | `00d7ec02-dd0c-423b-8862-3fe5e24923e2` |
| 421 | Tyagaraja | rAma rAma rAma nApai | kn,ml,sa,ta,te | 5 | 9 | `cf46edfe-080f-4931-bf5d-7145e618a3bd` |
| 422 | Tyagaraja | rAma rAma rAmacandra | kn,ml,sa,ta,te | 5 | 6 | `a525487e-f906-4e61-a600-3a2694b96bbb` |
| 423 | Tyagaraja | rAmacandra nI daya | kn,ml,sa,ta,te | 5 | 5 | `bbe4c6a3-5a76-4902-abf9-32d547c04d49` |
| 424 | Tyagaraja | rArA mAyiNTidAka | kn,ml,sa,ta,te | 5 | 5 | `dffbb6d7-4eb4-4c89-8124-396b951cae93` |
| 425 | Tyagaraja | rArA nannElukOrA | kn,ml,sa,ta,te | 5 | 5 | `a4bfcc4e-ee7d-42a3-807c-cb77e40a36ac` |
| 426 | Tyagaraja | rArA raghuvIra | kn,ml,sa,ta,te | 5 | 8 | `2e22c27b-4b03-4431-970e-af5b963d4082` |
| 427 | Tyagaraja | rE mAnasa | kn,ml,sa,ta,te | 5 | 8 | `df23bae3-d13f-4b59-91dd-493c39c4eee2` |
| 428 | Tyagaraja | rUkalu padi vElu | kn,ml,sa,ta,te | 5 | 3 | `e8c46f2f-e996-4fa2-b978-c8e0d843da11` |
| 429 | Tyagaraja | raghu nAyaka | kn,ml,sa,ta,te | 5 | 3 | `561c1792-bd6a-4969-a503-8dd829f7de50` |
| 430 | Tyagaraja | raghu vara nannu | kn,ml,sa,ta,te | 5 | 6 | `63c9ca63-d545-476e-ad9a-a5ed471f1072` |
| 431 | Tyagaraja | ramA ramaNa bhAramA | kn,ml,sa,te | 4 | 5 | `64e3ccdf-ec93-422b-969c-6ccfd2f3e43c` |
| 432 | Tyagaraja | ramincuvArevarurA | kn,ml,sa,ta,te | 5 | 3 | `902b62c9-a743-45e9-b206-62e56a3624f5` |
| 433 | Tyagaraja | ranga nAyaka | kn,ml,sa,ta,te | 5 | 3 | `d2e28055-6ebd-4502-bc9c-e39fdabafd06` |
| 434 | Tyagaraja | sAgaruNDu | kn,ml,sa,ta,te | 5 | 5 | `f4f086b2-d63d-45c0-9f69-dd8b609e85bf` |
| 435 | Tyagaraja | sAkEta nikEtana | kn,ml,sa,ta,te | 5 | 3 | `2905868f-4b50-4303-b6f0-f44d24e6b3ba` |
| 436 | Tyagaraja | sAkshi lEdanucu | kn,ml,sa,ta,te | 5 | 3 | `9a54e49d-2092-41cd-b705-b3ffc23fb219` |
| 437 | Tyagaraja | sAmiki sari | kn,ml,sa,ta,te | 5 | 5 | `7bb627c0-4204-4c39-89db-b92a858b70f6` |
| 438 | Tyagaraja | sAramE kAni | kn,ml,sa,te | 4 | 5 | `6cb08ce2-279f-4088-84ef-6c7e84dbd30d` |
| 439 | Tyagaraja | sArasa nEtra | kn,ml,sa,te | 4 | 2 | `451244d9-a0ee-43b3-b6aa-7a06cee44ce6` |
| 440 | Tyagaraja | sAri veDalina | kn,ml,sa,ta,te | 5 | 5 | `7762af58-19eb-4bd6-a6ce-d17a54dd7a73` |
| 441 | Tyagaraja | sArvabhauma | kn,ml,sa,ta,te | 5 | 3 | `a70de925-9076-47ab-b473-0b8a5056ff40` |
| 442 | Tyagaraja | sItA kalyANa | kn,ml,sa,ta,te | 5 | 7 | `f5081739-3f8e-4427-90c7-becb3b1eeba9` |
| 443 | Tyagaraja | sItA manOhara | kn,ml,sa,ta,te | 5 | 5 | `9cc07b0e-d05c-4c48-809d-e29f56119d93` |
| 444 | Tyagaraja | sItA pati kAvavayya | kn,ml,sa,ta,te | 5 | 4 | `5e7ba410-e01f-400a-a009-4efd245e4f1d` |
| 445 | Tyagaraja | sItA vara | kn,ml,sa,ta,te | 5 | 3 | `38759c7d-178c-41ee-add6-6fb6bfa447ff` |
| 446 | Tyagaraja | sItamma mAyamma | kn,ml,sa,ta,te | 5 | 3 | `b9063ef4-7e13-4609-9085-ad2d2134f70b` |
| 447 | Tyagaraja | sadA madini | kn,ml,sa,ta,te | 5 | 3 | `4a2f41e9-41fa-44eb-9d64-f42375f541b8` |
| 448 | Tyagaraja | samayamu EmarakE | kn,ml,sa,ta,te | 5 | 3 | `1a9e1fa0-2319-4a2b-afa2-311d2f7324b2` |
| 449 | Tyagaraja | samayamu telisi | kn,ml,sa,ta,te | 5 | 5 | `51c1a4e1-6aec-4a72-8724-43bebcfff79a` |
| 450 | Tyagaraja | samsArulaitE | kn,ml,sa,ta,te | 5 | 5 | `ddc9018a-3774-4a41-b7c8-7e3b25ef9874` |
| 451 | Tyagaraja | sandEhamunu | kn,ml,sa,ta,te | 5 | 3 | `42c81e5a-4af5-4b5f-8538-81af000f2329` |
| 452 | Tyagaraja | sandEhamuyElarA | kn,ml,sa,ta,te | 5 | 3 | `b5bc235e-0c6c-4b5b-9453-9b63fadb9dc4` |
| 453 | Tyagaraja | sangIta jnAnamu | kn,ml,sa,ta,te | 5 | 3 | `9c764bd2-a66c-4de8-bb35-9a8f07a0797e` |
| 454 | Tyagaraja | sarasIruha nayanE | kn,ml,sa,ta,te | 5 | 3 | `0b5f8096-d9e5-4ee2-b849-4bed529ae9de` |
| 455 | Tyagaraja | sarasIruha nayana | kn,ml,sa,ta,te | 5 | 7 | `e52f31c0-cf9f-49f3-84c9-abdd055ba15f` |
| 456 | Tyagaraja | sari jEsi vEduka | kn,ml,sa,ta,te | 5 | 3 | `33822898-08a7-437e-8734-df26ba0622d4` |
| 457 | Tyagaraja | sarivArilOna | kn,ml,sa,ta,te | 5 | 3 | `415cdb7f-0aec-4d27-a71c-c29d047b4981` |
| 458 | Tyagaraja | satta lEni | kn,ml,sa,ta,te | 5 | 3 | `50e60677-4457-41f1-93c4-8511155e43b9` |
| 459 | Tyagaraja | smaraNE sukhamu | kn,ml,sa,ta,te | 5 | 3 | `f885a354-b961-400f-ac3f-df5fee309d0c` |
| 460 | Tyagaraja | sudhA mAdhurya | kn,ml,sa,ta,te | 5 | 3 | `9be09efe-40d0-4d73-bbbf-7dd0f12200c6` |
| 461 | Tyagaraja | suguNamulE | kn,ml,sa,ta,te | 5 | 3 | `2c08a5d7-d828-459d-adf8-ceac8baec957` |
| 462 | Tyagaraja | sundarESvaruni | kn,ml,sa,ta,te | 5 | 5 | `e0151a7a-e0d9-413d-b07a-4c1b2854971f` |
| 463 | Tyagaraja | sundara tara dEhaM | kn,ml,sa,ta,te | 5 | 5 | `9ca70f2a-dac2-4490-8918-bbe0633eef09` |
| 464 | Tyagaraja | sundari nI divya | kn,ml,sa,te | 4 | 5 | `2192caff-da11-4a23-81ad-3036d3cdd173` |
| 465 | Tyagaraja | sundari nannindarilO | kn,ml,sa,ta,te | 5 | 5 | `cd283f28-6a3e-4d45-9db7-b885bee8dc60` |
| 466 | Tyagaraja | sundari ninnu | kn,ml,sa,te | 4 | 5 | `9c89d2d5-c211-4790-8577-90bfccbf697e` |
| 467 | Tyagaraja | svara rAga sudhA | kn,ml,sa,ta,te | 5 | 6 | `73b44194-00c1-4753-8c8a-924d9fa3e18b` |
| 468 | Tyagaraja | tIrunA nA lOni | kn,ml,sa,ta,te | 5 | 5 | `e8ac1d32-c7c9-42e3-9157-e4d06c301a43` |
| 469 | Tyagaraja | talli taNDrulu | kn,ml,sa,ta,te | 5 | 3 | `24c49b68-15a6-47ed-abea-0e1bf6a2616e` |
| 470 | Tyagaraja | tana mIdanE | kn,ml,sa,ta,te | 5 | 3 | `3432b7e1-7cc2-4d0d-8afd-5afcc60a22af` |
| 471 | Tyagaraja | tanalOnE dhyAninci | kn,ml,sa,te | 4 | 11 | `aa8402c5-457f-4eff-ba6f-fe68c32dca54` |
| 472 | Tyagaraja | tanavAri tanamu | kn,ml,sa,ta,te | 5 | 5 | `48708093-afc0-4d70-8b10-7696f8175380` |
| 473 | Tyagaraja | tappi bratiki | kn,ml,sa,ta,te | 5 | 5 | `91aa1a5e-66c5-49ee-a5e8-8603809925d2` |
| 474 | Tyagaraja | tatvameruga | kn,ml,sa,ta,te | 5 | 3 | `05868726-8f74-4dbc-99c9-379a982df9f8` |
| 475 | Tyagaraja | tava dAsOhaM | kn,ml,sa,ta,te | 5 | 8 | `fbbf74f2-3429-4c8c-be94-8e088034968b` |
| 476 | Tyagaraja | telisi rAma | kn,ml,sa,ta,te | 5 | 5 | `e48f4721-e331-4e3d-84c0-3e1064bd1ce8` |
| 477 | Tyagaraja | teliya lEru rAma | kn,ml,sa,ta,te | 5 | 3 | `9bb5ac84-38de-4d8d-a413-3a86e64601bb` |
| 478 | Tyagaraja | tera tIyaga | kn,ml,sa,ta,te | 5 | 5 | `c296bbc5-8cdb-4769-84c4-5383948e7342` |
| 479 | Tyagaraja | toli janmamuna | kn,ml,sa,ta,te | 5 | 3 | `b1b80b45-404e-45dc-8994-7bc51d114780` |
| 480 | Tyagaraja | toli nEnu jEsina | kn,ml,sa,ta,te | 5 | 3 | `15f7defb-f208-4eea-b4ca-9cc671862c6a` |
| 481 | Tyagaraja | tuLasi daLamulacE | kn,ml,sa,ta,te | 5 | 3 | `efd71f41-5405-402d-af29-c8fc6ddb6848` |
| 482 | Tyagaraja | tuLasi jagajjanani | kn,ml,sa,ta,te | 5 | 3 | `e020bc69-086e-47b1-be97-d3d8294f0379` |
| 483 | Tyagaraja | unna tAvuna | kn,ml,sa,ta,te | 5 | 7 | `211ce953-0b8c-4d7a-a8d2-0415d5cfc72e` |
| 484 | Tyagaraja | uyyAlalUgavayya | kn,ml,sa,ta,te | 5 | 5 | `0df490a3-9675-4f43-b370-3c0b42ee933f` |
| 485 | Tyagaraja | vADErA daivamu | kn,ml,sa,ta,te | 5 | 5 | `744d9aaa-7563-4bf4-9b5b-a7926568288f` |
| 486 | Tyagaraja | vAcAmagOcaramE | kn,ml,sa,ta,te | 5 | 3 | `93a47f0f-00ee-4562-9abb-f3339a42ad8d` |
| 487 | Tyagaraja | vAridhi nIku | kn,ml,sa,ta,te | 5 | 5 | `c97eab13-9938-4cc5-9cf4-4a398501a33d` |
| 488 | Tyagaraja | vAsu dEva vara guNa | kn,ml,sa,ta,te | 5 | 5 | `3996431c-54d4-4877-825d-c5ffb23b6e20` |
| 489 | Tyagaraja | vAsudEvayani | kn,ml,sa,ta,te | 5 | 5 | `04d710ce-ff2a-4319-a057-2f45f5f3d2a4` |
| 490 | Tyagaraja | vEda vAkyamani | kn,ml,sa,te | 4 | 10 | `eef82f61-9e42-4514-8142-e77b2cd0b68e` |
| 491 | Tyagaraja | vErevvarE gati | kn,ml,sa,ta,te | 5 | 3 | `91bb583a-c037-44c6-b078-af6d214be16e` |
| 492 | Tyagaraja | vaccunu hari | kn,ml,sa,ta,te | 5 | 5 | `9b732200-36d9-4854-b058-5d28e2090335` |
| 493 | Tyagaraja | vaddanE vAru | kn,ml,sa,ta,te | 5 | 3 | `ef184876-c808-401a-bb3d-b8321d265334` |
| 494 | Tyagaraja | vaddayuNDEdE | kn,ml,sa,ta,te | 5 | 5 | `d0d6a110-b908-4dff-9e07-a0d521502f6a` |
| 495 | Tyagaraja | valla kAdanaka | kn,ml,sa,ta,te | 5 | 3 | `09fc370e-ec19-482a-9519-41b0d903125c` |
| 496 | Tyagaraja | varAlandukommani | kn,ml,sa,ta,te | 5 | 5 | `586cd3e5-ba41-4693-9495-379acce3fd91` |
| 497 | Tyagaraja | vara lIla gAna | kn,ml,sa,te | 4 | 8 | `50772910-25eb-4778-bb6e-e6ebdc2459fc` |
| 498 | Tyagaraja | vara nArada | kn,ml,sa,ta,te | 5 | 3 | `03341b3f-7e95-42ed-a669-1efd9cbd91d1` |
| 499 | Tyagaraja | varadA navanItASa | kn,ml,sa,ta,te | 5 | 3 | `8ea31119-0764-4ab4-8f99-92264ba34d3f` |
| 500 | Tyagaraja | varamaina nEtrOtsava | kn,ml,sa,ta,te | 5 | 4 | `90d7b1fa-3d56-460a-a9b1-7b54730cd98a` |
| 501 | Tyagaraja | veDalenu kOdaNDa pANi | kn,ml,sa,ta,te | 5 | 3 | `83bde171-ad79-460a-a9c7-131a44227f35` |
| 502 | Tyagaraja | viDa jAladurA | kn,ml,sa,ta,te | 5 | 3 | `b6a09287-09a1-4929-a8db-12f1638c2c89` |
| 503 | Tyagaraja | vidhi SakrAdulaku | kn,ml,sa,te | 4 | 5 | `5e8cdd8c-65bf-4708-9a76-bfa20383bf1c` |
| 504 | Tyagaraja | vidulaku mrokkeda | kn,ml,sa,te | 4 | 3 | `64520f4d-bdef-4f3c-9395-19be699c756c` |
| 505 | Tyagaraja | vina rAdA | kn,ml,sa,te | 4 | 4 | `f1097b68-6d86-4a3c-8f98-08707295554e` |
| 506 | Tyagaraja | vinanAsakoni | kn,ml,sa,ta,te | 5 | 3 | `4fa55259-3152-4f57-b528-52bcdf41b833` |
| 507 | Tyagaraja | vinavE O manasA | kn,ml,sa,ta,te | 5 | 3 | `78eda9c2-9ed0-403d-ab7e-953b713519d8` |
| 508 | Tyagaraja | virAja turaga | kn,ml,sa,ta,te | 5 | 3 | `28935d89-dbfb-4d3b-8ce5-094c94136ab5` |
| 509 | Tyagaraja | vishNu vAhanuDu | kn,ml,sa,ta,te | 5 | 5 | `04ab08e7-0967-4a18-a070-0778232fb3dc` |
