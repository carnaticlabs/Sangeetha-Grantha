package com.sangita.grantha.shared.presentation.components

import com.sangita.grantha.shared.mobile.network.CatalogueFailure
import com.sangita.grantha.shared.presentation.RasikaCopy

sealed class LoadState {
    data object Idle : LoadState()
    data object Loading : LoadState()
    data object Empty : LoadState()
    data class Error(val message: String, val retryable: Boolean = true) : LoadState()
}

fun CatalogueFailure.userMessage(): String = when (this) {
    is CatalogueFailure.NotFound -> RasikaCopy.UNAVAILABLE
    is CatalogueFailure.Validation -> message ?: RasikaCopy.ERROR_TITLE
    is CatalogueFailure.Timeout -> RasikaCopy.OFFLINE_BODY
    is CatalogueFailure.Unavailable ->
        serverMessage?.takeIf { it.isNotBlank() } ?: RasikaCopy.OFFLINE_BODY
    is CatalogueFailure.Cancelled -> RasikaCopy.ERROR_TITLE
}
