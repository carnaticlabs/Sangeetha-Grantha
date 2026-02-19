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
