package com.sangita.grantha.shared.domain.support

/**
 * Common transliteration collapse rules used for deduplication and matching.
 * Maps aspirated/retroflex digraphs to a single canonical ASCII form.
 */
object TransliterationCollapse {
    val RULES: List<Pair<String, String>> = listOf(
        "ksh" to "ks",     // raksha → raksa (kṣ variants)
        "chh" to "c",      // achchutam → acutam
        "sh" to "s",       // shankarabharanam → sankarabaranam (ś/ṣ variants)
        "th" to "t",       // dikshithar → diksitar, thiruvarur → tiruvarur
        "dh" to "d",       // dhyana → dyana
        "bh" to "b",       // bhairavi → bairavi
        "ph" to "p",       // phalguni → palguni
        "gh" to "g",       // ghananatam → gananatam
        "jh" to "j",       // jhallaree → jallaree
        "ch" to "c",       // charanam → caranam
    )

    fun collapse(value: String): String {
        var result = value
        for ((from, to) in RULES) {
            result = result.replace(from, to)
        }
        return result
    }
}
