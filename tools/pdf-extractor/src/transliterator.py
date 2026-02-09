"""Indic script transliteration wrapper.

Provides automated conversion between scripts (Devanagari, Tamil, Telugu,
Kannada, Malayalam, Latin/IAST) using the indic-transliteration library.
"""

from __future__ import annotations

import logging
from typing import Optional

logger = logging.getLogger(__name__)

# Script name → indic_transliteration constant mapping
SCRIPT_MAP: dict[str, str] = {
    "devanagari": "DEVANAGARI",
    "tamil": "TAMIL",
    "telugu": "TELUGU",
    "kannada": "KANNADA",
    "malayalam": "MALAYALAM",
    "latin": "IAST",
    "iast": "IAST",
    "kolkata": "KOLKATA",
    "slp1": "SLP1",
}


class Transliterator:
    """Convert text between Indic scripts using indic-transliteration."""

    def __init__(self) -> None:
        """Initialize the transliterator, importing the library."""
        try:
            from indic_transliteration import sanscript
            from indic_transliteration.sanscript import transliterate

            self._sanscript = sanscript
            self._transliterate = transliterate
            self._available = True
        except ImportError:
            logger.warning("indic-transliteration not installed; transliteration unavailable")
            self._available = False

    @property
    def is_available(self) -> bool:
        """Whether the transliteration library is available."""
        return self._available

    def transliterate(
        self,
        text: str,
        from_script: str,
        to_script: str,
    ) -> Optional[str]:
        """Convert text from one script to another.

        Args:
            text: Input text in the source script.
            from_script: Source script name (devanagari, tamil, telugu, etc.)
            to_script: Target script name.

        Returns:
            Transliterated text, or None if conversion failed.
        """
        if not self._available:
            logger.error("Transliteration library not available")
            return None

        from_key = SCRIPT_MAP.get(from_script.lower())
        to_key = SCRIPT_MAP.get(to_script.lower())

        if not from_key or not to_key:
            logger.error(
                "Unknown script",
                extra={"from_script": from_script, "to_script": to_script},
            )
            return None

        try:
            from_const = getattr(self._sanscript, from_key)
            to_const = getattr(self._sanscript, to_key)
            result = self._transliterate(text, from_const, to_const)
            return result
        except Exception:
            logger.exception(
                "Transliteration failed",
                extra={"from_script": from_script, "to_script": to_script},
            )
            return None

    def detect_script(self, text: str) -> Optional[str]:
        """Attempt to detect the script of the given text.

        Returns the script name (devanagari, tamil, etc.) or None.
        """
        if not text.strip():
            return None

        # Simple Unicode block-based detection
        for char in text:
            cp = ord(char)
            if 0x0900 <= cp <= 0x097F:
                return "devanagari"
            if 0x0B80 <= cp <= 0x0BFF:
                return "tamil"
            if 0x0C00 <= cp <= 0x0C7F:
                return "telugu"
            if 0x0C80 <= cp <= 0x0CFF:
                return "kannada"
            if 0x0D00 <= cp <= 0x0D7F:
                return "malayalam"
            if 0x0041 <= cp <= 0x024F:
                return "latin"

        return None
