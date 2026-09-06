package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.models.CatalogueLike
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogueLikeTest {
    @Test
    fun `ordinary query is a case-insensitive contains pattern`() {
        assertEquals("%vatapi%", CatalogueLike.containsPattern("Vatapi"))
    }

    @Test
    fun `percent and underscore are escaped as literals`() {
        assertEquals("%\\%%", CatalogueLike.containsPattern("%"))
        assertEquals("%\\_%", CatalogueLike.containsPattern("_"))
        assertEquals("%foo\\%bar\\_baz%", CatalogueLike.containsPattern("foo%bar_baz"))
    }

    @Test
    fun `backslash is escaped before wildcards`() {
        assertEquals("%a\\\\b%", CatalogueLike.containsPattern("a\\b"))
    }
}
