import React, { useEffect } from 'react';
import { TabProps } from '../../types/krithi-editor.types';
import { Tag } from '../../types';
import { FormInput } from '../form';
import { SectionHeader } from '../common';

export const TagsTab: React.FC<TabProps> = ({ krithi, onChange, referenceData, readOnly }) => {
    const { tags, loadTags } = referenceData;
    const selectedTags = krithi.tags || [];
    const [tagSearchTerm, setTagSearchTerm] = React.useState('');
    const [showTagDropdown, setShowTagDropdown] = React.useState(false);

    // Load tags on mount if not loaded
    useEffect(() => {
        if (tags.length === 0) {
            if (referenceData.loadTags) referenceData.loadTags();
        }
    }, [tags.length, referenceData]);

    const handleAddTag = (tag: Tag) => {
        if (!selectedTags.some(t => t.id === tag.id)) {
            onChange('tags', [...selectedTags, tag]);
        }
        setTagSearchTerm('');
        setShowTagDropdown(false);
    };

    const handleRemoveTag = (tagId: string) => {
        onChange('tags', selectedTags.filter(t => t.id !== tagId));
    };

    // Filter available tags for dropdown
    const availableTags = tags.filter(tag => {
        if (selectedTags.some(t => t.id === tag.id)) return false;
        if (!tagSearchTerm) return false; // Only show when searching

        const term = tagSearchTerm.toLowerCase();
        return (
            (tag.displayName || '').toLowerCase().includes(term) ||
            (tag.category || '').toLowerCase().includes(term) ||
            (tag.slug || '').toLowerCase().includes(term)
        );
    });

    return (
        <div className="space-y-6">
            <div className="bg-white border border-border-light rounded-xl shadow-sm p-6 relative">
                <SectionHeader title="Tags & Classifications" className="mb-4" />

                {/* Assigned Tags List */}
                <div className="mb-8">
                    <h4 className="text-sm font-bold text-ink-900 mb-3">Assigned Tags</h4>
                    {selectedTags.length > 0 ? (
                        <div className="flex flex-wrap gap-2">
                            {selectedTags.map(tag => (
                                <span
                                    key={tag.id}
                                    className="inline-flex items-center gap-2 px-3 py-1.5 bg-primary-light text-primary-800 rounded-full text-sm border border-primary/20 shadow-sm"
                                >
                                    <span className="font-medium">{tag.displayName}</span>
                                    <span className="text-xs text-primary-600 border-l border-primary/20 pl-2 opacity-80">
                                        {tag.slug ? `#${tag.slug}` : tag.category}
                                    </span>
                                    {!readOnly && (
                                        <button
                                            type="button"
                                            onClick={() => handleRemoveTag(tag.id)}
                                            className="ml-1 p-0.5 hover:bg-primary-100 rounded-full transition-colors text-primary-600"
                                        >
                                            <span className="material-symbols-outlined text-[14px] block">close</span>
                                        </button>
                                    )}
                                </span>
                            ))}
                        </div>
                    ) : (
                        <p className="text-sm text-slate-400 italic">No tags assigned to this composition.</p>
                    )}
                </div>

                {/* Add Tag Search */}
                {!readOnly && (
                    <div className="max-w-xl">
                        <label className="block text-sm font-bold text-ink-900 mb-2">Add Tag</label>
                        <div className="relative">
                            <input
                                type="text"
                                value={tagSearchTerm}
                                onChange={(e) => {
                                    setTagSearchTerm(e.target.value);
                                    setShowTagDropdown(true);
                                }}
                                onFocus={() => setShowTagDropdown(true)}
                                onBlur={() => setTimeout(() => setShowTagDropdown(false), 200)}
                                placeholder="Search tags by name or category..."
                                className="w-full h-11 px-4 pr-10 rounded-lg bg-slate-50 border border-slate-200 text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent transition-all"
                            />
                            <span className="material-symbols-outlined absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-[20px]">search</span>

                            {/* Dropdown */}
                            {showTagDropdown && tagSearchTerm && (
                                <div className="absolute z-10 w-full mt-1 bg-white border border-slate-200 rounded-lg shadow-xl max-h-60 overflow-y-auto animate-fadeIn">
                                    {availableTags.length > 0 ? (
                                        availableTags.map(tag => (
                                            <button
                                                key={tag.id}
                                                onMouseDown={(e) => e.preventDefault()} // Prevent blur
                                                onClick={() => handleAddTag(tag)}
                                                className="w-full text-left px-4 py-2.5 hover:bg-slate-50 transition-colors border-b border-slate-100 last:border-0 group"
                                            >
                                                <div className="flex items-center justify-between">
                                                    <div>
                                                        <span className="font-medium text-slate-700 group-hover:text-primary-700">{tag.displayName}</span>
                                                        {tag.slug && <span className="ml-2 text-xs text-slate-400 font-mono">#{tag.slug}</span>}
                                                    </div>
                                                    <span className="text-xs text-slate-500 bg-slate-100 px-2 py-0.5 rounded capitalize">{tag.category}</span>
                                                </div>
                                            </button>
                                        ))
                                    ) : (
                                        <div className="px-4 py-3 text-sm text-slate-500 text-center">
                                            No matching tags found
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                        <p className="mt-2 text-xs text-slate-500">
                            Search for tags like "Ghanaraga", "Bhakti", "Diwali", etc.
                        </p>
                    </div>
                )}
            </div>
        </div>
    );
};
