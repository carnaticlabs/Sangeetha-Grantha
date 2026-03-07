package com.sangita.grantha.backend.api.services

import kotlinx.serialization.Serializable

@Serializable
data class AuditReport(
    val sectionCountMismatch: SectionCountAudit,
    val labelSequenceMismatch: LabelSequenceAudit,
    val orphanedLyricBlobs: OrphanedBlobsAudit,
    val summary: AuditSummary,
)

@Serializable
data class SectionCountAudit(
    val mismatches: List<SectionCountMismatch>,
    val composerSummary: List<ComposerAuditSummary>,
    val krithisWithoutSections: List<KrithiWithoutSections>,
)

@Serializable
data class SectionCountMismatch(
    val krithiId: String,
    val title: String,
    val composer: String,
    val canonicalSectionCount: Int,
    val language: String,
    val script: String,
    val isPrimary: Boolean,
    val variantSectionCount: Int,
    val sectionDrift: Int,
)

@Serializable
data class ComposerAuditSummary(
    val composer: String,
    val mismatchedKrithis: Int,
    val totalKrithis: Int,
    val mismatchPct: Double,
)

@Serializable
data class KrithiWithoutSections(
    val krithiId: String,
    val title: String,
    val composer: String,
    val variantCount: Int,
)

@Serializable
data class LabelSequenceAudit(
    val mismatches: List<LabelSequenceMismatch>,
    val composerSummary: List<ComposerAuditSummary>,
)

@Serializable
data class LabelSequenceMismatch(
    val krithiId: String,
    val title: String,
    val composer: String,
    val distinctSequences: Int,
    val language: String,
    val script: String,
    val isPrimary: Boolean,
    val sectionSequence: String,
)

@Serializable
data class OrphanedBlobsAudit(
    val orphanedVariants: List<OrphanedVariant>,
    val composerSummary: List<ComposerOrphanSummary>,
    val crossKrithiReferences: List<CrossKrithiReference>,
    val emptyShells: List<EmptyKrithiShell>,
)

@Serializable
data class OrphanedVariant(
    val krithiId: String,
    val title: String,
    val composer: String,
    val language: String,
    val script: String,
    val isPrimary: Boolean,
    val lyricsLength: Int,
)

@Serializable
data class ComposerOrphanSummary(
    val composer: String,
    val orphanedVariants: Int,
    val affectedKrithis: Int,
    val totalKrithis: Int,
    val orphanPct: Double,
)

@Serializable
data class CrossKrithiReference(
    val lyricSectionId: String,
    val variantKrithiTitle: String,
    val sectionKrithiTitle: String,
)

@Serializable
data class EmptyKrithiShell(
    val krithiId: String,
    val title: String,
    val composer: String,
    val workflowState: String,
)

@Serializable
data class AuditSummary(
    val totalKrithis: Int,
    val sectionCountMismatchCount: Int,
    val labelSequenceMismatchCount: Int,
    val orphanedVariantCount: Int,
    val emptyShellCount: Int,
    val crossKrithiReferenceCount: Int,
)
