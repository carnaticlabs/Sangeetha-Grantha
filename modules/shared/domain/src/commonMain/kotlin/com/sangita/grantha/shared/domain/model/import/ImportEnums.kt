package com.sangita.grantha.shared.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class ImportStatusDto { PENDING, IN_REVIEW, APPROVED, MAPPED, REJECTED, DISCARDED }

@Serializable
enum class BatchStatusDto { PENDING, RUNNING, PAUSED, SUCCEEDED, FAILED, CANCELLED }

@Serializable
enum class JobTypeDto { MANIFEST_INGEST, SCRAPE, ENRICH, ENTITY_RESOLUTION, REVIEW_PREP }

@Serializable
enum class TaskStatusDto { PENDING, RUNNING, SUCCEEDED, FAILED, RETRYABLE, BLOCKED, CANCELLED }
