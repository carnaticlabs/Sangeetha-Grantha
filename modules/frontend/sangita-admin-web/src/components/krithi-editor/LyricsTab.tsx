import React, { useState } from 'react';
import { TabProps } from '../../types/krithi-editor.types';
import { KrithiLyricVariant, Sampradaya } from '../../types';
import { LANGUAGE_CODE_OPTIONS, SCRIPT_CODE_OPTIONS, formatSectionType } from '../../utils/enums';
import { FormSelect, FormTextarea } from '../form';
import { Modal, ConfirmationModal, SectionHeader } from '../common';
import { TransliterationModal } from '../TransliterationModal';
import { transliterateContent } from '../../api/client';


export const LyricsTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const [editingVariantId, setEditingVariantId] = useState<string | null>(null);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [newVariant, setNewVariant] = useState<Partial<KrithiLyricVariant>>({
        language: 'te',
        script: 'telugu',
        transliterationScheme: 'slp1'
    });

    // Transliteration State
    const [isTransliterationModalOpen, setIsTransliterationModalOpen] = useState(false);
    const [transliterationInitialData, setTransliterationInitialData] = useState<Record<string, string>>({});


    const languageOptions = LANGUAGE_CODE_OPTIONS.map(l => ({ value: l.value, label: l.label }));
    const scriptOptions = SCRIPT_CODE_OPTIONS.map(s => ({ value: s.value, label: s.label }));
    const sampradayaOptions = referenceData.sampradayas.map(s => ({ value: s.id, label: s.name }));

    const handleAddVariant = () => {
        if (!newVariant.language || !newVariant.script) return;

        const variant: KrithiLyricVariant = {
            id: `temp-${Date.now()}`,
            language: newVariant.language || 'te',
            script: newVariant.script || 'telugu',
            transliterationScheme: newVariant.transliterationScheme,
            sampradaya: referenceData.sampradayas.find(s => s.id === newVariant.sampradaya?.id),
            sections: []
        };

        const updatedVariants = [...(krithi.lyricVariants || []), variant];
        onChange('lyricVariants', updatedVariants);
        setEditingVariantId(variant.id);
        setIsAddModalOpen(false);
        setNewVariant({ language: 'te', script: 'telugu', transliterationScheme: 'slp1' });
    };

    const handleUpdateVariant = (variantId: string, updates: Partial<KrithiLyricVariant>) => {
        const updatedVariants = (krithi.lyricVariants || []).map(v =>
            v.id === variantId ? { ...v, ...updates } : v
        );
        onChange('lyricVariants', updatedVariants);
    };

    const handleUpdateLyricText = (variantId: string, sectionId: string, text: string) => {
        const updatedVariants = (krithi.lyricVariants || []).map(v => {
            if (v.id === variantId) {
                const existingSectionIndex = v.sections.findIndex(s => s.sectionId === sectionId);
                let newSections = [...v.sections];

                if (existingSectionIndex >= 0) {
                    newSections[existingSectionIndex] = { ...newSections[existingSectionIndex], text };
                } else {
                    newSections.push({ sectionId, text });
                }
                return { ...v, sections: newSections };
            }
            return v;
        });
        onChange('lyricVariants', updatedVariants);
    };

    const getSectionText = (variant: KrithiLyricVariant, sectionId: string) => {
        return variant.sections.find(s => s.sectionId === sectionId)?.text || '';
    };

    return (
        <div className="space-y-6">
            <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 relative">
                <div className="flex items-center justify-between mb-6">
                    <SectionHeader title="Lyric Variants" />
                    {!readOnly && (
                        <div className="flex gap-2">
                            {/* Placeholder for AI Generation - could be added back if needed */}
                            <button
                                onClick={() => setIsAddModalOpen(true)}
                                className="flex items-center gap-2 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                            >
                                <span className="material-symbols-outlined text-[18px]">add</span>
                                Add Variant
                            </button>

                            {/* AI Generation Button */}
                            <button
                                onClick={() => {
                                    // Pre-fill content logic
                                    let initialData: Record<string, string> = {};
                                    let sourceVariant = editingVariantId
                                        ? krithi.lyricVariants?.find(v => v.id === editingVariantId)
                                        : (krithi.lyricVariants?.[0] || null);

                                    if (sourceVariant && sourceVariant.sections) {
                                        sourceVariant.sections.forEach(s => {
                                            if (s.text) initialData[s.sectionId] = s.text;
                                        });
                                    }

                                    setTransliterationInitialData(initialData);
                                    setIsTransliterationModalOpen(true);
                                }}
                                className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-lg text-sm font-medium hover:bg-purple-700 transition-colors shadow-sm shadow-purple-200"
                            >
                                <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                                Generate Variants
                            </button>

                        </div>
                    )}
                </div>

                {!krithi.lyricVariants || krithi.lyricVariants.length === 0 ? (
                    <div className="p-8 text-center text-ink-500 bg-slate-50 rounded-lg border border-dashed border-slate-200">
                        <p>No lyric variants added yet. Click "Add Variant" to get started.</p>
                    </div>
                ) : (
                    <div className="space-y-4">
                        {krithi.lyricVariants.map((variant, idx) => (
                            <div key={variant.id} className="border border-border-light rounded-lg p-4 bg-white shadow-sm">
                                <div className="flex items-start justify-between mb-4">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-3 mb-2">
                                            <span className="font-semibold text-ink-900">Variant {idx + 1}</span>
                                            <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs font-medium uppercase tracking-wide border border-slate-200">
                                                {LANGUAGE_CODE_OPTIONS.find(l => l.value === variant.language)?.label || variant.language}
                                            </span>
                                            {variant.script && (
                                                <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs border border-slate-200">
                                                    {SCRIPT_CODE_OPTIONS.find(s => s.value === variant.script)?.label || variant.script}
                                                </span>
                                            )}
                                            {variant.transliterationScheme && (
                                                <span className="px-2 py-0.5 bg-purple-50 text-purple-700 rounded text-xs border border-purple-100" title="Transliteration Scheme">
                                                    {variant.transliterationScheme}
                                                </span>
                                            )}
                                            {variant.sampradaya && (
                                                <span className="px-2 py-0.5 bg-blue-50 text-blue-700 rounded text-xs border border-blue-100">
                                                    {variant.sampradaya.name}
                                                </span>
                                            )}
                                        </div>

                                        {editingVariantId === variant.id ? (
                                            <div className="space-y-6 mt-6 animate-fadeIn">
                                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 bg-slate-50 p-4 rounded-lg border border-slate-100">
                                                    <FormSelect
                                                        label="Language"
                                                        name={`lang-${variant.id}`}
                                                        value={variant.language}
                                                        options={languageOptions}
                                                        onChange={(e) => handleUpdateVariant(variant.id, { language: e.target.value })}
                                                    />
                                                    <FormSelect
                                                        label="Script"
                                                        name={`script-${variant.id}`}
                                                        value={variant.script || ''}
                                                        options={scriptOptions}
                                                        onChange={(e) => handleUpdateVariant(variant.id, { script: e.target.value })}
                                                    />
                                                    <FormSelect
                                                        label="Sampradaya"
                                                        name={`samp-${variant.id}`}
                                                        value={variant.sampradaya?.id || ''}
                                                        options={sampradayaOptions}
                                                        onChange={(e) => handleUpdateVariant(variant.id, { sampradaya: referenceData.sampradayas.find(s => s.id === e.target.value) })}
                                                        placeholder="None"
                                                    />
                                                    <FormSelect
                                                        label="Transl. Scheme"
                                                        name={`scheme-${variant.id}`}
                                                        value={variant.transliterationScheme || ''}
                                                        options={[{ value: 'slp1', label: 'SLP1' }, { value: 'hk', label: 'Harvard-Kyoto' }]}
                                                        onChange={(e) => handleUpdateVariant(variant.id, { transliterationScheme: e.target.value })}
                                                        placeholder="None"
                                                    />
                                                </div>

                                                <div className="space-y-4">
                                                    <label className="block text-sm font-bold text-ink-900">Lyrics by Section</label>
                                                    {(krithi.sections || []).length > 0 ? (
                                                        (krithi.sections || []).map(section => (
                                                            <div key={section.id} className="bg-white">
                                                                <h4 className="text-xs font-semibold text-slate-500 mb-1 uppercase tracking-wider">
                                                                    {section.label || formatSectionType(section.sectionType)}
                                                                </h4>
                                                                <FormTextarea
                                                                    label=""
                                                                    name={`lyric-${variant.id}-${section.id}`}
                                                                    value={getSectionText(variant, section.id)}
                                                                    onChange={(e) => handleUpdateLyricText(variant.id, section.id, e.target.value)}
                                                                    rows={3}
                                                                    placeholder={`Enter lyrics...`}
                                                                    className="font-mono text-sm"
                                                                />
                                                            </div>
                                                        ))
                                                    ) : (
                                                        <p className="text-sm text-ink-500 italic">No sections defined. Add sections in Structure tab first.</p>
                                                    )}
                                                </div>

                                                <div className="flex gap-3 pt-2 border-t border-slate-100">
                                                    <button
                                                        onClick={() => setEditingVariantId(null)}
                                                        className="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm font-medium hover:bg-slate-900 transition-colors"
                                                    >
                                                        Done Editing
                                                    </button>
                                                    <button
                                                        onClick={() => {
                                                            if (window.confirm('Are you sure you want to remove this variant?')) {
                                                                onChange('lyricVariants', krithi.lyricVariants?.filter(v => v.id !== variant.id) || []);
                                                            }
                                                        }}
                                                        className="px-4 py-2 text-red-600 hover:bg-red-50 rounded-lg text-sm font-medium transition-colors"
                                                    >
                                                        Remove Variant
                                                    </button>
                                                </div>
                                            </div>
                                        ) : (
                                            <div className="mt-4">
                                                {variant.sections.length > 0 ? (
                                                    <div className="space-y-3">
                                                        {variant.sections.map((lyricSection, secIdx) => {
                                                            const section = krithi.sections?.find(s => s.id === lyricSection.sectionId);
                                                            return (
                                                                <div key={secIdx} className="text-sm">
                                                                    <span className="text-xs font-bold text-slate-500 uppercase tracking-wide block mb-1">
                                                                        {section ? (section.label || formatSectionType(section.sectionType)) : 'Section'}
                                                                    </span>
                                                                    <p className="text-ink-800 whitespace-pre-wrap font-serif leading-relaxed bg-slate-50 p-3 rounded-lg border border-slate-100">
                                                                        {lyricSection.text}
                                                                    </p>
                                                                </div>
                                                            );
                                                        })}
                                                    </div>
                                                ) : (
                                                    <p className="text-sm text-ink-400 italic">No lyrics added yet.</p>
                                                )}
                                                {!readOnly && (
                                                    <button
                                                        onClick={() => setEditingVariantId(variant.id)}
                                                        className="mt-4 text-sm text-primary font-semibold hover:text-primary-dark flex items-center gap-1"
                                                    >
                                                        <span className="material-symbols-outlined text-[16px]">edit</span>
                                                        Edit Content
                                                    </button>
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <Modal
                isOpen={isAddModalOpen}
                onClose={() => setIsAddModalOpen(false)}
                title="Add Lyric Variant"
                size="sm"
            >
                <div className="space-y-4">
                    <FormSelect
                        label="Language"
                        name="new-lang"
                        value={newVariant.language || 'te'}
                        options={languageOptions}
                        onChange={(e) => setNewVariant({ ...newVariant, language: e.target.value })}
                    />
                    <FormSelect
                        label="Script"
                        name="new-script"
                        value={newVariant.script || 'telugu'}
                        options={scriptOptions}
                        onChange={(e) => setNewVariant({ ...newVariant, script: e.target.value })}
                    />
                    <div className="pt-4 flex justify-end gap-3">
                        <button onClick={() => setIsAddModalOpen(false)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 rounded-md">Cancel</button>
                        <button onClick={handleAddVariant} className="px-4 py-2 text-sm bg-primary text-white rounded-md hover:bg-primary-dark">Add Variant</button>
                    </div>
                </div>
            </Modal>

            <TransliterationModal
                isOpen={isTransliterationModalOpen}
                onClose={() => setIsTransliterationModalOpen(false)}
                initialData={transliterationInitialData}
                sections={(krithi.sections || []).map(s => ({
                    id: s.id,
                    title: s.label ? `${formatSectionType(s.sectionType)} - ${s.label}` : formatSectionType(s.sectionType)
                }))}
                onTransliterate={async (content, targetScript) => {
                    const res = await transliterateContent(krithi.id || 'new', content, null, targetScript);
                    return res.transliterated;
                }}
                onConfirm={(results, script) => {
                    const newVariant: KrithiLyricVariant = {
                        id: `temp-ai-${Date.now()}`,
                        language: krithi.primaryLanguage || 'te',
                        script: script,
                        transliterationScheme: 'ISO-15919 (AI Generated)',
                        sampradaya: undefined,
                        sections: results.map(r => ({
                            sectionId: r.sectionId,
                            text: r.text
                        }))
                    };

                    const updatedVariants = [...(krithi.lyricVariants || []), newVariant];
                    onChange('lyricVariants', updatedVariants);
                    setEditingVariantId(newVariant.id);
                    setIsTransliterationModalOpen(false);
                }}
            />
        </div>
    );

};
