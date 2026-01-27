import React, { useEffect } from 'react';
import { TabProps } from '../../types/krithi-editor.types';
import { Tag } from '../../types';
import { FormInput } from '../form';

export const TagsTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const { tags, loadTags } = referenceData;
    const selectedTags = krithi.tags || [];

    // Load tags on mount if not loaded
    useEffect(() => {
        if (tags.length === 0) {
            if (referenceData.loadTags) referenceData.loadTags();
        }
    }, []);

    const handleToggleTag = (tag: Tag) => {
        if (selectedTags.some(t => t.id === tag.id)) {
            onChange('tags', selectedTags.filter(t => t.id !== tag.id));
        } else {
            onChange('tags', [...selectedTags, tag]);
        }
    };

    // Group tags by category
    const tagsByCategory = tags.reduce((acc, tag) => {
        if (!acc[tag.category]) acc[tag.category] = [];
        acc[tag.category].push(tag);
        return acc;
    }, {} as Record<string, Tag[]>);

    return (
        <div className="space-y-8">
            <div>
                <h3 className="text-lg font-medium text-slate-900 mb-4">Tags & Classifications</h3>
                <p className="text-sm text-slate-500 mb-6">Select tags to help categorize and improve searchability of this composition.</p>
            </div>

            {referenceData.loading && tags.length === 0 ? (
                <div className="text-center py-8 text-slate-500">Loading tags...</div>
            ) : (
                Object.entries(tagsByCategory).map(([category, categoryTags]) => (
                    <div key={category} className="bg-white p-6 rounded-lg border border-slate-200">
                        <h4 className="font-semibold text-slate-800 mb-4 capitalize">{category.replace('_', ' ')}</h4>
                        <div className="flex flex-wrap gap-2">
                            {categoryTags.map(tag => {
                                const isSelected = selectedTags.some(t => t.id === tag.id);
                                return (
                                    <button
                                        key={tag.id}
                                        onClick={() => !readOnly && handleToggleTag(tag)}
                                        disabled={readOnly}
                                        className={`px-3 py-1.5 rounded-full text-sm border transition-all ${isSelected
                                                ? 'bg-amber-100 border-amber-300 text-amber-800 font-medium'
                                                : 'bg-slate-50 border-slate-200 text-slate-600 hover:border-slate-300'
                                            }`}
                                    >
                                        {tag.displayName}
                                    </button>
                                );
                            })}
                        </div>
                    </div>
                ))
            )}
        </div>
    );
};
