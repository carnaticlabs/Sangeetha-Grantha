import React, { useState, useEffect } from 'react';
import { useEntityCrud, EntityType } from '../hooks/useEntityCrud';
import { EntityList, ComposerForm, RagaForm, TalaForm, TempleForm, DeityForm } from '../components/reference-data';

type ViewMode = 'HOME' | 'LIST' | 'FORM';

const ReferenceData: React.FC = () => {
    const { loading, stats, loadStats, loadData, saveEntity, deleteEntity } = useEntityCrud();

    const [viewMode, setViewMode] = useState<ViewMode>('HOME');
    const [activeEntity, setActiveEntity] = useState<EntityType | null>(null);
    const [entityData, setEntityData] = useState<any[]>([]);
    const [selectedItem, setSelectedItem] = useState<any | null>(null);

    useEffect(() => {
        loadStats();
    }, [loadStats]);

    const handleEntityClick = async (type: EntityType) => {
        setActiveEntity(type);
        setViewMode('LIST');
        setSelectedItem(null);
        // Load data
        const data = await loadData(type);
        setEntityData(data);
    };

    const handleBack = () => {
        if (viewMode === 'FORM') {
            setViewMode('LIST');
            setSelectedItem(null);
        } else {
            setViewMode('HOME');
            setActiveEntity(null);
            setSelectedItem(null);
        }
    };

    const handleCreate = () => {
        setSelectedItem(null);
        setViewMode('FORM');
    };

    const handleEdit = (item: any) => {
        setSelectedItem(item);
        setViewMode('FORM');
    };

    const handleDelete = async (item: any) => {
        if (activeEntity) {
            const success = await deleteEntity(activeEntity, item.id);
            if (success) {
                // Refresh list
                const data = await loadData(activeEntity);
                setEntityData(data);
                loadStats(); // Refresh stats too
            }
        }
    };

    const handleSave = async (data: any) => {
        if (activeEntity) {
            await saveEntity(activeEntity, data, selectedItem?.id);
            // Refresh list and go back
            const refreshedData = await loadData(activeEntity);
            setEntityData(refreshedData);
            loadStats();
            setViewMode('LIST');
        }
    };

    const renderCard = (title: string, count: number, description: string, colorClass: string, type: EntityType) => (
        <div
            onClick={() => handleEntityClick(type)}
            className="bg-surface-light border border-border-light rounded-xl p-6 hover:shadow-md transition-all cursor-pointer group relative overflow-hidden hover:-translate-y-1"
        >
            <div className={`absolute top-0 left-0 w-1 h-full ${colorClass}`}></div>
            <div className="flex justify-between items-start mb-4">
                <h3 className="font-display text-xl font-bold text-ink-900">{title}</h3>
                <span className="bg-slate-100 text-ink-600 text-xs font-bold px-2.5 py-1 rounded-full">{count}</span>
            </div>
            <p className="text-ink-500 text-sm leading-relaxed">{description}</p>
            <div className="mt-6 flex items-center text-primary text-sm font-bold opacity-0 group-hover:opacity-100 transition-opacity translate-y-2 group-hover:translate-y-0 duration-300">
                <span>Manage {title}</span>
                <span className="material-symbols-outlined text-[16px] ml-1">arrow_forward</span>
            </div>
        </div>
    );

    if (viewMode === 'HOME') {
        return (
            <div className="max-w-7xl mx-auto space-y-8 animate-fadeIn pb-12">
                <div className="flex flex-col md:flex-row md:items-end justify-between gap-4">
                    <div>
                        <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">Reference Data</h1>
                        <p className="text-ink-500 mt-2">Manage canonical datasets used across the archive.</p>
                    </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {renderCard('Composers', stats?.composerCount || 0, 'Vaggeyakaras and lyricists.', 'bg-orange-500', 'Composers')}
                    {renderCard('Ragas', stats?.ragaCount || 0, 'Melakarta and Janya ragas.', 'bg-blue-500', 'Ragas')}
                    {renderCard('Talas', stats?.talaCount || 0, 'Suladi Sapta and other talas.', 'bg-purple-500', 'Talas')}
                    {renderCard('Temples', stats?.templeCount || 0, 'Kshetras and holy sites.', 'bg-emerald-500', 'Temples')}
                    {renderCard('Deities', stats?.deityCount || 0, 'Gods and Goddesses.', 'bg-rose-500', 'Deities')}
                </div>
            </div>
        );
    }

    if (viewMode === 'LIST' && activeEntity) {
        return (
            <div className="max-w-5xl mx-auto pb-12">
                <EntityList
                    type={activeEntity}
                    data={entityData}
                    loading={loading && entityData.length === 0}
                    onEdit={handleEdit}
                    onDelete={handleDelete}
                    onCreate={handleCreate}
                    onBack={handleBack}
                />
            </div>
        );
    }

    if (viewMode === 'FORM' && activeEntity) {
        const commonProps = {
            initialData: selectedItem,
            onSave: handleSave,
            onCancel: handleBack,
            saving: loading // useEntityCrud sets loading true during save
        };

        return (
            <div className="max-w-5xl mx-auto pb-12 animate-fadeIn">
                {/* Header */}
                <div className="space-y-4 mb-8">
                    <nav className="flex items-center gap-2 text-sm text-ink-500">
                        <span className="hover:text-primary transition-colors cursor-pointer" onClick={() => setViewMode('HOME')}>Reference Data</span>
                        <span className="material-symbols-outlined text-base">chevron_right</span>
                        <span className="hover:text-primary transition-colors cursor-pointer" onClick={() => setViewMode('LIST')}>{activeEntity}</span>
                        <span className="material-symbols-outlined text-base">chevron_right</span>
                        <span className="text-ink-900 font-medium">{selectedItem ? 'Edit' : 'Create'} {activeEntity?.slice(0, -1)}</span>
                    </nav>
                    <div className="flex flex-col sm:flex-row sm:items-end sm:justify-between gap-4">
                        <div>
                            <h2 className="text-3xl md:text-4xl font-display font-bold text-ink-900 tracking-tight">
                                {selectedItem ? 'Edit' : 'Create'} {activeEntity?.slice(0, -1)}
                            </h2>
                            <p className="text-ink-500 mt-2 text-base">Define canonical details for this entity.</p>
                        </div>
                    </div>
                </div>

                <div className="bg-surface-light rounded-xl shadow-sm border border-border-light overflow-hidden p-6 md:p-8">
                    {activeEntity === 'Composers' && <ComposerForm {...commonProps} />}
                    {activeEntity === 'Ragas' && <RagaForm {...commonProps} />}
                    {activeEntity === 'Talas' && <TalaForm {...commonProps} />}
                    {activeEntity === 'Temples' && <TempleForm {...commonProps} />}
                    {activeEntity === 'Deities' && <DeityForm {...commonProps} />}
                </div>
            </div>
        );
    }

    return null;
};

export default ReferenceData;
