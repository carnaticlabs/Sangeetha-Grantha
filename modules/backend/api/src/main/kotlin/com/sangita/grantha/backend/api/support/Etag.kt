package com.sangita.grantha.backend.api.support

fun computeEtag(seed: String): String = seed.hashCode().toString()
