| Metadata | Value |
|:---|:---|
| **Status** | Report |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-11-11 |
| **Author** | Sangita Grantha Team |

# TRACK-148 / TRACK-149 re-index: before and after search

Owner: [TRACK-148](../../conductor/tracks/TRACK-148-syama-sastri-edition-consultation-adjudication.md), [TRACK-149](../../conductor/tracks/TRACK-149-tyagaraja-edition-consultation-collision-split.md).

Eight queries against `POST /v2/catalogue/search/{hybrid|semantic}` on the local dev stack, top 5 titles, captured before and after the 2026-11-11 re-index. 16 result sets: 6 unchanged, 10 changed. Only `[Mudra]`, `[Laya]` and `[Theme]` clauses were added, so the changes are expected to be small reorderings and a few new entries; this report does not claim they are better.

| Mode | Query | Result |
|:---|:---|:---|
| hybrid | Syama Krishna mudra | before: rAvE hima giri kumAri (Sya); (ambA) kAmAkshi anudinamu (Sya); kAmAkshi nI pada (Sya); parAmukham (Sya); SyAma sundarAnga (Tya)<br>after: mari vErE gati (Sya); kAmAkshi nI pada (Sya); SyAma sundarAnga (Tya); rAvE hima giri kumAri (Sya); parAmukham (Sya) |
| semantic | Syama Krishna mudra | before: parAmukham (Sya); brOvavammA tAmasamElE (Sya); mAyammA nannu (Sya); Sankari SaMkuru (Sya); brOva samayamidE (Sya)<br>after: parAmukham (Sya); SyAma sundarAnga (Tya); brOvavammA tAmasamElE (Sya); mAyammA nannu (Sya); Sankari SaMkuru (Sya) |
| hybrid | Tyagaraja mudra song praising Rama | before: Rama Kothanda Rama (Tya); rAma rAma rAmacandra (Tya); ramA ramaNa rArA (Tya); rAma Eva daivataM (Tya); Laavanya Rama (Tya)<br>after: jagadAnanda kAraka (Tya); rAma nAmamu janma (Tya); rAma rAma rAma nApai (Tya); Laavanya Rama (Tya); Sri Rama Paadamaa (Tya) |
| semantic | Tyagaraja mudra song praising Rama | before: rAma rAma rAma nApai (Tya); Laavanya Rama (Tya); Sri Rama Paadamaa (Tya); rAmA rAma rAma rAmAyani (Tya); Padavi Nee (Tya)<br>after: rAma rAma rAma nApai (Tya); Laavanya Rama (Tya); Sri Rama Paadamaa (Tya); mummUrtulu (Tya); SyAma sundarAnga (Tya) |
| hybrid | Kamakshi goddess kriti | before: kAmAkshi karuNA (Sya); kAmAkshi lOka sAkshiNI (Sya); (ambA) kAmAkshi anudinamu (Sya); nIrajAkshi kAmAkshi (Mut); kAmAkshi nI pada (Sya)<br>after: kAmAkshi lOka sAkshiNI (Sya); kAmAkshi karuNA (Sya); (ambA) kAmAkshi anudinamu (Sya); nIrajAkshi kAmAkshi (Mut); kAmAkshi nI pada (Sya) |
| semantic | Kamakshi goddess kriti | unchanged |
| hybrid | Srirangam Ranganatha kriti | before: parimaLa ranga nAthaM1 (Mut); parimaLa ranga nAthaM2 (Mut); SrI ranga nAthAya (Mut); ranga nAyakaM (Mut); SrI bhArgavi (Mut)<br>after: parimaLa ranga nAthaM1 (Mut); ranga nAyakaM (Mut); parimaLa ranga nAthaM2 (Mut); SrI ranga nAthAya (Mut); SrI bhArgavi (Mut) |
| semantic | Srirangam Ranganatha kriti | unchanged |
| hybrid | Lalgudi pancharatnam | before: lambOdarAya (Mut); panca bhUta kiraNAvaLIM (Mut); saindhavi rAga priyE (Mut); cidambara naTarAja mUrtiM (Mut); jagadAnanda kAraka (Tya)<br>after: Endaro Mahaanubhaavulu (Tya); lambOdarAya (Mut); kana kana rucirA (Tya); praNatArti haraM (Mut); parimaLa ranga nAthaM2 (Mut) |
| semantic | Lalgudi pancharatnam | unchanged |
| hybrid | Kovur pancharatnam | before: praNatArti haraM (Mut); paramESvara jagadISvara (Mut); Endaro Mahaanubhaavulu (Tya); kOdaNDa rAmaM (Mut); kana kana rucirA (Tya)<br>after: praNatArti haraM (Mut); kOdaNDa rAmaM (Mut); jagadAnanda kAraka (Tya); paramESvara jagadISvara (Mut); panca bhUta kiraNAvaLIM (Mut) |
| semantic | Kovur pancharatnam | unchanged |
| hybrid | Utsava Sampradaya kriti | before: pAhi mAM pArvati paramESvari (Mut); varamaina nEtrOtsava (Tya); guru guhAya bhakta (Mut); Rama Samayamu (Tya); sItA kalyANa (Tya)<br>after: varamaina nEtrOtsava (Tya); pAhi mAM pArvati paramESvari (Mut); SrI janaka tanayE (Tya); kAmAkshIM kalyANIM (Mut); rAma nAmamu janma (Tya) |
| semantic | Utsava Sampradaya kriti | before: varamaina nEtrOtsava (Tya); Rama Samayamu (Tya); sItA kalyANa (Tya); SrI janaka tanayE (Tya); bhajana sEyu mArgamu (Tya)<br>after: SrI janaka tanayE (Tya); Rama Samayamu (Tya); sItA kalyANa (Tya); samsArulaitE (Tya); Rama Rama Nee Vaaramu (Tya) |
| hybrid | viloma chapu laya Syama Sastri | unchanged |
| semantic | viloma chapu laya Syama Sastri | unchanged |

Reading: the Syama Krishna mudra query now surfaces *SyAma sundarAnga* (Tyagaraja) in the top 5, which is a literal match on the word and not a Syama Sastri mudra. Queries for the Lalgudi and Kovur sets still return unrelated Muthuswami Dikshitar items, because the kshetra membership claims are not in the embedded text.

## After the second re-index (2026-11-11, claims lifted)

The ritual, dramatic, theme, bhava and laya claims were lifted and both composers re-embedded (768 documents, 0 failed). Of the 16 result sets, 5 match the original baseline and 11 differ in the top 5. The "Utsava Sampradaya kriti" queries now return *sItA kalyANa*, *heccarikagA* and *patiki hArati* first, which carry the `[Ritual Function: Utsava Sampradaya]` clause. This is a change in ranking, not a measured improvement.

