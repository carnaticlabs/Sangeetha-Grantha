import { useCallback, useMemo } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
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

export const useReferenceData = (): ReferenceDataState => {
    const queryClient = useQueryClient();
    const staleTime = 60 * 60 * 1000; // 1 hour cache for reference data

    // Individual queries for granular caching and error handling
    const composersQuery = useQuery({
        queryKey: ['composers'],
        queryFn: getComposers,
        staleTime
    });

    const ragasQuery = useQuery({
        queryKey: ['ragas'],
        queryFn: getRagas,
        staleTime
    });

    const talasQuery = useQuery({
        queryKey: ['talas'],
        queryFn: getTalas,
        staleTime
    });

    const deitiesQuery = useQuery({
        queryKey: ['deities'],
        queryFn: getDeities,
        staleTime
    });

    const templesQuery = useQuery({
        queryKey: ['temples'],
        queryFn: getTemples,
        staleTime
    });

    const sampradayasQuery = useQuery({
        queryKey: ['sampradayas'],
        queryFn: getSampradayas,
        staleTime
    });

    const tagsQuery = useQuery({
        queryKey: ['tags'],
        queryFn: getTags,
        staleTime
    });

    const refreshDeities = useCallback(async () => {
        await queryClient.invalidateQueries({ queryKey: ['deities'] });
    }, [queryClient]);

    const refreshTemples = useCallback(async () => {
        await queryClient.invalidateQueries({ queryKey: ['temples'] });
    }, [queryClient]);

    const loadTags = useCallback(async () => {
        await queryClient.invalidateQueries({ queryKey: ['tags'] });
    }, [queryClient]);

    // Aggregate loading state
    const loading = [
        composersQuery, ragasQuery, talasQuery, deitiesQuery,
        templesQuery, sampradayasQuery, tagsQuery
    ].some(q => q.isLoading);

    // Aggregate error state
    const errorQuery = [
        composersQuery, ragasQuery, talasQuery, deitiesQuery,
        templesQuery, sampradayasQuery, tagsQuery
    ].find(q => q.error);

    const error = errorQuery ? (errorQuery.error as Error).message || 'Failed to load reference data' : null;

    return useMemo(() => ({
        composers: composersQuery.data || [],
        ragas: ragasQuery.data || [],
        talas: talasQuery.data || [],
        deities: deitiesQuery.data || [],
        temples: templesQuery.data || [],
        sampradayas: sampradayasQuery.data || [],
        tags: tagsQuery.data || [],
        loading,
        error,
        refreshDeities,
        refreshTemples,
        loadTags
    }), [
        composersQuery.data, ragasQuery.data, talasQuery.data,
        deitiesQuery.data, templesQuery.data, sampradayasQuery.data, tagsQuery.data,
        loading, error, refreshDeities, refreshTemples, loadTags
    ]);
};
