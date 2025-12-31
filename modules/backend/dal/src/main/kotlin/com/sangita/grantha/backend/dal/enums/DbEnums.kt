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
    OTHER("other");

    companion object {
        const val DB_TYPE = "raga_section_enum"
    }
}

enum class ImportStatus(override val dbValue: String) : DbEnum {
    PENDING("pending"),
    IN_REVIEW("in_review"),
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
