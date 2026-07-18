| Metadata | Value |
|:---|:---|
| **Status** | Advisory — informs modelling; ragamalika not yet re-extracted |
| **Version** | 1.0.0 |
| **Last Updated** | 2026-07-18 |
| **Author** | `carnatic-musicologist` review (for Seshadri) |
| **Backs** | `database/migrations/V47__demerge_ragamalika_visvanatham_from_natabharanam.sql` |

# Musicological Review — *viSva nAthaM bhajEhaM* (Sri Visvanatham)

Muthuswami Dikshitar. Reviews how this composition should be modelled after migration V47 separated
the ragamalika lyric variants that had been incorrectly merged into the Natabhranam krithi during
import. V47 is applied; the ragamalika still needs re-extraction as its own krithi, and the
recommendations below describe how it should be structured when that happens.

Current Database State
The ragamalika composition does not yet exist as its own krithi row. What exists today is:

Krithi ab99b150: titled "viSva nAthaM bhajEhaM", stored as musical_form = KRITHI, is_ragamalika = false, primary_raga = Natabhranam, with a single krithi_ragas row (Natabhranam, order_index 0). It has 2 sections: PALLAVI and SAMASHTI_CHARANAM. The lyric text is the Natabhranam-raga krithi only (the non-ragamalika composition).
The pending migration V47__demerge_ragamalika_visvanatham_from_natabharanam.sql correctly identifies that ragamalika lyric variants were incorrectly merged into this Natabhranam krithi during import. It removes those variants and resets the import sources for re-extraction as a separate krithi. This is the correct approach.

Once re-extracted, the ragamalika will need its own krithi row. Below are my recommendations for how to model it.

Answers to Your Questions
1. Canonical Section Structure (krithi_sections rows)
   The composition should have 3 krithi_sections rows:

order_index	section_type	label
1	PALLAVI	Pallavi
2	ANUPALLAVI	Anupallavi
3	CHARANAM	Charanam
The raga subsections within each structural section are not separate krithi_sections. They are internal subdivisions of the sahitya text within each section. The structural form is Pallavi / Anupallavi / Charanam -- the standard Krithi form. The raga changes happen within each section, and that raga-to-text mapping is captured by the krithi_ragas junction table (with section indicating which structural section each raga belongs to).

Rationale: The domain model (section 6.2) says to represent the raga sequence via KrithiRaga rows with orderIndex and optional section. The section-type enum does not have a "ragamalika subsection" type, nor should it -- these are not structural divisions in the way Pallavi/Anupallavi/Charanam are. The raga transitions are a property of the ragamalika form overlaid on the standard section structure.

2. Musical Form
   Confirmed: musical_form = 'KRITHI' with is_ragamalika = true.

This is a Krithi by form (Pallavi-Anupallavi-Charanam structure, sahitya-dominant, no Muktayi Swaram or Ettugada pattern). The ragamalika aspect is an additional property captured by the flag and the krithi_ragas rows.

3. Tala
   Confirmed: Adi tala (Chatusra-jati Triputa, 8 beats). This is well-attested in all published sources for this composition.

4. Return Ragas (Viloma) Classification
   The viloma (return) ragas at the end of the Charanam section -- going back through Bhupala to Sama, then Mohana through Sri -- belong to the Charanam section in the krithi_ragas.section column.

Musically, the viloma is a structural feature of this composition where the composer revisits the ragas in reverse order. But the performance practice is clear: after the forward Charanam ragas (7-14), the singer continues in the Charanam without returning to the Anupallavi or Pallavi as separate structural restarts. The entire viloma passage is sung as a continuation of the Charanam. The Anupallavi viloma (6 to 1) similarly stays within the Anupallavi section.

So the section assignment should be:

Anupallavi viloma ragas (Mohana back to Sri): section = 'anupallavi'
Charanam viloma ragas (Bhupala back to Sama, then Mohana back to Sri): section = 'charanam'
5. Raga Sequence for krithi_ragas
   The full ordered sequence with section assignments:

order_index	raga	section
1	Sri	pallavi
2	Arabhi	pallavi
3	Gauri	anupallavi
4	Nata	anupallavi
5	Gaula	anupallavi
6	Mohana	anupallavi
7	Mohana (viloma)	anupallavi
8	Gaula (viloma)	anupallavi
9	Nata (viloma)	anupallavi
10	Gauri (viloma)	anupallavi
11	Arabhi (viloma)	anupallavi
12	Sri (viloma)	anupallavi
13	Sama	charanam
14	Lalita	charanam
15	Bhairava	charanam
16	Saranga	charanam
17	Sankarabharanam	charanam
18	Kambhoji	charanam
19	Devakriya	charanam
20	Bhupala	charanam
21	Bhupala (viloma)	charanam
22	Devakriya (viloma)	charanam
23	Sankarabharanam (viloma)	charanam
24	Kambhoji (viloma)	charanam
25	Saranga (viloma)	charanam
26	Bhairava (viloma)	charanam
27	Lalita (viloma)	charanam
28	Sama (viloma)	charanam
29	Mohana (viloma)	charanam
30	Gaula (viloma)	charanam
31	Nata (viloma)	charanam
32	Gauri (viloma)	charanam
33	Arabhi (viloma)	charanam
34	Sri (viloma)	charanam
Important modeling note: The viloma entries reference the same raga_id as their forward counterparts (e.g., order_index 6 and 7 both point to the same Mohana raga row). The notes column on krithi_ragas should carry "viloma" to distinguish the return passage. The order_index captures the performance sequence unambiguously.

For primary_raga_id on the krithi row: since the domain model says "a single primaryRagaId is insufficient for a ragamalika," you could set it to Sri (the opening raga) as a convenience/display default, or leave it NULL. I recommend Sri as the primary since the composition begins and ends in Sri ragam.

6. Viloma Structure -- Is This Standard for Dikshitar?
   This is highly unusual and is one of the reasons this composition is considered among Dikshitar's most elaborate works. The standard pattern in Dikshitar's ragamalikas (and ragamalikas generally in the Carnatic tradition) is:

Standard ragamalika: Each section has its own raga(s), proceeding forward only. Example: Dikshitar's "Kamalamba Navavarana" cycle (each krithi in one raga, no viloma).
Simple ragamalika with viloma: Some compositions reverse a short sequence at the end to "close the circle." Example: a 5-raga malika that returns 5-4-3-2-1 at the end.
The "viSva nAthaM bhajEhaM" is exceptional because:

It uses 14 ragas (caturdaSa ragamalika) -- already rare.
It applies viloma within each major section (not just at the very end). The Anupallavi reverses through all ragas heard so far (6 to 1), and the Charanam reverses through all 14 ragas (14 to 1).
The total number of raga transitions in performance is therefore 34, not 14.
This palindromic/arch structure within sections is a compositional tour de force specific to this work. It is not the norm for Dikshitar's other ragamalikas (such as the Navagraha or Panchalinga krithis, which are sets of individual compositions rather than multi-raga single compositions). Among single-composition ragamalikas in the broader tradition, this level of viloma elaboration is rare and marks the piece as a showcase of compositional mastery.

Findings Summary
Incorrect
is_ragamalika = false on the existing krithi row ab99b150. However, this row is actually the Natabhranam krithi (the non-ragamalika composition with the same title), so it is correctly marked false. The ragamalika simply does not exist as a separate krithi yet.
Ragamalika lyric variants merged into wrong krithi. The V47 migration addresses this correctly by removing the 6 ragamalika lyric variants from the Natabhranam krithi and resetting import records for re-extraction.
Suspect (recommend human review at re-extraction time)
Charanam viloma scope: The guru-guha blogspot source shows the final viloma in the Charanam going all the way back to Sri (raga 1), passing through the Anupallavi and Pallavi ragas. Some published editions treat the final six ragas (Mohana through Sri) as a return to the Anupallavi/Pallavi sections rather than a continuation of the Charanam. I have assigned them to charanam above based on performance practice (the singer does not structurally restart the Anupallavi/Pallavi), but this deserves verification against the Semmangudi or T.K. Govinda Rao recordings when the composition is re-ingested.
Primary raga assignment: When the new ragamalika krithi row is created, confirm whether primary_raga_id should be Sri (opening raga) or NULL. The domain model discourages collapsing to a single raga but does not explicitly forbid a "lead" raga designation for display purposes.
Cosmetic
Raga name normalization: Ensure the raga entries in the database use consistent transliteration: "Sankarabharanam" (not "Shankarabharanam"), "Bhupala" (not "Bhoopalam"), "Devakriya" (not "Deva Kriya" with a space). Check that each of the 14 ragas has a corresponding row in the ragas table before creating krithi_ragas entries.
The V47 migration is the correct first step. After it runs and re-extraction produces the ragamalika as a new krithi, the ingestion pipeline should create the row with the structure described above: 3 sections, 34 krithi_ragas rows with viloma notes, is_ragamalika = true, and musical_form = 'KRITHI'.