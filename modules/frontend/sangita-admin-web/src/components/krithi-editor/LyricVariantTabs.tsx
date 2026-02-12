// =============================================================================
// LyricVariantTabs — Read-only tabs for lyric variants in the Krithi Editor
// TRACK-057 T57.8/T57.9
// =============================================================================

import React, { useState } from 'react';

interface LyricVariant {
    id: string;
    language: string;
    script: string;
    content: string;
    sourceLabel?: string | null;
}

interface LyricVariantTabsProps {
    variants: LyricVariant[];
}

const languageLabels: Record<string, string> = {
    en: 'English (IAST)',
    sa: 'Sanskrit (Devanagari)',
    ta: 'Tamil',
    te: 'Telugu',
    kn: 'Kannada',
    ml: 'Malayalam',
    hi: 'Hindi',
};

const scriptLabels: Record<string, string> = {
    latin: 'Latin',
    devanagari: 'Devanagari',
    tamil: 'Tamil',
    telugu: 'Telugu',
    kannada: 'Kannada',
    malayalam: 'Malayalam',
};

const LyricVariantTabs: React.FC<LyricVariantTabsProps> = ({ variants }) => {
    const [activeTab, setActiveTab] = useState(0);

    if (!variants || variants.length === 0) {
        return (
            <div className="bg-slate-50 border border-border-light rounded-xl p-6 text-center">
                <span className="material-symbols-outlined text-3xl text-ink-300 mb-2 block">translate</span>
                <p className="text-sm text-ink-500">No lyric variants available for this Krithi.</p>
                <p className="text-xs text-ink-400 mt-1">
                    Submit an enrichment extraction to add variants in other languages or scripts.
                </p>
            </div>
        );
    }

    const active = variants[activeTab];
    const tabLabel = (v: LyricVariant) => {
        const lang = languageLabels[v.language] || v.language?.toUpperCase() || 'Unknown';
        const script = scriptLabels[v.script] || v.script || '';
        return script ? `${lang}` : lang;
    };

    return (
        <div className="space-y-4">
            {/* Tab Bar */}
            <div className="flex items-center gap-1 border-b border-border-light">
                {variants.map((variant, idx) => (
                    <button
                        key={variant.id}
                        onClick={() => setActiveTab(idx)}
                        className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${activeTab === idx
                                ? 'border-primary text-primary'
                                : 'border-transparent text-ink-500 hover:text-ink-700 hover:border-ink-200'
                            }`}
                    >
                        <span className="material-symbols-outlined text-sm align-middle mr-1">
                            {variant.script === 'devanagari' ? 'language_hindi_devanagari'
                                : variant.script === 'tamil' ? 'language_tamil'
                                    : 'translate'}
                        </span>
                        {tabLabel(variant)}
                    </button>
                ))}
            </div>

            {/* Tab Content */}
            {active && (
                <div className="bg-white rounded-xl border border-border-light">
                    {/* Meta */}
                    <div className="px-5 py-3 border-b border-border-light flex items-center justify-between">
                        <div className="flex items-center gap-3">
                            <span className="text-xs font-semibold text-ink-500 uppercase tracking-wider">
                                {tabLabel(active)}
                            </span>
                            {active.sourceLabel && (
                                <span className="text-xs text-ink-400">
                                    Source: {active.sourceLabel}
                                </span>
                            )}
                        </div>
                        <span className="inline-flex items-center gap-1 px-2 py-0.5 text-[10px] font-bold bg-slate-100 text-ink-500 rounded-full">
                            <span className="material-symbols-outlined text-xs">lock</span>
                            READ-ONLY
                        </span>
                    </div>

                    {/* Lyric Content */}
                    <div className="p-5">
                        <pre className={`text-sm leading-relaxed whitespace-pre-wrap break-words font-serif ${active.script === 'devanagari' ? 'text-lg' : ''
                            }`}>
                            {active.content}
                        </pre>
                    </div>
                </div>
            )}
        </div>
    );
};

export default LyricVariantTabs;
