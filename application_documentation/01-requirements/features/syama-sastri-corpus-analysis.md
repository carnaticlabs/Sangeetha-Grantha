| Metadata | Value |
|:---|:---|
| **Status** | Implemented — research & remediation baseline |
| **Version** | 1.1.0 |
| **Last Updated** | 2026-10-14 |
| **Author** | Sangita Grantha Team |

# Syama Sastri corpus: source inventory and implementation analysis

Related track: [TRACK-146](../../../conductor/tracks/TRACK-146-syama-sastri-musicological-classification.md).

## Verified inventory

Snapshot date: 2026-09-30. Input: [Syama Sastri CSV](../../../database/for_import/Syama-Sastri-Krithi-For-Import.csv).

SHA-256: `ce6d7019d3c7fb63a0edd6b6135281eaf4fbed226dbabaec13fc1bec586dd7b7`. Parsed with Python's CSV reader: **71 candidate rows, 70 distinct exact URLs, 35 distinct raw raga labels, zero exact duplicate title strings**. These are source candidates, not a database composition census.

Rows 1 and 22 have different title strings but the same Bhairavi label and exact source URL. Treat them as an alias/duplicate candidate pair. This makes a 70-composition interpretation plausible; it does not prove that the historical 70 imports cover every candidate or that no other aliases exist. Preserve both input rows during reconciliation.

The CSV contains title, raga label and source URL only. It cannot establish tala, musical form, shrine, devotional interpretation, group membership, eduppu, arudi or notation. Raw spellings such as `Anandabhairavi` and `Ananda bhairavi` need dictionary resolution; raw-label counts are not canonical-raga counts. Row 44's `rItigauLa - Abheri` must remain unresolved until witnesses establish whether these are alternate settings; it is not evidence for Ragamalika.

## Repository findings and proposed safeguards

- [Group seed](../../../database/migrations/R__seed_07_canonical_cycle_tags.sql) deletes every Ratnatrayam tag link and rebuilds from broad composer/title matches. Scope cleanup to owned heuristic provenance and resolve exact canonical composer/composition identities before applying reviewed links.
- [Form seed](../../../database/migrations/R__seed_08_update_musical_forms.sql) repeats the broad `sastri` predicate. Correct its repeatable behavior alongside group reconciliation; preserve unrelated reviewed assignments and log genuine changes only.
- [V66](../../../database/migrations/V66__dikshitar_musicological_enrichment.sql) provides cycle memberships but lacks explicit claim-level reviewer/source fields. JSON attributes can preserve evidence references, but must not be represented as a full adjudication schema without a design decision.
- [Source evidence](../../../database/migrations/V24__krithi_source_evidence.sql) records composition/source contributions, not independent reviewed claims. Reuse source identities and artifact hashes; define claims in the structured master before deciding whether a normalized DB extension is needed.
- [Versioned canon](../../04-database/versioned-canon.md) requires source/extraction/revision history for corpus corrections. This conflicts with treating arbitrary lyric/attribution repairs as Flyway data fixes. Proposed boundary: Flyway for schema/reference dictionary and reviewed group memberships; existing audited canon/import paths for composition-content corrections.
- TRACK-145 now reports completed formatter, macro indexing and retrieval work. Reuse those components; inspect their actual interfaces before extending. Macro storage is not proof of public search API exposure.

## Evidence access and outstanding work

Read-only PostgreSQL connector returned an error without diagnostic details. Docker inspection was denied access to the local Docker socket. No live database counts, identities, import statuses or coverage are claimed here. The blog fetch returned HTTP 429; the Music Academy catalogue fetch failed. No source passage or notation was reviewed through those requests. The linked research starting points remain candidates, not verified evidence.

Every inventory row below needs canonical identity reconciliation and source-backed review. The Ratnatrayam rows are prioritized from the accepted Intent; this queue does not assert reviewed musical forms or group membership. All other rows require attribution, form/structure, language, tala, addressee, devotional address, geography and group checks. Unknown values remain unknown; each row must eventually be reviewed or explicitly unresolved with a reason.

## Complete source-row analysis queue

Row references are 1-based CSV data rows (header excluded). They are snapshot locators, not permanent composition IDs. Source URLs are preserved verbatim.

| Row | Source title | Raw raga | Analysis priority / issue | Source |
|:---|:---|:---|:---|:---|
| 1 | (ambA) kAmAkshi anudinamu | bhairavi | Ratnatrayam pilot; alias candidate with row 22 | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-kamakshi-anudinamu.html) |
| 2 | A dinamuninci | Anandabhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-dinamuninchi-raga.html) |
| 3 | akhilANDESvarI | karnATaka kApi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-akhilandesvari-raga.html) |
| 4 | birAna varAlicci | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-birana-varalicchi.html) |
| 5 | bRhannAyakI | madhyamAvati | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-brhannayaki-raga.html) |
| 6 | brOva samayamidE | punnAga varALi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-brova-samayamide.html) |
| 7 | brOvavammA | nIlAmbari | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-brovavamma-raga.html) |
| 8 | brOvavammA tAmasamElE | mAnji | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-brovavamma.html) |
| 9 | daya jUDa | jaganmOhini | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-daya-juda-raga.html) |
| 10 | dayA nidhE | bEgaDa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-daya-nidhe-begada.html) |
| 11 | dEvI brOva | cintAmaNi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-devi-brova-raga.html) |
| 12 | dEvI mIna nEtrI | SankarAbharaNam | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-devi-meena-netri.html) |
| 13 | dEvI nannu | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-devi-nannu-raga.html) |
| 14 | dEvI nI pada | kAmbhOji | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-devi-ni-pada-raga.html) |
| 15 | durusugA | sAvEri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-durusuga-raga-saveri.html) |
| 16 | Emani migula | tODi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/03/syama-krishna-vaibhavam-syama-sastry.html) |
| 17 | ennEramum un nAma | pUrvi kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-enneramum-un-nama.html) |
| 18 | ennEramum un pAda | punnAga varALi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-enneramum-un-pada.html) |
| 19 | himAcala tanaya | Ananda bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-himachala-tanaya.html) |
| 20 | himAdri sutE | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-himadri-sute-raga.html) |
| 21 | janani nata jana | sAvEri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-janani-nata-jana.html) |
| 22 | kAmAkshi anudinamu (ambA) | bhairavi | Ratnatrayam pilot; alias candidate with row 1 | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-kamakshi-anudinamu.html) |
| 23 | kAmAkshi bangAru | varALi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-kamakshi-bangaru.html) |
| 24 | kAmAkshi karuNA | paraju | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-kamakshi-karuna-raga.html) |
| 25 | kAmAkshi lOka sAkshiNI | madhyamAvati | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-kamakshi-loka.html) |
| 26 | kAmAkshI nAtO | bEgaDa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-kamakshi-nato-rage.html) |
| 27 | kAmAkshi nI pada | yadukula kAmbhOji | Ratnatrayam pilot; reconcile full incipit | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-kamakshi-ni-pada.html) |
| 28 | kanaka Saila | punnAga varAli | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-kanaka-saila-raga.html) |
| 29 | karuNa jUDa | SrI rAgam | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-karuna-juda-sri.html) |
| 30 | karuNa jUDavammA | varALi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-karuna-judavamma.html) |
| 31 | karuNA nidhi ilalO | tODi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/03/syama-krishna-vaibhavam-syama-sastry_26.html) |
| 32 | mahilO ambA | Ananda bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-mahilo-amba-raga.html) |
| 33 | mari vErE gati | Ananda bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-mari-vere-gati-raga.html) |
| 34 | mAyammA nannu | nATa kuranji | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-mayamma-nannu-raga.html) |
| 35 | mAyammAyani | Ahiri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/03/syama-sastry-kriti-mayammayani-ahiri.html) |
| 36 | mIna lOcanA | dhanyAsi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-meena-lochana-raga.html) |
| 37 | nA manavini | saurAshTram | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-naa-manavini-raga.html) |
| 38 | nannu brOva rAdA | janaranjani | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-nannu-brova-rada.html) |
| 39 | nannu brOvu | lalita | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-nannu-brovu-raga.html) |
| 40 | nanu brOva rAdA | gauLipantu | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-nanu-brova-rada-raga.html) |
| 41 | nIlAyatAkshi | paraju | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-nilayatakshi-raga.html) |
| 42 | ninnE namminAnu | tODi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/03/syama-sastry-kriti-ninne-namminanu-todi.html) |
| 43 | ninnE nammiti | kEdAra gauLa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-ninne-nammiti-raga.html) |
| 44 | ninnu vinA mari | rItigauLa - Abheri | Alternate-setting ambiguity; do not infer Ragamalika | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-ninnu-vina-mari.html) |
| 45 | ninnu vinAga | pUrvi kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-ninnu-vinaga-raga.html) |
| 46 | nIvE gatiyani | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-nive-gatiyani-raga.html) |
| 47 | O jagadambA | Ananda bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-o-jagadamba-raga.html) |
| 48 | pAhi mAM | nATa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-paahi-mam-nata-raga.html) |
| 49 | pAhi SrI giri rAja | Ananda bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-pahi-sri-giri-raja.html) |
| 50 | pAlayASu mAM | Arabhi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-paalayaasu-mam-raga.html) |
| 51 | pAlimpavammA | mukhAri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-palimpavamma-raga.html) |
| 52 | pAlincu kAmAkshi | madhyamAvati | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-palinchu-kamakshi.html) |
| 53 | parAkEla | kEdAra gauLa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-paraakela-raga.html) |
| 54 | parAmukham | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-paraamukham-raga.html) |
| 55 | pArvati janani | bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-parvati-janani-raga.html) |
| 56 | pArvati ninu | kalgaDa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-paravti-ninu-raga.html) |
| 57 | pura hara jAyE | gauLipantu | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-pura-hara-jaaye-raga.html) |
| 58 | rAvE hima giri kumAri | tODi | Ratnatrayam pilot; preserve source swara–sahitya structure | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/03/syama-sastry-kriti-rave-hima-giri.html) |
| 59 | rAvE parvata rAja | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-raave-parvata-raja.html) |
| 60 | sAmi ninnE | bEgaDa | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-sami-ninne-raga.html) |
| 61 | Sankari SaMkuru | sAvEri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-sankari-samkuru-raga.html) |
| 62 | Sankari Sankari | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-sankari-sankari-raga.html) |
| 63 | santatam ennai | paraju | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-santatam-ennai.html) |
| 64 | sArasAkshi | sAvEri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-saarasaakshi-raga.html) |
| 65 | sariyevarammA | bhairavi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-sariyevaramma-raga_24.html) |
| 66 | sarOja daLa nEtri | SankarAbharaNam | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-saroja-dala-netri.html) |
| 67 | SrI kAmAkshI | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-sri-kamakshi-raga.html) |
| 68 | SrIpati Mukha | sAvEri | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-sripati-mukha-raga.html) |
| 69 | talli ninnu | kalyANi | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/07/syama-sastry-kriti-talli-ninnu-raga.html) |
| 70 | taruNamidammA | gauLipantu | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-tarunam-idamma-raga.html) |
| 71 | trilOka mAtA | paraju | Full claim and identity review pending | [Witness](http://syamakrishnavaibhavam.blogspot.com/2011/04/syama-sastry-kriti-tri-loka-mata-raga.html) |

## Raw-label distribution

These counts include the duplicate candidate and preserve input case/spelling.

| Raw raga label | Source rows |
|:---|---:|
| Ahiri | 1 |
| Ananda bhairavi | 5 |
| Anandabhairavi | 1 |
| Arabhi | 1 |
| SankarAbharaNam | 2 |
| SrI rAgam | 1 |
| bEgaDa | 3 |
| bhairavi | 4 |
| cintAmaNi | 1 |
| dhanyAsi | 1 |
| gauLipantu | 3 |
| jaganmOhini | 1 |
| janaranjani | 1 |
| kAmbhOji | 1 |
| kEdAra gauLa | 2 |
| kalgaDa | 1 |
| kalyANi | 9 |
| karnATaka kApi | 1 |
| lalita | 1 |
| mAnji | 1 |
| madhyamAvati | 3 |
| mukhAri | 1 |
| nATa | 1 |
| nATa kuranji | 1 |
| nIlAmbari | 1 |
| pUrvi kalyANi | 2 |
| paraju | 4 |
| punnAga varALi | 2 |
| punnAga varAli | 1 |
| rItigauLa - Abheri | 1 |
| sAvEri | 5 |
| saurAshTram | 1 |
| tODi | 4 |
| varALi | 2 |
| yadukula kAmbhOji | 1 |

## Live database reconciliation — 2026-10-01

Read-only connector access succeeded on retry. Snapshot timestamp: `2026-10-01T02:18:22.647518+00:00` (07:48:22 IST). Exact canonical composer: `c259e3ea-a85c-41b8-951a-19bb3d0d2392`, name `Syama Sastri`. The surname search returned only this record; absence of other Sastri composer records is not a test of protection against future imports.

**All 71 CSV rows match source-evidence URLs on 70 distinct database compositions.** No source row is unmatched, and no scoped database composition lies outside the CSV URL mapping. Rows 1 and 22 map to the same canonical UUID, `f5a415fd-b4ed-4394-ae0b-6f89cacb4fca`. Thus the current source/database reconciliation accounts for the 71/70 difference as two inventory labels sharing one stored composition; it does not indicate a missing composition. URL linkage establishes catalogue reconciliation, not independent scholarly approval of identity or attribution.

| Stored-field coverage | Compositions / 70 |
|:---|---:|
| Primary raga | 70 |
| Ordered raga junction present | 70 |
| Tala | 70 |
| Primary language | 70 |
| At least one lyric variant | 70 |
| At least one structural section | 70 |
| At least one source-evidence record | 70 |
| Deity foreign key | 0 |
| Temple foreign key | 0 |
| Notation variant | 0 |

Stored forms: 67 KRITHI, 3 SWARAJATHI. Every composition has six stored lyric variants; that is observed storage coverage, not proof of variant completeness or accuracy. Some records have a single structural section and need witness comparison. There are three existing Ratnatrayam `krithi_tags` links, all with source `canonical_cycle`; these are legacy heuristic assignments, not new reviewed memberships.

Import-status query over records with the source domain or a mapped scoped composition returned 69 approved rows and 2 mapped rows. Per-status distinct mapped IDs are 69 and 2 respectively; those figures must not be summed as a distinct-composition count because identities can recur across statuses and sources. The exact-domain query found one approved Bhairavi Kamakshi source record and no domain-sourced mapping to a different composer. Canonical source evidence additionally attaches a Tyagaraja blog witness to `karuNa jUDavammA` (`cf983d2a-154e-4903-9670-6ea1a1172149`); investigate whether this is an erroneous cross-composer merge or legitimate contribution. No automatic correction is justified by that URL alone.

### Pilot witness observations (not approved canonical changes)

- **Bhairavi Kamakshi:** the [blog witness](https://syamakrishnavaibhavam.blogspot.com/2011/05/syama-sastry-kriti-kamakshi-anudinamu.html), introductory paragraph and Pallavi, identifies a svara jati, states Capu, and explicitly includes Kanchi in the lyric. Eight numbered swara-sahitya units follow. DB contains SWARAJATHI, Capu and nine sections; this is count-level agreement, not validation of each section or notation alignment. Record Kanchi as a source-supported address candidate; exact shrine assignment awaits adjudication and edition comparison.
- **Yadukula Kambhoji Kamakshi:** the [blog witness](https://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-kamakshi-ni-pada.html), introduction and numbered sections, identifies a svara jati in Capu with eleven swara-sahitya units. DB has SWARAJATHI, Capu and twelve sections. The witness interprets the golden-mountain phrase as Meru; it does not establish Thanjavur Bangaru Kamakshi from that phrase. Geography remains unresolved pending broader evidence.
- **Todi Rave hima giri:** the [blog witness](https://syamakrishnavaibhavam.blogspot.com/2011/03/syama-sastry-kriti-rave-hima-giri.html), introduction, Pallavi and swara-sahitya 6, states Adi and explicitly names Kanchi Kamakshi. Six swara-sahitya units follow. DB has SWARAJATHI, Adi and seven sections. The introduction calls it a kriti; detailed form acceptance should rely on structure and edition comparison rather than this generic title wording.
- **Row 44 Ninnu vina mari:** the [blog witness](https://syamakrishnavaibhavam.blogspot.com/2011/06/syama-sastry-kriti-ninnu-vina-mari.html), Comments after References, reports Abheri in some books while presenting Ritigaula. This supports a source-reported nomenclature/setting discrepancy, not a Ragamalika sequence. Identify those books before deciding whether it is alternate setting, historical nomenclature or error.

These witnesses provide lyric/section and source-stated tala evidence. No inspected notation establishes eduppu, arudi, kalai or subdivision, and the database has no notation variants. No rhythm values or canonical shrine/group assignments were added. Remote pages were inspected through browser-readable text; raw artifact hashes were not captured, so these observations are locatable preliminary research rather than immutable source snapshots.

### Source-row to current canonical UUID concordance

Mappings below use exact source-evidence URL equality within the exact composer scope. They preserve both aliases and do not assert reviewed classification.

| CSV row | Source title | Current canonical UUID |
|:---|:---|:---|
| 1 | (ambA) kAmAkshi anudinamu | `f5a415fd-b4ed-4394-ae0b-6f89cacb4fca` |
| 2 | A dinamuninci | `b79a0ba6-693f-44eb-af57-f996f688214a` |
| 3 | akhilANDESvarI | `670f6134-6de3-4a01-8619-c8da56a47f3a` |
| 4 | birAna varAlicci | `3c33a293-cd5e-40f4-9a55-8409f88a7790` |
| 5 | bRhannAyakI | `076a2e58-ee7a-4eee-816a-6a233f89cab1` |
| 6 | brOva samayamidE | `196813dd-c01e-4258-98a2-b193aefe016e` |
| 7 | brOvavammA | `9e9ea14b-2e34-4fe8-a6cf-afcfb84dc2f3` |
| 8 | brOvavammA tAmasamElE | `1d25d7cd-1137-4463-b585-b5eb606fc573` |
| 9 | daya jUDa | `1e500a38-d798-45da-b1ca-bb35f4e23188` |
| 10 | dayA nidhE | `3c9f2aea-abcb-4b67-8fd6-a91ffe9b151a` |
| 11 | dEvI brOva | `74f46a92-2be6-4376-813b-e9b2f70113af` |
| 12 | dEvI mIna nEtrI | `60aa1036-9063-41b0-bdf6-dd049d2fc620` |
| 13 | dEvI nannu | `6ee2d1cb-2231-4dd9-a107-abda8b323e82` |
| 14 | dEvI nI pada | `d292a5d8-8b3b-480b-bd16-d514d970f56c` |
| 15 | durusugA | `c205a192-f29e-47de-b652-3fdc395f3bee` |
| 16 | Emani migula | `3463c9df-013d-4604-8235-b6765e64c267` |
| 17 | ennEramum un nAma | `da4c5a52-d827-47da-bc1e-7a42c33c4893` |
| 18 | ennEramum un pAda | `a2cd4e72-8fbc-4352-ba9f-ffcb306e2a69` |
| 19 | himAcala tanaya | `009e42fb-28d9-47af-a0f9-7ebda3f0985e` |
| 20 | himAdri sutE | `68d196a0-9c34-41db-8b15-e91800f2cd79` |
| 21 | janani nata jana | `12e1933d-4b44-4a1a-a733-5801978d7cf1` |
| 22 | kAmAkshi anudinamu (ambA) | `f5a415fd-b4ed-4394-ae0b-6f89cacb4fca` |
| 23 | kAmAkshi bangAru | `41149426-0c2f-43ef-9c83-7e839a7001ef` |
| 24 | kAmAkshi karuNA | `47778a06-c335-453f-aac9-adc936c0c26d` |
| 25 | kAmAkshi lOka sAkshiNI | `68fd82df-a2ae-4601-950b-f5c00bbaf4cd` |
| 26 | kAmAkshI nAtO | `0349441a-61cc-4c46-94b9-6cb56b3f3acd` |
| 27 | kAmAkshi nI pada | `7c15e1c9-5c30-4f68-8ab4-a414847e06be` |
| 28 | kanaka Saila | `0a249616-6df6-4a39-8581-2f1cc94fb707` |
| 29 | karuNa jUDa | `268280f4-431d-4439-ba5e-aaa39506d8d9` |
| 30 | karuNa jUDavammA | `cf983d2a-154e-4903-9670-6ea1a1172149` |
| 31 | karuNA nidhi ilalO | `22000049-4fee-45ee-979b-d44ba0090cf1` |
| 32 | mahilO ambA | `bf85c52d-b996-4d44-8382-a050cd452c5d` |
| 33 | mari vErE gati | `d14d643b-bfc5-4e39-b2c5-d4433e5be9bf` |
| 34 | mAyammA nannu | `a7f08d79-c261-48f9-a1dc-0b4b83085c90` |
| 35 | mAyammAyani | `0c251190-3cde-4d05-b584-19930220924c` |
| 36 | mIna lOcanA | `1c7e43c2-e2b7-4b22-bdf3-7bf3e590e4a9` |
| 37 | nA manavini | `23d89356-781c-4392-9341-b44b5b93027c` |
| 38 | nannu brOva rAdA | `023acc0c-047f-4288-9c2f-6d6e2380cb1a` |
| 39 | nannu brOvu | `63b6c905-8a50-433f-8310-20a60158fd9e` |
| 40 | nanu brOva rAdA | `739e4b50-f918-48df-b4f8-d1ac1f59c565` |
| 41 | nIlAyatAkshi | `573c5703-4b15-4d51-9be0-baebc82c3dcc` |
| 42 | ninnE namminAnu | `6a60e471-6471-4e6b-a5c0-7db15a139109` |
| 43 | ninnE nammiti | `6a46cdb8-b92e-4c64-b30a-0e73daebb045` |
| 44 | ninnu vinA mari | `69f8d57f-83c4-4e55-aa3a-a993cc0fae98` |
| 45 | ninnu vinAga | `66e23087-a16d-4a00-b27b-4320cb53c4b9` |
| 46 | nIvE gatiyani | `8eb018ff-052c-4a6f-8d79-feca4b3820f5` |
| 47 | O jagadambA | `688daebb-ee8b-4d91-bd14-d42b9827af0d` |
| 48 | pAhi mAM | `fa0be9c6-5177-4718-9016-de912c11c54f` |
| 49 | pAhi SrI giri rAja | `b3c61e5e-d1b4-442b-beee-e4be010b87bf` |
| 50 | pAlayASu mAM | `521af0a6-8117-46ab-a1c1-91248282106e` |
| 51 | pAlimpavammA | `9d727a40-2221-41f2-a470-99b94bef3aa3` |
| 52 | pAlincu kAmAkshi | `348376bb-6e07-4547-8fef-a1f781072498` |
| 53 | parAkEla | `4f7f103e-22d8-4d31-9b25-8da5ac36a83e` |
| 54 | parAmukham | `6b81b280-71fa-46cf-8dfd-261dca578466` |
| 55 | pArvati janani | `d6199fcc-bf2d-4535-b033-f1f1368e7a37` |
| 56 | pArvati ninu | `09ecef79-a7c2-4ced-a175-89c9add1d967` |
| 57 | pura hara jAyE | `6b888365-7a66-46f6-9164-42123e9115f1` |
| 58 | rAvE hima giri kumAri | `2974996a-7cee-433f-b523-dbecbf691f68` |
| 59 | rAvE parvata rAja | `6c0c797e-04e2-4645-bfac-374144dc39f3` |
| 60 | sAmi ninnE | `1f85b5bf-ff9e-4b16-a15c-5c85ea938e1d` |
| 61 | Sankari SaMkuru | `9fbf271d-70da-4391-bbbe-288ad73d52b1` |
| 62 | Sankari Sankari | `2746f6fd-456c-40dd-8db8-00651ea7c3fc` |
| 63 | santatam ennai | `7d87f95d-9afa-48f4-ab06-f4becc750342` |
| 64 | sArasAkshi | `ec19dd1e-c0b1-444b-8a49-d61dc3915dca` |
| 65 | sariyevarammA | `ed01af31-6aed-4f81-8d25-27acc81f7100` |
| 66 | sarOja daLa nEtri | `b1f06cf0-96ed-4b99-9f90-9de28a029b7d` |
| 67 | SrI kAmAkshI | `f70f0e00-f4a9-47a4-90a8-bd9c301928bc` |
| 68 | SrIpati Mukha | `beea22de-739d-4f20-84df-16e17f41df53` |
| 69 | talli ninnu | `648b68d8-298b-4ae9-b35b-616600d73e90` |
| 70 | taruNamidammA | `a6082c81-0e7e-4b7d-9e0f-568698b1f2cb` |
| 71 | trilOka mAtA | `4be09c30-9cad-4a43-b9bc-fcb894ea94a8` |

## Implementation research and validation — 2026-10-01

The [shared contract](./musicological-classification-contract.md) and [generated concordance](./syama-sastri-classification-master.md) now cover all 70 reconciled identities and 71 source rows. The structured master records 516 unresolved claim observations, 70 fetched/hashed blog witnesses, a hashed non-lyric database snapshot and one unavailable historical-source lead. All compositions have explicit findings and unresolved reasons; no claims have been silently marked human-approved.

Source introductions identify three varnams (`dayA nidhE`, `nA manavini`, `nIvE gatiyani`) and five gitams (`kAmAkshi karuNA`, `kAmAkshi lOka sAkshiNI`, `pArvati janani`, `santatam ennai`, `sArasAkshi`) currently stored as KRITHI. Each requires structural/edition adjudication. The gitams have one OTHER section and no Pallavi section in current storage; this is not proof that a generic KRITHI section model is appropriate. GITAM is absent from the supported form vocabulary; no unapproved enum expansion or automatic correction was made.

A [1954 Music Academy article](https://musicacademymadras.in/catalogue/files/souv/1954_28th%20conference%20souvenir.pdf), S. Vidya's Navaratnamalika discussion on printed p. 11, is available as a search-index passage. Full PDF retrieval failed (403 / oversized content). Six provisional title/raga mappings are retained as unresolved formal-group claims. The list also labels Nannu Brova as Vasantha and leaves two titles unspecified; do not equate those slots with this catalogue's Lalita composition or manufacture a nine-member set. No unavailable witness is eligible for approved application.

Database audit after safeguard application: 70 compositions, 420 lyric variants, 330 structural sections, zero zero-section lyric variants, zero missing source-evidence identities, zero missing raga junctions and zero krithi_cycle_memberships rows. Three legacy Ratnatrayam tag links remain. Global audit rows stayed at 12,001 before/after the two Flyway repeatable updates. Source-labelled forms remain current observations; application manifest has no approved changes.

Validation evidence: `make test` and `make test-integration` passed; the isolated Flyway regression executes changed repeatables twice and preserves independently reviewed links, other Sastri composers, stored forms and audit totals. Python validation/formatter/macro tests passed; Ruff and strict mypy passed. The deterministic generator check confirms committed-artifact content matches the master and verifies CSV plus local snapshot hashes. Backend `/health`, admin UI and three catalogue identity endpoints responded HTTP 200 after the required stack restart. Ranked retrieval was not executed: exact-UUID pilot probes remain explicitly approval-gated and no paid embedding calls were made.

## Remediation and multi-witness reconciliation — 2026-10-14

Following the 2026-10-06 validation review, the master (`database/data/track146-syama-sastri-master.json`), application manifest (`database/data/track146-syama-sastri-application-manifest.json`), repeatable membership seed (`database/migrations/R__seed_10_syama_sastri_reviewed_memberships.sql`), and retrieval evaluation tooling were reconciled:

1. **Witness hygiene (85 total witnesses):** 78 witnesses are `fetched` with verified SHA-256 hashes (70 primary blog witnesses, 7 Music Academy journal/souvenir PDFs `ma:jma-*` / `ma:souvenir-1983`, and V. Raghavan's 1977 monograph `sahapedia-raghavan-1977`), 1 is `database_snapshot` (`db-snapshot`), and 6 are `unavailable` (`academy-1954-lead`, `analysis:user-supplied-2026-10-06`, and the 4 unconsulted print/archive bibliographic entries `vidya-shankar-1980`, `govinda-rao-1997`, `sambamoorthy-1962`, `jma-archive`). The local stub `track146-institutional-witnesses.json` was removed.
2. **Claim adjudication and re-anchoring (638 total claims):**
   - **18 `reviewed` claims** backed exclusively by fetched, hashed witnesses: 3 `SWARAJATHI` form claims (`SS-0001:form:1`, `SS-0026:form:1`, `SS-0057:form:1`), 3 numbered swara-sahitya unit counts (`8`, `11`, and `6` units on `SS-0001`, `SS-0026`, `SS-0057`), and 7 `Syama Krishna` mudra claims (`SS-0001`, `SS-0026`, `SS-0032`, `SS-0034`, `SS-0046`, `SS-0051`, `SS-0057`) re-anchored to their fetched blog sahitya witnesses; 3 `syama-sastri-swarajathi-ratnatrayam` group claims anchored to `sahapedia-raghavan-1977` (PDF p.2); and 2 `Misra Chapu` laya claims (`SS-0001:laya:1`, `SS-0026:laya:1`) anchored to `ma:jma-vol75-2002` (PDF p.90).
   - **40 `unreviewed` claims** citing the unconsulted print editions (`vidya-shankar-1980`, `govinda-rao-1997`, `sambamoorthy-1962`), with unverified page locators replaced by explicit unconsulted-edition notes.
   - **574 `unresolved` claims** and **6 `disputed` claims** (evaluative `Apex Sovereign raga` labels on Anandabhairavi compositions).
   - **Lakshana corrections:** *Sari Evaramma* (`SS-0064:laya:1`) and benchmark probe `SS-146-10` corrected to `Khanda Jhampa (5+1+2 = 8 aksharas)`; *Devi Brova* (`SS-0011:laya:1`) and sibling `Anagata Eduppu` claims (`SS-0004:laya:1`, `SS-0022:laya:2`, `SS-0055:laya:1`) enriched with explicit `units` (`matra`), `counting_origin` (`samam`), and `convention`.
3. **Guarded repeatable membership seed (`R__seed_10`):** Seeds the 3 Swarajathi Ratnatrayam rows in `krithi_cycle_memberships` with approved-only `discriminative_attributes` (omitting `raga_character`, `bhava`, Todi `Adi (2 kalai)` laya, and generic `Swarasahitya` labels), upgrades legacy `canonical_cycle` links in `krithi_tags` to `reviewed_cycle`, aborts on conflict with independently reviewed memberships or tags, and logs actual mutations to `audit_log` with system attribution (`actor_user_id = NULL`, `metadata.actor = 'system:TRACK-146'`).
