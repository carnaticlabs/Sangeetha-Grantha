package com.sangita.grantha.backend.dal.tables

import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object TempleSourceCacheTable : UUIDTable("temple_source_cache") {
    val sourceUrl = text("source_url").uniqueIndex()
    val sourceDomain = text("source_domain")
    val templeName = text("temple_name")
    val templeNameNormalized = text("temple_name_normalized").index()
    val deityName = text("deity_name").nullable().index()
    val kshetraText = text("kshetra_text").nullable()
    val city = text("city").nullable()
    val state = text("state").nullable()
    val country = text("country").nullable()
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val geoSource = text("geo_source").nullable()
    val geoConfidence = text("geo_confidence").nullable()
    val notes = text("notes").nullable()
    val rawPayload = text("raw_payload").nullable()
    val fetchedAt = timestampWithTimeZone("fetched_at").nullable()
    val updatedAt = timestampWithTimeZone("updated_at").nullable()
    val error = text("error").nullable()
}
