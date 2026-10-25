package com.sangita.grantha.backend.dal.models

/**
 * Public catalogue eligibility (TRACK-140 M2). LEGACY_KNOWN_FORMS never emits UNESTABLISHED;
 * ALL_PUBLISHED includes those published compositions in unfiltered reads.
 */
enum class CatalogueVisibility {
    LEGACY_KNOWN_FORMS,
    ALL_PUBLISHED,
}
