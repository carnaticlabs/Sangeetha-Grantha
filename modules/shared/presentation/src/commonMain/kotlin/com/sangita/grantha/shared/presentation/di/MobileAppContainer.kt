package com.sangita.grantha.shared.presentation.di

import com.sangita.grantha.shared.mobile.repository.CatalogueRepository
import com.sangita.grantha.shared.mobile.repository.FavouritesRepository
import com.sangita.grantha.shared.mobile.repository.PreferencesRepository
import com.sangita.grantha.shared.mobile.usage.MobileSession
import kotlinx.coroutines.CoroutineScope

data class MobileAppContainer(
    val catalogue: CatalogueRepository,
    val favourites: FavouritesRepository,
    val preferences: PreferencesRepository,
    val session: MobileSession,
    val appScope: CoroutineScope,
)
