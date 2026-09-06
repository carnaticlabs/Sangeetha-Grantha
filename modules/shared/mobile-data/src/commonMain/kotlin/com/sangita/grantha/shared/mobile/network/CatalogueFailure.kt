package com.sangita.grantha.shared.mobile.network

sealed class CatalogueFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class Validation(message: String) : CatalogueFailure(message)
    class NotFound(message: String = "This composition is not available.") : CatalogueFailure(message)
    class Unavailable(message: String = "The catalogue could not be reached.", cause: Throwable? = null) :
        CatalogueFailure(message, cause)
    class Timeout(message: String = "The request timed out.", cause: Throwable? = null) :
        CatalogueFailure(message, cause)
    class Cancelled : CatalogueFailure("Request cancelled")
}
