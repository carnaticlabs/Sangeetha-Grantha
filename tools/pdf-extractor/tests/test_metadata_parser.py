from src.metadata_parser import MetadataParser


def test_html_title_normalization_strips_blog_prefix_and_raga_suffix() -> None:
    parser = MetadataParser()
    metadata = parser.parse(
        header_text="pallavi\nakhilandesvari raksha mam",
        title_hint="Guru Guha Vaibhavam: Dikshitar Kriti - Akhilandesvari Raksha Mam - Raga Jujavanti",
    )

    assert metadata.title == "Akhilandesvari Raksha Mam"


def test_html_title_normalization_leaves_regular_titles_unchanged() -> None:
    parser = MetadataParser()
    metadata = parser.parse(
        header_text="pallavi\nakhilandesvari raksha mam",
        title_hint="akhilāṇḍeśvari rakṣa mām",
    )

    assert metadata.title == "akhilāṇḍeśvari rakṣa mām"


def test_title_normalization_strips_leading_serial_numbers() -> None:
    parser = MetadataParser()
    metadata = parser.parse(
        header_text="pallavi\nअखिलाण्डेश्वरी रक्ष माम",
        title_hint="१ अखिलाण्डेश्वरी रक्ष माम",
    )

    assert metadata.title == "अखिलाण्डेश्वरी रक्ष माम"


def test_extracts_inline_raga_tala_from_blog_line_without_colon() -> None:
    parser = MetadataParser()
    metadata = parser.parse(
        header_text=(
            "ardha nArISvaram\n"
            "ardha nArISvaram - rAgaM kumudakriyA - tALaM - rUpakaM"
        ),
        title_hint="Dikshitar Kriti - Ardha Naareesvaram - Raga Kumuda Kriya",
    )

    assert metadata.raga == "Kumudakriya"
    assert metadata.tala == "Rupakam"


def test_uses_first_line_metadata_when_title_hint_present() -> None:
    parser = MetadataParser()
    metadata = parser.parse(
        header_text=(
            "ambA nIlAyatAkshi - rAgaM nIlAmbari - tALaM Adi\n"
            "pallavi\n"
            "ambA nIlAyatAkshi karuNA kaTAkshi"
        ),
        title_hint="Guru Guha Vaibhavam: Dikshitar Kriti - Amba Neelaayathaakshi Karunaa - Raga Neelambari",
    )

    assert metadata.title == "ambA nIlAyatAkshi"
    assert metadata.raga == "Nilambari"
    assert metadata.tala == "Adi"
