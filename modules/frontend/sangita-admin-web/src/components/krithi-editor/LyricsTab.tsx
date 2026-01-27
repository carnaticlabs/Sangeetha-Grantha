import React, { useState } from 'react';
import { TabProps } from '../../types/krithi-editor.types';
import { KrithiLyricVariant, Sampradaya } from '../../types';
import { LANGUAGE_CODE_OPTIONS, SCRIPT_CODE_OPTIONS, formatSectionType } from '../../utils/enums';
import { FormSelect, FormTextarea } from '../form';
import { Modal, ConfirmationModal } from '../common';

export const LyricsTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const [activeVariantId, setActiveVariantId] = useState<string | null>(krithi.lyricVariants?.[0]?.id || null);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    // New Variant State
    const [newVariant, setNewVariant] = useState<Partial<KrithiLyricVariant>>({
        language: 'te',
        script: 'telugu',
        transliterationScheme: 'slp1'
    });

    const languageOptions = LANGUAGE_CODE_OPTIONS.map(l => ({ value: l.value, label: l.label }));
    const scriptOptions = SCRIPT_CODE_OPTIONS.map(s => ({ value: s.value, label: s.label }));
    const sampradayaOptions = referenceData.sampradayas.map(s => ({ value: s.id, label: s.name }));

    const activeVariant = krithi.lyricVariants?.find(v => v.id === activeVariantId);
    const sections = krithi.sections || [];

    const handleAddVariant = () => {
        if (!newVariant.language || !newVariant.script) return;

        const variant: KrithiLyricVariant = {
            id: `temp-${Date.now()}`,
            language: newVariant.language || 'te',
            script: newVariant.script || 'telugu',
            transliterationScheme: newVariant.transliterationScheme,
            sampradaya: referenceData.sampradayas.find(s => s.id === newVariant.sampradaya?.id),
            sections: [] // Empty initially
        };

        const updatedVariants = [...(krithi.lyricVariants || []), variant];
        onChange('lyricVariants', updatedVariants);
        setActiveVariantId(variant.id);
        setIsAddModalOpen(false);
        setNewVariant({ language: 'te', script: 'telugu', transliterationScheme: 'slp1' });
    };

    const handleUpdateLyricText = (sectionId: string, text: string) => {
        if (!activeVariantId) return;

        const updatedVariants = (krithi.lyricVariants || []).map(v => {
            if (v.id === activeVariantId) {
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

    const getSectionText = (sectionId: string) => {
        return activeVariant?.sections.find(s => s.sectionId === sectionId)?.text || '';
    };

    return (
        <div className="space-y-6">
            {/* Variant Selector Header */}
            <div className="flex flex-wrap items-center gap-4 border-b pb-4">
                <div className="flex gap-2 overflow-x-auto pb-2 flex-1">
                    {(krithi.lyricVariants || []).map(variant => (
                        <button
                            key={variant.id}
                            onClick={() => setActiveVariantId(variant.id)}
                            className={`px-4 py-2 rounded-full text-sm font-medium border whitespace-nowrap transition-colors ${activeVariantId === variant.id
                                ? 'bg-primary text-white border-primary'
                                : 'bg-white text-slate-600 border-slate-200 hover:border-primary/50'
                                }`}
                        >
                            {LANGUAGE_CODE_OPTIONS.find(l => l.value === variant.language)?.label || variant.language}
                            <span className="text-xs opacity-75 ml-1">
                                ({SCRIPT_CODE_OPTIONS.find(s => s.value === variant.script)?.label || variant.script})
                            </span>
                        </button>
                    ))}
                    {!readOnly && (
                        <button
                            onClick={() => setIsAddModalOpen(true)}
                            className="px-3 py-2 rounded-full border border-dashed border-slate-300 text-slate-500 hover:border-primary hover:text-primary text-sm font-medium"
                        >
                            + Add Variant
                        </button>
                    )}
                </div>
            </div>

            {/* Editor Area */}
            {activeVariant ? (
                <div className="space-y-8 animate-fadeIn">
                    <div className="bg-slate-50 p-4 rounded-lg border border-slate-200 grid grid-cols-1 md:grid-cols-3 gap-4">
                        <FormSelect
                            label="Language"
                            name="variant-lang"
                            value={activeVariant.language}
                            options={languageOptions}
                            disabled // Language ideally shouldn't change after creation to avoid confusion, or handle updates
                        />
                        <FormSelect
                            label="Script"
                            name="variant-script"
                            value={activeVariant.script || ''}
                            options={scriptOptions}
                            disabled
                        />
                        <FormSelect
                            label="Sampradaya (Optional)"
                            name="variant-samp"
                            value={activeVariant.sampradaya?.id || ''}
                            options={sampradayaOptions}
                            onChange={(e) => {
                                const updated = (krithi.lyricVariants || []).map(v =>
                                    v.id === activeVariant.id
                                        ? { ...v, sampradaya: referenceData.sampradayas.find(s => s.id === e.target.value) }
                                        : v
                                );
                                onChange('lyricVariants', updated);
                            }}
                            placeholder="None"
                            disabled={readOnly}
                        />
                    </div>

                    {sections.length === 0 ? (
                        <div className="text-center p-8 border-2 border-dashed border-slate-200 rounded-lg">
                            <p className="text-slate-500">
                                No structural sections defined. Please define sections in the <strong>Structure</strong> tab first.
                            </p>
                        </div>
                    ) : (
                        <div className="space-y-6">
                            {sections.map(section => (
                                <div key={section.id} className="bg-white">
                                    <h4 className="font-medium text-slate-700 mb-2 flex items-center gap-2">
                                        <span className="bg-slate-100 text-xs px-2 py-1 rounded border text-slate-500 font-mono">
                                            {section.orderIndex + 1}
                                        </span>
                                        {section.label || formatSectionType(section.sectionType)}
                                    </h4>
                                    <FormTextarea
                                        label=""
                                        name={`lyric-${section.id}`}
                                        value={getSectionText(section.id)}
                                        onChange={(e) => handleUpdateLyricText(section.id, e.target.value)}
                                        rows={3}
                                        placeholder={`Enter lyrics for ${section.label || formatSectionType(section.sectionType)}...`}
                                        disabled={readOnly}
                                        className="mb-0"
                                    />
                                </div>
                            ))}
                        </div>
                    )}

                    {/* AI Transliteration Button (Mock for now, or connect to existing modal logic later) */}
                    <div className="flex justify-end pt-4 border-t">
                        <button className="text-sm text-primary font-medium hover:underline flex items-center gap-1">
                            <span className="material-symbols-outlined text-[16px]">auto_awesome</span>
                            Generate from Transliteration (Coming Soon)
                        </button>
                    </div>
                </div>
            ) : (
                <div className="text-center py-12">
                    <p className="text-slate-500">Select a lyric variant or add a new one to start editing.</p>
                </div>
            )}

            {/* Add Variant Modal */}
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
                    <FormSelect
                        label="Transl. Scheme (Optional)"
                        name="new-scheme"
                        value={newVariant.transliterationScheme || 'slp1'}
                        options={[{ value: 'slp1', label: 'SLP1' }, { value: 'hk', label: 'Harvard-Kyoto' }]}
                        onChange={(e) => setNewVariant({ ...newVariant, transliterationScheme: e.target.value })}
                    />
                    <div className="pt-4 flex justify-end gap-3">
                        <button onClick={() => setIsAddModalOpen(false)} className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-50 rounded-md">Cancel</button>
                        <button onClick={handleAddVariant} className="px-4 py-2 text-sm bg-primary text-white rounded-md hover:bg-primary-dark">Add Variant</button>
                    </div>
                </div>
            </Modal>
        </div>
    );
};
