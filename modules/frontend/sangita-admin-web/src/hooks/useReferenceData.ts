import { useState, useEffect } from 'react';
import {
    getComposers,
    getRagas,
    getTalas,
    getDeities,
    getTemples,
    getSampradayas,
    getTags
} from '../api/client';
import { ReferenceDataState } from '../types/krithi-editor.types';

// Internal state doesn't have the function
type ReferenceDataInternal = Omit<ReferenceDataState, 'loadTags'>;

export const useReferenceData = (): ReferenceDataState => {
    const [data, setData] = useState<ReferenceDataInternal>({
        composers: [],
        ragas: [],
        talas: [],
        deities: [],
        temples: [],
        tags: [],
        sampradayas: [],
        loading: true,
        error: null
    });

    const loadData = async () => {
        setData(prev => ({ ...prev, loading: true, error: null }));
        try {
            const [c, r, t, d, tm, s] = await Promise.all([
                getComposers(),
                getRagas(),
                getTalas(),
                getDeities(),
                getTemples(),
                getSampradayas()
            ]);

            setData({
                composers: c,
                ragas: r,
                talas: t,
                deities: d,
                temples: tm,
                tags: [], // Loaded separately
                sampradayas: s,
                loading: false,
                error: null
            });
        } catch (err: any) {
            setData(prev => ({
                ...prev,
                loading: false,
                error: err.message || 'Failed to load reference data'
            }));
        }
    };

    const refreshDeities = async () => {
        try {
            const d = await getDeities();
            setData(prev => ({ ...prev, deities: d }));
        } catch (e) {
            console.error(e);
        }
    };

    const refreshTemples = async () => {
        try {
            const t = await getTemples();
            setData(prev => ({ ...prev, temples: t }));
        } catch (e) {
            console.error(e);
        }
    };

    const loadTags = async () => {
        try {
            const t = await getTags();
            setData(prev => ({ ...prev, tags: t }));
        } catch (e) {
            console.error(e);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    return { ...data, loadTags };
};
