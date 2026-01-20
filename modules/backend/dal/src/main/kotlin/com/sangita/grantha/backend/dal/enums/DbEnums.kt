package com.sangita.grantha.backend.dal.enums

interface DbEnum {
    val dbValue: String
}

inline fun <reified T> enumByDbValue(value: String): T where T : Enum<T>, T : DbEnum =
    enumValues<T>().first { it.dbValue == value }

enum class WorkflowState(override val dbValue: String) : DbEnum {
    DRAFT("draft"),
    IN_REVIEW("in_review"),
    PUBLISHED("published"),
    ARCHIVED("archived");

    companion object {
        const val DB_TYPE = "workflow_state_enum"
    }
}

enum class LanguageCode(override val dbValue: String) : DbEnum {
    SA("sa"),
    TA("ta"),
    TE("te"),
    KN("kn"),
    ML("ml"),
    HI("hi"),
    EN("en");

    companion object {
        const val DB_TYPE = "language_code_enum"
    }
}

enum class ScriptCode(override val dbValue: String) : DbEnum {
    DEVANAGARI("devanagari"),
    TAMIL("tamil"),
    TELUGU("telugu"),
    KANNADA("kannada"),
    MALAYALAM("malayalam"),
    LATIN("latin");

    companion object {
        const val DB_TYPE = "script_code_enum"
    }
}

enum class RagaSection(override val dbValue: String) : DbEnum {
    PALLAVI("pallavi"),
    ANUPALLAVI("anupallavi"),
    CHARANAM("charanam"),
    SAMASHTI_CHARANAM("samashti_charanam"),
    CHITTASWARAM("chittaswaram"),
    SWARA_SAHITYA("swara_sahitya"),
    MADHYAMA_KALA("madhyama_kala"),
    SOLKATTU_SWARA("solkattu_swara"),
    ANUBANDHA("anubandha"),
    MUKTAYI_SWARA("muktayi_swara"),
    ETTUGADA_SWARA("ettugada_swara"),
    ETTUGADA_SAHITYA("ettugada_sahitya"),
    VILOMA_CHITTASWARAM("viloma_chittaswaram"),
    OTHER("other");

    companion object {
        const val DB_TYPE = "raga_section_enum"
    }
}

enum class ImportStatus(override val dbValue: String) : DbEnum {
    PENDING("pending"),
    IN_REVIEW("in_review"),
    APPROVED("approved"),
    MAPPED("mapped"),
    REJECTED("rejected"),
    DISCARDED("discarded");

    companion object {
        const val DB_TYPE = "import_status_enum"
    }
}

enum class MusicalForm(override val dbValue: String) : DbEnum {
    KRITHI("KRITHI"),
    VARNAM("VARNAM"),
    SWARAJATHI("SWARAJATHI");

    companion object {
        const val DB_TYPE = "musical_form_enum"
    }
}

enum class BatchStatus(override val dbValue: String) : DbEnum {
    PENDING("pending"),
    RUNNING("running"),
    PAUSED("paused"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    CANCELLED("cancelled");

    companion object {
        const val DB_TYPE = "batch_status_enum"
    }
}

enum class JobType(override val dbValue: String) : DbEnum {
    MANIFEST_INGEST("manifest_ingest"),
    SCRAPE("scrape"),
    ENRICH("enrich"),
    ENTITY_RESOLUTION("entity_resolution"),
    REVIEW_PREP("review_prep");

    companion object {
        const val DB_TYPE = "job_type_enum"
    }
}

enum class TaskStatus(override val dbValue: String) : DbEnum {
    PENDING("pending"),
    RUNNING("running"),
    SUCCEEDED("succeeded"),
    FAILED("failed"),
    RETRYABLE("retryable"),
    BLOCKED("blocked"),
    CANCELLED("cancelled");

    companion object {
        const val DB_TYPE = "task_status_enum"
    }
}
