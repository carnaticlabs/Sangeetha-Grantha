import React, { useState, useEffect } from 'react';
import { SCRIPT_CODE_OPTIONS } from '../utils/enums';

interface TransliterationModalProps {
    isOpen: boolean;
    onClose: () => void;
    onTransliterate: (content: string, targetScript: string) => Promise<string>;
    onConfirm: (results: { sectionId: string; text: string }[], targetScript: string) => void;
    sections: { id: string; title: string }[];
    initialData?: Record<string, string>;
}

export const TransliterationModal: React.FC<TransliterationModalProps> = ({
    isOpen, onClose, onTransliterate, onConfirm, sections, initialData = {}
}) => {
    // Map sectionId -> content
    const [sectionContent, setSectionContent] = useState<Record<string, string>>(initialData);
    const [targetScript, setTargetScript] = useState('latn');
    // Map sectionId -> result
    const [sectionResults, setSectionResults] = useState<Record<string, string>>({});
    const [loading, setLoading] = useState(false);

    // Update content when initialData changes or modal opens
    useEffect(() => {
        if (isOpen) {
            setSectionContent(initialData);
            setSectionResults({});
        }
    }, [isOpen, initialData]);

    if (!isOpen) return null;

    const handleGenerate = async () => {
        setLoading(true);
        try {
            const results: Record<string, string> = {};
            // Process all sections that have content
            const promises = sections.map(async (section) => {
                const text = sectionContent[section.id];
                if (text && text.trim()) {
                    try {
                        const res = await onTransliterate(text, targetScript);
                        results[section.id] = res;
                    } catch (e) {
                        console.error(`Failed to transliterate section ${section.title}`, e);
                    }
                }
            });

            await Promise.all(promises);
            setSectionResults(results);
        } catch (e) {
            alert('Transliteration failed');
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = () => {
        const formattedResults = Object.entries(sectionResults).map(([sectionId, text]) => ({
            sectionId,
            text
        }));
        onConfirm(formattedResults, targetScript);
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div className="bg-white rounded-xl shadow-xl w-full max-w-4xl max-h-[90vh] flex flex-col overflow-hidden animate-fadeIn">
                <div className="p-6 border-b border-border-light flex justify-between items-center bg-purple-50">
                    <div className="flex items-center gap-3">
                        <span className="material-symbols-outlined text-purple-600">auto_awesome</span>
                        <div>
                            <h2 className="text-xl font-bold text-ink-900">AI Transliteration</h2>
                            <p className="text-xs text-ink-500">Powered by Gemini 2.0 Flash</p>
                        </div>
                    </div>
                    <button onClick={onClose} className="text-ink-400 hover:text-ink-600 transition-colors">
                        <span className="material-symbols-outlined">close</span>
                    </button>
                </div>

                <div className="flex-1 overflow-y-auto p-6 space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6 h-full">
                        {/* Left: Source */}
                        <div className="space-y-4 flex flex-col h-full overflow-hidden">
                            <label className="block text-sm font-bold text-ink-900 flex-shrink-0">Source Content</label>
                            <div className="flex-1 overflow-y-auto space-y-4 pr-2">
                                {sections.length > 0 ? (
                                    sections.map((section) => (
                                        <div key={section.id} className="space-y-2">
                                            <label className="text-xs font-semibold text-ink-500 uppercase tracking-wider">
                                                {section.title}
                                            </label>
                                            <textarea
                                                value={sectionContent[section.id] || ''}
                                                onChange={e => setSectionContent(prev => ({
                                                    ...prev,
                                                    [section.id]: e.target.value
                                                }))}
                                                className="w-full min-h-[120px] p-3 rounded-lg bg-slate-50 border border-border-light focus:ring-2 focus:ring-primary font-mono text-sm leading-relaxed resize-y"
                                                placeholder={`Enter text for ${section.title}...`}
                                            />
                                        </div>
                                    ))
                                ) : (
                                    <div className="p-8 text-center bg-slate-50 rounded-lg text-ink-500 border border-dashed border-border-light">
                                        No sections defined. Please add sections to the Krithi first.
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Right: Target/Preview */}
                        <div className="space-y-4 flex flex-col h-full overflow-hidden">
                            <div className="flex items-center justify-between flex-shrink-0">
                                <label className="block text-sm font-bold text-ink-900">Target Script</label>
                                <select
                                    value={targetScript}
                                    onChange={e => setTargetScript(e.target.value)}
                                    className="h-8 px-2 rounded border border-border-light text-sm focus:ring-2 focus:ring-primary"
                                >
                                    {SCRIPT_CODE_OPTIONS.map(opt => (
                                        <option key={opt.value} value={opt.value}>{opt.label}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex-1 overflow-y-auto space-y-4 pr-2 relative">
                                {loading && (
                                    <div className="absolute inset-0 z-10 flex flex-col items-center justify-center text-purple-600 bg-white/50 backdrop-blur-[1px] rounded-lg">
                                        <span className="material-symbols-outlined text-4xl animate-spin mb-2">autorenew</span>
                                        <p className="text-sm font-medium">Generating...</p>
                                    </div>
                                )}

                                {sections.length > 0 ? (
                                    sections.map((section) => {
                                        const result = sectionResults[section.id];

                                        return (
                                            <div key={section.id} className="space-y-2">
                                                <label className="text-xs font-semibold text-purple-600 uppercase tracking-wider">
                                                    {section.title}
                                                </label>
                                                <div className={`w-full min-h-[120px] p-3 rounded-lg border ${result ? 'bg-purple-50 border-purple-200' : 'bg-slate-50 border-dashed border-border-light'}`}>
                                                    {result ? (
                                                        <pre className="font-mono text-sm whitespace-pre-wrap leading-relaxed text-ink-900">{result}</pre>
                                                    ) : (
                                                        <div className="h-full flex items-center justify-center">
                                                            <p className="text-xs text-ink-400 italic text-center py-8">
                                                                Transliterated output for {section.title} will appear here
                                                            </p>
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                        );
                                    })
                                ) : (
                                    <div className="h-full flex flex-col items-center justify-center text-ink-400 border border-dashed border-border-light rounded-lg bg-slate-50">
                                        <span className="material-symbols-outlined text-4xl mb-2">translate</span>
                                        <p className="text-sm">Select target script and click generate</p>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="p-6 border-t border-border-light flex justify-end gap-3 bg-slate-50">
                    <button onClick={onClose} className="px-4 py-2 text-sm font-medium text-ink-600 hover:text-ink-900 transition-colors">Cancel</button>
                    <button
                        onClick={handleGenerate}
                        disabled={Object.values(sectionContent).every(v => !v?.trim()) || loading}
                        className="px-4 py-2 bg-white border border-border-light text-ink-900 rounded-lg text-sm font-medium hover:bg-slate-100 disabled:opacity-50 transition-colors"
                    >
                        {Object.keys(sectionResults).length > 0 ? 'Regenerate' : 'Generate Preview'}
                    </button>
                    {Object.keys(sectionResults).length > 0 && (
                        <button
                            onClick={handleConfirm}
                            className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 shadow-sm shadow-purple-200 transition-colors"
                        >
                            <span className="material-symbols-outlined text-[18px]">check</span>
                            Accept & Create Variant
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};
