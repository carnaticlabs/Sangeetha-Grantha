import { useCallback, useReducer, useState } from 'react';
import * as nav from '../components/mobile-preview/previewNavigator';
import type { PreviewDestination, PreviewPreferences, PreviewTab } from '../types/mobilePreview.types';

type Action =
    | { type: 'selectTab'; tab: PreviewTab }
    | { type: 'open'; destination: PreviewDestination }
    | { type: 'replace'; destination: PreviewDestination }
    | { type: 'back' }
    | { type: 'root' };

const reduce = (state: nav.NavigatorState, action: Action): nav.NavigatorState => {
    switch (action.type) {
        case 'selectTab': return nav.selectTab(state, action.tab);
        case 'open': return nav.open(state, action.destination);
        case 'replace': return nav.replace(state, action.destination);
        case 'back': return nav.back(state);
        case 'root': return nav.returnToRoot(state);
    }
};

const PREFS_KEY = 'mobilePreview.preferences';
const DEFAULT_PREFS: PreviewPreferences = { appearance: 'light', textSize: 'medium', preferredScript: null };

const loadPrefs = (): PreviewPreferences => {
    try {
        const raw = localStorage.getItem(PREFS_KEY);
        return raw ? { ...DEFAULT_PREFS, ...(JSON.parse(raw) as Partial<PreviewPreferences>) } : DEFAULT_PREFS;
    } catch {
        return DEFAULT_PREFS;
    }
};

/**
 * Navigator state and preview-local preferences for the phone frame. Preferences live only in this
 * browser (never sent to the server); storage may be unavailable, so every access is guarded.
 */
export function useMobilePreview(initial?: PreviewDestination) {
    const [state, dispatch] = useReducer(reduce, undefined, () => {
        const start = nav.initialNavigator();
        return initial ? nav.open(nav.selectTab(start, 'Explore'), initial) : start;
    });
    const [preferences, setPreferencesState] = useState<PreviewPreferences>(loadPrefs);

    const setPreferences = useCallback((next: Partial<PreviewPreferences>) => {
        setPreferencesState((prev) => {
            const merged = { ...prev, ...next };
            try { localStorage.setItem(PREFS_KEY, JSON.stringify(merged)); } catch { /* storage unavailable */ }
            return merged;
        });
    }, []);

    return {
        state,
        destination: nav.current(state),
        canGoBack: nav.canGoBack(state),
        preferences,
        setPreferences,
        selectTab: (tab: PreviewTab) => dispatch({ type: 'selectTab', tab }),
        open: (destination: PreviewDestination) => dispatch({ type: 'open', destination }),
        replace: (destination: PreviewDestination) => dispatch({ type: 'replace', destination }),
        back: () => dispatch({ type: 'back' }),
        returnToRoot: () => dispatch({ type: 'root' }),
    };
}
