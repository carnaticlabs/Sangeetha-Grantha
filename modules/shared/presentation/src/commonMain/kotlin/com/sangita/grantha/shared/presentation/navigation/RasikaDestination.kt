package com.sangita.grantha.shared.presentation.navigation

import kotlin.uuid.Uuid

enum class RasikaTab {
    Home,
    Explore,
    Library,
    Settings,
}

sealed class RasikaDestination {
    data object Home : RasikaDestination()
    data object Explore : RasikaDestination()
    data object Library : RasikaDestination()
    data object Settings : RasikaDestination()
    data object Browse : RasikaDestination()
    data class RagaDetail(val ragaId: Uuid) : RasikaDestination()
    data class ComposerDetail(val composerId: Uuid) : RasikaDestination()
    data class KrithiReader(val krithiId: Uuid) : RasikaDestination()
}

class RasikaNavigator(
    initialTab: RasikaTab = RasikaTab.Home,
) {
    companion object {
        const val MAX_STACK_DEPTH: Int = 8
    }

    private val stacks = mutableMapOf(
        RasikaTab.Home to mutableListOf<RasikaDestination>(RasikaDestination.Home),
        RasikaTab.Explore to mutableListOf<RasikaDestination>(RasikaDestination.Explore),
        RasikaTab.Library to mutableListOf<RasikaDestination>(RasikaDestination.Library),
        RasikaTab.Settings to mutableListOf<RasikaDestination>(RasikaDestination.Settings),
    )
    var selectedTab: RasikaTab = initialTab
        private set
    var stackLimitReached: Boolean = false
        private set

    val current: RasikaDestination
        get() = stacks.getValue(selectedTab).last()

    fun selectTab(tab: RasikaTab) {
        selectedTab = tab
    }

    fun open(destination: RasikaDestination): Boolean {
        val stack = stacks.getValue(selectedTab)
        if (stack.last() == destination) return true
        if (stack.size >= MAX_STACK_DEPTH) {
            stackLimitReached = true
            return false
        }
        stack.add(destination)
        return true
    }

    fun openOn(tab: RasikaTab, destination: RasikaDestination): Boolean {
        selectedTab = tab
        return open(destination)
    }

    /** @return true if a destination was popped */
    fun back(): Boolean {
        val stack = stacks.getValue(selectedTab)
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun returnToRoot() {
        val stack = stacks.getValue(selectedTab)
        val root = stack.first()
        stack.clear()
        stack.add(root)
        stackLimitReached = false
    }

    fun acknowledgeStackLimit() {
        stackLimitReached = false
    }

    fun stackSnapshot(): List<RasikaDestination> = stacks.getValue(selectedTab).toList()
}
