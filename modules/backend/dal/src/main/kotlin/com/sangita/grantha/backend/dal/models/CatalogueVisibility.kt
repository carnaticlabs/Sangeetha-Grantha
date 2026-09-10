package com.sangita.grantha.backend.dal.models

/**
 * Public catalogue eligibility (TRACK-140 M2). V1 never emits UNESTABLISHED;
 * V2 includes those published compositions in unfiltered reads.
 */
enum class CatalogueVisibility {
    V1,
    V2,
}
