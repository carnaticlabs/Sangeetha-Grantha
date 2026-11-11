"""TRACK-149: a footnote digit before the inline P label must not hide the pallavi."""

from src.structure_parser import StructureParser

SOURCE = """paripAlaya paripAlaya-rItigauLa
1 P paripAlaya paripAlaya paripAlaya
raghu nAtha
C1 tanuvE nIk(a)nuvaina sadanamaurA
raghu nAtha (pari)
C2 sthira cittamu vara cAmIkara pIThamu
raghu nAtha (pari)
"""


def _types(text: str) -> list[str]:
    return [str(s.section_type.value) for s in StructureParser().parse(text).sections]


def test_leading_footnote_digit_still_marks_the_pallavi():
    assert _types(SOURCE)[:1] == ["PALLAVI"]
    assert _types(SOURCE).count("CHARANAM") == 2


def test_label_after_the_digit_is_not_part_of_the_lyric():
    sections = StructureParser().parse(SOURCE).sections
    pallavi = next(s for s in sections if s.section_type.value == "PALLAVI")
    assert pallavi.text.startswith("paripAlaya")


def test_footnote_after_the_label_is_unchanged():
    text = SOURCE.replace("1 P paripAlaya", "P 1paripAlaya")
    assert _types(text)[:1] == ["PALLAVI"]


def test_digit_glued_to_the_label_is_also_accepted():
    assert _types(SOURCE.replace("1 P paripAlaya", "1P paripAlaya"))[:1] == ["PALLAVI"]


GLUED = """rAmaM bhajEhaM-sAvEri
1pallavi
rAmam bhaj(E)ham sadA

anupallavi
SyAmam sakala sujana hRdaya jaladhi
sOmam janaka sut(A)bhi(rAmam)

3caraNam
sundara mukham aravinda nayanam agha
bRnda su-bhUshaNa vandita caritam (rAmam)
"""


def test_footnote_digit_glued_to_a_plain_pallavi_label():
    assert _types(GLUED)[:3] == ["PALLAVI", "ANUPALLAVI", "CHARANAM"]


def test_footnote_digit_glued_to_a_plain_caranam_label():
    assert _types(GLUED).count("CHARANAM") == 1


HYPHEN_SVARA = """duDuku gala-gauLa
pallavi
duDuku gala nann(E) dora
koDuku brOcurA entO (duDuku)

anupallavi
kaDu dur-vishay(A)kRshTuDai
gaDiya gaDiyaku niNDAru (duDuku)

svara-sAhitya 1
SrI vanitA hRt-kumud(A)bja
avAG-mAnasa gOcara (duDuku)

svara-sAhitya 2
sakala bhUtamula(y)andu nIvai-
(y)uNDaga mati lEka pOyina (duDuku)
"""


def test_hyphenated_svara_sahitya_stanzas_are_the_charanams_when_no_charanam_heading_exists():
    assert _types(HYPHEN_SVARA) == ["PALLAVI", "ANUPALLAVI", "CHARANAM", "CHARANAM"]


def test_hyphenated_svara_sahitya_stays_when_the_page_has_real_charanams():
    text = HYPHEN_SVARA + "\nsvara-sAhitya 3\nciruta prAyamu nADE (duDuku)\n\ncaraNam\npara dhanamula koraku (duDuku)\n"
    assert "CHARANAM" in _types(text)
    assert _types(text).count("CHARANAM") == 1


def test_a_single_hyphenated_svara_sahitya_header_is_not_promoted():
    text = HYPHEN_SVARA.split("svara-sAhitya 2")[0]
    assert "CHARANAM" not in _types(text)


def test_other_scripts_keep_every_charanam_of_a_hyphenated_svara_sahitya_page():
    page = (
        HYPHEN_SVARA + "\nDevanagari\n\nप. दुडुकु गल न(न्ने) दॊर\nकॊडुकु ब्रोचुरा ऎन्तो (दु)\n\n"
        "अ. कडु दुर्विष(या)कृष्टुडै\nगडिय गडियकु निण्डारु (दु)\n\n"
        "स्वर साहित्य -\n1. श्री वनिता हृत्कुमु(दा)ब्ज\nअवाङ्-मानस गोचर (दु)\n\n"
        "2. सकल भूतमुल(य)न्दु नीवै-\n(यु)ण्डग मति लेक पोयिन (दु)\n"
    )
    result = StructureParser().parse(page)
    devanagari = next(v for v in result.lyric_variants if v.script.lower().startswith("deva"))
    assert [s.section_type.value for s in devanagari.sections] == ["PALLAVI", "ANUPALLAVI", "CHARANAM", "CHARANAM"]
