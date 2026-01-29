import React, { useState } from 'react';
import { FormInput, FormSelect, FormCheckbox, FormTextarea, ReferenceField } from '../form';
import { SectionHeader, ReferenceSelectionModal } from '../common';
import { KrithiDetail, MusicalForm, Composer, Raga, Tala, Deity, Temple } from '../../types';
import { TabProps } from '../../types/krithi-editor.types';
import { LANGUAGE_CODE_OPTIONS } from '../../utils/enums';

export const MetadataTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const { composers, ragas, talas, deities, temples } = referenceData;

    // Modal State
    const [modalConfig, setModalConfig] = useState<{
        type: 'COMPOSER' | 'RAGA' | 'TALA' | 'DEITY' | 'TEMPLE' | null;
        isOpen: boolean;
    }>({ type: null, isOpen: false });

    // Enums
    const musicalFormOptions = Object.values(MusicalForm).map(f => ({ value: f, label: f }));
    const languageOptions = LANGUAGE_CODE_OPTIONS.map(l => ({ value: l.value, label: l.label }));
    const statusOptions = [
        { value: 'DRAFT', label: 'Draft' },
        { value: 'IN_REVIEW', label: 'In Review' },
        { value: 'PUBLISHED', label: 'Published' },
        { value: 'ARCHIVED', label: 'Archived' },
    ];

    const openModal = (type: 'COMPOSER' | 'RAGA' | 'TALA' | 'DEITY' | 'TEMPLE') => {
        setModalConfig({ type, isOpen: true });
    };

    const closeModal = () => {
        setModalConfig({ ...modalConfig, isOpen: false });
    };

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
                {/* Left Column (2/3 width) */}
                <div className="lg:col-span-2 space-y-8">

                    {/* 1. Identity */}
                    <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 relative">
                        <SectionHeader title="Identity" />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="md:col-span-2">
                                <FormInput
                                    label="Name (Transliterated)"
                                    name="title"
                                    value={krithi.title || ''}
                                    onChange={(e) => onChange('title', e.target.value)}
                                    required
                                    disabled={readOnly}
                                />
                            </div>
                            <div className="md:col-span-2">
                                <FormInput
                                    label="Incipit"
                                    name="incipit"
                                    value={krithi.incipit || ''}
                                    onChange={(e) => onChange('incipit', e.target.value)}
                                    placeholder="First line or popular handle"
                                    disabled={readOnly}
                                />
                            </div>
                        </div>
                    </div>

                    {/* 2. Canonical Links */}
                    <div className="bg-white border border-border-light rounded-xl shadow-sm p-6">
                        <SectionHeader title="Canonical Links" />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <ReferenceField
                                label="Composer"
                                value={krithi.composer?.name}
                                onModify={() => openModal('COMPOSER')}
                                required
                                disabled={readOnly}
                                placeholder="Select Composer"
                            />

                            {/* Raga Logic */}
                            <div>
                                <label className="block text-sm font-semibold text-ink-900 mb-2">
                                    {krithi.isRagamalika ? 'Ragas (Ragamalika)' : 'Raga'}
                                    <span className="text-primary ml-1">*</span>
                                </label>

                                {krithi.isRagamalika && krithi.ragas && krithi.ragas.length > 0 && (
                                    <div className="flex flex-wrap gap-2 mb-2">
                                        {krithi.ragas.map(raga => (
                                            <span key={raga.id} className="inline-flex items-center gap-1 px-3 py-1 bg-primary-light text-primary rounded-full text-sm border border-primary/20">
                                                {raga.name}
                                                <button
                                                    type="button"
                                                    onClick={() => onChange('ragas', krithi.ragas?.filter(r => r.id !== raga.id) || [])}
                                                    className="ml-1 hover:text-primary-dark"
                                                    disabled={readOnly}
                                                >
                                                    <span className="material-symbols-outlined text-[16px]">close</span>
                                                </button>
                                            </span>
                                        ))}
                                    </div>
                                )}

                                <div className="flex gap-2">
                                    <div className="flex-1 flex items-center px-4 h-12 rounded-lg border border-border-light bg-slate-50">
                                        <span className={`truncate ${!krithi.isRagamalika && krithi.ragas?.[0] ? 'text-ink-900' : 'text-slate-400 italic'}`}>
                                            {!krithi.isRagamalika
                                                ? (krithi.ragas?.[0]?.name || 'Select Raga')
                                                : 'Add Raga...'}
                                        </span>
                                    </div>
                                    <button
                                        type="button"
                                        onClick={() => openModal('RAGA')}
                                        disabled={readOnly}
                                        className="px-4 h-12 bg-white border border-border-light text-ink-900 font-medium rounded-lg hover:bg-slate-50 hover:border-slate-300 transition-colors whitespace-nowrap"
                                    >
                                        {krithi.isRagamalika ? 'Add' : 'Modify'}
                                    </button>
                                </div>
                            </div>

                            <ReferenceField
                                label="Tala"
                                value={krithi.tala?.name}
                                onModify={() => openModal('TALA')}
                                disabled={readOnly}
                                placeholder="Select Tala"
                            />
                            <ReferenceField
                                label="Deity"
                                value={krithi.deity?.name}
                                onModify={() => openModal('DEITY')}
                                disabled={readOnly}
                                placeholder="Select Deity"
                            />
                            <ReferenceField
                                label="Temple"
                                value={krithi.temple?.name || krithi.temple?.canonicalName}
                                onModify={() => openModal('TEMPLE')}
                                disabled={readOnly}
                                placeholder="Select Temple"
                            />
                        </div>
                    </div>

                    {/* 3. Language & Form */}
                    <div className="bg-white border border-border-light rounded-xl shadow-sm p-6">
                        <SectionHeader title="Language & Form" />
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <FormSelect
                                label="Primary Language"
                                name="primaryLanguage"
                                value={krithi.primaryLanguage || 'te'}
                                options={languageOptions}
                                onChange={(e) => onChange('primaryLanguage', e.target.value)}
                                required
                                disabled={readOnly}
                            />
                            <div>
                                <FormSelect
                                    label="Musical Form"
                                    name="musicalForm"
                                    value={krithi.musicalForm || MusicalForm.KRITHI}
                                    options={musicalFormOptions}
                                    onChange={(e) => onChange('musicalForm', e.target.value)}
                                    disabled={readOnly}
                                />
                                {(krithi.musicalForm === MusicalForm.VARNAM || krithi.musicalForm === MusicalForm.SWARAJATHI) && (
                                    <p className="text-xs text-blue-600 mt-1 flex items-center gap-1">
                                        <span className="material-symbols-outlined text-[14px]">info</span>
                                        Enables Notation Editor
                                    </p>
                                )}
                            </div>
                            <div className="md:col-span-2">
                                <FormCheckbox
                                    label="Ragamalika (Multiple Ragas)"
                                    name="isRagamalika"
                                    checked={krithi.isRagamalika || false}
                                    onChange={(e) => onChange('isRagamalika', e.target.checked)}
                                    disabled={readOnly}
                                />
                            </div>
                        </div>
                    </div>

                    {/* 4. Additional Info */}
                    <div className="bg-white border border-border-light rounded-xl shadow-sm p-6">
                        <SectionHeader title="Additional Information" />
                        <div className="space-y-6">
                            <FormTextarea
                                label="Sahitya Summary"
                                name="sahityaSummary"
                                value={krithi.sahityaSummary || ''}
                                onChange={(e) => onChange('sahityaSummary', e.target.value)}
                                rows={4}
                                placeholder="Short prose summary or meaning of the composition"
                                disabled={readOnly}
                            />
                            <FormTextarea
                                label="Notes"
                                name="notes"
                                value={krithi.notes || ''}
                                onChange={(e) => onChange('notes', e.target.value)}
                                rows={3}
                                placeholder="Additional notes, context, or metadata"
                                disabled={readOnly}
                            />
                        </div>
                    </div>
                </div>

                {/* Right Column (1/3 width) - Status */}
                <div className="space-y-6">
                    <div className="bg-white border border-border-light rounded-xl shadow-sm p-5">
                        <h4 className="text-xs font-bold text-ink-500 uppercase tracking-wider mb-4">Status</h4>
                        <FormSelect
                            label=""
                            name="status"
                            value={krithi.status || 'DRAFT'}
                            options={statusOptions}
                            onChange={(e) => onChange('status', e.target.value)}
                            disabled={readOnly}
                            className="mb-0"
                        />
                    </div>
                </div>
            </div>

            {/* Reference Selection Modals */}
            <ReferenceSelectionModal<Composer>
                isOpen={modalConfig.isOpen && modalConfig.type === 'COMPOSER'}
                onClose={closeModal}
                title="Select Composer"
                items={composers}
                getId={(item) => item.id}
                getLabel={(item) => item.name}
                onSelect={(item) => onChange('composer', item)}
            />
            <ReferenceSelectionModal<Raga>
                isOpen={modalConfig.isOpen && modalConfig.type === 'RAGA'}
                onClose={closeModal}
                title={krithi.isRagamalika ? "Add Raga" : "Select Raga"}
                items={ragas}
                getId={(item) => item.id}
                getLabel={(item) => item.name}
                getSubtitle={(item) => item.melakartaNumber ? `Melakarta #${item.melakartaNumber}` : (item.parentRagaId ? 'Janya' : '')}
                onSelect={(item) => {
                    if (krithi.isRagamalika) {
                        if (!krithi.ragas?.some(r => r.id === item.id)) {
                            onChange('ragas', [...(krithi.ragas || []), item]);
                        }
                    } else {
                        onChange('ragas', [item]);
                    }
                }}
            />
            <ReferenceSelectionModal<Tala>
                isOpen={modalConfig.isOpen && modalConfig.type === 'TALA'}
                onClose={closeModal}
                title="Select Tala"
                items={talas}
                getId={(item) => item.id}
                getLabel={(item) => item.name}
                onSelect={(item) => onChange('tala', item)}
            />
            <ReferenceSelectionModal<Deity>
                isOpen={modalConfig.isOpen && modalConfig.type === 'DEITY'}
                onClose={closeModal}
                title="Select Deity"
                items={deities}
                getId={(item) => item.id}
                getLabel={(item) => item.name}
                onSelect={(item) => onChange('deity', item)}
            />
            <ReferenceSelectionModal<Temple>
                isOpen={modalConfig.isOpen && modalConfig.type === 'TEMPLE'}
                onClose={closeModal}
                title="Select Temple"
                items={temples}
                getId={(item) => item.id}
                getLabel={(item) => item.name || item.canonicalName || 'Unknown'}
                onSelect={(item) => onChange('temple', item)}
            />

        </div>
    );
};
