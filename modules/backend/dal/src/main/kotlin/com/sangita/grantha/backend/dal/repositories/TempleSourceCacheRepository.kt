package com.sangita.grantha.backend.dal.repositories

import com.sangita.grantha.backend.dal.DatabaseFactory
import com.sangita.grantha.backend.dal.tables.TempleSourceCacheTable
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.jdbc.*
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import com.sangita.grantha.backend.dal.support.toJavaUuid


@Serializable
data class TempleSourceCacheDto(
    @Serializable(with = com.sangita.grantha.backend.dal.support.UuidSerializer::class)
    val id: UUID,
    val sourceUrl: String,
    val sourceDomain: String,
    val templeName: String,
    val deityName: String?,
    val kshetraText: String?,
    val city: String?,
    val state: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val geoSource: String?,
    val geoConfidence: String?,
    val notes: String?,
    val rawPayload: String?,
    val fetchedAt: String?, // LocalDateTime as String for Serializable
    val error: String?
)

class TempleSourceCacheRepository {

    suspend fun findByUrl(url: String): TempleSourceCacheDto? = DatabaseFactory.dbQuery {
        val query = TempleSourceCacheTable.selectAll()
        query.where { TempleSourceCacheTable.sourceUrl eq url }
            .map { mapRow(it) }
            .singleOrNull()
    }

    suspend fun save(
        sourceUrl: String,
        sourceDomain: String,
        templeName: String,
        templeNameNormalized: String,
        deityName: String?,
        kshetraText: String?,
        city: String?,
        state: String?,
        country: String?,
        latitude: Double?,
        longitude: Double?,
        geoSource: String?,
        geoConfidence: String?,
        notes: String?,
        rawPayload: String?,
        error: String?
    ): TempleSourceCacheDto = DatabaseFactory.dbQuery {
        val existing = TempleSourceCacheTable.selectAll().where { TempleSourceCacheTable.sourceUrl eq sourceUrl }.singleOrNull()
        
        if (existing != null) {
            TempleSourceCacheTable.update({ TempleSourceCacheTable.sourceUrl eq sourceUrl }) {
                it[TempleSourceCacheTable.templeName] = templeName
                it[TempleSourceCacheTable.templeNameNormalized] = templeNameNormalized
                it[TempleSourceCacheTable.deityName] = deityName
                it[TempleSourceCacheTable.kshetraText] = kshetraText
                it[TempleSourceCacheTable.city] = city
                it[TempleSourceCacheTable.state] = state
                it[TempleSourceCacheTable.country] = country
                it[TempleSourceCacheTable.latitude] = latitude
                it[TempleSourceCacheTable.longitude] = longitude
                it[TempleSourceCacheTable.geoSource] = geoSource
                it[TempleSourceCacheTable.geoConfidence] = geoConfidence
                it[TempleSourceCacheTable.notes] = notes
                it[TempleSourceCacheTable.rawPayload] = rawPayload
                it[TempleSourceCacheTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                it[TempleSourceCacheTable.error] = error
            }
            findByUrl(sourceUrl)!!
        } else {
            val id = TempleSourceCacheTable.insertAndGetId {
                it[TempleSourceCacheTable.sourceUrl] = sourceUrl
                it[TempleSourceCacheTable.sourceDomain] = sourceDomain
                it[TempleSourceCacheTable.templeName] = templeName
                it[TempleSourceCacheTable.templeNameNormalized] = templeNameNormalized
                it[TempleSourceCacheTable.deityName] = deityName
                it[TempleSourceCacheTable.kshetraText] = kshetraText
                it[TempleSourceCacheTable.city] = city
                it[TempleSourceCacheTable.state] = state
                it[TempleSourceCacheTable.country] = country
                it[TempleSourceCacheTable.latitude] = latitude
                it[TempleSourceCacheTable.longitude] = longitude
                it[TempleSourceCacheTable.geoSource] = geoSource
                it[TempleSourceCacheTable.geoConfidence] = geoConfidence
                it[TempleSourceCacheTable.notes] = notes
                it[TempleSourceCacheTable.rawPayload] = rawPayload
                it[TempleSourceCacheTable.fetchedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                it[TempleSourceCacheTable.updatedAt] = OffsetDateTime.now(ZoneOffset.UTC)
                it[TempleSourceCacheTable.error] = error
            }

            TempleSourceCacheDto(
                id = id.value,
                sourceUrl = sourceUrl,
                sourceDomain = sourceDomain,
                templeName = templeName,
                deityName = deityName,
                kshetraText = kshetraText,
                city = city,
                state = state,
                country = country,
                latitude = latitude,
                longitude = longitude,
                geoSource = geoSource,
                geoConfidence = geoConfidence,
                notes = notes,
                rawPayload = rawPayload,
                fetchedAt = OffsetDateTime.now(ZoneOffset.UTC).toString(),
                error = error
            )
        }
    }

    private fun mapRow(row: ResultRow): TempleSourceCacheDto {
        return TempleSourceCacheDto(
            id = row[TempleSourceCacheTable.id].value,
            sourceUrl = row[TempleSourceCacheTable.sourceUrl],
            sourceDomain = row[TempleSourceCacheTable.sourceDomain],
            templeName = row[TempleSourceCacheTable.templeName],
            deityName = row[TempleSourceCacheTable.deityName],
            kshetraText = row[TempleSourceCacheTable.kshetraText],
            city = row[TempleSourceCacheTable.city],
            state = row[TempleSourceCacheTable.state],
            country = row[TempleSourceCacheTable.country],
            latitude = row[TempleSourceCacheTable.latitude],
            longitude = row[TempleSourceCacheTable.longitude],
            geoSource = row[TempleSourceCacheTable.geoSource],
            geoConfidence = row[TempleSourceCacheTable.geoConfidence],
            notes = row[TempleSourceCacheTable.notes],
            rawPayload = row[TempleSourceCacheTable.rawPayload],
            fetchedAt = row[TempleSourceCacheTable.fetchedAt]?.toString(),
            error = row[TempleSourceCacheTable.error]
        )
    }
}
