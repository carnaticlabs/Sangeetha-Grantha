import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
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
import { useToast } from '../Toast';

interface NotationTabProps {
    krithiId: string;
    musicalForm: MusicalForm;
}

const NotationTab: React.FC<NotationTabProps> = ({ krithiId, musicalForm }) => {
    const queryClient = useQueryClient();
    const toast = useToast();

    // State
    const [selectedVariantId, setSelectedVariantId] = useState<string | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [editingVariant, setEditingVariant] = useState<NotationVariant | undefined>(undefined);

    // Query
    const queryKey = ['notation', krithiId, musicalForm];

    const {
        data,
        isLoading: loading,
        error
    } = useQuery({
        queryKey,
        queryFn: () => getAdminKrithiNotation(krithiId, musicalForm),
        staleTime: 1000 * 60 * 5, // 5 minutes
    });

    // Effect to auto-select first variant
    useEffect(() => {
        if (data && data.variants.length > 0 && !selectedVariantId) {
            setSelectedVariantId(data.variants[0].variant.id);
        }
    }, [data, selectedVariantId]);

    // Mutations
    const saveVariantMutation = useMutation({
        mutationFn: async (payload: Partial<NotationVariant>) => {
            if (editingVariant) {
                return updateNotationVariant(editingVariant.id, payload);
            } else {
                return createNotationVariant(krithiId, payload);
            }
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey });
            setIsModalOpen(false);
            toast.success(editingVariant ? 'Variant updated' : 'Variant created');
        },
        onError: (err: any) => toast.error(`Failed to save variant: ${err.message}`)
    });

    const deleteVariantMutation = useMutation({
        mutationFn: deleteNotationVariant,
        onSuccess: (_, deletedId) => {
            queryClient.invalidateQueries({ queryKey });
            if (selectedVariantId === deletedId) setSelectedVariantId(null);
            toast.success('Variant deleted');
        },
        onError: (err: any) => toast.error(`Failed to delete: ${err.message}`)
    });

    const setPrimaryMutation = useMutation({
        mutationFn: (variantId: string) => updateNotationVariant(variantId, { isPrimary: true }),
        onSuccess: () => queryClient.invalidateQueries({ queryKey }),
        onError: (err: any) => toast.error(`Failed to set primary: ${err.message}`)
    });

    const createRowMutation = useMutation({
        mutationFn: async ({ variantId, payload }: { variantId: string, payload: any }) => {
            return createNotationRow(variantId, payload);
        },
        onSuccess: () => queryClient.invalidateQueries({ queryKey }),
        onError: (err: any) => toast.error(`Failed to add row: ${err.message}`)
    });

    const updateRowMutation = useMutation({
        mutationFn: async ({ rowId, payload }: { rowId: string, payload: Partial<NotationRow> }) => {
            return updateNotationRow(rowId, payload);
        },
        // We ideally optimistically update here, but for now just invalidate
        onSuccess: () => queryClient.invalidateQueries({ queryKey }),
        onError: (err: any) => console.error("Failed to save row", err) // Silent or minimal error for row updates to not spam
    });

    const deleteRowMutation = useMutation({
        mutationFn: deleteNotationRow,
        onSuccess: () => queryClient.invalidateQueries({ queryKey }),
        onError: (err: any) => toast.error(`Failed to delete row: ${err.message}`)
    });

    // Handlers
    const handleAddVariant = () => {
        setEditingVariant(undefined);
        setIsModalOpen(true);
    };

    const handleEditVariant = (variant: NotationVariant) => {
        setEditingVariant(variant);
        setIsModalOpen(true);
    };

    const handleSaveVariant = (payload: Partial<NotationVariant>) => {
        saveVariantMutation.mutate(payload);
    };

    const handleDeleteVariant = (variantId: string) => {
        if (confirm("Are you sure? This will delete all rows in this variant.")) {
            deleteVariantMutation.mutate(variantId);
        }
    };

    const handleSetPrimary = (variantId: string) => {
        setPrimaryMutation.mutate(variantId);
    };

    const handleAddRow = (sectionId: string) => {
        if (!selectedVariantId || !data) return;

        const variantData = data.variants.find(v => v.variant.id === selectedVariantId);
        if (!variantData) return;

        const rowsBySectionId = variantData.rowsBySectionId || {};
        const rows = rowsBySectionId[sectionId] || [];

        // Next order index logic
        const existingOrderIndices = new Set(
            rows
                .map(r => r?.orderIndex)
                .filter((idx): idx is number => idx != null && typeof idx === 'number' && idx > 0)
        );

        let nextOrderIndex = 1;
        while (existingOrderIndices.has(nextOrderIndex)) {
            nextOrderIndex++;
        }

        createRowMutation.mutate({
            variantId: selectedVariantId,
            payload: {
                sectionId,
                orderIndex: nextOrderIndex,
                swaraText: '',
                sahityaText: ''
            }
        });
    };

    const handleUpdateRow = (rowId: string, payload: Partial<NotationRow>) => {
        // Optimistic update logic from before could be moved to onMutate, 
        // but simple invalidation is safer for now to avoid complexity.
        // If typing performance is bad, we can debounce in the child component.
        updateRowMutation.mutate({ rowId, payload });
    };

    const handleDeleteRow = (rowId: string) => {
        if (confirm("Delete this row?")) {
            deleteRowMutation.mutate(rowId);
        }
    };

    if (loading) return <div className="p-8 text-center text-ink-500">Loading notation...</div>;
    if (error) return <div className="p-8 text-center text-red-600">Error loading data</div>;
    if (!data) return null;

    const activeVariantData = data.variants.find(v => v.variant.id === selectedVariantId);

    return (
        <div className="h-[calc(100vh-250px)] flex gap-6 animate-fadeIn">
            {/* Left: Variant List */}
            <div className="w-1/4 min-w-[250px] bg-white border border-border-light rounded-xl shadow-sm overflow-hidden flex flex-col p-4">
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
            <div className="flex-1 bg-white border border-border-light rounded-xl shadow-sm overflow-hidden flex flex-col p-4">
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
                    <div className="h-full flex items-center justify-center text-ink-400 italic">
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
