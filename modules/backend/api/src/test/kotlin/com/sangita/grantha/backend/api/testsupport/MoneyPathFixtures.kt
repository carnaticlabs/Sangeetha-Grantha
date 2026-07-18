package com.sangita.grantha.backend.api.testsupport

import com.sangita.grantha.backend.api.models.ImportKrithiRequest
import com.sangita.grantha.backend.api.models.ImportReviewRequest
import com.sangita.grantha.backend.dal.SangitaDal
import com.sangita.grantha.backend.api.services.IImportService
import com.sangita.grantha.shared.domain.model.ImportStatusDto
import com.sangita.grantha.shared.domain.model.import.CanonicalExtractionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricSectionDto
import com.sangita.grantha.shared.domain.model.import.CanonicalLyricVariantDto
import kotlinx.serialization.json.Json
import java.nio.file.Path
import kotlin.uuid.Uuid

/**
 * TRACK-112 Phase 3 — fixture builders for the money-path suites.
 *
 * These live in the **api** test source set rather than `:modules:backend:test-support` because
 * they build api-module request models (`ImportKrithiRequest`, …) and test-support depends only on
 * `:modules:backend:dal`. Putting them there would invert the module dependency. Anything that
 * needs only DAL/domain types belongs in `TestFixtures` instead.
 *
 * Style follows the existing `TestFixtures`: default-argument factory functions rather than a
 * receiver-lambda DSL, so a call site overrides only the field the scenario is actually about.
 */
object MoneyPathFixtures {

    // ── Deterministic identifiers ────────────────────────────────────────

    /**
     * A stable UUID for a given seed — same seed, same value, every run.
     *
     * Use where a test asserts on an identifier or needs two references to agree. Do **not** use
     * for rows that must be unique across tests in the same class: the per-test truncate clears
     * transactional tables, but reference rows persist, so prefer [Uuid.random] for throwaway
     * entities and reserve seeded ids for the handful the assertions actually name.
     */
    fun testUuid(seed: Int): Uuid =
        Uuid.parse("00000000-0000-7000-8000-%012d".format(seed))

    /** A stable source URL per seed, so idempotency/dedup scenarios can restate the same key. */
    fun testSourceKey(seed: Int, host: String = "example.com"): String =
        "http://$host/money-path/$seed"

    // ── Request builders ─────────────────────────────────────────────────

    /**
     * A well-formed import request. Defaults resolve against the real `R__` reference seed
     * (Tyagaraja / Kalyani / Adi), so entity resolution finds seeded rows rather than creating
     * duplicates — see `TestFixtures.seedReferenceData`.
     */
    fun anImportRequest(
        source: String = "money-path-test",
        sourceKey: String = testSourceKey(1),
        rawTitle: String = "Nagumomu Ganaleni",
        rawComposer: String? = "Tyagaraja",
        rawRaga: String? = "Kalyani",
        rawTala: String? = "Adi",
        rawLanguage: String? = "te",
        rawLyrics: String? = null,
        rawDeity: String? = null,
        rawTemple: String? = null,
    ): ImportKrithiRequest = ImportKrithiRequest(
        source = source,
        sourceKey = sourceKey,
        rawTitle = rawTitle,
        rawComposer = rawComposer,
        rawRaga = rawRaga,
        rawTala = rawTala,
        rawLanguage = rawLanguage,
        rawLyrics = rawLyrics,
        rawDeity = rawDeity,
        rawTemple = rawTemple,
    )

    /**
     * A batch of [count] valid import requests with distinct source keys, optionally seeded with
     * [malformed] rows that omit the composer — the field `reviewImport` requires to create a
     * krithi. Used by S5 (partial-failure batches).
     */
    fun anImportBatch(
        count: Int,
        malformed: Int = 0,
        keyPrefix: String = "batch",
    ): List<ImportKrithiRequest> {
        require(malformed <= count) { "malformed ($malformed) cannot exceed count ($count)" }
        return (0 until count).map { i ->
            anImportRequest(
                sourceKey = "http://example.com/$keyPrefix/$i",
                rawTitle = "Batch Krithi $i",
                // The malformed rows are the tail of the batch, so the good ones keep stable keys.
                rawComposer = if (i >= count - malformed) null else "Tyagaraja",
            )
        }
    }

    // ── Persisted-state builders ─────────────────────────────────────────

    /** Create a real user row. Revision attribution and JWT `userId` claims both FK into `users`. */
    suspend fun aCurator(dal: SangitaDal, name: String = "Test Curator"): Uuid =
        dal.users.create(email = "curator-${Uuid.random()}@example.test", fullName = name).id

    /** Submit one import and return its id, without reviewing it. */
    suspend fun aPendingImport(
        importService: IImportService,
        request: ImportKrithiRequest = anImportRequest(),
    ): Uuid = importService.submitImports(listOf(request)).first().id

    /**
     * Drive one import all the way to canon and return the resulting krithi id.
     *
     * Attributes the approval to a real curator so the ADR-014 revision is actually written —
     * an approval with neither a resolvable extraction nor a reviewer is deliberately skipped
     * by `ImportService`, which silently yields a krithi with no revision.
     */
    suspend fun anApprovedKrithi(
        dal: SangitaDal,
        importService: IImportService,
        request: ImportKrithiRequest = anImportRequest(),
        reviewerUserId: Uuid? = null,
    ): Uuid {
        val importId = aPendingImport(importService, request)
        val reviewer = reviewerUserId ?: aCurator(dal)
        return importService.reviewImport(
            importId,
            ImportReviewRequest(status = ImportStatusDto.APPROVED),
            reviewerUserId = reviewer,
        ).mappedKrithiId ?: error("approval did not produce a canonical krithi")
    }

    // ── Golden payloads ──────────────────────────────────────────────────

    /**
     * The cross-language golden extraction document.
     *
     * TRACK-112 Phase 3 called for golden payloads under `src/test/resources/payloads/`. One
     * already exists in a better place: `shared/domain/model/import/fixtures/canonical-extraction-golden.json`
     * is the **shared** fixture the Python worker suite and `CanonicalExtractionGoldenFixtureTest`
     * both assert against (TRACK-113 W2), so it pins the Kotlin↔Python contract rather than just
     * this suite's expectations. Copying it into the api test resources would fork that contract,
     * so the money-path suites reuse it through this accessor instead.
     */
    fun goldenExtraction(): CanonicalExtractionDto =
        Json.decodeFromString(CanonicalExtractionDto.serializer(), goldenExtractionJson())

    fun goldenExtractionJson(): String {
        var dir = Path.of(System.getProperty("user.dir")).toAbsolutePath()
        while (true) {
            val candidate = dir.resolve("shared/domain/model/import/fixtures/canonical-extraction-golden.json")
            if (candidate.toFile().exists()) return candidate.toFile().readText()
            dir = dir.parent ?: error("golden fixture not found walking up from user.dir")
        }
    }

    /** A canonical payload carrying [sectionTexts] as one latin-script variant, ready to persist. */
    fun aCanonicalPayloadJson(
        title: String = "Nagumomu Ganaleni",
        composer: String = "Tyagaraja",
        ragaName: String = "Kalyani",
        tala: String = "Adi",
        sourceUrl: String = testSourceKey(1),
        language: String = "te",
        sectionTexts: List<String> = listOf("Pallavi text", "Anupallavi text", "Charanam text"),
    ): String {
        val extraction = com.sangita.grantha.backend.testsupport.TestFixtures.buildCanonicalExtraction(
            title = title,
            composer = composer,
            ragaName = ragaName,
            tala = tala,
            sourceUrl = sourceUrl,
            lyricVariants = listOf(
                CanonicalLyricVariantDto(
                    language = language,
                    script = "latin",
                    sections = sectionTexts.mapIndexed { i, text ->
                        CanonicalLyricSectionDto(sectionOrder = i + 1, text = text)
                    },
                )
            ),
        )
        return Json.encodeToString(CanonicalExtractionDto.serializer(), extraction)
    }
}
