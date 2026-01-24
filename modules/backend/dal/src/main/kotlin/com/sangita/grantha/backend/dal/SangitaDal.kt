package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.repositories.AuditLogRepository
import com.sangita.grantha.backend.dal.repositories.BulkImportRepository
import com.sangita.grantha.backend.dal.repositories.ComposerRepository
import com.sangita.grantha.backend.dal.repositories.DeityRepository
import com.sangita.grantha.backend.dal.repositories.EntityResolutionCacheRepository
import com.sangita.grantha.backend.dal.repositories.ImportRepository
import com.sangita.grantha.backend.dal.repositories.KrithiNotationRepository
import com.sangita.grantha.backend.dal.repositories.KrithiRepository
import com.sangita.grantha.backend.dal.repositories.RagaRepository
import com.sangita.grantha.backend.dal.repositories.SampradayaRepository
import com.sangita.grantha.backend.dal.repositories.TagRepository
import com.sangita.grantha.backend.dal.repositories.TalaRepository
import com.sangita.grantha.backend.dal.repositories.TempleRepository
import com.sangita.grantha.backend.dal.repositories.UserRepository

class SangitaDal(
    val composers: ComposerRepository = ComposerRepository(),
    val ragas: RagaRepository = RagaRepository(),
    val talas: TalaRepository = TalaRepository(),
    val deities: DeityRepository = DeityRepository(),
    val temples: TempleRepository = TempleRepository(),
    val tags: TagRepository = TagRepository(),
    val sampradayas: SampradayaRepository = SampradayaRepository(),
    val krithis: KrithiRepository = KrithiRepository(),
    val krithiNotation: KrithiNotationRepository = KrithiNotationRepository(),
    val imports: ImportRepository = ImportRepository(),
    val bulkImport: BulkImportRepository = BulkImportRepository(),
    val auditLogs: AuditLogRepository = AuditLogRepository(),
    val users: UserRepository = UserRepository(),
    val entityResolutionCache: EntityResolutionCacheRepository = EntityResolutionCacheRepository(), // TRACK-013
)
