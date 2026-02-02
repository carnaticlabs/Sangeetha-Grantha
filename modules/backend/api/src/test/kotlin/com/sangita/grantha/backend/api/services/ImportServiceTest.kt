package com.sangita.grantha.backend.api.services

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.api.support.TestDatabaseFactory
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ImportServiceTest {
    private lateinit var dal: SangitaDal
    private lateinit var service: IImportService

    @BeforeEach
    fun setup() {
        TestDatabaseFactory.connectTestDb()
        dal = com.sangita.grantha.backend.dal.SangitaDalImpl()
        val dummyReviewer = object : ImportReviewer {
            override suspend fun reviewImport(id: kotlin.uuid.Uuid, request: ImportReviewRequest) =
                throw UnsupportedOperationException("Not used in tests")
        }
        val autoApproval = AutoApprovalService(dummyReviewer)
        val env = com.sangita.grantha.backend.api.config.ApiEnvironment(
            adminToken = "test",
            geminiApiKey = "test"
        )
        val entityResolver = EntityResolutionServiceImpl(dal, NameNormalizationService())
        service = ImportServiceImpl(dal, env, entityResolver) { autoApproval }
    }

    @AfterEach
    fun teardown() {
        TestDatabaseFactory.reset()
    }

    @Test
    fun `submitImports stores imports`() = runTest {
        val created = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "test-key",
                    rawTitle = "Test Krithi"
                )
            )
        )

        assertEquals(1, created.size)
        assertNotNull(created.first().id)

        val imports = service.getImports()
        assertEquals(1, imports.size)
    }

    @Test
    fun `reviewImport updates status`() = runTest {
        val created = service.submitImports(
            listOf(
                ImportKrithiRequest(
                    source = "TestSource",
                    sourceKey = "review-key",
                    rawTitle = "Review Krithi"
                )
            )
        )

        val importId = created.first().id
        val updated = service.reviewImport(importId, ImportReviewRequest(status = ImportStatusDto.REJECTED))

        assertEquals(ImportStatusDto.REJECTED, updated.importStatus)
    }
}
