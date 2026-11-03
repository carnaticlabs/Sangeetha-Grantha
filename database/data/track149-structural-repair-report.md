| Metadata | Value |
|:---|:---|
| **Status** | Report |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-03 |
| **Author** | Sangita Grantha Team |

# TRACK-149 structural repair report

Owner: [TRACK-149](../../conductor/tracks/TRACK-149-tyagaraja-edition-consultation-collision-split.md), requirement 7. The classification master listed 27 `missing_pallavi` and 4 `missing_charanam` identities (26 and 4 after the Sri Narada Muni reingest, 25 and 4 now).

**Finding.** These are not missing source text. The source pages carry the pallavi as an inline `P` label whose footnote digit sits before or after the label (`P 1cEra rAv(a)dEmirA`, `1 P paripAlaya`). Stored sections come from an older parser. The current parser finds the pallavi on all 25 cached pages, after one fix made in this track for the `1 P` form (`src/structure_parser.py`, test `tests/test_structure_parser_footnote_label.py`). The four missing-charanam records have the charanam merged into the anupallavi text (`3 caraNam` inside the anupallavi), and their pages are not cached.

**Repair path.** Nothing is inferred or invented. A record is repaired by re-extracting its source page with the current parser and reingesting it (`POST /v1/admin/imports/{id}/reingest`, audited as `REINGEST_MAPPED_KRITHI`). One record, *Sri Rama Jaya Rama*, already had the pallavi in its stored payload and was repaired that way. The other 29 need a controlled re-extraction run, which starts the extraction worker and fetches live pages. That run is not part of this change and needs the owner's go-ahead.

| Composition | Krithi | State | Stored | Gap | Current parser | Status |
|:---|:---|:---|:---|:---|:---|:---|
| Aanandamaananda | d9b7b93f | published | P0 A0 C9 | missing_pallavi | P1 A0 C9 | current parser finds the missing section; needs re-extract and reingest |
| Baale Baalendu | 4629d110 | published | P0 A1 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| Chera Raavademi | 3747ba8d | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| duDuku gala | b4029df0 | published | P1 A1 C0 | missing_charanam | no cached page | needs a live fetch, then re-extract and reingest |
| Dvaitamu Sukhamaa | 1cc1eb20 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Edi Nee Baahu Bala | 7c4ff154 | published | P0 A0 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| Ela Teliya Lero | 7a163c9f | published | P0 A1 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| Enaati Nomu | b2626e28 | published | P0 A0 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| Inta Saukhyamani | 6f1b9810 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Kalugunaa | fbbfd918 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| kanugoNTini | 07df20a5 | published | P1 A1 C0 | missing_charanam | no cached page | needs a live fetch, then re-extract and reingest |
| madilOna yOcana | e931e5ec | published | P1 A1 C0 | missing_charanam | no cached page | needs a live fetch, then re-extract and reingest |
| Mitri Bhaagyamae | f6acd658 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Mokshamu Galadaa | 24e54ad6 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| munupE teliyaka | 3f6e3166 | published | P1 A1 C0 | missing_charanam | no cached page | needs a live fetch, then re-extract and reingest |
| Naada Loludai | f5e86fd2 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Naati Maata | 8d1452e5 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Nijamaitae Mundara | 2d0ab787 | published | P0 A1 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| O Rama Rama Sarvonnata | 9ccd7f15 | published | P0 A0 C7 | missing_pallavi | P1 A0 C7 | current parser finds the missing section; needs re-extract and reingest |
| Paahi Mam Sri Ramachandra | e5f5a5ac | published | P0 A0 C9 | missing_pallavi | P1 A0 C9 | current parser finds the missing section; needs re-extract and reingest |
| Paripaalaya Paripaalaya | 947faba5 | published | P0 A0 C10 | missing_pallavi | P1 A0 C10 | current parser finds the missing section; needs re-extract and reingest |
| Peridi Ninu | 50942fd4 | published | P0 A1 C4 | missing_pallavi | P1 A1 C4 | current parser finds the missing section; needs re-extract and reingest |
| Raaraa Seetha Ramani | 0879def4 | published | P0 A1 C3 | missing_pallavi | P1 A1 C3 | current parser finds the missing section; needs re-extract and reingest |
| Raksha Pettarae | 6625f250 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Rama Neeyeda | 97d3a36f | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| rAmaM bhajEhaM | fff09e5d | published | P0 A1 C3 | missing_pallavi | no cached page | needs a live fetch, then re-extract and reingest |
| Sri Raghuvara Sugunaalaya | 5835ba5e | published | P0 A1 C4 | missing_pallavi | P1 A1 C4 | current parser finds the missing section; needs re-extract and reingest |
| Sri Rama Jaya Rama | 1dfe4864 | published | P1 A0 C17 | none | P1 + C17 (payload) | repaired 2026-11-03 by audited reingest (REINGEST_MAPPED_KRITHI) |
| Videmu Seyavae | 6a33383f | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
| Yochanaa Kamala | 4a53d422 | published | P0 A1 C1 | missing_pallavi | P1 A1 C1 | current parser finds the missing section; needs re-extract and reingest |
