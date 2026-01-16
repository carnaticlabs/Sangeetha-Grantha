import React, { useState, useEffect } from 'react';
import {
    getAdminKrithiNotation,
    createNotationVariant,
    updateNotationVariant,
    deleteNotationVariant,
    createNotationRow,
    updateNotationRow,
    deleteNotationRow
} from '../../api/client';
import { NotationResponse, NotationVariant, NotationRow, MusicalForm } from '../../types';
import NotationVariantList from './NotationVariantList';
import NotationRowsEditor from './NotationRowsEditor';
import NotationVariantModal from './NotationVariantModal';

interface NotationTabProps {
    krithiId: string;
    musicalForm: MusicalForm;
}

const NotationTab: React.FC<NotationTabProps> = ({ krithiId, musicalForm }) => {
    // State
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [data, setData] = useState<NotationResponse | null>(null);
    const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingVariant, setEditingVariant] = useState<NotationVariant | undefined>(undefined);

    // Fetch Data
    const fetchData = async (): Promise<NotationResponse | null> => {
        setLoading(true);
        setError(null);
        try {
            const response = await getAdminKrithiNotation(krithiId, musicalForm);
            setData(response);

            // Auto-select first variant if none selected
            if (response.variants.length > 0 && !selectedVariantId) {
                setSelectedVariantId(response.variants[0].variant.id);
            }
            return response;
        } catch (err: any) {
            console.error("API Call failed", err);
            setError(err.message || 'Failed to load notation');
            return null;
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
    }, [krithiId, musicalForm]);

    // Variant Actions
    const handleAddVariant = () => {
        setEditingVariant(undefined);
        setIsModalOpen(true);
    };

    const handleEditVariant = (variant: NotationVariant) => {
        setEditingVariant(variant);
        setIsModalOpen(true);
    };

    const handleSaveVariant = async (payload: Partial<NotationVariant>) => {
        try {
            if (editingVariant) {
                await updateNotationVariant(editingVariant.id, payload);
            } else {
                await createNotationVariant(krithiId, payload);
            }
            setIsModalOpen(false);
            fetchData(); // Refresh
        } catch (err: any) {
            alert(`Failed to save variant: ${err.message}`);
        }
    };

    const handleDeleteVariant = async (variantId: string) => {
        if (!confirm("Are you sure? This will delete all rows in this variant.")) return;
        try {
            await deleteNotationVariant(variantId);
            if (selectedVariantId === variantId) setSelectedVariantId(null);
            fetchData();
        } catch (err: any) {
            alert(`Failed to delete: ${err.message}`);
        }
    };

    const handleSetPrimary = async (variantId: string) => {
        try {
            await updateNotationVariant(variantId, { isPrimary: true });
            fetchData();
        } catch (err: any) {
            alert(`Failed to set primary: ${err.message}`);
        }
    };

    // Row Actions
    const handleAddRow = async (sectionId: string) => {
        if (!selectedVariantId) return;
        try {
            // Refresh data first to ensure we have the latest state
            // This prevents duplicate key errors from stale data
            const freshData = await fetchData();
            if (!freshData) {
                alert('Failed to load current notation data');
                return;
            }
            
            // Calculate next available order index with fresh data
            const variantData = freshData.variants.find(v => v.variant.id === selectedVariantId);
            if (!variantData) {
                alert('Variant not found');
                return;
            }
            
            const rowsBySectionId = variantData.rowsBySectionId || {};
            const rows = rowsBySectionId[sectionId] || [];
            
            // Debug: log what we're seeing
            console.log('Section ID:', sectionId);
            console.log('Rows for section:', rows);
            console.log('RowsBySectionId:', rowsBySectionId);
            
            // Get all existing order_index values for this section (filter out null/undefined/0)
            const existingOrderIndices = new Set(
                rows
                    .map(r => r?.orderIndex)
                    .filter((idx): idx is number => idx != null && typeof idx === 'number' && idx > 0)
            );
            
            console.log('Existing order indices:', Array.from(existingOrderIndices));
            
            // Find the next available order_index
            // Start from 1 and find the first number that doesn't exist
            let nextOrderIndex = 1;
            while (existingOrderIndices.has(nextOrderIndex)) {
                nextOrderIndex++;
            }
            
            console.log('Next order index to use:', nextOrderIndex);

            await createNotationRow(selectedVariantId, {
                sectionId,
                orderIndex: nextOrderIndex,
                swaraText: '',
                sahityaText: ''
            });
            // Refresh data to get the new row
            await fetchData();
        } catch (err: any) {
            console.error('Failed to add row:', err);
            alert(`Failed to add row: ${err.message}`);
            // Refresh data on error to sync state
            fetchData();
        }
    };

    const handleUpdateRow = async (rowId: string, payload: Partial<NotationRow>) => {
        // Optimistic update for text input performance
        if (data && selectedVariantId) {
            const newData = { ...data };
            const vIdx = newData.variants.findIndex(v => v.variant.id === selectedVariantId);
            if (vIdx >= 0) {
                const variantData = newData.variants[vIdx];
                // Ensure rowsBySectionId exists
                if (!variantData.rowsBySectionId) {
                    variantData.rowsBySectionId = {};
                }
                // Find section and row
                for (const secId in variantData.rowsBySectionId) {
                    const rows = variantData.rowsBySectionId[secId];
                    if (Array.isArray(rows)) {
                        const rIdx = rows.findIndex(r => r.id === rowId);
                        if (rIdx >= 0) {
                            rows[rIdx] = { ...rows[rIdx], ...payload };
                            setData(newData);
                            break;
                        }
                    }
                }
            }
        }

        // Debounce actual save could be good here, but for now direct call
        // (Might want to use a useDebounce hook in production)
        try {
            await updateNotationRow(rowId, payload);
        } catch (err) {
            console.error("Failed to save row", err);
            // Revert?
        }
    };

    const handleDeleteRow = async (rowId: string) => {
        if (!confirm("Delete this row?")) return;
        try {
            await deleteNotationRow(rowId);
            fetchData();
        } catch (err: any) {
            alert(`Failed to delete row: ${err.message}`);
        }
    };

    if (loading && !data) return <div className="p-8 text-center text-ink-500">Loading notation...</div>;
    if (error) return <div className="p-8 text-center text-red-600">Error: {error}</div>;
    if (!data) return null;

    const activeVariantData = data.variants.find(v => v.variant.id === selectedVariantId);

    return (
        <div className="h-[calc(100vh-250px)] flex gap-6 animate-fadeIn">
            {/* Left: Variant List */}
            <div className="w-1/4 min-w-[250px]">
                <NotationVariantList
                    variants={data.variants.map(v => v.variant)}
                    selectedVariantId={selectedVariantId}
                    onSelectVariant={(v) => setSelectedVariantId(v.id)}
                    onAddVariant={handleAddVariant}
                    onEditVariant={handleEditVariant}
                    onDeleteVariant={handleDeleteVariant}
                    onSetPrimary={handleSetPrimary}
                />
            </div>

            {/* Right: Rows Editor */}
            <div className="flex-1">
                {selectedVariantId && activeVariantData ? (
                    <NotationRowsEditor
                        variantId={selectedVariantId}
                        sections={data.sections}
                        rowsBySectionId={activeVariantData.rowsBySectionId || {}}
                        onAddRow={handleAddRow}
                        onUpdateRow={handleUpdateRow}
                        onDeleteRow={handleDeleteRow}
                    />
                ) : (
                    <div className="bg-slate-50 border border-border-light border-dashed rounded-xl h-full flex items-center justify-center text-ink-400">
                        Select a variant to edit notation
                    </div>
                )}
            </div>

            {isModalOpen && (
                <NotationVariantModal
                    krithiId={krithiId}
                    existingVariant={editingVariant}
                    onClose={() => setIsModalOpen(false)}
                    onSave={handleSaveVariant}
                />
            )}
        </div>
    );
};

export default NotationTab;
