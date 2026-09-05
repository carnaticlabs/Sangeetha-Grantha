# TRACK-133 pallavi-echo fixtures

Source: [Thyagaraja Vaibhavam — Ramaa Ramana Rara](https://thyagaraja-vaibhavam.blogspot.com/2008/03/thyagaraja-kriti-ramaa-ramana-rara-raga.html), fetched 2026-09-05.

- `rama_ramana_rara_explicit_charanams.txt` contains the six lyric blocks from
  `HtmlTextExtractor.extract` → `normalize_garbled_diacritics`. Navigation,
  pronunciation guides, and commentary are omitted. The English language header
  is added for the language-block parser path. All original section markers remain.
- `rama_ramana_rara_internal_echo.txt` is the same text with only the four C5
  prefixes (`च5.`, `చ5.`, `ಚ5.`, `ച5.`) removed. This reproduces the merged C4+C5
  shape documented at the end of TRACK-133; it is **not** an unmodified historical
  source capture. No lyric text, closing refrain, or English/Tamil marker is changed.
- `rama_ramana_rara_echo.expected.json` records exact type, order, label, and text
  from the **pre-fix** parser on the complete downloaded HTML. Every language has
  P + 6C. The live source currently includes explicit C5 markers and already parsed
  correctly before this fix; the missing-marker fixture exercises the recovery.

`tests/test_pallavi_echo_split.py` uses these fixtures for both ordinary language
blocks and Indic blocks after a metadata boundary. Negative cases cover ordinary
refrains, inline parentheses, non-charanam blocks, and ambiguous deficits.

Downloaded HTML SHA-256:
`4c6199883b2a9a2c94db7c45a5602e7511ca7c420da9be6bf840c3681e21f3be`.
