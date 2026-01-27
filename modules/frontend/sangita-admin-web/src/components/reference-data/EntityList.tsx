import React, { useState } from 'react';
import { EntityType } from '../../hooks/useEntityCrud';

interface EntityListProps {
    type: EntityType;
    data: any[];
    loading: boolean;
    onEdit: (item: any) => void;
    onDelete?: (item: any) => void;
    onCreate: () => void;
    onBack: () => void;
}

export const EntityList: React.FC<EntityListProps> = ({
    type,
    data,
    loading,
    onEdit,
    onDelete,
    onCreate,
    onBack
}) => {
    const [searchTerm, setSearchTerm] = useState('');

    const filteredData = data.filter(item =>
        item.name?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.normalizedName?.toLowerCase().includes(searchTerm.toLowerCase()) ||
        item.canonicalName?.toLowerCase().includes(searchTerm.toLowerCase()) // For temples
    );

    // Dynamic columns based on type
    const renderSubtitle = (item: any) => {
        switch (type) {
            case 'Composers': return `${item.place || 'Unknown Place'} • ${item.birthYear || '?'} - ${item.deathYear || '?'}`;
            case 'Ragas': return `Melakarta: ${item.melakartaNumber || 'N/A'}`;
            case 'Talas': return `Angas: ${item.angaStructure}`;
            case 'Temples': return `${item.city || ''}, ${item.state || ''}`;
            case 'Deities': return item.description ? item.description.substring(0, 50) + '...' : 'No description';
            default: return '';
        }
    };

    return (
        <div className="space-y-6 animate-fadeIn">
            {/* Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <nav className="flex items-center gap-2 text-sm text-ink-500">
                    <span onClick={onBack} className="hover:text-primary cursor-pointer transition-colors">Reference Data</span>
                    <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                    <span className="font-medium text-ink-900">{type}</span>
                </nav>
                <button
                    onClick={onCreate}
                    className="px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors flex items-center gap-2 w-full sm:w-auto justify-center"
                >
                    <span className="material-symbols-outlined text-[18px]">add</span>
                    Create New
                </button>
            </div>

            {/* Search */}
            <div className="relative">
                <span className="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-ink-400">search</span>
                <input
                    type="text"
                    placeholder={`Search ${type}...`}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-11 pr-4 py-3 bg-white border border-border-light rounded-lg text-ink-900 focus:ring-2 focus:ring-primary focus:border-transparent outline-none transition-all"
                />
            </div>

            {/* List */}
            {loading ? (
                <div className="text-center py-12 text-ink-500">Loading {type}...</div>
            ) : filteredData.length === 0 ? (
                <div className="text-center py-12 bg-slate-50 rounded-lg border border-border-light">
                    <p className="text-ink-500">No {type.toLowerCase()} found.</p>
                    {searchTerm && <button onClick={() => setSearchTerm('')} className="text-primary hover:underline mt-2 text-sm">Clear Search</button>}
                </div>
            ) : (
                <div className="bg-white rounded-xl border border-border-light overflow-hidden shadow-sm">
                    <div className="divide-y divide-border-light">
                        {filteredData.map((item) => (
                            <div
                                key={item.id}
                                className="p-4 hover:bg-slate-50 transition-colors flex justify-between items-center group cursor-pointer"
                                onClick={() => onEdit(item)}
                            >
                                <div>
                                    <h4 className="font-bold text-ink-900">{item.name || item.canonicalName}</h4>
                                    <p className="text-sm text-ink-500 mt-1">{renderSubtitle(item)}</p>
                                </div>
                                <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button
                                        onClick={(e) => { e.stopPropagation(); onEdit(item); }}
                                        className="p-2 text-ink-400 hover:text-primary hover:bg-white rounded-full transition-colors"
                                    >
                                        <span className="material-symbols-outlined">edit</span>
                                    </button>
                                    {onDelete && (
                                        <button
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                if (confirm('Are you sure you want to delete this item?')) {
                                                    onDelete(item);
                                                }
                                            }}
                                            className="p-2 text-ink-400 hover:text-red-500 hover:bg-white rounded-full transition-colors"
                                        >
                                            <span className="material-symbols-outlined">delete</span>
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                    <div className="p-3 bg-slate-50 text-xs text-center text-ink-500 border-t border-border-light">
                        Showing {filteredData.length} of {data.length} items
                    </div>
                </div>
            )}
        </div>
    );
};
