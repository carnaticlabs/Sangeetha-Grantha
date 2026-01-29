import React from 'react';
import { KrithiSection } from '../../types';
import { TabProps } from '../../types/krithi-editor.types';
import { SECTION_TYPE_LABELS, formatSectionType } from '../../utils/enums';
import { FormSelect, FormInput } from '../form';
import { SectionHeader } from '../common';

// Pre-defined section types from enum
const SECTION_TYPES = Object.keys(SECTION_TYPE_LABELS).map(key => ({
    value: key,
    label: SECTION_TYPE_LABELS[key as keyof typeof SECTION_TYPE_LABELS]
}));

export const StructureTab: React.FC<TabProps> = ({ krithi, onChange, readOnly }) => {
    const sections = krithi.sections || [];

    const handleAddSection = () => {
        const newSection: KrithiSection = {
            id: `temp-${Date.now()}`, // Temporary ID
            sectionType: 'PALLAVI',
            orderIndex: sections.length,
            label: undefined
        };
        onChange('sections', [...sections, newSection]);
    };

    const handleRemoveSection = (index: number) => {
        const newSections = sections.filter((_, i) => i !== index);
        // Re-index
        const reindexed = newSections.map((s, i) => ({ ...s, orderIndex: i }));
        onChange('sections', reindexed);
    };

    const handleUpdateSection = (index: number, field: keyof KrithiSection, value: any) => {
        const newSections = [...sections];
        newSections[index] = { ...newSections[index], [field]: value };
        onChange('sections', newSections);
    };

    const handleMoveSection = (index: number, direction: 'up' | 'down') => {
        if (direction === 'up' && index === 0) return;
        if (direction === 'down' && index === sections.length - 1) return;

        const newSections = [...sections];
        const targetIndex = direction === 'up' ? index - 1 : index + 1;

        // Swap
        [newSections[index], newSections[targetIndex]] = [newSections[targetIndex], newSections[index]];

        // Re-index
        const reindexed = newSections.map((s, i) => ({ ...s, orderIndex: i }));
        onChange('sections', reindexed);
    };

    return (
        <div className="space-y-6">
            <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 relative">
                <SectionHeader
                    title="Composition Structure"
                    action={
                        !readOnly && (
                            <button
                                onClick={handleAddSection}
                                className="px-4 py-2 bg-primary-50 text-primary-700 rounded-lg text-sm font-semibold hover:bg-primary-100 transition-colors border border-primary-100 flex items-center gap-2"
                            >
                                <span className="material-symbols-outlined text-[18px]">add</span>
                                Add Section
                            </button>
                        )
                    }
                />

                <div className="space-y-4">
                    {sections.length === 0 ? (
                        <p className="text-slate-500 italic p-4 text-center bg-slate-50 rounded-lg">
                            No sections defined. Add a section to define the structure.
                        </p>
                    ) : (
                        sections.map((section, index) => (
                            <div
                                key={section.id}
                                className="flex items-start gap-4 p-4 bg-white border border-slate-200 rounded-lg shadow-sm hover:border-slate-300 transition-colors"
                            >
                                {/* Order / Move Controls */}
                                <div className="flex flex-col items-center gap-1 mt-2">
                                    <span className="text-xs font-bold text-slate-400 w-6 text-center">{index + 1}</span>
                                    {!readOnly && (
                                        <>
                                            <button
                                                onClick={() => handleMoveSection(index, 'up')}
                                                disabled={index === 0}
                                                className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                                                title="Move Up"
                                            >
                                                ▲
                                            </button>
                                            <button
                                                onClick={() => handleMoveSection(index, 'down')}
                                                disabled={index === sections.length - 1}
                                                className="p-1 text-slate-400 hover:text-slate-600 disabled:opacity-30"
                                                title="Move Down"
                                            >
                                                ▼
                                            </button>
                                        </>
                                    )}
                                </div>

                                {/* Fields */}
                                <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-4">
                                    <FormSelect
                                        label="Section Type"
                                        name={`section-type-${index}`}
                                        value={section.sectionType}
                                        options={SECTION_TYPES}
                                        onChange={(e) => handleUpdateSection(index, 'sectionType', e.target.value)}
                                        disabled={readOnly}
                                        className="mb-0" // Override default margin
                                    />
                                    <FormInput
                                        label="Custom Label (Optional)"
                                        name={`section-label-${index}`}
                                        value={section.label || ''}
                                        placeholder={formatSectionType(section.sectionType)}
                                        onChange={(e) => handleUpdateSection(index, 'label', e.target.value)}
                                        disabled={readOnly}
                                        className="mb-0"
                                    />
                                </div>

                                {/* Remove */}
                                {!readOnly && (
                                    <button
                                        onClick={() => handleRemoveSection(index)}
                                        className="mt-8 p-1.5 text-rose-400 hover:text-rose-600 hover:bg-rose-50 rounded-md transition-colors"
                                        title="Remove Section"
                                    >
                                        <span className="material-symbols-outlined text-[20px]">delete</span>
                                    </button>
                                )}
                            </div>
                        ))
                    )}
                </div>
            </div>
        </div>
    );
};
