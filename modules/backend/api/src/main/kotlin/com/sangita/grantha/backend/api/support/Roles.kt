package com.sangita.grantha.backend.api.support

/**
 * Role codes recognised by the API.
 *
 * These must match the `roles` reference table, seeded by `R__seed_01_reference.sql`. The seed
 * currently defines exactly one role, so authorisation is a single admin tier; a finer taxonomy
 * (viewer / curator / admin) is a TRACK-119 decision and needs both a seed migration and a route
 * mapping, not just new constants here.
 */
object Roles {
    /** Full administrative access. Assigned by `bootstrap-admin`; gates every admin route. */
    const val ADMIN = "grp_sangita_admin"
}
