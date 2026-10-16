#!/usr/bin/env python3
"""Populate the TRACK-147 Tyagaraja classification master, snapshot, concordance, and manifest.

Reads:
  - database/data/track147-tyagaraja-master.json
  - output/track-144/pallavis.json
  - database/data/track144-tala-evidence-tyagaraja-complete.json
  - output/track-144/source-cache/*.html

Writes:
  - database/data/track147-catalogue-snapshot.json
  - database/data/track147-tyagaraja-master.json
  - database/data/track147-tyagaraja-application-manifest.json
  - application_documentation/01-requirements/features/tyagaraja-classification-master.md
"""

from __future__ import annotations

import hashlib
import json
import os
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from src.tyagaraja_classification import (
    GHANA_GROUP_VALUE,
    Approval,
    ClaimAxis,
    DramaticItemType,
    EvidenceKind,
    GroupType,
    ReviewState,
    SubjectKind,
    TyagarajaClassificationMaster,
    Witness,
    WitnessStatus,
    assert_track147_census,
    build_application_manifest,
    derive_geography_state,
    dump_master,
    render_markdown,
    verify_source,
)

ROOT = Path(__file__).resolve().parents[3]
MASTER_PATH = ROOT / "database" / "data" / "track147-tyagaraja-master.json"
SNAPSHOT_PATH = ROOT / "database" / "data" / "track147-catalogue-snapshot.json"
MANIFEST_PATH = ROOT / "database" / "data" / "track147-tyagaraja-application-manifest.json"
MARKDOWN_PATH = (
    ROOT / "application_documentation" / "01-requirements" / "features" / "tyagaraja-classification-master.md"
)
PALLAVIS_PATH = ROOT / "output" / "track-144" / "pallavis.json"
T144_DOSSIER_PATH = ROOT / "database" / "data" / "track144-tala-evidence-tyagaraja-complete.json"
CACHE_DIR = ROOT / "output" / "track-144" / "source-cache"

DEFERRAL_REASON = (
    "TRACK-147 completion (2026-10-15): approved from verified snapshot/witness sahitya, "
    "but no reviewed cycle membership row carries this composition in R__seed_11. "
    "Database and search-header application deferred to TRACK-149."
)

# 5 Approved Ghana-raga Pancharatnam compositions
GHANA_APPROVED: dict[str, dict[str, Any]] = {
    "2e115182-dcc2-4a41-962c-8381821a7489": {
        "order": 1,
        "raga": "Nāṭṭai",
        "excerpt": "jagadAnanda kAraka jaya jAnakI prANa nAyaka",
        "theme": "Rama Bhakti & Nama Mahima",
        "note": (
            "Approved canonical Ghana-raga Pancharatnam membership (Nattai, Aradhana order 1) "
            "verified against fetched witness and catalogue snapshot."
        ),
    },
    "b4029df0-a209-470b-ba56-3d64fa8aebc4": {
        "order": 2,
        "raga": "Gowla",
        "excerpt": "duDuku gala nannE dora koDuku brOcuentO",
        "theme": "Ethical Reflection & Self-Reproach",
        "note": (
            "Approved canonical Ghana-raga Pancharatnam membership (Gowla, Aradhana order 2) "
            "verified against fetched witness and catalogue snapshot."
        ),
    },
    "6fd7860c-0e5d-428f-af9f-cd02c3ad2da8": {
        "order": 3,
        "raga": "Ārabhi",
        "excerpt": "sAdhincenE O manasA bodhincina sanmArgavacanamula",
        "theme": "Narrative Allusion & Leela Praise",
        "note": (
            "Approved canonical Ghana-raga Pancharatnam membership (Arabhi, Aradhana order 3, "
            "7 Swara-Sahitya sections) verified against fetched witness and catalogue snapshot."
        ),
    },
    "d59984e7-fabc-4536-b5a2-60e550277626": {
        "order": 4,
        "raga": "Varāli",
        "excerpt": "kana kana rucirA kanaka vasana ninnu",
        "theme": "Rama Bhakti & Darshana Bliss",
        "note": (
            "Approved canonical Ghana-raga Pancharatnam membership (Varali, Aradhana order 4) "
            "verified against fetched witness and catalogue snapshot."
        ),
    },
    "2b65609e-9fbf-4232-baae-096cafdf4e79": {
        "order": 5,
        "raga": "Sri",
        "excerpt": "endarO mahAnubhAvul-(a)ndariki vandanamu",
        "theme": "Saints and Teachers & Nadopasana",
        "note": (
            "Approved canonical Ghana-raga Pancharatnam membership (Sri, Aradhana order 5) "
            "verified against fetched witness and catalogue snapshot."
        ),
    },
}

# Hallmark non-cycle approved compositions (application="deferred")
DEFERRED_APPROVED: dict[str, dict[str, Any]] = {
    "73b44194-00c1-4753-8c8a-924d9fa3e18b": {
        "deity": "Rama",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "svara rAga sudhA rasa yuta bhakti svarg( 1 A)pavargamurA O manasA",
        "note": "Approved Nadopasana & Sangita Sastra hallmark claim attested in snapshot pallavi.",
    },
    "6c9d0ab2-70a2-4c6b-a0df-c4a925bf8ae9": {
        "deity": "Siva (Sankara)",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "nAda tanum-aniSam Sankaram namAmi mE manasA SirasA",
        "note": "Approved Shiva addressee and Nadopasana claim attested in snapshot pallavi.",
    },
    "f0fd6665-d378-48fd-8772-6610a997c34d": {
        "deity": "Siva, Vishnu & Brahma (Trimurti)",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "1 nAd(O)pAsanacE Sankara nArAyaNa vidhulu velasiri O manasA",
        "note": "Approved Nadopasana claim attested in snapshot pallavi.",
    },
    "da47bd3e-75ff-4227-8d7f-174aa3bb0d87": {
        "deity": "Rama",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "sangIta SAstra jnAnamu 1 sArUpya saukhyadamE manasA",
        "note": "Approved Musical Knowledge (Sangita Sastra Jnanamu) claim attested in snapshot pallavi.",
    },
    "d4a411e4-f029-46c9-8aad-699e2d235cae": {
        "deity": "Rama",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "rAga sudhA rasa pAnamu jEsi 1 ranjillavE 2 O manasA",
        "note": "Approved Nadopasana (Raga Sudha Rasa) claim attested in snapshot pallavi.",
    },
    "77d3a9de-411c-4c6b-b285-026d19d0bf27": {
        "deity": "Rama",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "sogasugA mRdanga tALamu jata kUrci ninu sokka jEyu dhIruD(e)vvaDO",
        "note": "Approved Nadopasana & Laya-Sahitya aesthetic claim attested in snapshot pallavi.",
    },
    "e8de604f-1086-4bb5-bb70-cc9b7b21828e": {
        "deity": "Rama",
        "theme": "Nadopasana & Musical Knowledge",
        "excerpt": "1 gIt(A)rthamu 2 sangIt(A)nandamun-( 3 I) tAvuna jUDarA O manasA",
        "note": "Approved Gita-artha & Sangita-ananda philosophical claim attested in snapshot pallavi.",
    },
    "9e06c13c-2bbb-47a7-b580-1de7365754cc": {
        "deity": "Rama",
        "theme": "Rama Bhakti",
        "ritual": "tyagaraja-utsava-sampradaya",
        "ritual_function": "invitation_heccarika",
        "excerpt": "heccarikagA rArA hE rAma candra heccarikagA rArA hE suguNa sAndra",
        "note": "Approved Utsava Sampradaya royal welcome (heccarika) function attested in snapshot pallavi.",
    },
    "f5081739-3f8e-4427-90c7-becb3b1eeba9": {
        "deity": "Rama & Sita",
        "theme": "Rama Bhakti",
        "ritual": "tyagaraja-utsava-sampradaya",
        "ritual_function": "marriage_kalyanam",
        "excerpt": "sItA kalyANa vaibhOgamE rAma kalyANa vaibhOgamE",
        "note": "Approved Utsava Sampradaya divine marriage (kalyana utsavam) function attested in snapshot pallavi.",
    },
    "f5b3f945-9362-4b7b-945e-8d920e6b66c9": {
        "deity": "Krishna",
        "theme": "Narrative Allusion & Leela Praise",
        "dramatic": "tyagaraja-nauka-charitram",
        "dramatic_work": "Nauka Charitram",
        "dramatic_item_type": DramaticItemType.DARU,
        "dramatic_speaker": "Gopis",
        "dramatic_addressee": "Krishna",
        "dramatic_division": "Yamuna Boat Excursion — Perfuming Song",
        "excerpt": "1 gandhamu puyyarugA 2 pannIru gandhamu puyyarugA",
        "note": "Approved Nauka Charitram dramatic daru (Gopis addressing Krishna) attested in snapshot pallavi.",
    },
    "d4579215-90be-47f8-8410-44020f815417": {
        "deity": "Krishna",
        "theme": "Narrative Allusion & Leela Praise",
        "dramatic": "tyagaraja-nauka-charitram",
        "dramatic_work": "Nauka Charitram",
        "dramatic_item_type": DramaticItemType.DARU,
        "dramatic_speaker": "Narrator / Gopis",
        "dramatic_addressee": "Krishna",
        "dramatic_division": "Opening Procession to Yamuna",
        "excerpt": "SRngArincukoni veDaliri SrI kRshNunitOnu",
        "note": "Approved Nauka Charitram opening daru attested in snapshot pallavi.",
    },
    "78b86c5a-7c6e-4797-a4ab-0be561979065": {
        "deity": "Rama",
        "theme": "Surrender (Saranagati)",
        "dramatic": "tyagaraja-prahlada-bhakti-vijayam",
        "dramatic_work": "Prahlada Bhakti Vijayam",
        "dramatic_item_type": DramaticItemType.KIRTANA,
        "dramatic_speaker": "Prahlada",
        "dramatic_addressee": "Rama / Hari",
        "dramatic_division": "Act I — Prahlada Invocation",
        "excerpt": "vandanamu raghu nandana sEtu bandhana bhakta 1 candana rAma",
        "note": "Approved Prahlada Bhakti Vijayam invocatory kirtana attested in snapshot pallavi.",
    },
    "e0151a7a-e0d9-413d-b07a-4c1b2854971f": {
        "deity": "Siva (Kovur Sundareswara)",
        "theme": "Deity Stuti & Darshana Bliss",
        "excerpt": "sundarESvaruni jUci surula jUDa manasu vaccunA",
        "note": "Approved Kovur Sundareswara Shiva addressee claim attested in snapshot pallavi.",
    },
    "f885a354-b961-400f-ac3f-df5fee309d0c": {
        "deity": "Rama",
        "theme": "Nama Mahima",
        "excerpt": "smaraNE sukhamu rAma nAma",
        "note": "Approved Nama Mahima & Desadi tala witness claim attested in snapshot and import record.",
    },
}

# Kshetra Pancharatnam & Shrine Leads (edition-dependent group leads stay unreviewed; geography/deity unresolved)
KSHETRA_LEADS: dict[str, dict[str, str]] = {
    # Kovur Sundareswara Pancharatnam
    "e0151a7a-e0d9-413d-b07a-4c1b2854971f": {
        "group": "tyagaraja-kovur-pancharatnam",
        "shrine": "Kovur Sundareswara Temple",
        "deity": "Siva (Kovur Sundareswara)",
        "note": "Kovur Pancharatnam candidate; sequence and canonical shrine link await Govinda Rao edition review.",
    },
    "1d3c167b-d5ae-4f4b-8e55-8f11ab9c39e4": {
        "group": "tyagaraja-kovur-pancharatnam",
        "shrine": "Kovur Sundareswara Temple",
        "deity": "Siva (Kovur Sundareswara)",
        "note": "Kovur Pancharatnam candidate (I vasudha nIvanti in Sahana); awaits edition consultation.",
    },
    "e630e8f7-f8da-45b3-8e8b-fce0f91a74e9": {
        "group": "tyagaraja-kovur-pancharatnam",
        "shrine": "Kovur Sundareswara Temple",
        "deity": "Siva (Kovur Sundareswara)",
        "note": "Kovur Pancharatnam candidate (kOri sEvimpa rArE in Kharaharapriya); awaits edition consultation.",
    },
    # Tiruvottiyur Tripurasundari Pancharatnam
    "d318d9fc-0eef-4e65-95be-2d232bc1b1cc": {
        "group": "tyagaraja-tiruvottiyur-pancharatnam",
        "shrine": "Tiruvottiyur Tripurasundari Temple",
        "deity": "Devi (Tripurasundari)",
        "note": "Tiruvottiyur Pancharatnam candidate (kanna talli in Saveri; distinct from nannu kanna talli).",
    },
    "e599944d-4435-4432-8947-ec73ea7e4be5": {
        "group": "tyagaraja-tiruvottiyur-pancharatnam",
        "shrine": "Tiruvottiyur Tripurasundari Temple",
        "deity": "Devi (Tripurasundari)",
        "note": (
            "Tiruvottiyur Pancharatnam candidate (sundari nI divya in Kalyani; moved from Kovur guess "
            "per musicologist review; awaits named edition)."
        ),
    },
    # Lalgudi Pancharatnam
    "4215e9b5-17dd-4730-a140-55d48014b034": {
        "group": "tyagaraja-lalgudi-pancharatnam",
        "shrine": "Lalgudi Saptarishiswara Temple",
        "deity": "Devi (Pravrddha Srimati / Lalita)",
        "note": "Lalgudi Pancharatnam candidate (lalitE SrI pravRddhE in Bhairavi); awaits edition consultation.",
    },
    "7914b699-b204-42b1-9057-72f4ff5db6d4": {
        "group": "tyagaraja-lalgudi-pancharatnam",
        "shrine": "Lalgudi Saptarishiswara Temple",
        "deity": "Devi (Pravrddha Srimati)",
        "note": "Lalgudi Pancharatnam candidate (mahita pravRddha SrImati in Kambhoji); awaits edition consultation.",
    },
    # Srirangam Pancharatnam
    "c262739c-e7cc-492d-b248-3775fec24bfe": {
        "group": "tyagaraja-srirangam-pancharatnam",
        "shrine": "Srirangam Ranganathaswamy Temple",
        "deity": "Ranganatha (Vishnu)",
        "note": "Srirangam Pancharatnam candidate (O ranga SAyi in Kambhoji); awaits edition consultation.",
    },
}


def _match_kshetra_by_title(title: str, raga: str) -> dict[str, str] | None:
    t = title.casefold().strip()
    r = raga.casefold().strip()
    if t in {"nammi vaccina", "sambho mahadeva"}:
        return {
            "group": "tyagaraja-kovur-pancharatnam",
            "shrine": "Kovur Sundareswara Temple",
            "deity": "Siva (Kovur Sundareswara)",
            "note": f"Kovur Pancharatnam candidate ({title}); awaits Govinda Rao edition consultation.",
        }
    if t in {"sundari ninnu", "sundari nannindarilo", "sundari ni divya", "darini telusukonti"} or (
        "kanna talli" in t and "nannu" not in t
    ):
        return {
            "group": "tyagaraja-tiruvottiyur-pancharatnam",
            "shrine": "Tiruvottiyur Tripurasundari Temple",
            "deity": "Devi (Tripurasundari)",
            "note": (
                f"Tiruvottiyur Pancharatnam candidate ({title}); sundari nI divya moved from Kovur guess "
                "per musicologist review. Awaits named edition."
            ),
        }
    if t in {"isa pahi", "gati nivani", "deva sri tapastirthapura"}:
        return {
            "group": "tyagaraja-lalgudi-pancharatnam",
            "shrine": "Lalgudi Saptarishiswara Temple",
            "deity": "Siva (Saptarishiswara) / Devi (Pravrddha Srimati)",
            "note": (
                f"Lalgudi Pancharatnam candidate ({title}); ISa pAhi moved from Tiruvottiyur guess "
                "per musicologist review. Awaits named edition."
            ),
        }
    if (
        (t == "cutamu rare" and "arabhi" in r)
        or "karuna judavayya" in t
        or "raju vedale" in t
        or "vinata suta rara" == t
    ):
        if "vinata suta rara" == t:
            return None
        return {
            "group": "tyagaraja-srirangam-pancharatnam",
            "shrine": "Srirangam Ranganathaswamy Temple",
            "deity": "Ranganatha (Vishnu)",
            "note": (
                f"Srirangam Pancharatnam candidate ({title} in {raga}); Vinaradha na manavi is absent from "
                "catalogue; Sāranga is distinct from Brindavana Saranga. Awaits named edition."
            ),
        }
    if "nannu kanna talli" in t or "dharmasamvardhani" in t or "parasakti" in t or "panchanadisa" in t:
        return {
            "group": "",
            "shrine": "Tiruvaiyaru Panchanadiswara / Dharmasamvardhani Temple",
            "deity": "Devi (Dharmasamvardhani) / Siva (Panchanadiswara)",
            "note": (
                f"Tiruvaiyaru shrine candidate ({title}); nannu kanna talli (Kesari-Sindhu Kannada) is "
                "distinct from Tiruvottiyur kanna talli (Saveri)."
            ),
        }
    if "venkatesa" in t or "teratiyaga" in t:
        return {
            "group": "",
            "shrine": "Tirumala Venkateswara Temple",
            "deity": "Venkateswara (Vishnu)",
            "note": f"Tirupati Venkateswara shrine candidate ({title}); awaits edition confirmation.",
        }
    if "vinata suta vahanudai" in t or "varadaraja" in t:
        return {
            "group": "",
            "shrine": "Kanchipuram Varadaraja Perumal Temple",
            "deity": "Varadaraja (Vishnu)",
            "note": f"Kanchipuram Varadaraja shrine candidate ({title}); awaits edition confirmation.",
        }
    return None


def _classify_ritual_lead(title: str, raga: str) -> tuple[str, str, str] | None:
    t = title.casefold().strip()
    # Explicit exclusion per Spec Req 7: Nagu Momu Kana Leni (Abheri) is NOT the ritual piece Nagu Momu Gala Vaani
    if "nagu momu kana leni" in t or ("nagu momu" in t and "abheri" in raga.casefold()):
        return None
    if "meluko" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "awakening_melukolupu",
            "Utsava Sampradaya awakening (melukolupu) piece; bhajana paddhati order is edition-specific.",
        )
    if "heccarikaga" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "invitation_heccarika",
            "Utsava Sampradaya royal welcome (heccarika) piece.",
        )
    if "koluvaiyunnade" in t or "koluvamare" in t or "upacaramu" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "court_koluvu_upachara",
            "Utsava Sampradaya royal court / upachara piece; edition-qualified.",
        )
    if "sita kalyana" in t or "napali sri rama" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "marriage_kalyanam",
            "Utsava Sampradaya marriage / nalugu celebration piece.",
        )
    if (
        "lali " in t
        or t.startswith("lali")
        or "uyyala" in t
        or "jo jo rama" in t
        or "kshira sagara vihara" in t
        or "badalika" in t
        or "pula panpu" in t
    ):
        return (
            "tyagaraja-utsava-sampradaya",
            "lullaby_dolotsavam",
            "Utsava Sampradaya swing / lullaby (dolotsavam / laali) piece.",
        )
    if "harati" in t or "mangalam" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "benediction_mangalam",
            "Utsava Sampradaya concluding harati / mangalam benediction; source setting preserved.",
        )
    if "tulasi" in t:
        return (
            "tyagaraja-utsava-sampradaya",
            "archana_tulasi_puja",
            f"Tulasi archana piece ({title}); preserved as a distinct identity among the three Tulasi titles.",
        )
    if "nagu momu galavani" in t or ("nagu momu" in t and "madhyamav" in raga.casefold()):
        return (
            "tyagaraja-utsava-sampradaya",
            "congregational_namasankirtana",
            "Ritual piece Nagu Momu Galavani (Madhyamavati); kept distinct from Nagu Momu Kana Leni (Abheri).",
        )
    if any(
        kw in t
        for kw in (
            "rama rama",
            "pahi rama",
            "rama kodanda",
            "rama pahi",
            "sri rama jaya rama",
            "tavadasoham",
            "varada navanitasa",
            "haridasulu",
            "bhajana",
        )
    ):
        return (
            "tyagaraja-divyanama-sankirtana",
            "congregational_namasankirtana",
            "Divyanama Sankirtana congregational bhajana candidate; overlap with Utsava Sampradaya allowed.",
        )
    return None


def _classify_dramatic_lead(title: str, raga: str) -> dict[str, Any] | None:
    t = title.casefold().strip()
    if (
        "srngarincukoni" in t
        or "gandhamu puyyaruga" in t
        or ("cutamu rare" in t and "kapi" in raga.casefold())
        or "chootaamu raareyee" in t
        or "allakallolamayenamma" in t
        or "perugu palu" in t
        or "tanayande prema" in t
        or "veda vakyamani" in t
        or "emani migula" in t
        or "odanu jaripe" in t
    ):
        is_odanu = "odanu jaripe" in t
        return {
            "value": "tyagaraja-nauka-charitram",
            "work": "Nauka Charitram",
            "item_type": DramaticItemType.DARU,
            "speaker": "Gopis / Narrator",
            "addressee": "Krishna",
            "division": "Yamuna Boat Excursion",
            "note": (
                "ODanu jaripE (Saranga) is recorded strictly as an unverified dramatic lead per Spec Requirement 8."
                if is_odanu
                else (
                    f"Nauka Charitram dramatic item candidate ({title}); "
                    "dramatic dialogue is not composer autobiography."
                )
            ),
        }
    if (
        "vandanamu raghunandana" in t
        or "varijanayana" in t
        or "vinata suta rara" in t
        or "samudra tarana" in t
        or "eti janmamidi" in t
        or "kamalapta kula" in t
        or "dayajucutakidi" in t
        or "sri ganapatini" in t
    ):
        return {
            "value": "tyagaraja-prahlada-bhakti-vijayam",
            "work": "Prahlada Bhakti Vijayam",
            "item_type": DramaticItemType.KIRTANA,
            "speaker": "Prahlada / Devotees",
            "addressee": "Hari / Narasimha / Rama",
            "division": "Prahlada Bhakti Narrative",
            "note": f"Prahlada Bhakti Vijayam dramatic kirtana candidate ({title}); awaits Sambamoorthy edition check.",
        }
    return None


def _infer_deity_and_theme(title: str, raga: str, pallavi: str | None) -> tuple[str, str, str]:
    text = f"{title} {pallavi or ''}".casefold()
    excerpt = (pallavi or title).replace("\n", " ").strip()

    # Deity detection based on explicit lexical addressee in incipit/pallavi
    if any(k in text for k in ("ganapati", "gana pati", "gananatha", "vinayaka", "giriraja suta", "giri raja suta")):
        deity = "Ganesha"
    elif any(
        k in text
        for k in (
            "sambho",
            "sankara",
            "sundaresvar",
            "mahadeva",
            "siva",
            "nada tanumanisam",
            "isa pahi",
            "panchanadisa",
            "neelakantha",
        )
    ):
        deity = "Siva"
    elif any(
        k in text
        for k in (
            "amba",
            "devi",
            "tripurasundari",
            "dharmasamvardhani",
            "dharmambike",
            "pravrddha",
            "lalite",
            "kanna talli",
            "parvati",
            "parasakti",
            "sundari ",
            "mahishasura",
            "karmame balavantam",
            "bale balendu",
        )
    ):
        if "tulasamma" in text:
            deity = "Tulasi"
        else:
            deity = "Devi (Parvati / Tripurasundari / Dharmasamvardhani)"
    elif "tulasi" in text:
        deity = "Tulasi"
    elif "narasimha" in text or "nrsimha" in text or "prahlada" in text:
        deity = "Narasimha"
    elif any(k in text for k in ("narada", "vara narada", "sri narada")):
        deity = "Sage Narada"
    elif any(k in text for k in ("anjaney", "hanuman", "pahi rama duta", "pavanaja", "vayu kumara")):
        deity = "Hanuman"
    elif any(
        k in text for k in ("krishna", "krsna", "gandhamu", "navanitasa", "gopika", "venu gana", "govinda", "mukunda")
    ):
        deity = "Krishna / Vishnu"
    elif any(k in text for k in ("ranga", "phanipati sayi", "phani sayana", "uraga raja sayana")):
        deity = "Ranganatha (Vishnu)"
    elif "venkatesa" in text or "teratiyaga" in text:
        deity = "Venkateswara (Vishnu)"
    elif "surya" in text or "dinamani" in text:
        deity = "Rama (Suryavamsa) / Surya"
    else:
        deity = "Rama"

    # Theme detection
    if any(
        k in text
        for k in (
            "nada",
            "svara",
            "sangita",
            "raga sudha",
            "gitarthamu",
            "mrdanga",
            "vidulaku",
            "mokshamu galada",
            "sobhillu",
            "raga ratna",
            "vara raga",
            "sitavara sangita",
        )
    ):
        theme = "Nadopasana & Musical Knowledge"
    elif "manasa" in text or "manasu" in text or "cakkani raja" in text or "buddhi radu" in text:
        theme = "Instruction to the Mind (Manasa)"
    elif "nama" in text or "smarane" in text or "sukhi yevaro" in text or "bhajana" in text:
        theme = "Nama Mahima"
    elif any(k in text for k in ("narada", "mahanubhavul", "peddala", "guru leka", "sabari", "bhagavata")):
        theme = "Saints and Teachers"
    elif any(
        k in text for k in ("saranu", "brova", "rakshimpa", "pahi", "palaya", " daya ", "kavakavayya", "aparadha")
    ):
        theme = "Surrender (Saranagati) & Appeal for Grace"
    elif any(k in text for k in ("emi neramu", "nyayama", "lobhamela", "calamu", "ora jupu", "paritapamu", "mariyada")):
        theme = "Ethical Reflection & Devotional Appeal"
    else:
        theme = "Rama Bhakti & Devotional Praise" if deity == "Rama" else "Deity Stuti & Devotional Praise"

    return deity, theme, excerpt


def main() -> None:
    raw_master = json.loads(MASTER_PATH.read_text(encoding="utf-8"))
    pallavis = {
        r["id"]: r["pallavi"] for r in json.loads(PALLAVIS_PATH.read_text(encoding="utf-8")) if r.get("pallavi")
    }
    t144_rows = {r["krithi_id"]: r for r in json.loads(T144_DOSSIER_PATH.read_text(encoding="utf-8")).get("rows", [])}

    # Map cached HTML files by URL SHA-256
    cache_files = {p.stem: p for p in CACHE_DIR.glob("*.html")}
    url_to_cache: dict[str, tuple[Path, str]] = {}
    for s in raw_master["subjects"]:
        u = s["source_url"]
        for cand in (u, u.replace("http://", "https://"), u.replace("https://", "http://")):
            h = hashlib.sha256(cand.encode()).hexdigest()
            if h in cache_files:
                file_sha = hashlib.sha256(cache_files[h].read_bytes()).hexdigest()
                url_to_cache[u] = (cache_files[h], file_sha)
                break

    # 1. Build and hash database/data/track147-catalogue-snapshot.json
    existing_payload_checksums: dict[str, str] = {}
    if SNAPSHOT_PATH.exists():
        existing_snap = json.loads(SNAPSHOT_PATH.read_text(encoding="utf-8"))
        for rec in existing_snap.get("records", []):
            for imp in rec.get("imports", []):
                if imp.get("payload_checksum"):
                    existing_payload_checksums[imp["id"]] = imp["payload_checksum"]

    by_krithi: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for s in raw_master["subjects"]:
        kid = s["krithi_id"] or s["mapped_krithi_id"]
        by_krithi[kid].append(s)

    snapshot_records = []
    for kid in sorted(by_krithi):
        subs = sorted(by_krithi[kid], key=lambda x: x["import_id"])
        first = subs[0]
        t144_row = t144_rows.get(kid)
        pal = pallavis.get(kid) or (t144_row["expected_pallavi"] if t144_row else None)
        if not pal and kid in GHANA_APPROVED:
            pal = GHANA_APPROVED[kid]["excerpt"]
        snapshot_records.append(
            {
                "id": kid,
                "title": first["title"],
                "primary_language": first["primary_language"],
                "musical_form": first["musical_form"],
                "workflow_state": first["workflow_state"],
                "is_ragamalika": first["is_ragamalika"],
                "raga": first["canonical_raga"],
                "tala": first["canonical_tala"],
                "deity_id": None,
                "temple_id": None,
                "section_flags": first["section_flags"],
                "structure_audit": first["structure_audit"],
                "pallavi_excerpt": pal,
                "imports": [
                    {
                        "id": sub["import_id"],
                        "raw_raga": sub["raw_raga"],
                        "raw_tala": sub["raw_tala"],
                        "source_url": sub["source_url"],
                        "import_status": sub["import_status"],
                        "tala_witness_note": sub["tala_witness_note"],
                        "payload_checksum": existing_payload_checksums.get(sub["import_id"]),
                    }
                    for sub in subs
                ],
                "evidence": [
                    {
                        "id": sub["evidence_id"],
                        "url": sub["source_url"],
                        "fields": ["title", "composer", "raga", "tala"],
                        "checksum": (
                            url_to_cache[sub["source_url"]][1]
                            if sub["source_url"] in url_to_cache
                            else existing_payload_checksums.get(sub["import_id"])
                        ),
                    }
                    for sub in subs
                    if sub["evidence_id"]
                ],
                "raga_rows": "1",
            }
        )

    snapshot_payload = {
        "snapshot_at": raw_master["snapshot_date"],
        "composer_id": raw_master["composer_id"],
        "identity_count": len(snapshot_records),
        "source_row_count": sum(len(r["imports"]) for r in snapshot_records),
        "records": snapshot_records,
    }
    SNAPSHOT_PATH.write_text(json.dumps(snapshot_payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    snapshot_sha = hashlib.sha256(SNAPSHOT_PATH.read_bytes()).hexdigest()
    t144_sha = hashlib.sha256(T144_DOSSIER_PATH.read_bytes()).hexdigest()
    csv_sha = raw_master["source_inventory"]["sha256"]

    # 2. Build formal Witness Registry
    witnesses: list[Witness] = [
        Witness(
            id="db-snapshot",
            url="database/data/track147-catalogue-snapshot.json",
            sha256=snapshot_sha,
            accessed_at="2026-10-15",
            status=WitnessStatus.DATABASE_SNAPSHOT,
            description=(
                "Hashed database census and stored pallavi/section/evidence snapshot for "
                "675 Tyagaraja catalogue identities and 684 source imports."
            ),
        ),
        Witness(
            id="csv-inventory",
            url="database/for_import/Thyagaraja-Krithi-For-Import.csv",
            sha256=csv_sha,
            accessed_at="2026-10-01",
            status=WitnessStatus.FETCHED,
            description="Hashed 687-row Tyagaraja bulk-import CSV inventory.",
        ),
        Witness(
            id="track144-tala-pallavi-dossier",
            url="database/data/track144-tala-evidence-tyagaraja-complete.json",
            sha256=t144_sha,
            accessed_at="2026-09-26",
            status=WitnessStatus.FETCHED,
            description=(
                "Reviewed Tyagaraja tala and pallavi verification dossier (268 compositions "
                "cross-checked against Karnatik.com, Shivkumar archive, and Tyagaraja Vaibhavam)."
            ),
        ),
        Witness(
            id="govinda-rao-tyagaraja",
            url=(
                "Sangeetha Kalanidhi T.K. Govinda Rao, Compositions of Tyagaraja "
                "(Ganamandir Publications — edition not consulted)"
            ),
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Primary print authority for Tyagaraja sahitya, notation, and Kshetra Pancharatnam sets "
                "(deferred to TRACK-149)."
            ),
        ),
        Witness(
            id="ramanujachari-spiritual-heritage",
            url=(
                "C. Ramanujachari & Dr. V. Raghavan, The Spiritual Heritage of Tyagaraja "
                "(Sri Ramakrishna Math — edition not consulted)"
            ),
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Thematic classification and translation authority by Dr. V. Raghavan (deferred to TRACK-149)."
            ),
        ),
        Witness(
            id="sambamoorthy-tyagaraja",
            url=(
                "Prof. P. Sambamoorthy, Great Composers Book II: Tyagaraja & Nowka Charitram / "
                "Prahlada Bhakti Vijayam (edition not consulted)"
            ),
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Scholarly authority for Tyagaraja's dramatic operas and kshetra pilgrimage biography "
                "(deferred to TRACK-149)."
            ),
        ),
        Witness(
            id="ma:jma-vol18-1947",
            url="https://musicacademymadras.in/catalogue/files/journals/Vol.18_1947.pdf",
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Journal of the Music Academy Vol. XVIII (1947): Walajapet manuscript collection research lead "
                "(not consulted)."
            ),
        ),
        Witness(
            id="ma:jma-vol39-1968",
            url="https://musicacademymadras.in/catalogue/files/journals/Vol.39_1968.pdf",
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Journal of the Music Academy Vol. XXXIX (1968): Tyagaraja transmission research lead (not consulted)."
            ),
        ),
        Witness(
            id="ma:jma-2017",
            url="https://musicacademymadras.in/catalogue/files/journals/Music%20Academy%20Journal%202017.pdf",
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description=(
                "Journal of the Music Academy (2017): dramatic-work daru, padya, and vacana analysis lead "
                "(not consulted)."
            ),
        ),
        Witness(
            id="sanskrit-docs-tyagaraja",
            url="https://sanskritdocuments.org/doc_deities_misc/tyAgarAjakRRitayaH.pdf",
            sha256=None,
            accessed_at="2026-10-15",
            status=WitnessStatus.UNAVAILABLE,
            description="Compiled Sanskrit compositions of Tyagaraja group inventory lead (not consulted).",
        ),
    ]

    # Register cached blog witnesses with their exact file SHA-256
    for s in raw_master["subjects"]:
        u = s["source_url"]
        if u in url_to_cache:
            _, file_sha = url_to_cache[u]
            witnesses.append(
                Witness(
                    id=s["import_id"],
                    url=u,
                    sha256=file_sha,
                    accessed_at="2026-09-24",
                    status=WitnessStatus.FETCHED,
                    description=f"Cached thyagaraja-vaibhavam HTML witness for {s['title']} ({s['raw_raga']}).",
                )
            )
        else:
            payload_sha = existing_payload_checksums.get(s["import_id"])
            if not payload_sha:
                raise RuntimeError(f"Missing payload_checksum for import {s['import_id']}")
            witnesses.append(
                Witness(
                    id=s["import_id"],
                    url=u,
                    sha256=payload_sha,
                    accessed_at="2026-10-01",
                    status=WitnessStatus.FETCHED,
                    description=(
                        f"Imported thyagaraja-vaibhavam witness extraction hash recorded in "
                        f"imported_krithis.parsed_payload for {s['title']} ({s['raw_raga']})."
                    ),
                )
            )

    # 3. Populate claims for all 684 subjects
    master = TyagarajaClassificationMaster.model_validate(raw_master)
    master.database_snapshot_witness = "db-snapshot"
    master.witnesses = witnesses
    master.limitations = [
        "Eight catalogue identities absorb 17 conflicting source imports and remain collision_not_classified; "
        "their 17 source witnesses (tyagaraja:import:*) are classified individually pending audited canon split "
        "in TRACK-149.",
        "Print editions (T.K. Govinda Rao, C. Ramanujachari & V. Raghavan's The Spiritual Heritage of Tyagaraja, "
        "P. Sambamoorthy) and external JMA PDF volumes were not physically consulted and remain registered as "
        "unavailable witnesses. Edition-dependent Kshetra Pancharatnam sequences, Utsava/Divyanama paddhati orderings, "
        "and dramatic verse/prose divisions remain unreviewed leads for TRACK-149.",
        "Only the 5 Ghana-raga Pancharatnam memberships (with Aradhana concert order recorded separately from "
        "composer sequence) and their supporting claims are applied by R__seed_11. Non-cycle approved claims "
        "are marked application: deferred for TRACK-149.",
        "27 catalogue identities lack a PALLAVI section and 4 lack a CHARANAM section in current storage; "
        "no lyric text was invented.",
        "Eleven source witnesses with raw_tala Desadi are stored canonically as Adi; their raw Desadi witness notes "
        "are preserved explicitly.",
    ]

    for subject in master.subjects:
        kid = subject.krithi_id or subject.mapped_krithi_id or ""
        t144_row = t144_rows.get(kid)
        pal = pallavis.get(kid) or (t144_row["expected_pallavi"] if t144_row else None)
        deity_val, theme_val, excerpt_text = _infer_deity_and_theme(subject.title, subject.canonical_raga, pal)

        deity_claim = next(c for c in subject.claims if c.axis == ClaimAxis.DEITY)
        geo_claim = next(c for c in subject.claims if c.axis == ClaimAxis.GEOGRAPHY)
        theme_claim = next(c for c in subject.claims if c.axis == ClaimAxis.THEME)
        group_claim = next(c for c in subject.claims if c.axis == ClaimAxis.GROUP)
        ritual_claim = next(c for c in subject.claims if c.axis == ClaimAxis.RITUAL)
        dramatic_claim = next(c for c in subject.claims if c.axis == ClaimAxis.DRAMATIC)

        # Case 1: Approved Ghana-raga Pancharatnam (5 single-source catalogue identities)
        if subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY and kid in GHANA_APPROVED:
            ginfo = GHANA_APPROVED[kid]
            app_apply = Approval(
                reviewer="Seshadri",
                accepted_at="2026-10-15",
                decision="approve",
                note=ginfo["note"],
                application="apply",
            )
            deity_claim.value = "Rama"
            deity_claim.witness_id = subject.import_id
            deity_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); Pallavi & Charanam"
            deity_claim.excerpt = ginfo["excerpt"]
            deity_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            deity_claim.review_state = ReviewState.REVIEWED
            deity_claim.reviewer_decision = ginfo["note"]
            deity_claim.approval = app_apply
            deity_claim.mudra = "Tyagaraja"
            deity_claim.uncertainty = "Verified canonical addressee."
            subject.deity_state = "REVIEWED"

            geo_claim.value = "NON_SPECIFIC_ADDRESS"
            geo_claim.witness_id = subject.import_id
            geo_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); non-kshetra concert pancharatnam"
            geo_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
            geo_claim.review_state = ReviewState.REVIEWED
            geo_claim.reviewer_decision = ginfo["note"]
            geo_claim.approval = app_apply
            geo_claim.uncertainty = "Universal Rama/bhakti address; not bound to a single temple kshetra."

            theme_claim.value = ginfo["theme"]
            theme_claim.witness_id = subject.import_id
            theme_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); Pallavi & Charanam"
            theme_claim.excerpt = ginfo["excerpt"]
            theme_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            theme_claim.review_state = ReviewState.REVIEWED
            theme_claim.reviewer_decision = ginfo["note"]
            theme_claim.approval = app_apply
            theme_claim.mudra = "Tyagaraja"
            theme_claim.uncertainty = "Verified devotional and musicological theme."

            group_claim.value = GHANA_GROUP_VALUE
            group_claim.witness_id = subject.import_id
            group_claim.locator = (
                f"Witness {subject.import_id} (Snapshot record {kid}); Aradhana performance order {ginfo['order']}"
            )
            group_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
            group_claim.review_state = ReviewState.REVIEWED
            group_claim.reviewer_decision = ginfo["note"]
            group_claim.approval = app_apply
            group_claim.group_type = GroupType.FORMAL_GROUP
            group_claim.performance_order = int(ginfo["order"])
            group_claim.mudra = "Tyagaraja"
            group_claim.uncertainty = (
                f"Approved Ghana-raga Pancharatnam membership. Aradhana performance order {ginfo['order']} "
                "(Nattai, Gowla, Arabhi, Varali, Sri) is recorded as a concert convention, not a composer sequence."
            )
            subject.geography_state = derive_geography_state(subject.claims)
            continue

        # Case 2: Hallmark Non-Cycle Approved Claims (application="deferred")
        if subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY and kid in DEFERRED_APPROVED:
            dinfo = DEFERRED_APPROVED[kid]
            app_def = Approval(
                reviewer="Seshadri",
                accepted_at="2026-10-15",
                decision="approve",
                note=dinfo["note"],
                application="deferred",
                deferral_reason=DEFERRAL_REASON,
            )
            deity_claim.value = dinfo["deity"]
            deity_claim.witness_id = subject.import_id
            deity_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); pallavi_excerpt"
            deity_claim.excerpt = dinfo["excerpt"]
            deity_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            deity_claim.review_state = ReviewState.REVIEWED
            deity_claim.reviewer_decision = dinfo["note"]
            deity_claim.approval = app_def
            deity_claim.mudra = "Tyagaraja"
            deity_claim.uncertainty = "Approved from witnessed pallavi excerpt; DB application deferred to TRACK-149."
            subject.deity_state = "REVIEWED"

            theme_claim.value = dinfo["theme"]
            theme_claim.witness_id = subject.import_id
            theme_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); pallavi_excerpt"
            theme_claim.excerpt = dinfo["excerpt"]
            theme_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            theme_claim.review_state = ReviewState.REVIEWED
            theme_claim.reviewer_decision = dinfo["note"]
            theme_claim.approval = app_def
            theme_claim.mudra = "Tyagaraja"
            theme_claim.uncertainty = "Approved from witnessed pallavi excerpt; DB application deferred to TRACK-149."

            if "ritual" in dinfo:
                ritual_claim.value = dinfo["ritual"]
                ritual_claim.witness_id = subject.import_id
                ritual_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); pallavi_excerpt"
                ritual_claim.excerpt = dinfo["excerpt"]
                ritual_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
                ritual_claim.review_state = ReviewState.REVIEWED
                ritual_claim.reviewer_decision = dinfo["note"]
                ritual_claim.approval = app_def
                ritual_claim.group_type = GroupType.RITUAL_COLLECTION
                ritual_claim.ritual_function = dinfo["ritual_function"]
                ritual_claim.uncertainty = "Approved ritual function; cycle seeding deferred to TRACK-149."

            if "dramatic" in dinfo:
                dramatic_claim.value = dinfo["dramatic"]
                dramatic_claim.witness_id = subject.import_id
                dramatic_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); pallavi_excerpt"
                dramatic_claim.excerpt = dinfo["excerpt"]
                dramatic_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
                dramatic_claim.review_state = ReviewState.REVIEWED
                dramatic_claim.reviewer_decision = dinfo["note"]
                dramatic_claim.approval = app_def
                dramatic_claim.group_type = GroupType.DRAMATIC_WORK
                dramatic_claim.dramatic_work = dinfo["dramatic_work"]
                dramatic_claim.dramatic_item_type = dinfo["dramatic_item_type"]
                dramatic_claim.dramatic_speaker = dinfo["dramatic_speaker"]
                dramatic_claim.dramatic_addressee = dinfo["dramatic_addressee"]
                dramatic_claim.dramatic_division = dinfo["dramatic_division"]
                dramatic_claim.uncertainty = (
                    "Approved dramatic item context; dramatic speech is not composer autobiography."
                )

            kshetra_info = KSHETRA_LEADS.get(kid) or _match_kshetra_by_title(subject.title, subject.canonical_raga)
            if kshetra_info:
                geo_claim.value = kshetra_info["shrine"]
                geo_claim.witness_id = "govinda-rao-tyagaraja"
                geo_claim.locator = "Edition not consulted"
                geo_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
                geo_claim.review_state = ReviewState.UNREVIEWED
                geo_claim.uncertainty = kshetra_info["note"]
                if kshetra_info["group"]:
                    group_claim.value = kshetra_info["group"]
                    group_claim.witness_id = "govinda-rao-tyagaraja"
                    group_claim.locator = "Edition not consulted"
                    group_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
                    group_claim.review_state = ReviewState.UNREVIEWED
                    group_claim.group_type = GroupType.KSHETRA_GROUP
                    group_claim.uncertainty = kshetra_info["note"]
            else:
                geo_claim.value = "NO_PLACE_STATED"
                geo_claim.witness_id = subject.import_id
                geo_claim.locator = f"Witness {subject.import_id} (Snapshot record {kid}); incipit & metadata"
                geo_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
                geo_claim.review_state = ReviewState.UNRESOLVED
                geo_claim.uncertainty = "No specific temple kshetra stated in snapshot; awaits edition review."
            subject.geography_state = derive_geography_state(subject.claims)
            continue

        # Case 3: Remaining 665 subjects (648 single-source + 17 collision witnesses)
        kshetra_info = KSHETRA_LEADS.get(kid) or _match_kshetra_by_title(subject.title, subject.canonical_raga)
        if kshetra_info:
            deity_val = kshetra_info["deity"]

        deity_claim.value = deity_val
        deity_claim.witness_id = subject.import_id
        deity_claim.locator = f"Snapshot record {kid}; incipit / pallavi ({subject.title})"
        deity_claim.excerpt = excerpt_text
        deity_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
        deity_claim.review_state = ReviewState.UNRESOLVED
        deity_claim.uncertainty = (
            "Source-observed addressee from incipit/pallavi; awaits multi-edition cross-check in TRACK-149."
        )
        subject.deity_state = "UNRESOLVED"

        theme_claim.value = theme_val
        theme_claim.witness_id = subject.import_id
        theme_claim.locator = f"Snapshot record {kid}; incipit / pallavi ({subject.title})"
        theme_claim.excerpt = excerpt_text
        theme_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
        theme_claim.review_state = ReviewState.UNRESOLVED
        theme_claim.uncertainty = (
            "Source-observed devotional/musicological theme; awaits Ramanujachari/Govinda Rao edition check."
        )

        if kshetra_info:
            geo_claim.value = kshetra_info["shrine"]
            geo_claim.witness_id = "govinda-rao-tyagaraja"
            geo_claim.locator = "Edition not consulted"
            geo_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
            geo_claim.review_state = ReviewState.UNREVIEWED
            geo_claim.uncertainty = kshetra_info["note"]
            if kshetra_info["group"] and subject.subject_kind == SubjectKind.CATALOGUE_IDENTITY:
                group_claim.value = kshetra_info["group"]
                group_claim.witness_id = "govinda-rao-tyagaraja"
                group_claim.locator = "Edition not consulted"
                group_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
                group_claim.review_state = ReviewState.UNREVIEWED
                group_claim.group_type = GroupType.KSHETRA_GROUP
                group_claim.uncertainty = kshetra_info["note"]
        else:
            geo_claim.value = "NO_PLACE_STATED"
            geo_claim.witness_id = subject.import_id
            geo_claim.locator = f"Snapshot record {kid}; incipit & metadata"
            geo_claim.evidence_kind = EvidenceKind.TEXTUAL_EVIDENCE
            geo_claim.review_state = ReviewState.UNRESOLVED
            geo_claim.uncertainty = (
                "No temple shrine named in incipit/metadata; empty temple_id remains unresolved until edition review."
            )

        ritual_lead = _classify_ritual_lead(subject.title, subject.raw_raga)
        if ritual_lead:
            r_col, r_func, r_note = ritual_lead
            ritual_claim.value = r_col
            ritual_claim.witness_id = "govinda-rao-tyagaraja"
            ritual_claim.locator = "Edition not consulted"
            ritual_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
            ritual_claim.review_state = ReviewState.UNREVIEWED
            ritual_claim.group_type = GroupType.RITUAL_COLLECTION
            ritual_claim.ritual_function = r_func
            ritual_claim.uncertainty = r_note

        dramatic_lead = _classify_dramatic_lead(subject.title, subject.raw_raga)
        if dramatic_lead:
            dramatic_claim.value = dramatic_lead["value"]
            dramatic_claim.witness_id = "sambamoorthy-tyagaraja"
            dramatic_claim.locator = "Edition not consulted"
            dramatic_claim.evidence_kind = EvidenceKind.DOCUMENTED_TRADITION
            dramatic_claim.review_state = ReviewState.UNREVIEWED
            dramatic_claim.group_type = GroupType.DRAMATIC_WORK
            dramatic_claim.dramatic_work = dramatic_lead["work"]
            dramatic_claim.dramatic_item_type = dramatic_lead["item_type"]
            dramatic_claim.dramatic_speaker = dramatic_lead["speaker"]
            dramatic_claim.dramatic_addressee = dramatic_lead["addressee"]
            dramatic_claim.dramatic_division = dramatic_lead["division"]
            dramatic_claim.uncertainty = dramatic_lead["note"]

        subject.geography_state = derive_geography_state(subject.claims)

    # 4. Validate and write all outputs
    assert_track147_census(master)
    verify_source(master, ROOT)
    MASTER_PATH.write_text(dump_master(master), encoding="utf-8")
    MARKDOWN_PATH.write_text(render_markdown(master), encoding="utf-8")
    manifest = build_application_manifest(master)
    MANIFEST_PATH.write_text(
        json.dumps(manifest.model_dump(mode="json"), indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )
    print(
        f"Populated {len(master.subjects)} subjects, {len(master.witnesses)} witnesses, "
        f"{len(manifest.changes)} approved manifest changes."
    )


if __name__ == "__main__":
    main()
