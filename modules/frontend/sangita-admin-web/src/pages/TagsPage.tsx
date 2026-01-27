import React, { useState, useEffect } from 'react';
import { getAllTags, createTag, updateTag, deleteTag } from '../api/client';
import { Tag } from '../types';
import { formatTagCategory, TAG_CATEGORY_LABELS } from '../utils/enums';
import { useToast } from '../components/Toast';

const TAG_CATEGORIES = Object.keys(TAG_CATEGORY_LABELS);

const TagsPage: React.FC = () => {
    const { success, error } = useToast();
    const toast = { success, error };
    const [tags, setTags] = useState<Tag[]>([]);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [editingTag, setEditingTag] = useState<Tag | null>(null);
    const [isCreating, setIsCreating] = useState(false);
    const [formData, setFormData] = useState({
        category: 'OTHER',
        slug: '',
        displayNameEn: '',
        descriptionEn: '',
    });

    useEffect(() => {
        loadTags();
    }, []);

    const loadTags = async () => {
        setLoading(true);
        try {
            const data = await getAllTags();
            setTags(data);
        } catch (err) {
            console.error('Failed to load tags:', err);
            const message = err instanceof Error ? err.message : 'Unknown error';
            toast.error('Failed to load tags: ' + message);
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = async (e?: React.MouseEvent) => {
        e?.preventDefault();
        e?.stopPropagation();

        if (!formData.slug || !formData.displayNameEn) {
            toast.error('Slug and Display Name are required');
            return;
        }

        try {
            await createTag({
                category: formData.category,
                slug: formData.slug,
                displayNameEn: formData.displayNameEn,
                descriptionEn: formData.descriptionEn || null,
            });
            toast.success('Tag created successfully');
            setIsCreating(false);
            setFormData({ category: 'OTHER', slug: '', displayNameEn: '', descriptionEn: '' });
            await loadTags();
        } catch (err) {
            console.error('Failed to create tag:', err);
            const message = err instanceof Error ? err.message : 'Unknown error';
            toast.error('Failed to create tag: ' + message);
        }
    };

    const handleUpdate = async () => {
        if (!editingTag) return;
        if (!formData.slug || !formData.displayNameEn) {
            toast.error('Slug and Display Name are required');
            return;
        }

        try {
            await updateTag(editingTag.id, {
                category: formData.category,
                slug: formData.slug,
                displayNameEn: formData.displayNameEn,
                descriptionEn: formData.descriptionEn || null,
            });
            toast.success('Tag updated successfully');
            setEditingTag(null);
            setFormData({ category: 'OTHER', slug: '', displayNameEn: '', descriptionEn: '' });
            loadTags();
        } catch (err) {
            console.error('Failed to update tag:', err);
            const message = err instanceof Error ? err.message : 'Unknown error';
            toast.error('Failed to update tag: ' + message);
        }
    };

    const handleDelete = async (id: string) => {
        if (!confirm('Are you sure you want to delete this tag?')) return;

        try {
            await deleteTag(id);
            toast.success('Tag deleted successfully');
            loadTags();
        } catch (err) {
            console.error('Failed to delete tag:', err);
            const message = err instanceof Error ? err.message : 'Unknown error';
            toast.error('Failed to delete tag: ' + message);
        }
    };

    const startEdit = (tag: Tag) => {
        setEditingTag(tag);
        setFormData({
            category: tag.category,
            slug: tag.slug || '',
            displayNameEn: tag.displayNameEn || tag.displayName,
            descriptionEn: tag.descriptionEn || '',
        });
        setIsCreating(false);
    };

    const startCreate = () => {
        setIsCreating(true);
        setEditingTag(null);
        setFormData({ category: 'OTHER', slug: '', displayNameEn: '', descriptionEn: '' });
    };

    const cancelEdit = () => {
        setIsCreating(false);
        setEditingTag(null);
        setFormData({ category: 'OTHER', slug: '', displayNameEn: '', descriptionEn: '' });
    };

    const filteredTags = tags.filter(tag => {
        const searchLower = searchTerm.toLowerCase();
        return (
            (tag.displayNameEn || tag.displayName).toLowerCase().includes(searchLower) ||
            tag.category.toLowerCase().includes(searchLower) ||
            (tag.slug && tag.slug.toLowerCase().includes(searchLower))
        );
    });

    return (
        <div className="max-w-7xl mx-auto h-full flex flex-col space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
                <div>
                    <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Tags</h1>
                    <p className="text-ink-500 mt-2">Manage tags for categorizing compositions.</p>
                </div>
                <button
                    type="button"
                    onClick={startCreate}
                    className="flex items-center gap-2 px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20"
                >
                    <span className="material-symbols-outlined text-[20px]">add</span>
                    Add New Tag
                </button>
            </div>

            {/* Create/Edit Form */}
            {(isCreating || editingTag) && (
                <div className="bg-surface-light border border-border-light rounded-xl shadow-sm p-6">
                    <h2 className="text-lg font-semibold text-ink-900 mb-4">
                        {isCreating ? 'Create New Tag' : 'Edit Tag'}
                    </h2>
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-ink-700 mb-1">Category</label>
                            <select
                                value={formData.category}
                                onChange={(e) => setFormData({ ...formData, category: e.target.value })}
                                className="w-full px-3 py-2 border border-border-light rounded-lg bg-white text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            >
                                {TAG_CATEGORIES.map(cat => (
                                    <option key={cat} value={cat}>{formatTagCategory(cat)}</option>
                                ))}
                            </select>
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-ink-700 mb-1">Slug *</label>
                            <input
                                type="text"
                                value={formData.slug}
                                onChange={(e) => setFormData({ ...formData, slug: e.target.value })}
                                placeholder="tag-slug"
                                className="w-full px-3 py-2 border border-border-light rounded-lg bg-white text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            />
                        </div>
                        <div className="md:col-span-2">
                            <label className="block text-sm font-medium text-ink-700 mb-1">Display Name (English) *</label>
                            <input
                                type="text"
                                value={formData.displayNameEn}
                                onChange={(e) => setFormData({ ...formData, displayNameEn: e.target.value })}
                                placeholder="Tag Display Name"
                                className="w-full px-3 py-2 border border-border-light rounded-lg bg-white text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            />
                        </div>
                        <div className="md:col-span-2">
                            <label className="block text-sm font-medium text-ink-700 mb-1">Description (English)</label>
                            <textarea
                                value={formData.descriptionEn}
                                onChange={(e) => setFormData({ ...formData, descriptionEn: e.target.value })}
                                placeholder="Optional description"
                                rows={3}
                                className="w-full px-3 py-2 border border-border-light rounded-lg bg-white text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent"
                            />
                        </div>
                    </div>
                    <div className="flex gap-2 mt-4">
                        <button
                            type="button"
                            onClick={(e) => {
                                if (isCreating) {
                                    handleCreate(e);
                                } else {
                                    handleUpdate();
                                }
                            }}
                            className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors"
                        >
                            {isCreating ? 'Create' : 'Update'}
                        </button>
                        <button
                            type="button"
                            onClick={cancelEdit}
                            className="px-4 py-2 bg-slate-100 text-ink-700 rounded-lg text-sm font-medium hover:bg-slate-200 transition-colors"
                        >
                            Cancel
                        </button>
                    </div>
                </div>
            )}

            {/* Tags List */}
            <div className="bg-surface-light border border-border-light rounded-xl shadow-sm flex flex-col overflow-hidden">
                {/* Search */}
                <div className="p-4 border-b border-border-light bg-slate-50/50">
                    <div className="relative max-w-lg">
                        <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-ink-400 text-[20px]">search</span>
                        <input
                            type="text"
                            placeholder="Search tags by name, category, or slug..."
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            className="w-full pl-10 pr-4 py-2 bg-white border border-border-light rounded-lg text-sm focus:ring-2 focus:ring-primary focus:border-transparent transition-shadow"
                        />
                    </div>
                </div>

                {/* Table */}
                <div className="overflow-x-auto">
                    <table className="w-full text-left border-collapse">
                        <thead>
                            <tr className="bg-slate-50/50 border-b border-border-light text-xs font-semibold uppercase tracking-wider text-ink-500">
                                <th className="px-6 py-4">Display Name</th>
                                <th className="px-6 py-4">Category</th>
                                <th className="px-6 py-4">Slug</th>
                                <th className="px-6 py-4 text-right">Actions</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-100 bg-white">
                            {loading ? (
                                <tr>
                                    <td colSpan={4} className="px-6 py-8 text-center text-ink-500">Loading...</td>
                                </tr>
                            ) : filteredTags.length === 0 ? (
                                <tr>
                                    <td colSpan={4} className="px-6 py-8 text-center text-ink-500">
                                        {searchTerm ? 'No tags found matching your search.' : 'No tags found.'}
                                    </td>
                                </tr>
                            ) : filteredTags.map((tag) => (
                                <tr key={tag.id} className="hover:bg-slate-50 transition-colors">
                                    <td className="px-6 py-4">
                                        <span className="font-medium text-ink-900 text-sm">{tag.displayNameEn || tag.displayName}</span>
                                    </td>
                                    <td className="px-6 py-4">
                                        <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-primary-light text-primary">
                                            {formatTagCategory(tag.category)}
                                        </span>
                                    </td>
                                    <td className="px-6 py-4 text-sm text-ink-600 font-mono">
                                        {tag.slug || '-'}
                                    </td>
                                    <td className="px-6 py-4 text-right">
                                        <div className="flex items-center justify-end gap-2">
                                            <button
                                                onClick={() => startEdit(tag)}
                                                className="p-2 text-ink-500 hover:text-primary hover:bg-primary-light rounded-lg transition-colors"
                                                title="Edit"
                                            >
                                                <span className="material-symbols-outlined text-[18px]">edit</span>
                                            </button>
                                            <button
                                                onClick={() => handleDelete(tag.id)}
                                                className="p-2 text-ink-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                title="Delete"
                                            >
                                                <span className="material-symbols-outlined text-[18px]">delete</span>
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
};

export default TagsPage;
