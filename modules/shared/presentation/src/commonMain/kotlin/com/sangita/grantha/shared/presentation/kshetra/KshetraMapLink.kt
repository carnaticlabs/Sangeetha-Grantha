package com.sangita.grantha.shared.presentation.kshetra

import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTempleDetailDto
import com.sangita.grantha.shared.domain.model.catalogue.CatalogueTemplePlacePrecisionDto

/**
 * The "Open in Google Maps" link for a kshetra (TRACK-151 Spec 25). Built on the device from the
 * stored place id; no API key, no coordinates and nothing Google-derived is kept.
 */
data class KshetraMapLink(
    val url: String,
    /** Name shown to the user as the thing the link opens. */
    val targetName: String,
    /** True when the link opens the parent complex because this shrine has no pin of its own. */
    val opensParentComplex: Boolean,
) {
    companion object {
        private const val SEARCH = "https://www.google.com/maps/search/?api=1"

        /** Null when the detail carries no place: no control and no pin. */
        fun from(detail: CatalogueTempleDetailDto): KshetraMapLink? {
            val place = detail.place?.takeIf { it.googlePlaceId.isNotBlank() } ?: return null
            val inherited = place.precision == CatalogueTemplePlacePrecisionDto.PARENT_COMPLEX
            val target = if (inherited) detail.parent?.name ?: detail.name else detail.name
            return KshetraMapLink(
                url = build(target, place.googlePlaceId),
                targetName = target,
                opensParentComplex = inherited,
            )
        }

        fun build(name: String, googlePlaceId: String): String =
            "$SEARCH&query=${encode(name)}&query_place_id=${encode(googlePlaceId)}"

        private const val HEX = "0123456789ABCDEF"

        /** RFC 3986 percent-encoding of the UTF-8 bytes; only unreserved characters stay literal. */
        internal fun encode(value: String): String = buildString {
            for (byte in value.encodeToByteArray()) {
                val b = byte.toInt() and 0xFF
                val c = b.toChar()
                if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '.' || c == '_' || c == '~') {
                    append(c)
                } else {
                    append('%').append(HEX[b shr 4]).append(HEX[b and 0x0F])
                }
            }
        }
    }
}
