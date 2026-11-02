/**
 * Pure port of `RasikaNavigator` (kotlin: navigation/RasikaDestination.kt): one back stack per tab,
 * a depth cap of 8, open/replace/back/returnToRoot. Behaviour must stay identical; the unit test pins it.
 */
import type { PreviewDestination, PreviewTab } from '../../types/mobilePreview.types';

export const MAX_STACK_DEPTH = 8;

export interface NavigatorState {
    selectedTab: PreviewTab;
    stacks: Record<PreviewTab, PreviewDestination[]>;
    stackLimitReached: boolean;
}

export const initialNavigator = (tab: PreviewTab = 'Home'): NavigatorState => ({
    selectedTab: tab,
    stacks: {
        Home: [{ kind: 'Home' }],
        Explore: [{ kind: 'Explore' }],
        Library: [{ kind: 'Library' }],
        Settings: [{ kind: 'Settings' }],
    },
    stackLimitReached: false,
});

export const current = (s: NavigatorState): PreviewDestination => {
    const stack = s.stacks[s.selectedTab];
    return stack[stack.length - 1];
};

const same = (a: PreviewDestination, b: PreviewDestination): boolean =>
    JSON.stringify(a) === JSON.stringify(b);

const withStack = (s: NavigatorState, stack: PreviewDestination[]): NavigatorState => ({
    ...s,
    stacks: { ...s.stacks, [s.selectedTab]: stack },
});

export const selectTab = (s: NavigatorState, tab: PreviewTab): NavigatorState => ({ ...s, selectedTab: tab });

export const open = (s: NavigatorState, destination: PreviewDestination): NavigatorState => {
    const stack = s.stacks[s.selectedTab];
    if (same(stack[stack.length - 1], destination)) return s;
    if (stack.length >= MAX_STACK_DEPTH) return { ...s, stackLimitReached: true };
    return withStack(s, [...stack, destination]);
};

/** Swaps the top destination instead of pushing, so stepping through a cycle does not use the back stack. */
export const replace = (s: NavigatorState, destination: PreviewDestination): NavigatorState => {
    const stack = s.stacks[s.selectedTab];
    if (stack.length <= 1) return open(s, destination);
    return withStack(s, [...stack.slice(0, -1), destination]);
};

export const openOn = (s: NavigatorState, tab: PreviewTab, destination: PreviewDestination): NavigatorState =>
    open(selectTab(s, tab), destination);

export const back = (s: NavigatorState): NavigatorState => {
    const stack = s.stacks[s.selectedTab];
    return stack.length <= 1 ? s : withStack(s, stack.slice(0, -1));
};

export const returnToRoot = (s: NavigatorState): NavigatorState =>
    ({ ...withStack(s, [s.stacks[s.selectedTab][0]]), stackLimitReached: false });

export const acknowledgeStackLimit = (s: NavigatorState): NavigatorState => ({ ...s, stackLimitReached: false });

export const canGoBack = (s: NavigatorState): boolean => s.stacks[s.selectedTab].length > 1;
