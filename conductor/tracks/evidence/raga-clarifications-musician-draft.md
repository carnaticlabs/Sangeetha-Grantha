# Raga Clarifications — Questions for Expert Review

We are compiling a carefully verified reference of the ragas used in the compositions of the
Trinity — Tyagaraja, Muthuswami Dikshitar and Syama Sastri.

While cross-checking our list against published sources, a small number of entries could not be
settled with confidence. In each case below, either two names appear for what may be the same raga,
or an arohanam/avarohanam we hold does not look right. Rather than guess, we would be very grateful
for your guidance.

Where relevant we have noted the kritis concerned, since the composition itself is often the clearest
evidence.

Notation used throughout: **S R1 R2 R3 G1 G2 G3 M1 M2 P D1 D2 D3 N1 N2 N3 S**

---

## Part A — The three we most need settled

### A1. Purvi — which form does Dikshitar's *Srī Guru Guhasya* use?

We hold two different ragas both called Purvi:

| | Arohanam | Avarohanam |
|:--|:--|:--|
| **Purvi** (as a janya of Mayamalavagowla, 15) | S R1 G3 M1 D1 N3 S | S N3 D1 P M1 G3 R1 S |
| **Purvi** (with prati madhyamam) | S R1 G3 M2 P D1 N3 S | S N3 D1 P M2 G3 R1 S |

The second is the North Indian Purvi. The kriti in question is Dikshitar's **`Srī Guru Guhasya`**.

**Questions**
1. Which of these two does *Srī Guru Guhasya* use?
2. Is the prati-madhyamam form sung in Carnatic concerts as a raga in its own right, or should we
   treat it as the same raga rendered differently?
3. If they are two distinct ragas, is there a better name for each so they are not confused?

*Your answer:* **Purvi is a rare *bhashanga janya* raga of Mayamalavagowla (15)** — i.e. the first
form in our table (S R1 G3 M1 D1 N3 S / S N3 D1 P M1 G3 R1 S), the *shuddha madhyamam* form. This is
the raga of Dikshitar's *Srī Guru Guhasya*. (Bhashanga = it admits an anya swaram outside the parent
scale.) The prati-madhyamam "North Indian Purvi" is **not** the raga of this kriti.

<br>

### A2. Gamakapriya and Gamanapriya — one raga or two?

We hold both names, each shown as a janya of Pantuvarali (51), and — which seems unlikely to be
right — with **exactly the same scale**:

| Name | Arohanam | Avarohanam |
|:--|:--|:--|
| Gamakapriya | S R1 G3 M2 P N3 D1 S | S D1 P M2 G3 R1 S |
| Gamanapriya | S R1 G3 M2 P N3 D1 S | S D1 P M2 G3 R1 S |

We also hold **Gayakapriya** (melakarta 13) separately, which we believe is unrelated to both.

**Questions**
1. Are Gamakapriya and Gamanapriya one raga under two names, or two distinct ragas?
2. If two, what is the correct arohanam and avarohanam of each?
3. Which name is the one in common use today?

*Your answer:* Gamakapriya and Gamanapriya do **not** exist as two distinct standard scales. The names
most likely stem from a mishearing / spelling variation of **Gamakakriya** and **Gamanashrama**, which
are the same melodic entity under two nomenclature traditions:
- **Gamanashrama** — the official name of the **53rd Melakarta** in the modern Govindacharya
  (Kanakangi–Ratnangi) system.
- **Gamakakriya** — the exact equivalent name for that same 53rd parent scale under the older
  Venkatamakhin (Asampurna Mela) nomenclature.

The names may therefore need to be updated to **Gamanashrama / Gamakakriya**. Reference kriti:
Dikshitar's *Meenakshi Me Mudam* (in Gamakakriya = Gamanashrama, mela 53).
Source: [Gamanashrama, Wikipedia](https://en.wikipedia.org/wiki/Gamanashrama).

> ⚠️ **Reconciliation note (needs a follow-up, do not apply blindly):** Our question asked about
> *Gamakapriya* / *Gamanapriya* as **janyas of Pantuvarali (51)**, with a prati-madhyamam scale
> (S R1 G3 M2 P N3 D1 S). The expert's answer instead resolves the names to **Gamanashrama /
> Gamakakriya = melakarta 53** (S R1 G3 M2 P D2 N3 S). These are **not** the same scale — the 53rd
> mela has **D2 N3**, whereas our held entries have **N3 D1** (vakra) as a Pantuvarali janya. So
> either (a) our two entries were misnamed corruptions of the mela-53 name and should be replaced by
> a single Gamanashrama entry, or (b) there is a genuine Pantuvarali janya that the answer has
> conflated with the mela. Confirm which of our kritis (if any) actually sit on these entries before
> merging — the presence/absence of a real Pantuvarali janya decides it.

> ✅ **RESOLVED via DB audit (2026-08-29):** The DB decides it in favour of the expert. We hold three
> relevant entries:
> - **Gamakakriyā** — janya of **Gamanāśrama (mela 53)**, scale `S R1 G3 M2 P D2 S / S N3 D2 P M2 G3
>   R1 S` (the D2/N3 mela-53 form). **Carries all 5 real kritis:** `mInAkshi mE mudaM`,
>   `kASi viSAlAkshIM`, `EkAmra nAthaM bhajEhaM`, `nava ratna mAlinIM`, `tiruvaTISvaraM`.
> - **Gamakapriyā** and **Gamanapriyā** — both janyas of **Panthuvarāli (mela 51)**, identical scale
>   `S R1 G3 M2 P N3 D1 S / S D1 P M2 G3 R1 S`, **zero kritis attached** on either.
>
> So Gamakapriyā / Gamanapriyā are orphan misnamed corruptions of the mela-53 name (option (a)); the
> correctly-named, kriti-bearing **Gamakakriyā** already exists under Gamanāśrama (53). *Meenakshi Me
> Mudam* is confirmed in **Gamaka Kriya** by the Guru Guha (Dikshitar) source:
> [guru-guha.blogspot.com](https://guru-guha.blogspot.com/2008/04/dikshitar-kriti-meenakshi-memudam-raga.html).
> **Action:** retire/merge the two empty Pantuvarali-51 entries (Gamakapriyā, Gamanapriyā) — no kriti
> re-tagging needed since neither has any — keeping **Gamakakriyā (janya of Gamanāśrama, 53)** as
> canonical. Consider adding *Gamanashrama / Gamanapriya / Gamakapriya* as aliases on it.

<br>

### A3. Dharmavati and Dhamavati — the same raga?

We hold Dharmavati as **melakarta 59**, and Dhamavati separately — but with an identical scale:

| Name | Arohanam | Avarohanam |
|:--|:--|:--|
| Dharmavati (mela 59) | S R2 G2 M2 P D2 N3 S | S N3 D2 P M2 G2 R2 S |
| Dhamavati | S R2 G2 M2 P D2 N3 S | S N3 D2 P M2 G2 R2 S |

Two Dikshitar kritis are attributed to Dhamavati: **`Parandhāmavatī`** and
**`Rāma Candrasya Dāsōhaṃ`**.

**Questions**
1. Is Dhamavati simply another name for melakarta 59 Dharmavati, or a distinct raga?
2. If distinct, how does it differ in scale or in the way it is handled?
3. Which name should we use for these two kritis?

*Your answer:* **Yes — Dhamavati (often spelled Dhaamavati) is simply another name for melakarta 59
Dharmavati**, not a distinct raga. The identical scale in our table is correct. The name variance is
purely the two nomenclature traditions:
- **Venkatamakhin / Dikshitar (asampurna mela):** the 59th mela is named **Dhaamavati**. Dikshitar
  embeds it as a raga mudra (lyrical signature) in *Parandhāmavatī* ("Param-**dhāmavatī**").
- **Govindacharya (sampurna melakarta, the modern 72-mela standard):** the 59th raga is named
  **Dharmavati**.

Both *Parandhāmavatī* and *Rāma Candrasya Dāsōhaṃ* are in this one raga. Recommendation: store as
**Dharmavati (mela 59)** with **Dhamavati / Dhaamavati** as an alias.
Sources: [Dharmavati, Wikipedia](https://en.wikipedia.org/wiki/Dharmavati),
[darbar.org](https://darbar.org/raga/dharmavati/).

<br>

---

## Part B — Two names, possibly one raga

### B1. Andhali / Andali

We hold this raga as a janya of Harikambhoji (28):

- Arohanam: **S R2 M1 P N2 S**
- Avarohanam: **S N2 P M1 R2 G3 M1 R2 S**

Two kritis are involved — Tyagaraja's **`Abhimānamu Lēdēmi`** and Dikshitar's
**`Bṛhannāyaki Varadāyaki`**.

This raga does not appear in the published lists we have been able to consult, so we have no
authority for the spelling at all.

**Questions**
1. Are Andhali and Andali the same raga? Which spelling is correct?
2. Is the scale above right, and is Harikambhoji the correct parent?
3. Are both kritis above in this raga?

*Your answer:* **Andhali and Andali are the same rare raga, a janya of Harikambhoji (28).** The
correct spelling is **Andhali** (aspirated 'dh') — the historically documented form in the treatises
(Sangraha Chudamani; Subbarama Dikshitar's *Sangita Sampradaya Pradarshini*). "Andali" is a variant.
Source: [rasika.life](https://rasika.life/carnatic/ragas/andhali-38awqkbyxqJfKxZxzT8y3LHuN6f).

> ✅ **DB audit (2026-08-29):** We hold **both** — `Andhali` (full: parent Harikāmbhōji 28, scale
> `S R2 M1 P N2 S / S N2 P M1 R2 G3 M1 R2 S`) **and** an empty duplicate `Andali` (no scale, no
> parent, name-only). **Action:** keep **Andhali** as canonical, fold `Andali` in as an alias / delete
> the empty duplicate. Still need to confirm the two kritis (`Abhimānamu Lēdēmi`, `Bṛhannāyaki
> Varadāyaki`) are tagged to Andhali — see note below.

<br>

### B2. Kalavathi / Kalavati

Here we hold two entries that look genuinely different, and we would like to confirm that they
really are two ragas rather than one entered twice:

| Name | Parent | Arohanam | Avarohanam |
|:--|:--|:--|:--|
| Kalavathi | Yagapriya (31) | S R3 G3 M1 P D1 N1 S | S N1 D1 P M1 G3 R3 S |
| Kalavati | Chakravakam (16) | S R1 M1 P D2 S | S D2 P M1 G3 S R1 S |

Three kritis sit on the second: Tyagaraja's **`Ennaḍu Jūtunō`** and **`Oka Pāri Jūḍaga`**, and
Dikshitar's **`Kalāvati Kamalāsana`**.

**Questions**
1. Are these two distinct ragas?
2. Is the second scale correct? A five-note ascent with G3 appearing only in descent looked unusual
   to us.
3. Are the three kritis all in the same one of the two?

*Your answer:* **Yes — two genuinely distinct ragas that share the name (homonyms across traditions):**
- **Kalāvatī, Mēḷa 31** (Yāgapriyā / asampurna Ragāṅga tradition — Venkatamakhin, Subbarama
  Dikshitar). Vivādi scale: **R3 G3 M1 P D1 N1**.
- **Kalāvatī, Mēḷa 16** (janya of Cakravākam — Govindacharya's Sangraha Chudamani tradition, followed
  by Tyagaraja). Scale: **R1 G3 M1 P D2**.

**Second scale confirmed correct:** ārohaṇam `S R1 M1 P D2 S`, avarohaṇam `S D2 P M1 G3 S R1 S`
(or `S D2 P M1 G3 R1 S`). The 5-up / 6-down asymmetry with G3 only in descent is a legitimate
audava–shadava / vakra structure (cf. Garudadhvani, Malahari, Bahudari) — not an error.

**The three kritis split across the two:**
| Kriti | Composer | Raga |
|:--|:--|:--|
| Kalāvatī Kamalāsana | Dikshitar | **Kalāvatī (Mēḷa 31)** — Yāgapriyā equivalent |
| Ennaḍu Jūtunō | Tyagaraja | Kalāvatī (Mēḷa 16) — janya of Cakravākam |
| Oka Pāri Jūḍaga | Tyagaraja | Kalāvatī (Mēḷa 16) — janya of Cakravākam |

> ⚠️ **DB audit (2026-08-29) — MIS-TAG found:** We correctly hold both entries with matching scales —
> `Kalāvathi` (parent Yāgapriyā 31, `S R3 G3 M1 P D1 N1 S / …`) and `Kalāvati` (parent Chakravākam 16,
> `S R1 M1 P D2 S / S D2 P M1 G3 S R1 S`). **But all three kritis are currently tagged to `Kalāvati`
> (mela 16)**, including Dikshitar's `kalAvati kamalAsana`. Per the expert, `kalAvati kamalAsana`
> should move to **`Kalāvathi` (mela 31)**; the two Tyagaraja kritis stay on mela 16. **Action:**
> re-tag `kalAvati kamalAsana` from Kalāvati(16) → Kalāvathi(31). *(Dikshitar's Kalavati attribution
> has some scholarly debate; the expert's SSP-based mela-31 placement is the authority we should
> follow — worth a one-line confirmation before applying.)*

<br>

### B3. Sreemati / Srimati *(lower priority — no kritis attached)*

| Name | Parent | Arohanam | Avarohanam |
|:--|:--|:--|:--|
| Sreemati | Ratnangi (2) | S R1 G1 P D1 S | S N2 D1 P G1 R1 S |
| Srimati | Hanumatodi (8) | S G2 R1 G2 M1 P D1 P D2 N2 S | S N2 D1 P M1 P M1 G2 R1 S |

**Question:** are these two different ragas, or one raga recorded twice with differing scales?

*Your answer:* **Two completely different ragas** that share phonetic variants of the same name —
distinct janyas of different parent melas:
- **Sreemati** (Shrimati) — janya of **Ratnangi (2)**; audava–shadava (5 up, 6 down); swaras
  **S R1 G1 P D1 N2**; uses **Suddha Gandhara (G1)**; strictly within the parent scale (not bhashanga).
- **Srimati** — janya of **Hanumatodi (8)**; vakra/zig-zag; swaras **S R1 G2 M1 P D1 D2 N2**; uses
  **Sadharana Gandhara (G2)** and is **bhashanga** — introduces the anya swaram **Chatusruti Dhaivata
  (D2)** in ascent.

They cannot be the same: different gandharam (G1 vs G2), different parent, and linear vs. vakra flow.

> ⚠️ **DB audit (2026-08-29):** We hold **only** the Mela-8 `Srimati` (parent Hanumatodi, vakra scale
> with D2 anya swara — matches, 0 kritis). We do **not** hold the Mela-2 `Sreemati` (Ratnangi) at all.
> **Action:** our single entry is correct as-is; optionally **add** a `Sreemati` (Ratnangi 2,
> `S R1 G1 P D1 S / S N2 D1 P G1 R1 S`) entry for completeness. Low priority — neither has kritis
> attached.

<br>

---

## Part C — Scales we would like confirmed

### C1. Brindavana Saranga

The scale we hold looks wrong to us — it has no nishadam at all:

- Arohanam: **S R2 G2 M1 P D2 S**
- Avarohanam: **S D2 P M1 G2 S**

**Questions**
1. What are the correct arohanam and avarohanam?
2. We understand the raga's character rests on using **N3 in ascent and N2 in descent** — is that
   right, and is gandharam omitted?

*Your answer:* **Correct — our held scale is wrong** (it omits nishadam N and wrongly includes
dhaivatham D). The standard Carnatic scales are:
- Arohanam: **S R2 M1 P N3 S**
- Avarohanam: **S N2 P M1 R2 G2 R2 S** (the close variant *Brindavani* simplifies to
  `S N2 P M1 R2 S`).

So the raga does use **N3 in ascent and N2 in descent** (a bhashanga trait), and **gandharam (G2)
appears only in the descent** — not omitted entirely, but ascent-varjya. Parent is Kharaharapriya (22).
Sources: [ragasurabhi](https://www.ragasurabhi.com/carnatic-music/raga/raga--brindavana-saranga.html),
[anuradhamahesh](https://anuradhamahesh.wordpress.com/2011/02/23/brindavana-saranga-a-most-pleasing-and-emotional-raga/).

> ✅ **DB audit (2026-08-29):** We already hold a **correct** entry —
> `Brindāvana Sāranga{Hindustani}` (parent Kharaharapriyā 22, `S R2 M1 P N3 S / S N2 P M1 R2 G2 R2 S`,
> anya swara N3) — plus the correct `Brindāvani{Hindustani}` variant. The **wrong** entry is the bare
> `Brindāvana Sāranga` (`S R2 G2 M1 P D2 S / S D2 P M1 G2 S`, no parent). **Actions:** (a) fix/delete
> the bad bare entry, merging onto the correct scale; (b) review the `{Hindustani}` suffix — this is
> the **Carnatic** Brindavana Saranga, so the label likely wants to drop that tag (the Hindustani
> counterpart is *Brindavani Sarang*).

<br>

### C2. Veeravasantham

- Arohanam: **S R2 G2 M1 P S**
- Avarohanam: **S N3 D3 P M1 G2 R2 S**

We have it as a janya of Varunapriya (24). The ascent ending at P, together with D3 in the descent,
seemed worth checking. The kritis are Tyagaraja's **`Ēmani Pogaḍudurā`** and Dikshitar's
**`Ēkāmra Nāthāya`**.

**Questions**
1. Is this scale correct?
2. Is Varunapriya the right parent?
3. We also see the name written as *Vīra Vasanta* and *Vīra Vasantam* — is one form preferred?

*Your answer:* **Our avarohanam is wrong.** The scale we hold
(`S R2 G2 M1 P S / S N3 D3 P M1 G2 R2 S`) is actually the linear *krama* of the **parent mela
Varunapriya (24)** — not Veeravasantham, which is a crooked (vakra) raga. Correct scales:
- **Govindacharya system (Tyagaraja):** Arohanam `S R2 G2 M1 P S` — our ascent is right — but
  Avarohanam **`S N3 P M1 G2 R2 S`** (**D3 omitted** in descent).
- **Dikshitar / Venkatamakhin system:** Ārohanam `S R2 M1 P N3 D3 N3 S`, Avarohanam
  `S N3 P M1 M1 R2 G2 S`.

**Parent Varunapriya (24) is correct** — as a janya under Govindacharya; under the Venkatamakhin
asampurna scheme the 24th mela *is* named Vīra Vasanta. **Name:** both forms are valid —
**Vīra Vasanta / Viravasanta** is the Sanskrit/scholarly form (SSP, Dikshitar's style);
**Veeravasantham / Vīravasantam** is the Telugu/Tamil phonetic form used for Tyagaraja's kritis in
concert usage. Sources:
[rasika.life](https://rasika.life/carnatic/ragas/viravasanta-38bEGaYRIzJQFLjlefqXhvsk2Tm),
[Reddit r/Carnatic](https://www.reddit.com/r/Carnatic/comments/1qvxlor/hello_is_varunapriya_and_veeravasantham_same/).

> ⚠️ **DB audit (2026-08-29):** Our `Veeravasantham` (Varunapriya 24) holds the **wrong Govindacharya
> avarohanam** `S N3 D3 P M1 G2 R2 S` — **remove D3** → `S N3 P M1 G2 R2 S`. We also hold two **empty
> name-only duplicates** `vIra vasanta` and `vIra vasantaM` (no scale/parent). **Actions:** (a) fix the
> avarohanam (drop D3); (b) fold the two empty duplicates in as aliases / delete. Decide whether to
> record the Dikshitar (Venkatamakhin) scale as a separate variant, since `Ēkāmra Nāthāya` is a
> Dikshitar kriti.

<br>

### C3. Narirītigowla

We hold this as a janya of Natabhairavi (20):

- Arohanam: **S G2 R2 G2 M1 N2 D1 M1 N2 N2 S**
- Avarohanam: **S N2 D1 M1 G2 M1 P M1 G2 R2 S**

The kritis are Dikshitar's **`Srī Nīlōtpala Nāyikē`** and **`Nīlōtpalāmbāṃ Bhajarē`**.

**Questions**
1. Is the name correctly *Narirītigowla*? We have also seen it written *Nārērētigowla*.
2. Is the scale above right, and is Natabhairavi the correct parent? We ask because the closely
   related Reethigowla is usually placed under Kharaharapriya (22).

*Your answer:* **Correct spelling is Nārīrītigowla** (also Nārīrītigaula). The **Nārī** prefix encodes
the katapayadi number: Na=0, Ra=2 → read reversed = **20**, its melakarta position. *Nārērētigowla*
is a phonetic corruption. **Our scale and parent are correct** — Natabhairavi (20), using **Shuddha
Dhaivatam (D1)**.

The Reethigowla relationship: **Nārīrītigowla is the archaic older form of Reethigowla.** Over time
the dhaivatam shifted D1 → D2 in mainstream practice, and modern Reethigowla is placed under
Kharaharapriya (22) with **D2**. Dikshitar preserved the original **D1** form — in *Srī Nīlōtpala
Nāyikē* he embeds both the raga and parent names in the anupallavi ("**rīti gauravē**",
"**nata bhairavē**"). Sources:
[Reetigowla, Wikipedia](https://en.wikipedia.org/wiki/Reetigowla),
[guru-guha.blogspot](http://guru-guha.blogspot.com/2008/08/dikshitar-kriti-sri-neelotpala-nayike.html).

> ⚠️ **DB audit (2026-08-29):** Our full entry uses the **corrupt spelling** `Nārērētigowla`
> (scale + Natabhairavi 20 parent are correct). There is also an **empty duplicate** `nArI rItigauLa`
> (no scale/parent) — closer to the right spelling. **Action:** rename the full entry to
> **Nārīrītigowla**, keep `Nārērētigowla` / `nArI rItigauLa` as aliases, and fold the empty duplicate
> in. Confirm both Dikshitar kritis (`Srī Nīlōtpala Nāyikē`, `Nīlōtpalāmbāṃ Bhajarē`) stay tagged to
> it.

<br>

### C4. Dwijavanthi / Jujavanthi

- Arohanam: **S R2 M1 G3 M1 P D2 S**
- Avarohanam: **S N2 D2 P M1 G3 M1 R2 G2 R2 S N2 D2 N2 S**

**Questions**
1. Is this scale correct, and is Harikambhoji (28) the right parent?
2. We understand the raga takes **G2 as an anya swaram** alongside G3 — is that right?
3. Which name is preferred in Carnatic usage — Dwijavanthi or Jujavanthi?

*Your answer:* **Confirmed on all three points.** Dwijavanthi is a valid **vakra janya of Harikambhoji
(28)**; our zig-zag scale is correct (it reflects the raga's Hindustani roots as Jaijaivanti). **G2
(sadharana gandharam) is correctly used as an anya swaram** alongside the regular G3 (antara), appearing
in the descending phrase to create the characteristic Jaijaivanti nuance. **Preferred name:
Dwijavanthi / Dwijavanti** — the standard modern Carnatic spelling; *Jujavanthi / Dwijayavanthi* are
older-text variants (Sangita Makaranda). Sources:
[sujamusic](https://sujamusic.wordpress.com/dwijavanti/),
[ragasurabhi](https://www.ragasurabhi.com/carnatic-music/raga/raga--dwijavanti.html).

> ⚠️ **DB audit (2026-08-29):** We hold the correct combined entry `Dwijāvanthi /Jujāvanthi`
> (Harikāmbhōji 28, matching scale, anya swaras G2/N3 flagged) — but ALSO an **empty-parent duplicate**
> `Jujāvanti` (no parent) and a separate Hindustani entry `Jaijaiwanti{Hindustani}`. `Jujahuli` is a
> **different** Harikambhoji janya — leave it alone. **Action:** set canonical name to **Dwijavanthi**,
> fold `Jujāvanti` / `Jujavanthi` in as aliases (drop the slashed name), keep `Jaijaiwanti{Hindustani}`
> as the distinct Hindustani cross-reference.

<br>

---

## A general question, if you have the patience for it

Several of the confusions above come from the same root: the same raga written in different ways by
different sources, and occasionally two genuinely different ragas whose names look almost identical
(Kanada and Kannada, for instance, or Abheri and Bhairavi).

**Is there a printed reference you would recommend as the most reliable authority** for raga names,
their parent melakartas, and their arohanam/avarohanam — one we could adopt as our standard and cite
consistently? We currently rely on general published lists, which is how several of these errors
crept in.

*Your answer:*

<br>

---

Thank you very much for your time. Even partial answers, or a note that a particular question has no
settled answer, would help us a great deal — knowing that something is genuinely disputed is as
useful to us as knowing the answer.
