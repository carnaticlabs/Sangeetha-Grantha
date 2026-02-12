"""Tests for Velthuis Devanagari font decoder."""

from src.velthuis_decoder import VelthuisDecoder


class TestVelthuisDecoder:
    """Tests using known raw→decoded mappings from spec Section 7.5."""

    def setup_method(self) -> None:
        self.decoder = VelthuisDecoder()

    # ── Raga/Tala labels ──

    def test_raga_label(self) -> None:
        """rAg\\ → रागं"""
        raw = "rAg\\"
        expected = "रागं"
        assert self.decoder.decode_text(raw) == expected

    def test_tala_label(self) -> None:
        r"""tA\x0f\\ → ताळं"""
        raw = "tA\x0f\\"
        expected = "ताळं"
        assert self.decoder.decode_text(raw) == expected

    def test_tala_name_adi(self) -> None:
        """aAEd → आदि"""
        raw = "aAEd"
        # a=अ, A=ा, E=ि (left-side mātrā, goes after next consonant), d=द
        # After merge: अ+ा=आ; then ि+द → दि
        expected = "आदि"
        assert self.decoder.decode_text(raw) == expected

    # ── Section labels ──

    def test_charanam_label(self) -> None:
        """crZm^ → चरणम्"""
        raw = "crZm^"
        expected = "चरणम्"
        assert self.decoder.decode_text(raw) == expected

    # ── Simple consonant + mātrā sequences ──

    def test_consonant_with_aa_matra(self) -> None:
        """rA → रा"""
        raw = "rA"
        expected = "रा"
        assert self.decoder.decode_text(raw) == expected

    def test_consonant_with_i_matra(self) -> None:
        """Ev → वि (E=ि before v=व, reordered)"""
        raw = "Ev"
        expected = "वि"
        assert self.decoder.decode_text(raw) == expected

    def test_consonant_with_ii_matra(self) -> None:
        """dF → दी"""
        raw = "dF"
        expected = "दी"
        assert self.decoder.decode_text(raw) == expected

    def test_consonant_with_u_matra(self) -> None:
        """p\x01 → पू"""
        raw = "p\x01"
        expected = "पू"
        assert self.decoder.decode_text(raw) == expected

    def test_consonant_with_e_matra(self) -> None:
        """d\x03 → दे"""
        raw = "d\x03"
        expected = "दे"
        assert self.decoder.decode_text(raw) == expected

    # ── Half forms and conjuncts ──

    def test_halfna_ta(self) -> None:
        """\x06t → न्त"""
        raw = "\x06t"
        expected = "न्त"
        assert self.decoder.decode_text(raw) == expected

    def test_halfnna_dda(self) -> None:
        """\x17X → ण्ड"""
        raw = "\x17X"
        expected = "ण्ड"
        assert self.decoder.decode_text(raw) == expected

    def test_ksa_conjunct(self) -> None:
        """byte 34 → क्ष"""
        raw = chr(34)  # "
        expected = "क्ष"
        assert self.decoder.decode_text(raw) == expected

    def test_sh_v_conjunct(self) -> None:
        """byte 152 → श्व"""
        raw = chr(152)
        expected = "श्व"
        assert self.decoder.decode_text(raw) == expected

    def test_sh_r_conjunct(self) -> None:
        """byte 153 → श्र"""
        raw = chr(153)
        expected = "श्र"
        assert self.decoder.decode_text(raw) == expected

    # ── Compound words from spec page 17 ──

    def test_mam_virama(self) -> None:
        """mAm^ → माम्"""
        raw = "mAm^"
        expected = "माम्"
        assert self.decoder.decode_text(raw) == expected

    def test_rk_sa(self) -> None:
        r"""r" → रक्ष (r=र, "=byte34=क्ष)"""
        raw = "r\x22"
        expected = "रक्ष"
        assert self.decoder.decode_text(raw) == expected

    def test_anusvara(self) -> None:
        r"""mA\\ → मां (m=म, A=ा, \\=ं)"""
        raw = "mA\\"
        expected = "मां"
        assert self.decoder.decode_text(raw) == expected

    # ── Vowel merging ──

    def test_a_aa_merge(self) -> None:
        """aA → आ (independent अ + ā-mātrā merges)"""
        raw = "aA"
        expected = "आ"
        assert self.decoder.decode_text(raw) == expected

    # ── Left-side mātrā reordering with clusters ──

    def test_matra_after_halfform_cluster(self) -> None:
        """E\x06t → न्ति (imatra + halfna + ta → reorder to न + ् + त + ि)"""
        raw = "E\x06t"
        expected = "न्ति"
        assert self.decoder.decode_text(raw) == expected

    # ── Font detection ──

    def test_is_velthuis_font(self) -> None:
        assert VelthuisDecoder.is_velthuis_font("Velthuis-dvng10") is True
        assert VelthuisDecoder.is_velthuis_font("Velthuis-dvngi10") is True
        assert VelthuisDecoder.is_velthuis_font("Velthuis-dvng8") is True
        assert VelthuisDecoder.is_velthuis_font("Utopia-Regular") is False
        assert VelthuisDecoder.is_velthuis_font("CMR17") is False

    # ── Edge cases ──

    def test_empty_string(self) -> None:
        assert self.decoder.decode_text("") == ""

    def test_space_preserved(self) -> None:
        """Spaces between words preserved."""
        raw = "rAm r"
        expected = "राम र"
        assert self.decoder.decode_text(raw) == expected

    def test_visarga(self) -> None:
        """, → ः"""
        raw = "d\x03v,"
        expected = "देवः"
        assert self.decoder.decode_text(raw) == expected
