"""Tests for the canonical extraction schema validation."""

from pydantic import ValidationError

from src.schema import (
    CanonicalExtraction,
    CanonicalIdentityCandidate,
    CanonicalIdentityCandidates,
    CanonicalLyricSection,
    CanonicalLyricVariant,
    CanonicalMetadataBoundary,
    CanonicalMetadataEnrichment,
    CanonicalRaga,
    CanonicalSection,
    ExtractionMethod,
    MusicalForm,
    SectionType,
)


def test_minimal_extraction() -> None:
    """A minimal valid extraction with required fields only."""
    extraction = CanonicalExtraction(
        title="Sri Nathadi",
        composer="Muthuswami Dikshitar",
        ragas=[CanonicalRaga(name="Sankarabharanam")],
        tala="Adi",
        source_url="https://guruguha.org/mdskt.pdf",
        source_name="guruguha.org",
        source_tier=1,
        extraction_method=ExtractionMethod.PDF_PYMUPDF,
    )
    assert extraction.title == "Sri Nathadi"
    assert extraction.composer == "Muthuswami Dikshitar"
    assert extraction.musical_form == MusicalForm.KRITHI
    assert len(extraction.ragas) == 1
    assert extraction.source_tier == 1


def test_full_extraction_with_sections() -> None:
    """A complete extraction with sections and lyric variants."""
    extraction = CanonicalExtraction(
        title="Vatapi Ganapatim Bhaje",
        alternate_title="Vatapi Ganapatim",
        composer="Muthuswami Dikshitar",
        musical_form=MusicalForm.KRITHI,
        ragas=[CanonicalRaga(name="Hamsadhvani", order=1)],
        tala="Adi",
        sections=[
            CanonicalSection(type=SectionType.PALLAVI, order=1, label="Pallavi"),
            CanonicalSection(type=SectionType.ANUPALLAVI, order=2, label="Anupallavi"),
            CanonicalSection(type=SectionType.SAMASHTI_CHARANAM, order=3, label="Samashti Charanam"),
        ],
        lyric_variants=[
            CanonicalLyricVariant(
                language="sa",
                script="devanagari",
                sections=[
                    CanonicalLyricSection(section_order=1, text="वातापि गणपतिं भजेऽहम्"),
                    CanonicalLyricSection(section_order=2, text="भूतादि संसेवित चरणम्"),
                    CanonicalLyricSection(section_order=3, text="हरिहर पुत्रम्"),
                ],
            )
        ],
        metadata_boundaries=[
            CanonicalMetadataBoundary(label="MEANING", start_offset=120, end_offset=127),
        ],
        deity="Ganapati",
        temple="Vatapi",
        source_url="https://guruguha.org/mdskt.pdf",
        source_name="guruguha.org",
        source_tier=1,
        extraction_method=ExtractionMethod.PDF_PYMUPDF,
        page_range="42-43",
        checksum="abc123def456",
    )

    assert len(extraction.sections) == 3
    assert extraction.sections[2].type == SectionType.SAMASHTI_CHARANAM
    assert len(extraction.lyric_variants) == 1
    assert len(extraction.lyric_variants[0].sections) == 3
    assert len(extraction.metadata_boundaries) == 1
    assert extraction.metadata_boundaries[0].label == "MEANING"
    assert extraction.deity == "Ganapati"


def test_json_serialization_camel_case() -> None:
    """Verify that JSON output uses camelCase for Kotlin interop."""
    extraction = CanonicalExtraction(
        title="Test",
        composer="Test Composer",
        ragas=[CanonicalRaga(name="TestRaga")],
        tala="Adi",
        source_url="http://example.com",
        source_name="test",
        source_tier=5,
        extraction_method=ExtractionMethod.HTML_JSOUP,
    )
    json_dict = extraction.to_json_dict()
    assert "sourceUrl" in json_dict
    assert "sourceName" in json_dict
    assert "sourceTier" in json_dict
    assert "extractionMethod" in json_dict
    assert "musicalForm" in json_dict
    assert "metadataBoundaries" in json_dict
    # Ensure snake_case is NOT in the output
    assert "source_url" not in json_dict
    assert "source_name" not in json_dict


def test_source_tier_validation() -> None:
    """Source tier must be between 1 and 5."""
    import pytest

    with pytest.raises(ValidationError):
        CanonicalExtraction(
            title="Test",
            composer="Test",
            ragas=[CanonicalRaga(name="Test")],
            tala="Adi",
            source_url="http://example.com",
            source_name="test",
            source_tier=0,  # Invalid
            extraction_method=ExtractionMethod.MANUAL,
        )

    with pytest.raises(ValidationError):
        CanonicalExtraction(
            title="Test",
            composer="Test",
            ragas=[CanonicalRaga(name="Test")],
            tala="Adi",
            source_url="http://example.com",
            source_name="test",
            source_tier=6,  # Invalid
            extraction_method=ExtractionMethod.MANUAL,
        )


def test_ragamalika_extraction() -> None:
    """Extraction with multiple ragas (ragamalika composition)."""
    extraction = CanonicalExtraction(
        title="Ragamalika Test",
        composer="Test Composer",
        musical_form=MusicalForm.KRITHI,
        ragas=[
            CanonicalRaga(name="Sankarabharanam", order=1, section="Pallavi"),
            CanonicalRaga(name="Kalyani", order=2, section="Anupallavi"),
            CanonicalRaga(name="Kharaharapriya", order=3, section="Charanam"),
        ],
        tala="Adi",
        source_url="http://example.com",
        source_name="test",
        source_tier=3,
        extraction_method=ExtractionMethod.HTML_JSOUP_GEMINI,
    )
    assert len(extraction.ragas) == 3
    assert extraction.ragas[1].section == "Anupallavi"


def test_identity_candidates_and_metadata_enrichment_serialization() -> None:
    extraction = CanonicalExtraction(
        title="Akhilandesvari",
        composer="Unknown",
        ragas=[CanonicalRaga(name="Unknown")],
        tala="Unknown",
        source_url="https://example.com",
        source_name="fixture",
        source_tier=5,
        extraction_method=ExtractionMethod.HTML_JSOUP,
        identity_candidates=CanonicalIdentityCandidates(
            composers=[
                CanonicalIdentityCandidate(
                    entity_id="composer-1",
                    name="Muttuswami Dikshitar",
                    score=97,
                    confidence="HIGH",
                    matched_on="alias",
                )
            ],
            ragas=[
                CanonicalIdentityCandidate(
                    entity_id="raga-1",
                    name="Dwijavanti",
                    score=93,
                    confidence="HIGH",
                    matched_on="canonical",
                )
            ],
        ),
        metadata_enrichment=CanonicalMetadataEnrichment(
            provider="google-genai",
            model="gemini-2.5-flash",
            applied=True,
            confidence=0.91,
            fields_updated=["composer", "raga"],
        ),
    )

    payload = extraction.to_json_dict()
    assert payload["identityCandidates"]["composers"][0]["entityId"] == "composer-1"
    assert payload["metadataEnrichment"]["provider"] == "google-genai"
    assert payload["metadataEnrichment"]["fieldsUpdated"] == ["composer", "raga"]
