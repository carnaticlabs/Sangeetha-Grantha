import React from 'react';
import { FormInput, FormSelect, FormCheckbox, FormTextarea } from '../form';
import { KrithiDetail, MusicalForm } from '../../types';
import { TabProps } from '../../types/krithi-editor.types';
import { LANGUAGE_CODE_OPTIONS } from '../../utils/enums';

export const MetadataTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const { composers, ragas, talas, deities, temples, tags } = referenceData;

    // Helpers for select options
    const composerOptions = composers.map(c => ({ value: c.id, label: c.name }));
    const ragaOptions = ragas.map(r => ({ value: r.id, label: r.name }));
    const talaOptions = talas.map(t => ({ value: t.id, label: t.name }));
    const deityOptions = deities.map(d => ({ value: d.id, label: d.name }));
    const templeOptions = temples.map(t => ({ value: t.id, label: t.name || t.canonicalName || 'Unknown' }));

    const musicalFormOptions = Object.values(MusicalForm).map(f => ({ value: f, label: f }));
    const languageOptions = LANGUAGE_CODE_OPTIONS.map(l => ({ value: l.value, label: l.label }));

    const statusOptions = [
        { value: 'DRAFT', label: 'Draft' },
        { value: 'IN_REVIEW', label: 'In Review' },
        { value: 'PUBLISHED', label: 'Published' },
        { value: 'ARCHIVED', label: 'Archived' },
    ];

    return (
        <div className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Basic Info */}
                <div className="space-y-4">
                    <h3 className="text-lg font-medium text-slate-900 border-b pb-2">Basic Info</h3>

                    <FormInput
                        label="Title"
                        name="title"
                        value={krithi.title || ''}
                        onChange={(e) => onChange('title', e.target.value)}
                        required
                        disabled={readOnly}
                    />

                    <FormInput
                        label="Incipit (Start of Lyric)"
                        name="incipit"
                        value={krithi.incipit || ''}
                        onChange={(e) => onChange('incipit', e.target.value)}
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Musical Form"
                        name="musicalForm"
                        value={krithi.musicalForm || MusicalForm.KRITHI}
                        options={musicalFormOptions}
                        onChange={(e) => onChange('musicalForm', e.target.value)}
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Primary Language"
                        name="primaryLanguage"
                        value={krithi.primaryLanguage || 'te'}
                        options={languageOptions}
                        onChange={(e) => onChange('primaryLanguage', e.target.value)}
                        required
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Status"
                        name="status"
                        value={krithi.status || 'DRAFT'}
                        options={statusOptions}
                        onChange={(e) => onChange('status', e.target.value)}
                        disabled={readOnly}
                    />
                </div>

                {/* Musical Details */}
                <div className="space-y-4">
                    <h3 className="text-lg font-medium text-slate-900 border-b pb-2">Context</h3>

                    <FormSelect
                        label="Composer"
                        name="composer"
                        value={krithi.composer?.id || ''}
                        options={composerOptions}
                        onChange={(e) => {
                            const selected = composers.find(c => c.id === e.target.value);
                            onChange('composer', selected);
                        }}
                        required
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Raga"
                        name="raga"
                        value={krithi.ragas?.[0]?.id || ''}
                        options={ragaOptions}
                        onChange={(e) => {
                            const selected = ragas.find(r => r.id === e.target.value);
                            onChange('ragas', selected ? [selected] : []);
                        }}
                        required
                        disabled={readOnly}
                    />

                    <FormCheckbox
                        label="Is Ragamalika?"
                        name="isRagamalika"
                        checked={krithi.isRagamalika || false}
                        onChange={(e) => onChange('isRagamalika', e.target.checked)}
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Tala"
                        name="tala"
                        value={krithi.tala?.id || ''}
                        options={talaOptions}
                        onChange={(e) => {
                            const selected = talas.find(t => t.id === e.target.value);
                            onChange('tala', selected);
                        }}
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Deity"
                        name="deity"
                        value={krithi.deity?.id || ''}
                        options={deityOptions}
                        onChange={(e) => {
                            const selected = deities.find(d => d.id === e.target.value);
                            onChange('deity', selected);
                        }}
                        disabled={readOnly}
                    />

                    <FormSelect
                        label="Temple"
                        name="temple"
                        value={krithi.temple?.id || ''}
                        options={templeOptions}
                        onChange={(e) => {
                            const selected = temples.find(t => t.id === e.target.value);
                            onChange('temple', selected);
                        }}
                        disabled={readOnly}
                    />
                </div>
            </div>

            <div className="space-y-4">
                <h3 className="text-lg font-medium text-slate-900 border-b pb-2">Content</h3>
                <FormTextarea
                    label="Sahitya Summary"
                    name="sahityaSummary"
                    value={krithi.sahityaSummary || ''}
                    onChange={(e) => onChange('sahityaSummary', e.target.value)}
                    rows={3}
                    disabled={readOnly}
                />
                <FormTextarea
                    label="Notes"
                    name="notes"
                    value={krithi.notes || ''}
                    onChange={(e) => onChange('notes', e.target.value)}
                    rows={3}
                    disabled={readOnly}
                />
            </div>

        </div>
    );
};
