package com.sangita.grantha.backend.dal

import com.sangita.grantha.backend.dal.repositories.AuditLogRepository
import com.sangita.grantha.backend.dal.repositories.BulkImportRepository
import com.sangita.grantha.backend.dal.repositories.ComposerAliasRepository
import com.sangita.grantha.backend.dal.repositories.ComposerRepository
import com.sangita.grantha.backend.dal.repositories.DeityRepository
import com.sangita.grantha.backend.dal.repositories.EntityResolutionCacheRepository
import com.sangita.grantha.backend.dal.repositories.ImportRepository
import com.sangita.grantha.backend.dal.repositories.KrithiNotationRepository
import com.sangita.grantha.backend.dal.repositories.KrithiRepository
//import com.sangita.grantha.backend.dal.repositories.NotationRepository
import com.sangita.grantha.backend.dal.repositories.RagaRepository
//import com.sangita.grantha.backend.dal.repositories.RoleRepository
import com.sangita.grantha.backend.dal.repositories.SampradayaRepository
import com.sangita.grantha.backend.dal.repositories.TagRepository
import com.sangita.grantha.backend.dal.repositories.TalaRepository
import com.sangita.grantha.backend.dal.repositories.TempleRepository
import com.sangita.grantha.backend.dal.repositories.TempleSourceCacheRepository
import com.sangita.grantha.backend.dal.repositories.UserRepository
//import com.sangita.grantha.backend.dal.repositories.UserRoleRepository

interface SangitaDal {
    val users: UserRepository
//    val roles: RoleRepository
//    val userRoles: UserRoleRepository
    val krithis: KrithiRepository
    val composerAliases: ComposerAliasRepository
    val composers: ComposerRepository
    val ragas: RagaRepository
    val talas: TalaRepository
    val deities: DeityRepository
    val temples: TempleRepository
    val imports: ImportRepository
    val auditLogs: AuditLogRepository
    val bulkImport: BulkImportRepository
//    val notations: NotationRepository
    val entityResolutionCache: EntityResolutionCacheRepository
    val templeSourceCache: TempleSourceCacheRepository
    val sampradayas: SampradayaRepository
    val tags: TagRepository
    val krithiNotation: KrithiNotationRepository
}

class SangitaDalImpl : SangitaDal {
    override val users = UserRepository()
//    override val roles = RoleRepository()
//    override val userRoles = UserRoleRepository()
    override val krithis = KrithiRepository()
    override val composerAliases = ComposerAliasRepository()
    override val composers = ComposerRepository(composerAliases)
    override val ragas = RagaRepository()
    override val talas = TalaRepository()
    override val deities = DeityRepository()
    override val temples = TempleRepository()
    override val imports = ImportRepository()
    override val auditLogs = AuditLogRepository()
    override val bulkImport = BulkImportRepository()
//    override val notations = NotationRepository()
    override val entityResolutionCache = EntityResolutionCacheRepository()
    override val templeSourceCache = TempleSourceCacheRepository()
    override val sampradayas = SampradayaRepository()
    override val tags = TagRepository()
    override val krithiNotation = KrithiNotationRepository()
}
