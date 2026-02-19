"""Velthuis Devanagari font decoder for Sanskrit PDF extraction.

The Velthuis-dvng* TeX fonts used in scholarly Sanskrit PDFs (e.g.
guruguha.org mdskt.pdf) have no /ToUnicode CMap.  PyMuPDF extracts raw
byte values that correspond to glyph codes in the font's internal encoding.

This module provides a decoder that maps those byte values to Unicode
Devanagari using the encoding vector extracted from the embedded Type 1
font program (155 entries).  It also handles left-side mātrā reordering
and basic vowel merging.

Reference: spec Section 7.4 (encoding map), 7.5 (proof-of-concept decode).
"""

from __future__ import annotations

import logging
import re
import zlib
from typing import Optional

logger = logging.getLogger(__name__)


# ─── Static glyph→Unicode mapping ───────────────────────────────────────
#
# Built from the encoding vector in the Velthuis-dvng10 font program.
# Key: byte value (0–255)   Value: Unicode string

_GLYPH_MAP: dict[int, str] = {
    # ── Vowels (independent) ──
    97:  "\u0905",  # a  → अ
    105: "\u0907",  # i  → इ
    117: "\u0909",  # u  → उ
    101: "\u090F",  # e  → ए

    # ── Mātrās (dependent vowel signs) ──
    65:  "\u093E",  # A  → ा (ā-mātrā)
    69:  "\u093F",  # E  → ि (i-mātrā) — left-side, needs reordering
    70:  "\u0940",  # F  → ी (ī-mātrā)
    0:   "\u0941",  # \x00 → ु (u-mātrā)
    1:   "\u0942",  # \x01 → ू (ū-mātrā)
    3:   "\u0947",  # \x03 → े (e-mātrā)
    111: "\u094B",  # o  → ो (o-mātrā)
    123: "\u0948",  # {  → ै (ai-mātrā)
    79:  "\u094C",  # O  → ौ (au-mātrā)
    2:   "\u0943",  # \x02 → ृ (ṛ-mātrā)

    # ── Consonants ──
    107: "\u0915",  # k → क
    75:  "\u0916",  # K → ख
    103: "\u0917",  # g → ग
    71:  "\u0918",  # G → घ
    99:  "\u091A",  # c → च
    67:  "\u091B",  # C → छ
    106: "\u091C",  # j → ज
    74:  "\u091D",  # J → झ
    86:  "\u091F",  # V → ट
    87:  "\u0920",  # W → ठ
    88:  "\u0921",  # X → ड
    89:  "\u0922",  # Y → ढ
    90:  "\u0923",  # Z → ण
    116: "\u0924",  # t → त
    84:  "\u0925",  # T → थ
    100: "\u0926",  # d → द
    68:  "\u0927",  # D → ध
    110: "\u0928",  # n → न
    112: "\u092A",  # p → प
    80:  "\u092B",  # P → फ
    98:  "\u092C",  # b → ब
    66:  "\u092D",  # B → भ
    109: "\u092E",  # m → म
    121: "\u092F",  # y → य
    114: "\u0930",  # r → र
    108: "\u0932",  # l → ल
    15:  "\u0933",  # \x0f → ळ (retroflex ḷa, DEVANAGARI LETTER LLA)
    118: "\u0935",  # v → व
    102: "\u0936",  # f → श (śa)
    113: "\u0937",  # q → ष (ṣa)
    115: "\u0938",  # s → स
    104: "\u0939",  # h → ह

    # ── Half forms (consonant + virāma) ──
    6:   "\u0928\u094D",  # halfna  → न्
    23:  "\u0923\u094D",  # halfnna → ण्
    91:  "\u0936\u094D",  # halfsha → श्
    77:  "\u092E\u094D",  # halfma  → म्
    5:   "\u092F\u094D",  # halfya  → य्
    4:   "\u0930\u094D",  # halfra  → र्
    7:   "\u0932\u094D",  # halfla  → ल्
    8:   "\u0924\u094D",  # halfta  → त्
    9:   "\u0926\u094D",  # halfda  → द्
    10:  "\u0915\u094D",  # halfka  → क्
    11:  "\u0917\u094D",  # halfga  → ग्
    12:  "\u092A\u094D",  # halfpa  → प्
    14:  "\u092C\u094D",  # halfba  → ब्
    16:  "\u0935\u094D",  # halfva  → व्
    17:  "\u091A\u094D",  # halfca  → च्
    18:  "\u091C\u094D",  # halfja  → ज्
    19:  "\u0938\u094D",  # halfsa  → स्
    20:  "\u0939\u094D",  # halfha  → ह्
    21:  "\u0937\u094D",  # halfssa → ष्
    22:  "\u0928\u094D",  # halfna2 → न् (alternate)

    # ── Conjunct ligatures ──
    34:  "\u0915\u094D\u0937",  # ksa   → क्ष
    152: "\u0936\u094D\u0935",  # sh_v  → श्व
    153: "\u0936\u094D\u0930",  # sh_r  → श्र
    165: "\u0932\u094D\u0932",  # l_l   → ल्ल
    163: "\u0937\u094D\u091F",  # ss_tt → ष्ट
    226: "\u091C\u094D\u091E",  # j_ny  → ज्ञ
    129: "\u0924\u094D\u0924",  # t_t   → त्त
    130: "\u0926\u094D\u0926",  # d_d   → द्द
    131: "\u0926\u094D\u0927",  # d_dh  → द्ध
    132: "\u0926\u094D\u0935",  # d_v   → द्व
    133: "\u0926\u094D\u092F",  # d_y   → द्य
    134: "\u0939\u094D\u0928",  # h_n   → ह्न
    135: "\u0939\u094D\u092E",  # h_m   → ह्म
    136: "\u0939\u094D\u092F",  # h_y   → ह्य
    137: "\u0939\u094D\u0930",  # h_r   → ह्र
    138: "\u0939\u094D\u0932",  # h_l   → ह्ल
    139: "\u0939\u094D\u0935",  # h_v   → ह्व
    164: "\u0924\u094D\u0930",  # t_r   → त्र
    166: "\u0928\u094D\u0928",  # n_n   → न्न
    167: "\u0928\u094D\u0926",  # n_d   → न्द
    168: "\u0928\u094D\u0927",  # n_dh  → न्ध

    # ── Special markers ──
    92:  "\u0902",  # \  → ं (anusvāra)
    94:  "\u094D",  # ^  → ् (virāma / halant)
    44:  "\u0903",  # ,  → ः (visarga)
    13:  "\u0930\u094D",  # \r → र् (repha — superscript r)

    # ── Numerals ──
    # Devanagari digits are usually rendered in Utopia (Latin digits), not
    # Velthuis, but include mappings for completeness
    48:  "0",  49:  "1",  50:  "2",  51:  "3",  52:  "4",
    53:  "5",  54:  "6",  55:  "7",  56:  "8",  57:  "9",

    # ── Punctuation (usually from CMR17 font, not Velthuis) ──
    32:  " ",   # space
    40:  "(",   # open paren
    41:  ")",   # close paren
    58:  ":",   # colon
    46:  ".",   # period / danda placeholder
}

# Characters that are left-side mātrās in Devanagari (drawn before
# consonant visually, but encoded after consonant in Unicode)
_LEFT_MATRAS = {"\u093F"}  # ि (i-mātrā)

# Devanagari consonant range (U+0915–U+0939) and virāma
_CONSONANT_RANGE = range(0x0915, 0x093A)
_VIRAMA = "\u094D"


def _is_consonant(ch: str) -> bool:
    return len(ch) == 1 and ord(ch) in _CONSONANT_RANGE


def _is_half_form(s: str) -> bool:
    """Check if a string is a half-form (consonant + virāma)."""
    return len(s) == 2 and _is_consonant(s[0]) and s[1] == _VIRAMA


class VelthuisDecoder:
    """Decode text from Velthuis-dvng* TeX fonts to Unicode Devanagari."""

    def __init__(self) -> None:
        self._glyph_map = dict(_GLYPH_MAP)

    def decode_text(self, raw_text: str) -> str:
        """Decode a raw text string from Velthuis font to Unicode Devanagari.

        Args:
            raw_text: Text as extracted by PyMuPDF from a Velthuis-font span.

        Returns:
            Unicode Devanagari string with mātrā reordering applied.
        """
        # Step 1: Map each byte/char to its Unicode equivalent
        decoded_parts: list[str] = []
        for ch in raw_text:
            code = ord(ch)
            if code in self._glyph_map:
                decoded_parts.append(self._glyph_map[code])
            else:
                # Unknown glyph — pass through (could be punctuation from
                # another font that bled into this span)
                decoded_parts.append(ch)

        result = "".join(decoded_parts)

        # Step 2: Reorder left-side mātrās
        result = self._reorder_matras(result)

        # Step 3: Merge independent vowel + mātrā sequences
        result = self._merge_vowels(result)

        return result

    def _reorder_matras(self, text: str) -> str:
        """Reorder left-side mātrā (ि) to appear after its consonant cluster.

        In Velthuis font encoding, i-mātrā appears BEFORE the consonant
        in the byte stream (visual order).  Unicode requires it AFTER.

        For consonant clusters like न्ति (halfna + ta + imatra), the
        mātrā needs to be placed after the entire cluster.
        """
        result: list[str] = list(text)
        i = 0
        while i < len(result):
            if result[i] in _LEFT_MATRAS:
                matra = result[i]
                # Find the end of the consonant cluster following the mātrā
                j = i + 1
                while j < len(result):
                    ch = result[j]
                    if _is_consonant(ch):
                        # Check if next char is virāma (making this a half-form)
                        if j + 1 < len(result) and result[j + 1] == _VIRAMA:
                            j += 2  # Skip consonant + virāma, continue cluster
                        else:
                            j += 1  # Final consonant of cluster
                            break
                    else:
                        break

                if j > i + 1:
                    # Move mātrā to after the consonant cluster
                    del result[i]
                    result.insert(j - 1, matra)
                else:
                    i += 1
                    continue
            i += 1

        return "".join(result)

    def _merge_vowels(self, text: str) -> str:
        """Merge independent vowel अ + ā-mātrā ा → आ.

        When a vowel and its mātrā appear in separate spans, the decoder
        produces अा instead of आ.
        """
        # अ + ा → आ
        text = text.replace("\u0905\u093E", "\u0906")
        # इ + ी → ई  (less common but possible)
        text = text.replace("\u0907\u0940", "\u0908")
        # उ + ू → ऊ
        text = text.replace("\u0909\u0942", "\u090A")
        return text

    @staticmethod
    def is_velthuis_font(font_name: str) -> bool:
        """Check if a font name indicates a Velthuis Devanagari font."""
        return font_name.lower().startswith("velthuis-dvng")

    @staticmethod
    def extract_encoding_from_pdf(pdf_path: str, xref: int) -> dict[int, str] | None:
        """Extract encoding vector from a Type 1 font stream (optional).

        This can be used to validate or extend the hardcoded mapping.
        Returns None if extraction fails.

        Args:
            pdf_path: Path to PDF file.
            xref: Cross-reference number of the font stream object.

        Returns:
            Dict mapping byte positions to glyph names, or None.
        """
        try:
            import fitz
            doc = fitz.open(pdf_path)
            stream = doc.xref_stream_raw(xref)
            decompressed = zlib.decompress(stream)
            # First ~4200 bytes contain the encoding vector
            header = decompressed[:5000].decode("latin-1")
            entries = re.findall(r"dup\s+(\d+)\s+/(\S+)\s+put", header)
            doc.close()
            return {int(pos): name for pos, name in entries} if entries else None
        except Exception:
            logger.debug("Could not extract encoding from xref %d", xref, exc_info=True)
            return None
