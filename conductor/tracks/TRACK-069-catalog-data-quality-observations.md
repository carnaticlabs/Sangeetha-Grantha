# TRACK-069: Catalog Data Quality — Browser Observations

**Date:** 2026-02-25
**Source books:** `database/for_import/mdeng.pdf` (IAST), `database/for_import/mdskt.pdf` (Devanagari)
**Environment:** STAGING (localhost:5001)

---

## 1. Krithis Listing Page (`/krithis`)

### Screenshot summary
- **50 results** shown (all composers, all in Draft status)
- Columns: Title · Composer · Raga · Language · Actions

### Raga Column — Critical Finding
**Every single row shows `-` (dash).** The Raga column is uniformly empty across all 50 visible entries.

However, when navigating to an individual krithi, the **Canonical Links section does have raga data**. This means the listing table is not sourcing the raga from the canonical link — it appears to be pulling from a separate (unpopulated) `raga` field on the krithi record itself. The canonical raga reference exists but isn't surfaced in the table.

### Language Column — Critical Finding
**Every single row shows `TE` (Telugu).** This is incorrect for Muthuswami Dikshitar's compositions, which are almost entirely in **Sanskrit**. This appears to be a blanket default or import error — all imported Dikshitar krithis have been tagged as Telugu.

---

## 2. Krithi Deep-Dive: *akhilāṇḍeśvari rakṣa māṃ*

**UUID:** `a425edb4-4f49-4458-a654-9d53119e4e77`
**Source:** Krithi #1 in both mdeng.pdf and mdskt.pdf

### 2.1 Metadata Tab

| Field | App Value | Source (PDF) | Match? |
|---|---|---|---|
| Name (Transliterated) | akhilāṇḍeśvari rakṣa māṃ | akhilāṇḍeśvari rakṣa māṃ | ✓ |
| Incipit | *(empty)* | — | — |
| Composer | Muthuswami Dikshitar | Muttusvāmi Dīkṣitar | ✓ |
| Raga (Canonical Link) | jujāvanti *(displayed with "Modi" badge)* | jujāvanti (mela 28) | ✓ raga correct |
| Tala (Canonical Link) | ādi | ādi | ✓ |
| Deity | *(not set)* | Akhilāṇḍeśvari | ✗ missing |
| Temple | *(not set)* | — | — |
| Primary Language | Telugu | Sanskrit | ✗ **WRONG** |
| Musical Form | KRITHI | — | ✓ |
| Status | Draft | — | — |
| Sahitya Summary | *(empty)* | — | — |

**Note on "Modi" badge:** The Raga field in Canonical Links displays a small "Modi" chip before "jujāvanti". This appears to be the raga's parent category or group label in the reference data system — worth investigating whether this is correct classification.

### 2.2 Structure Tab

The composition has **3 sections in the wrong order**:

| Position in App | Section Type | Correct Position |
|---|---|---|
| 1 | **Charanam** | 3 |
| 2 | **Pallavi** | 1 |
| 3 | **Anupallavi** | 2 |

**Expected order:** Pallavi → Anupallavi → Charanam

Additionally, the source PDF contains a **Madhyamakālasāhityam** subsection within the Charanam. This is not represented in the structure at all — no section type exists for it in the current 3-section structure.

### 2.3 Lyrics Tab

The "Lyrics" tab renders the same view as "Lyric Variants" (heading: "Lyric Variants").

**Variant 1 — ENGLISH / Latin script**

Section assignments follow the (incorrect) structure order:

| Section Label | Content in App |
|---|---|
| CHARANAM | *(empty — no text)* |
| PALLAVI | *(empty — no text)* |
| ANUPALLAVI | **All lyrics dumped here** (see below) |

Full text found under ANUPALLAVI (which should be empty):

```
akhilāṇḍeśvari rakṣa māṃ āgamasaṃpradāyanipuṇe śrī   ← this is the Pallavi
lambodara guruguha pūjite lambālakodbhāvite hasite     ← this is Charanam line 1
vāgdevatārādhite varade varaśailarājanute śārade        ← Charanam line 2
akhilāṇḍeśvari rakṣa māṃ                               ← Pallavi refrain
jambhārisambhāvite janārdananute jujāvanti rāganute     ← Madhyamakalasahityam line 1
jhallīmaddaḷajharjaravādyanādamudite jñānaprade         ← Madhyamakalasahityam line 2
sarvāśāparipūrakacakreśvaryai guptayoginyai             ← ⚠ NOT in source PDF
sarvārthasādhakacakreśvaryai kulottīrṇayoginyai         ← ⚠ NOT in source PDF
sarvarogaharacakreśvaryai rahasyayoginyai               ← ⚠ NOT in source PDF
sarvaraksākaracakreśvaryai nigarbhayoginyai             ← ⚠ NOT in source PDF
sarvasaubhāgyacakreśvaryai sampradāyayoginyai           ← ⚠ NOT in source PDF
sarvasiddhipradacakreśvaryai atirahasyayoginyai         ← ⚠ NOT in source PDF
naḷinyai parandhāmaprakīrtinyai                         ← ⚠ NOT in source PDF
sarvasamkṣobhaṇacakreśvaryai guptatarayoginyai         ← ⚠ NOT in source PDF
trailokyamohanacakreśvaryai prakatayoginyai             ← ⚠ NOT in source PDF
sarvānandamayacakreśvaryai parāparādirahasyayoginyai    ← ⚠ NOT in source PDF
śrīcidānandanātha guruguhā yai sādhuhirdayasadrśavaśinyai  ← ⚠ NOT in source PDF
```

**Variant 2 — SANSKRIT / Devanagari:** "No lyrics added yet" — completely empty.

### 2.4 Lyric Variants Tab

Two sub-tabs: **English (IAST)** and **Sanskrit (Devanagari)** — both READ-ONLY.

- English (IAST): Same raw text as above, no section headers, READ-ONLY
- Sanskrit (Devanagari): **Empty text area** — no Devanagari content at all

---

## 3. Side-by-Side Comparison: App vs. Source PDF

**Source:** `mdeng-akhila.pdf` / `mdskt-akhila.pdf` (Krithi #1)

| Element | Source PDF | Application | Status |
|---|---|---|---|
| Raga | jujāvanti (mela 28) | jujāvanti | ✓ Correct |
| Tala | ādi | ādi | ✓ Correct |
| Language | Sanskrit | Telugu | ✗ Wrong |
| Composer | Muttusvāmi Dīkṣitar | Muthuswami Dikshitar | ✓ (variant spelling) |
| **Pallavi** | akhilāṇḍeśvari rakṣa māṃ āgamasaṃpradāyanipuṇe śrī | In "Anupallavi" section | ✗ Wrong section |
| **Anupallavi** | nikhilalokanityātmike vimale / nirmale śyāmaḷe sakalakaḷe | **MISSING ENTIRELY** | ✗ Absent |
| **Caraṇam line 1** | lambodara guruguha pūjite lambālakodbhāvite hasite | In "Anupallavi" section | ✗ Wrong section |
| **Caraṇam line 2** | vāgdevatārādhite varade varaśailarājanute śārade | In "Anupallavi" section | ✗ Wrong section |
| **Madhyamakalasāhityam** | jambhārisambhāvite janārdananute jujāvanti rāganute / jhallīmaddaḷajharjharavādyanādamudite jñānaprade | Present but in "Anupallavi" | ~ Text ok, section wrong |
| **Footnote variant** | varaśailarājasute | Not found | ✗ Missing |
| **Extra lines not in PDF** | — | 11 "sarvāśā..." / "cakra" lines present | ✗ Extraneous data |
| **Devanagari lyrics** | Full text in mdskt.pdf | Empty | ✗ Missing |
| **Structure order** | Pallavi → Anupallavi → Charanam | Charanam → Pallavi → Anupallavi | ✗ Inverted |
| **Madhyamakalasāhityam** as distinct section | Present as subsection of Caranam | Not modelled | ✗ Missing |

---

## 4. Krithi: *Endaro Mahanubhavulu* (Tyagaraja)

**UUID:** `6f3e6b37-d739-44db-a488-1b65ac3eab55`

| Field | Value |
|---|---|
| Composer | Tyagaraja |
| Raga (Canonical) | Sri |
| Tala | Adi *(note: capitalised, cf. "ādi" for Dikshitar — inconsistent tala naming)* |
| Language | Telugu ✓ *(correct for Tyagaraja)* |
| Status | Draft |

**Structure tab:** "No sections defined" — completely empty.
**Lyrics tab:** "No lyric variants added yet" — completely empty.

This krithi has canonical metadata only; no structure or lyrics have been imported/entered.

---

## 5. Krithi: *akhilāṇdeśvaryai namaste* (Dikshitar)

**UUID:** `91a09852-9bbc-403e-9f78-716ac469496e`

| Field | Value | Notes |
|---|---|---|
| Composer | Muthuswami Dikshitar | ✓ |
| Raga (Canonical) | āarabhi *(shown with "Modi" badge)* | Spelling: "āarabhi" has double-a; standard spelling is "arabhi" or "ārabhi" |
| Tala | Adi | Capitalised, inconsistent with ādi elsewhere |
| Language | Telugu | ✗ Should be Sanskrit for Dikshitar |
| Status | Draft | |

### Structure Tab
2 sections, **wrong order and incomplete**:

| Position | Section Type | Issue |
|---|---|---|
| 1 | Anupallavi | Should be position 2 |
| 2 | Pallavi | Should be position 1 |
| — | *(Charanam missing)* | ✗ No Charanam section |

### Lyrics Tab

**Variant 1 — ENGLISH / Latin:**
- Anupallavi section has text: "nikhilāgamasannutavaradāyai nirvikārāyai nityamuktāyai samī sārabhī tibhañjanāyai śaraccandrikāsītaḷāyai sāgaramekhalāyai tripurāyai..."
- ⚠️ **Contains an embedded base64-encoded JPEG image** in the middle of the lyric content — a data corruption artifact from the import process

**Variant 2 — SANSKRIT / Devanagari:**
- Anupallavi: `सुखतरो अखण्डैकरसपूर्णो अखिलाण्डेश्वरिसहितानन्दयुतो सद्गुरुगुहप्रबोधिनो`
- Pallavi: `३ अखिलाण्डेश्वरो रक्षतु`
  - ⚠️ The numeral `३` (Devanagari 3) is prepended to the Pallavi — this is the **page/entry number from the source PDF that was incorrectly captured as lyric content**

---

## 6. Cross-Cutting Issues Summary

| # | Issue | Severity | Affected Krithis |
|---|---|---|---|
| 1 | Raga column in listing table shows `-` for all entries | High | All 50+ |
| 2 | Primary Language set to Telugu for all Dikshitar krithis (should be Sanskrit) | High | All Dikshitar |
| 3 | Section order incorrect: Charanam first instead of Pallavi | High | akhilāṇḍeśvari rakṣa māṃ |
| 4 | All lyrics assigned to Anupallavi section instead of correct sections | High | akhilāṇḍeśvari rakṣa māṃ |
| 5 | Actual Anupallavi text ("nikhilalokanityātmike...") completely missing | High | akhilāṇḍeśvari rakṣa māṃ |
| 6 | ~11 extraneous "sarvāśā.../cakra" lines not from source PDF present in lyrics | Medium | akhilāṇḍeśvari rakṣa māṃ |
| 7 | Devanagari lyric variant completely empty | Medium | akhilāṇḍeśvari rakṣa māṃ |
| 8 | Madhyamakālasāhityam not modelled as a structure section type | Medium | Dikshitar krithis generally |
| 9 | Footnote/variant readings from PDF not captured anywhere | Medium | All |
| 10 | No structure or lyrics at all for Endaro Mahanubhavulu | High | Endaro Mahanubhavulu |
| 11 | Embedded base64 JPEG image in lyric text content | High | akhilāṇdeśvaryai namaste |
| 12 | Devanagari entry number (३) captured as lyric text | Medium | akhilāṇdeśvaryai namaste |
| 13 | Raga name "āarabhi" has erroneous double-a prefix | Low | akhilāṇdeśvaryai namaste |
| 14 | Tala naming inconsistent: "ādi" vs "Adi" across krithis | Low | Multiple |
| 15 | Deity and Temple canonical links not populated | Low | All Dikshitar |
| 16 | Incipit field empty for all observed krithis | Low | All |
| 17 | "Modi" badge on raga field — meaning unclear (parent mela group?) | Needs clarification | Dikshitar krithis |
| 18 | All krithis in Draft status — none published | Info | All |

---

## 7. Root Cause Hypotheses

1. **Language bug:** The bulk import pipeline likely defaulted `primary_language` to `TE` (Telugu) for all krithis regardless of actual composition language.

2. **Structure ordering:** The import extracted sections but stored them with Charanam first (possibly because the PDF page was processed bottom-up or the extraction prompt returned sections out of order).

3. **Lyrics section assignment:** All lyric text was likely assigned to a single section during extraction rather than being distributed to the correct pallavi/anupallavi/charanam buckets. The section with position matching the index in the DB happened to be Anupallavi.

4. **Extraneous "cakra" lines:** These "sarvāśāpari..." lines appear to be from a **Lalita Sahasranama** excerpt or a different Akhilandesvari stotram. They may have been concatenated from adjacent text in the PDF source (page overflow / extraction boundary error).

5. **Base64 image in lyrics:** The source PDF contains a scan/image on the same page as the krithi. The PDF extraction pipeline captured the image bytes and inserted them into the text field rather than discarding non-text content.

6. **Devanagari entry number (३):** The mdskt.pdf has entry numbers in Devanagari numerals at the start of each krithi. The extraction did not strip these before inserting them into the lyrics field.

7. **Raga column empty in listing:** The `raga_id` foreign key on the main krithi record is likely NULL for all imported krithis — the raga is only linked via the Canonical Links relationship, which the listing query does not join on.

---

## 8. Observations on UI/App Quality

- **"Lyrics" tab label** actually shows the "Lyric Variants" view — the tab name is slightly misleading
- **"Modi" prefix** on raga in Canonical Links is rendered as a distinct chip/badge — purpose not explained in UI
- **Lyric Variants tab** offers two sub-tabs (IAST and Devanagari) with READ-ONLY display — no inline editing at this level; editing happens via "Edit Content" in the Lyrics tab
- **Generate Variants** AI button is present — presumably would generate the Devanagari variant from the IAST
- **Structure section type dropdown** includes a rich set of options: Pallavi, Anupallavi, Charanam, Samashti Charanam, Chittaswaram, Swara Sahitya, **Madhyama Kala** ✓, Solkattu Swara, Anubandha, Muktayi Swara, Ettugada Swara, Ettugada Sahitya, Viloma Chittaswaram, Other — the vocabulary is comprehensive but the import didn't use it correctly
- **All 50 visible krithis are in Draft** — the data is clearly in an early/raw import state

---

*Generated from browser exploration on 2026-02-25. PDF source: P. P. Narayanaswami, Muttusvāmi Dīkṣitar Kīrtana Samāhāram (484 kritis), 2007.*
