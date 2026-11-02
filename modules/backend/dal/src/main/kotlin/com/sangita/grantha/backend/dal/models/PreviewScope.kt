package com.sangita.grantha.backend.dal.models

/**
 * Which compositions a catalogue read may return (TRACK-158). Mobile and every V1/V2 route use
 * [PUBLISHED_ONLY]. [INCLUDE_UNPUBLISHED] is reachable only through the admin-gated preview routes
 * and adds draft and in-review compositions; archived ones stay hidden in both.
 */
enum class PreviewScope {
    PUBLISHED_ONLY,
    INCLUDE_UNPUBLISHED,
}
