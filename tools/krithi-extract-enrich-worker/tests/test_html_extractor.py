from src.html_extractor import HtmlTextExtractor


def test_extracts_main_content_and_links() -> None:
    html = """
    <html>
      <head><title>Sample Krithi</title></head>
      <body>
        <nav>Navigation</nav>
        <div class="post-body">
          <p>Pallavi</p>
          <p>Line 1<br>Line 2</p>
          <a href="/lyrics">Lyrics Link</a>
        </div>
        <footer>Footer</footer>
      </body>
    </html>
    """
    extractor = HtmlTextExtractor(max_chars=10_000)
    result = extractor.extract(html, base_url="https://example.com/page")

    assert result.title == "Sample Krithi"
    assert "Pallavi" in result.text
    assert "Line 1" in result.text
    assert "Line 2" in result.text
    assert "Lyrics Link (https://example.com/lyrics)" in result.text
    assert "Navigation" not in result.text
    assert "Footer" not in result.text


def test_guru_guha_blog_navigation_stripped() -> None:
    """TRACK-097: guru-guha.blogspot.com pages have language navigation tables
    and 'Meaning of Kriti' links that must be stripped to prevent false metadata
    boundary truncation."""
    html = """
    <html>
      <head><title>Dikshitar Kriti - abhayAmbA - Raga Kalyaani</title></head>
      <body>
        <div class="post-body">
          <h3>abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi</h3>
          <a href="/meanings/1">Meaning of Kriti-1</a>
          <a name="#English"></a>
          <h3>English</h3>
          <table id="English">
            <tr>
              <td><a href="#English">English</a></td>
              <td><a href="#Devanagari">Devanagari</a></td>
              <td><a href="#Tamil">Tamil</a></td>
            </tr>
          </table>
          <span>
            pallavi<br>
            abhayAmbA jagadambA nija dAsam mAm pAlaya<br><br>
            anupallavi<br>
            abhaya varada haste hastinAtirUpa prabhAve<br><br>
            caraNam<br>
            mUla prakRti svarUpiNi mUlAdhAra nilaye<br>
          </span>
        </div>
      </body>
    </html>
    """
    extractor = HtmlTextExtractor(max_chars=10_000)
    result = extractor.extract(html, base_url="http://guru-guha.blogspot.com/page.html")

    # Navigation table should be stripped
    assert "English | Devanagari" not in result.text or "English\nDevanagari" not in result.text
    # "Meaning of Kriti-1" navigation link should be stripped
    assert "Meaning of Kriti" not in result.text
    # Actual lyrics should be preserved
    assert "pallavi" in result.text
    assert "abhayAmbA jagadambA nija dAsam" in result.text
    assert "anupallavi" in result.text
    assert "caraNam" in result.text


def test_structure_parser_no_false_meaning_boundary() -> None:
    """TRACK-097: Even if 'Meaning of Kriti' text survives extraction,
    the structure parser should not treat it as a metadata boundary."""
    from src.structure_parser import StructureParser

    text = """abhayAmbA jagadambA - rAgaM kalyANi - tALaM Adi
Meaning of Kriti-1
English
pallavi
abhayAmbA jagadambA nija dAsam mAm pAlaya

anupallavi
abhaya varada haste hastinAtirUpa prabhAve

caraNam
mUla prakRti svarUpiNi mUlAdhAra nilaye

Meaning
This krithi is about Goddess Abhayamba
"""
    parser = StructureParser()
    result = parser.parse(text)

    # Should find real sections, not truncate at "Meaning of Kriti"
    assert len(result.sections) >= 3, f"Expected ≥3 sections, got {len(result.sections)}"
    section_types = [s.section_type.value for s in result.sections]
    assert "PALLAVI" in section_types
    assert "ANUPALLAVI" in section_types
    assert "CHARANAM" in section_types

    # Real "Meaning" boundary at the end should still be detected
    assert len(result.metadata_boundaries) >= 1
    meaning_boundaries = [b for b in result.metadata_boundaries if b.label == "MEANING"]
    assert len(meaning_boundaries) == 1, "Should detect the real Meaning section"
    # The real Meaning boundary should NOT be at offset ~48
    assert meaning_boundaries[0].start_pos > 200


def test_falls_back_to_body_when_selector_missing() -> None:
    html = """
    <html>
      <head><title>Fallback Page</title></head>
      <body>
        <p>Anandamrta-karshini</p>
      </body>
    </html>
    """
    extractor = HtmlTextExtractor()
    result = extractor.extract(html)

    assert result.title == "Fallback Page"
    assert "Anandamrta-karshini" in result.text
