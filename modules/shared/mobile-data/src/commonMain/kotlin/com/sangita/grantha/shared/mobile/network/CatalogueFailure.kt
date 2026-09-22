package com.sangita.grantha.shared.mobile.network

sealed class CatalogueFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Validation(message: String) : CatalogueFailure(message)
    class NotFound(message: String = "This composition is not available.") : CatalogueFailure(message)

    /**
     * [serverMessage] is the discovery route's `{ message }` body (HTTP 503).
     * Catalogue outages leave it null so the offline copy stays unchanged.
     */
    class Unavailable(
        message: String = "The catalogue could not be reached.",
        cause: Throwable? = null,
        val serverMessage: String? = null,
    ) : CatalogueFailure(serverMessage?.takeIf { it.isNotBlank() } ?: message, cause)

    class Timeout(message: String = "The request timed out.", cause: Throwable? = null) :
        CatalogueFailure(message, cause)
    class Cancelled : CatalogueFailure("Request cancelled")
}
