package com.sangita.grantha.shared.presentation.navigation

import kotlin.uuid.Uuid

enum class RasikaTab {
    Search,
    Browse,
    Favourites,
}

sealed class RasikaDestination {
    data object Search : RasikaDestination()
    data object Browse : RasikaDestination()
    data object Favourites : RasikaDestination()
    data class RagaDetail(val ragaId: Uuid) : RasikaDestination()
    data class ComposerDetail(val composerId: Uuid) : RasikaDestination()
    data class KrithiReader(val krithiId: Uuid) : RasikaDestination()
    data object Preferences : RasikaDestination()
}

class RasikaNavigator(
    initialTab: RasikaTab = RasikaTab.Search,
) {
    private val stacks = mutableMapOf(
        RasikaTab.Search to mutableListOf<RasikaDestination>(RasikaDestination.Search),
        RasikaTab.Browse to mutableListOf<RasikaDestination>(RasikaDestination.Browse),
        RasikaTab.Favourites to mutableListOf<RasikaDestination>(RasikaDestination.Favourites),
    )
    var selectedTab: RasikaTab = initialTab
        private set

    val current: RasikaDestination
        get() = stacks.getValue(selectedTab).last()

    fun selectTab(tab: RasikaTab) {
        selectedTab = tab
    }

    fun open(destination: RasikaDestination) {
        val stack = stacks.getValue(selectedTab)
        if (stack.last() == destination) return
        stack.add(destination)
    }

    fun openOn(tab: RasikaTab, destination: RasikaDestination) {
        selectedTab = tab
        open(destination)
    }

    /** @return true if a destination was popped */
    fun back(): Boolean {
        val stack = stacks.getValue(selectedTab)
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun stackSnapshot(): List<RasikaDestination> = stacks.getValue(selectedTab).toList()
}
