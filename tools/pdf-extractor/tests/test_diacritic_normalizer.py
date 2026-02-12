"""Tests for garbled diacritic normalisation."""

from src.diacritic_normalizer import cleanup_raga_tala_name, normalize_garbled_diacritics


class TestNormalizeGarbledDiacritics:
    """Tests for normalize_garbled_diacritics()."""

    def test_raga_label(self) -> None:
        assert normalize_garbled_diacritics("r\u00AFaga \u02D9m") == "rāgaṁ"

    def test_tala_label(self) -> None:
        # The dot in t¯al.a is consonant-dot (rule 8), now resolved to ḷ
        assert normalize_garbled_diacritics("t\u00AFal.a \u02D9m") == "tāḷaṁ"

    def test_jujavanti(self) -> None:
        assert normalize_garbled_diacritics("juj\u00AFavanti") == "jujāvanti"

    def test_adi(self) -> None:
        assert normalize_garbled_diacritics("\u00AFadi") == "ādi"

    def test_misra_capu(self) -> None:
        assert normalize_garbled_diacritics("mi\u00B4sra c\u00AFapu") == "miśra cāpu"

    def test_nilambari(self) -> None:
        assert normalize_garbled_diacritics("n\u00AFıl\u00AFambari") == "nīlāmbari"

    def test_rupakam(self) -> None:
        assert normalize_garbled_diacritics("r\u00AFupakam") == "rūpakam"

    def test_suddhasaveri(self) -> None:
        assert normalize_garbled_diacritics("\u00B4suddhas\u00AFaveri") == "śuddhasāveri"

    def test_camaram(self) -> None:
        assert normalize_garbled_diacritics("c\u00AFamaram") == "cāmaram"

    def test_dot_above_n(self) -> None:
        assert normalize_garbled_diacritics("sa\u02D9n") == "saṅ"

    def test_tilde_n(self) -> None:
        assert normalize_garbled_diacritics("j\u02DCn\u00AFana") == "jñāna"

    def test_macron_with_space(self) -> None:
        """Macron separated from base char by whitespace."""
        assert normalize_garbled_diacritics("r\u00AF aga") == "rāga"

    def test_no_garbled_chars(self) -> None:
        """Plain text passes through unchanged."""
        assert normalize_garbled_diacritics("raga tala pallavi") == "raga tala pallavi"

    def test_already_iast(self) -> None:
        """Precomposed IAST passes through unchanged."""
        assert normalize_garbled_diacritics("rāga tāla") == "rāga tāla"

    def test_empty_string(self) -> None:
        assert normalize_garbled_diacritics("") == ""

    def test_dotless_i_macron(self) -> None:
        """Dotless i (ı, U+0131) after macron → ī."""
        assert normalize_garbled_diacritics("n\u00AFıla") == "nīla"

    def test_arabhi(self) -> None:
        assert normalize_garbled_diacritics("\u00AFarabhi") == "ārabhi"

    def test_gurjjari(self) -> None:
        """No garbled chars — passes through unchanged."""
        assert normalize_garbled_diacritics("gurjjari") == "gurjjari"


class TestCleanupRagaTalaName:
    """Tests for cleanup_raga_tala_name()."""

    def test_jujavanti_with_mela(self) -> None:
        assert cleanup_raga_tala_name("juj\u00AFavanti (28)") == "Jujāvanti"

    def test_adi(self) -> None:
        assert cleanup_raga_tala_name("\u00AFadi") == "Ādi"

    def test_arabhi_with_mela(self) -> None:
        assert cleanup_raga_tala_name("\u00AFarabhi (29)") == "Ārabhi"

    def test_suddhasaveri_with_mela(self) -> None:
        assert cleanup_raga_tala_name("\u00B4suddhas\u00AFaveri (1)") == "Śuddhasāveri"

    def test_lalita_with_mela(self) -> None:
        assert cleanup_raga_tala_name("lalita (15)") == "Lalita"

    def test_misra_capu(self) -> None:
        assert cleanup_raga_tala_name("mi\u00B4sra c\u00AFapu") == "Miśra Cāpu"

    def test_nilambari_with_mela(self) -> None:
        assert cleanup_raga_tala_name("n\u00AFıl\u00AFambari (29)") == "Nīlāmbari"

    def test_camaram_with_mela(self) -> None:
        assert cleanup_raga_tala_name("c\u00AFamaram (56)") == "Cāmaram"

    def test_rupakam(self) -> None:
        assert cleanup_raga_tala_name("r\u00AFupakam") == "Rūpakam"

    def test_gurjjari_with_mela(self) -> None:
        assert cleanup_raga_tala_name("gurjjari (15)") == "Gurjjari"

    def test_empty_string(self) -> None:
        assert cleanup_raga_tala_name("") == ""

    def test_none_passthrough(self) -> None:
        assert cleanup_raga_tala_name(None) is None  # type: ignore[arg-type]

    def test_already_clean(self) -> None:
        assert cleanup_raga_tala_name("Sankarabharanam") == "Sankarabharanam"


class TestRule8ConsonantDot:
    """Tests for Rule 8: consonant-dot patterns (TRACK-059)."""

    def test_raksa(self) -> None:
        """s. → ṣ in rakṣa."""
        assert normalize_garbled_diacritics("raks.a") == "rakṣa"

    def test_retroflex_n_d(self) -> None:
        """n. d. → ṇḍ (retroflex nasal + retroflex stop)."""
        assert normalize_garbled_diacritics("n. d. e") == "ṇḍe"

    def test_retroflex_t(self) -> None:
        """t. → ṭ."""
        assert normalize_garbled_diacritics("pat.t.") == "paṭṭ"

    def test_retroflex_l(self) -> None:
        """l. → ḷ."""
        assert normalize_garbled_diacritics("mel.a") == "meḷa"

    def test_vocalic_r(self) -> None:
        """r. → ṛ."""
        assert normalize_garbled_diacritics("kr.s.n. a") == "kṛṣṇa"

    def test_visarga(self) -> None:
        """h. → ḥ."""
        assert normalize_garbled_diacritics("namah.") == "namaḥ"

    def test_akhilandesvari_title(self) -> None:
        """Full garbled title → clean IAST (the critical sample)."""
        garbled = "akhil\u00AFan. d. e\u00B4svari raks.a m\u00AFam"
        expected = "akhilāṇḍeśvari rakṣa mām"
        assert normalize_garbled_diacritics(garbled) == expected

    def test_annapurne_visalaksi(self) -> None:
        """Another garbled title with multiple consonant-dots."""
        garbled = "annap\u00AFurn. e vi\u00B4s\u00AFal\u00AFaks.i"
        expected = "annapūrṇe viśālākṣi"
        assert normalize_garbled_diacritics(garbled) == expected

    def test_no_false_positive_decimal(self) -> None:
        """Decimal numbers should not be affected (no letter before dot)."""
        assert normalize_garbled_diacritics("page 3.5") == "page 3.5"

    def test_no_false_positive_abbreviation(self) -> None:
        """Sentence-ending dots after non-matching consonants should be safe."""
        assert normalize_garbled_diacritics("Dr. Smith") == "Dr. Smith"

    def test_combined_macron_and_consonant_dot(self) -> None:
        """Macron + consonant-dot together in one word: t¯al.a → tāḷa."""
        assert normalize_garbled_diacritics("t\u00AFal.a") == "tāḷa"

    def test_mamava_pattabhirama(self) -> None:
        """Title with double retroflex t: pat.t.¯abhir¯ama → paṭṭābhirāma."""
        garbled = "m\u00AFamava pat.t.\u00AFabhir\u00AFama"
        expected = "māmava paṭṭābhirāma"
        assert normalize_garbled_diacritics(garbled) == expected
