package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.ComposerAliasesTable
import com.sangita.grantha.backend.dal.tables.ComposersTable
import com.sangita.grantha.backend.dal.models.toComposerDto
import com.sangita.grantha.backend.dal.support.toKotlinUuid
import com.sangita.grantha.shared.domain.model.ComposerDto
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*

/**
 * TRACK-031: Resolve alias_normalized to canonical composer.
 */
class ComposerAliasRepository {

    /**
     * Returns the canonical composer for the given normalized alias, or null.
     */
    suspend fun findComposerByAliasNormalized(aliasNormalized: String): ComposerDto? = DatabaseFactory.dbQuery {
        val composerId = ComposerAliasesTable
            .select(ComposerAliasesTable.composerId)
            .where { ComposerAliasesTable.aliasNormalized eq aliasNormalized }
            .map { it[ComposerAliasesTable.composerId] }
            .singleOrNull()
            ?: return@dbQuery null

        ComposersTable
            .selectAll()
            .where { ComposersTable.id eq composerId }
            .map { it.toComposerDto() }
            .singleOrNull()
    }

    /**
     * Returns the canonical composer ID for the given normalized alias, or null.
     */
    suspend fun findComposerIdByAliasNormalized(aliasNormalized: String): Uuid? = DatabaseFactory.dbQuery {
        ComposerAliasesTable
            .select(ComposerAliasesTable.composerId)
            .where { ComposerAliasesTable.aliasNormalized eq aliasNormalized }
            .map { it[ComposerAliasesTable.composerId].toKotlinUuid() }
            .singleOrNull()
    }

    /**
     * Load all alias -> composer_id pairs (for in-memory cache in EntityResolutionService).
     */
    suspend fun loadAliasToComposerIdMap(): Map<String, Uuid> = DatabaseFactory.dbQuery {
        ComposerAliasesTable
            .selectAll()
            .associate { row ->
                row[ComposerAliasesTable.aliasNormalized] to row[ComposerAliasesTable.composerId].toKotlinUuid()
            }
    }
}
