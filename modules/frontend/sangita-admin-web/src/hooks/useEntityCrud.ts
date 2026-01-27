import { useState, useCallback } from 'react';
import {
    getReferenceStats,
    getDeities,
    getComposers, getRagas, getTalas, getTemples,
    createComposer, updateComposer, deleteComposer,
    createRaga, updateRaga, deleteRaga,
    createTala, updateTala, deleteTala,
    createTemple, updateTemple, deleteTemple,
    createDeity, updateDeity, deleteDeity
} from '../api/client';
import { useToast } from '../components/Toast';

export type EntityType = 'Composers' | 'Ragas' | 'Talas' | 'Temples' | 'Deities';

export const useEntityCrud = () => {
    const toast = useToast();
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState<any>(null);

    const loadStats = useCallback(async () => {
        try {
            const data = await getReferenceStats();
            setStats(data);
        } catch (e) {
            console.error("Failed to load reference stats", e);
        }
    }, []);

    const loadData = useCallback(async (type: EntityType) => {
        setLoading(true);
        try {
            switch (type) {
                case 'Composers': return await getComposers();
                case 'Ragas': return await getRagas();
                case 'Talas': return await getTalas();
                case 'Temples': return await getTemples();
                case 'Deities': return await getDeities();
            }
        } catch (err: any) {
            console.error(`Failed to load ${type}:`, err);
            toast.error(`Failed to load ${type}`);
            return [];
        } finally {
            setLoading(false);
        }
    }, [toast]);

    const saveEntity = useCallback(async (type: EntityType, data: any, id?: string) => {
        setLoading(true); // Helper to show loading state during save too
        try {
            let result;
            if (id) {
                // Update
                switch (type) {
                    case 'Composers': result = await updateComposer(id, data); break;
                    case 'Ragas': result = await updateRaga(id, data); break;
                    case 'Talas': result = await updateTala(id, data); break;
                    case 'Temples': result = await updateTemple(id, data); break;
                    case 'Deities': result = await updateDeity(id, data); break;
                }
                toast.success(`${type.slice(0, -1)} updated successfully`);
            } else {
                // Create
                switch (type) {
                    case 'Composers': result = await createComposer(data); break;
                    case 'Ragas': result = await createRaga(data); break;
                    case 'Talas': result = await createTala(data); break;
                    case 'Temples': result = await createTemple(data); break;
                    case 'Deities': result = await createDeity(data); break;
                }
                toast.success(`${type.slice(0, -1)} created successfully`);
            }
            return result;
        } catch (err: any) {
            console.error(`Failed to save ${type}:`, err);
            toast.error(`Failed to save: ${err.message || 'Unknown error'}`);
            throw err;
        } finally {
            setLoading(false);
        }
    }, [toast]);

    const deleteEntity = useCallback(async (type: EntityType, id: string) => {
        try {
            switch (type) {
                case 'Composers': await deleteComposer(id); break;
                case 'Ragas': await deleteRaga(id); break;
                case 'Talas': await deleteTala(id); break;
                case 'Temples': await deleteTemple(id); break;
                case 'Deities': await deleteDeity(id); break;
            }
            toast.success(`${type.slice(0, -1)} deleted successfully`);
            return true;
        } catch (err: any) {
            console.error(`Failed to delete ${type}:`, err);
            toast.error(`Failed to delete: ${err.message || 'Unknown error'}`);
            return false;
        }
    }, [toast]);

    return {
        loading,
        stats,
        loadStats,
        loadData,
        saveEntity,
        deleteEntity
    };
};
