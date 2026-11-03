import React from 'react';
import { FRAME, paletteFor, TEXT_SIZE_PX } from './previewTokens';
import type { PreviewPreferences, PreviewTab } from '../../types/mobilePreview.types';

const TABS: { tab: PreviewTab; label: string; icon: string }[] = [
    { tab: 'Home', label: 'Home', icon: 'home' },
    { tab: 'Explore', label: 'Explore', icon: 'search' },
    { tab: 'Library', label: 'Library', icon: 'bookmarks' },
    { tab: 'Settings', label: 'Settings', icon: 'settings' },
];

interface PhoneFrameProps {
    preferences: PreviewPreferences;
    selectedTab: PreviewTab;
    onSelectTab: (tab: PreviewTab) => void;
    /** Shown in the top bar; null on a tab root. */
    onBack: (() => void) | null;
    title: string;
    subtitle?: string | null;
    children: React.ReactNode;
}

/** A fixed-size phone screen themed only by the ported Rasika tokens. Prop-driven; no data fetching. */
const PhoneFrame: React.FC<PhoneFrameProps> = ({ preferences, selectedTab, onSelectTab, onBack, title, subtitle, children }) => {
    const p = paletteFor(preferences.appearance);
    return (
        <div
            data-testid="phone-frame"
            className="preview-scale mx-auto shrink-0 overflow-hidden border-[10px] border-ink-900 shadow-xl flex flex-col"
            style={{
                width: FRAME.width + 20,
                height: FRAME.height + 20,
                borderRadius: FRAME.radius,
                background: p.ground,
                color: p.ink,
                fontSize: TEXT_SIZE_PX[preferences.textSize],
                '--preview-px': `${TEXT_SIZE_PX[preferences.textSize]}px`,
            } as React.CSSProperties}
        >
            <header
                className="flex items-center gap-2 px-4 h-14 shrink-0"
                style={{ background: p.creamDeep, borderBottom: `1px solid ${p.hairline}` }}
            >
                {onBack ? (
                    <button type="button" aria-label="Back" onClick={onBack} className="h-9 w-9 rounded-full" style={{ color: p.primaryDeep }}>
                        <span className="material-symbols-outlined">arrow_back</span>
                    </button>
                ) : null}
                <div className="min-w-0">
                    <h2 className="font-display text-lg font-medium truncate">{title}</h2>
                    {subtitle ? <p className="text-xs truncate" style={{ color: p.inkSoft }}>{subtitle}</p> : null}
                </div>
            </header>
            <div className="flex-1 overflow-y-auto" data-testid="phone-screen">{children}</div>
            <nav aria-label="Preview tabs" className="flex shrink-0" style={{ background: p.creamDeep, borderTop: `1px solid ${p.hairline}` }}>
                {TABS.map(({ tab, label, icon }) => {
                    const active = tab === selectedTab;
                    return (
                        <button
                            key={tab}
                            type="button"
                            aria-current={active ? 'page' : undefined}
                            onClick={() => onSelectTab(tab)}
                            className="flex-1 flex flex-col items-center gap-0.5 py-2 text-xs font-semibold"
                            style={{ color: active ? p.primaryDeep : p.inkSoft }}
                        >
                            <span className="material-symbols-outlined">{icon}</span>
                            {label}
                        </button>
                    );
                })}
            </nav>
        </div>
    );
};

export default PhoneFrame;
