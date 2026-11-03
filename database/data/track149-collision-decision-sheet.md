# TRACK-149 collision decision sheet (read-only, generated 2026-11-03)

Method: for each of the 8 collided `krithis` rows, the mapped `imported_krithis` witnesses were compared on stated raga/tala and on the Latin-script sections stored in `parsed_payload`, after diacritic stripping and removal of bracketed refrains. Sources are all `thyagaraja-vaibhavam.blogspot.com` pages (`secondary_text`). Similarity is `difflib` ratio, an aid not a verdict. **Status: recommendation only; no decision is recorded until the owner approves.**

Key finding: in 7 of 8 rows the lyric variants attached to the krithi came from exactly **one** witness, and that witness's raga equals the catalogue raga. **Correction (2026-11-03, found while executing):** *Sri Narada Muni* is mixed. Its Latin variant came from the Bhairavi witness (84ffdbb8) but its other five scripts came from the *pantuvarALi* witness (c702d09d), so those five variants belong to the composition being split off. The other witnesses' text lives only in `parsed_payload`. For the 7 clean rows, splitting needed no deletion of lyric data, only unmapping the other witness. *Sri Narada Muni* needed its five wrongly-merged variants removed first; the owner approved this on 2026-11-03 (see Execution log).

| Catalogue krithi | Witnesses (raga / tala / sections) | Pallavi sim. | Recommendation | Retains `krithi_id` |
|:---|:---|:---|:---|:---|
| jaya mangaLaM | b26f89a5 nAda nAmakriya / Adi / 7; 6eb15e4d mOhanaM / – / 5; 5d70a340 ghaNTA / Jhampa / 4 | 1.00 (shared opening line) | **Split into 3.** Same refrain *jaya mangaLaM nitya subha mangaLaM*, but the stanzas are three different texts. Human call per Spec 11(c). | b26f89a5 |
| pAhi rAma candra | 604a9623 yadukula kAmbhOji / – / 10; 3943b77a SankarAbharaNaM / – / 5 | 0.54 | **Split into 2.** | 604a9623 |
| paripUrNa kAma | 4707048d pUri kalyANi / Adi / 3; 0082d54a hamsa bhramari / Adi / 3 | 0.50 | **Split into 2.** Different pallavis. | 4707048d |
| Rama Rama Rama Sita | 3946f1d2 sAvEri / Adi / 6; 3115095a Huseni / – / 14 | 0.62 | **Split into 2.** Section counts 6 and 14. | 3946f1d2 |
| rAma sItA rAma | ab6c4e31 balahamsa / – / 6; 1daabb05 SankarAbharaNaM / – / 10 | 0.63 | **Split into 2.** | ab6c4e31 |
| Sri Narada Muni | 84ffdbb8 Bhairavi / – / 5; c702d09d pantuvarALi / Rupakam / 5 | 0.47 | **Split into 2.** | 84ffdbb8 |
| SrI nArasiMha | 40e2d47b phala ranjani / – / 3; 733ec39f bilahari / Isra Capu / 5 | 0.46 | **Split into 2.** | 40e2d47b |
| Sri Rama Jaya Rama | 1f73ab63 Madhyamavati / – / 18; 3916fad4 varALi / Isra Capu / 12 | 0.64 | **Split into 2.** | 1f73ab63 |

If all eight are approved: 8 retained rows plus **9 new rows**; catalogue 675 becomes 684, matching the 684 classification subjects.

Caveats: (1) these are blog transcriptions, so "distinct text" shows distinct witnesses, not that a scholarly edition lists two compositions; (2) the Tyagaraja attribution of each new row is unverified beyond the blog; (3) the *pAhi rAma candra* pages use two different URL spellings, worth a second look; (4) tala is `Unknown` for most witnesses, so tala gives little evidence here.


## Execution log (2026-11-03)
All 9 splits executed through `POST /imports/{id}/unmap` then `/review` with `forceNew`, after a `pg_dump` backup. New krithis are `draft` / `UNESTABLISHED`, 6 variants each, 1 evidence row. *Sri Narada Muni*: the unmap guard first refused (mixed variants). With owner approval ("Yes, delete 5") the five variants were cleared through the existing audited `POST /imports/84ffdbb8.../reingest` on the retained Bhairavi witness (`REINGEST_MAPPED_KRITHI`), which rebuilt only that witness's Latin variant (5 sections). Then c702d09d was unmapped and re-promoted with `forceNew`, giving new krithi `6d55561e-eedf-48c1-b1ea-2961a2750965` (*nArada muni*, draft / UNESTABLISHED, 6 variants, 5 sections, 1 evidence row, 1 raga link). Retained krithi `76706063...` now has 1 variant and 2 evidence rows.


### Import to krithi map after the splits (2026-11-03)

| Import | Krithi | Title | Raga / tala | Form / state |
|:---|:---|:---|:---|:---|
| 5d70a340 | 191c44c1-9744-4afe-9f90-5a9cb45ade22 | jaya mangaLaM | Ghanta / Jhampa | UNESTABLISHED / draft |
| 6eb15e4d | 474faeba-0c01-4688-a1e1-1ad41b536ee3 | jaya mangaLaM | Mohanam / Unknown | UNESTABLISHED / draft |
| b26f89a5 | 6ddd3f36-022a-4a78-9a63-c769c5941508 | jaya mangaLaM | Nadanamakriya / Adi | KRITHI / published |
| c702d09d | 6d55561e-eedf-48c1-b1ea-2961a2750965 | nArada muni | Panthuvarāli / Rupaka | UNESTABLISHED / draft |
| 733ec39f | ff293925-2765-420a-831d-5d6314c456df | narasiMha | Bilahari / Isra Capu | UNESTABLISHED / draft |
| 3943b77a | 7e7529c6-93ee-46fe-890e-c491854fbeee | pAhi rAma candra | SankarAbharaNaM / Unknown | UNESTABLISHED / draft |
| 604a9623 | ba2ec6a9-1804-48f1-a379-0fc505078892 | pAhi rAma candra | Yadukula Kāmbhoji / Adi | KRITHI / published |
| 0082d54a | 20fb3477-37b1-4a67-bf22-cfb57fd45192 | paripUrNa kAma | Hamsabhramari / Adi | UNESTABLISHED / draft |
| 4707048d | 4abda836-1e15-4b0b-a7a7-58035712c55a | paripUrNa kAma | pUri kalyANi / Adi | KRITHI / published |
| 3115095a | f2700b5a-d26a-498b-b5c7-61806ebbb729 | Rama Rama Rama Sita | Huseni / Unknown | UNESTABLISHED / draft |
| 3946f1d2 | 42d35ca5-4110-4fd8-bece-9ced6a37014e | Rama Rama Rama Sita | Sāveri / Adi | KRITHI / published |
| 1daabb05 | db90b28a-2b7f-43ee-a6dc-078558b40cf9 | rAma sItA rAma | SankarAbharaNaM / Unknown | UNESTABLISHED / draft |
| ab6c4e31 | 4f90dd54-c76e-4cce-88f8-1c460cdb3566 | rAma sItA rAma | Bālahamsa / Adi | KRITHI / published |
| 84ffdbb8 | 76706063-efdd-49a7-b091-4817afed5b57 | Sri Narada Muni | Bhairavi / Adi | KRITHI / published |
| 40e2d47b | 12058429-320a-4040-b682-78c26399e0ae | SrI nArasiMha | Phalaranjani / Adi | KRITHI / published |
| 1f73ab63 | 1dfe4864-6224-460a-845a-5328aed9b638 | Sri Rama Jaya Rama | Madhyamāvathi / Adi | KRITHI / published |
| 3916fad4 | e9d91067-f4c7-4b2b-ae5c-06db11815ac2 | SrI rAma jaya rAma | Varāli / Isra Capu | UNESTABLISHED / draft |

The retained krithi of each pair keeps its original id. Retained *Sri Narada Muni* now shows a pallavi, so the catalogue-wide missing-pallavi count fell from 27 to 26.
