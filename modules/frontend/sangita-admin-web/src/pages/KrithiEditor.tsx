import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useKrithiData } from '../hooks/useKrithiData';
import { useReferenceData } from '../hooks/useReferenceData';
import { useKrithiEditorReducer } from '../hooks/useKrithiEditorReducer';
import { useToast, ToastContainer } from '../components/Toast';
import {
    MetadataTab,
    StructureTab,
    LyricsTab,
    TagsTab,
    AuditTab
} from '../components/krithi-editor';
import NotationTab from '../components/notation/NotationTab'; // Existing component
import { formatLanguageCode, getWorkflowStateColor, formatWorkflowState } from '../utils/enums';
import { MusicalForm, KrithiDetail } from '../types';

const KrithiEditor: React.FC = () => {
    const { id: paramKrithiId } = useParams();
    const isNew = paramKrithiId === 'new';
    const krithiId = isNew ? undefined : paramKrithiId;

    const navigate = useNavigate();
    const toast = useToast();

    // 1. Reference Data State
    const referenceData = useReferenceData();

    // 2. Local Editor State (Reducer)
    const { state, dispatch } = useKrithiEditorReducer();

    // 3. Data Operations Hook
    const {
        useKrithiQuery,
        loadKrithi,
        saveKrithi,
        loadSections,
        loadVariants,
        loadKrithiTags,
        loading: dataLoading,
        saving
    } = useKrithiData(referenceData);

    // 4. Data Query
    const { data: serverKrithi, isLoading: isQueryLoading } = useKrithiQuery(
        krithiId,
        !isNew && !referenceData.loading // Only fetch when not new and refs are ready
    );

    // Sync Query Data to Reducer State and load sections/tags so they appear after navigate-back.
    // GET /admin/krithis/:id returns only core metadata; sections and tags are separate endpoints.
    useEffect(() => {
        if (!serverKrithi || !krithiId) {
            if (serverKrithi) dispatch({ type: 'SET_KRITHI', payload: serverKrithi });
            return;
        }
        let cancelled = false;
        (async () => {
            const [sections, tags] = await Promise.all([
                loadSections(krithiId),
                loadKrithiTags(krithiId),
            ]);
            if (cancelled) return;
            dispatch({
                type: 'SET_KRITHI',
                payload: {
                    ...serverKrithi,
                    sections: sections?.length ? sections : serverKrithi.sections ?? [],
                    tags: tags?.length ? tags : serverKrithi.tags ?? [],
                },
            });
            if (sections?.length) dispatch({ type: 'SET_SECTIONS_LOADED', payload: true });
        })();
        return () => { cancelled = true; };
    }, [serverKrithi, krithiId, loadSections, loadKrithiTags]);

    // Handle Tab Changes (Lazy Loading)
    const handleTabChange = async (tab: typeof state.activeTab) => {
        dispatch({ type: 'SET_ACTIVE_TAB', payload: tab });

        if (tab === 'Structure' && krithiId && !state.sectionsLoaded) {
            const sections = await loadSections(krithiId);
            if (sections) {
                dispatch({ type: 'UPDATE_FIELD', field: 'sections', value: sections });
                dispatch({ type: 'SET_SECTIONS_LOADED', payload: true });
            }
        }

        if (tab === 'Lyrics' && krithiId && !state.lyricVariantsLoaded) {
            const variants = await loadVariants(krithiId, referenceData.sampradayas);
            if (variants) {
                dispatch({ type: 'UPDATE_FIELD', field: 'lyricVariants', value: variants });
                dispatch({ type: 'SET_VARIANTS_LOADED', payload: true });
            }
        }

        if (tab === 'Tags' && krithiId) {
            referenceData.loadTags();
            const tags = await loadKrithiTags(krithiId);
            dispatch({ type: 'UPDATE_FIELD', field: 'tags', value: tags ?? [] });
        }
    };

    const handleFieldChange = <K extends keyof KrithiDetail>(field: K, value: KrithiDetail[K]) => {
        dispatch({ type: 'UPDATE_FIELD', field, value });
    };

    const onSave = async () => {
        dispatch({ type: 'SET_SAVING', payload: true });
        const savedId = await saveKrithi(state.krithi, isNew, krithiId);
        dispatch({ type: 'SET_SAVING', payload: false });

        if (savedId) {
            if (isNew) {
                navigate(`/krithis/${savedId}`, { replace: true });
            } else {
                // Reload base Krithi
                const updated = await loadKrithi(savedId);

                // Reload Tags (Assigned)
                const tags = await loadKrithiTags(savedId);

                // Reload Sections (if previously loaded or currently on Structure tab)
                let sections = undefined;
                if (state.sectionsLoaded || state.activeTab === 'Structure') {
                    sections = await loadSections(savedId);
                }

                if (updated) {
                    const merged = {
                        ...updated,
                        tags: tags || [],
                        sections: sections || updated.sections
                    };
                    dispatch({ type: 'SET_KRITHI', payload: merged });

                    if (sections) {
                        dispatch({ type: 'SET_SECTIONS_LOADED', payload: true });
                    }
                }
            }
        }
    };

    const renderWorkflowPill = (status: string = 'draft') => {
        return (
            <span className={`px-2.5 py-0.5 rounded-full text-xs font-bold border tracking-wide ${getWorkflowStateColor(status)}`}>
                {formatWorkflowState(status)}
            </span>
        );
    };

    if (referenceData.loading || (isQueryLoading && !isNew)) {
        return (
            <div className="p-12 text-center">
                <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary mb-4"></div>
                <p className="text-ink-500">Loading Editor...</p>
            </div>
        );
    }

    return (
        <>
            <ToastContainer toasts={toast.toasts} onRemove={toast.removeToast} />
            <div className="max-w-7xl mx-auto animate-fadeIn pb-12 relative">

                {/* Header */}
                <div className="mb-6">
                    <nav className="flex items-center gap-2 text-sm text-ink-500 mb-4">
                        <button onClick={() => navigate('/krithis')} className="hover:text-primary transition-colors">Kritis</button>
                        <span className="material-symbols-outlined text-[14px]">chevron_right</span>
                        <span className="text-ink-900 font-medium">{isNew ? 'New Composition' : 'Edit Composition'}</span>
                    </nav>

                    <div className="flex flex-col lg:flex-row lg:items-start justify-between gap-6">
                        <div>
                            <div className="flex items-center gap-3 mb-2 flex-wrap">
                                <h1 className="font-display text-3xl font-bold text-ink-900 tracking-tight">
                                    {state.krithi.title || 'Untitled Krithi'}
                                </h1>
                                {renderWorkflowPill(state.krithi.status)}
                            </div>
                            <p className="text-ink-500 font-medium text-sm flex items-center gap-2">
                                {state.krithi.composer?.name || 'Unknown Composer'} <span className="text-ink-300">•</span>
                                {state.krithi.ragas?.[0]?.name || 'Unknown Raga'} <span className="text-ink-300">•</span>
                                {state.krithi.tala?.name || 'Unknown Tala'} <span className="text-ink-300">•</span>
                                {formatLanguageCode(state.krithi.primaryLanguage || 'te')}
                            </p>
                        </div>

                        <div className="flex gap-2 flex-wrap">
                            <button
                                disabled={saving}
                                onClick={onSave}
                                className="px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors shadow-sm shadow-blue-500/20 disabled:opacity-50"
                            >
                                {saving ? 'Saving...' : 'Save Changes'}
                            </button>
                        </div>
                    </div>
                </div>

                {/* Tabs */}
                <div className="border-b border-border-light mb-8">
                    <nav className="flex gap-8 overflow-x-auto">
                        {(['Metadata', 'Structure', 'Lyrics', 'Notation', 'Tags', 'Audit'] as const).map((tab) => {
                            // Hide Notation for unsupported types
                            if (tab === 'Notation' &&
                                state.krithi.musicalForm !== MusicalForm.VARNAM &&
                                state.krithi.musicalForm !== MusicalForm.SWARAJATHI &&
                                state.krithi.musicalForm !== MusicalForm.KRITHI) {
                                return null;
                            }

                            return (
                                <button
                                    key={tab}
                                    onClick={() => handleTabChange(tab)}
                                    className={`pb-4 text-sm font-bold border-b-2 transition-colors whitespace-nowrap ${state.activeTab === tab
                                        ? 'border-primary text-primary'
                                        : 'border-transparent text-ink-500 hover:text-ink-900 hover:border-slate-300'
                                        }`}
                                >
                                    {tab}
                                </button>
                            );
                        })}
                    </nav>
                </div>

                {/* Content */}
                <div className="animate-fadeIn">
                    {state.activeTab === 'Metadata' && (
                        <MetadataTab
                            krithi={state.krithi}
                            onChange={handleFieldChange}
                            referenceData={referenceData}
                        />
                    )}
                    {state.activeTab === 'Structure' && (
                        <StructureTab
                            krithi={state.krithi}
                            onChange={handleFieldChange}
                            referenceData={referenceData}
                        />
                    )}
                    {state.activeTab === 'Lyrics' && (
                        <LyricsTab
                            krithi={state.krithi}
                            onChange={handleFieldChange}
                            referenceData={referenceData}
                        />
                    )}
                    {state.activeTab === 'Tags' && (
                        <TagsTab
                            krithi={state.krithi}
                            onChange={handleFieldChange}
                            referenceData={referenceData}
                        />
                    )}
                    {state.activeTab === 'Audit' && (
                        <AuditTab
                            krithi={state.krithi}
                            onChange={handleFieldChange}
                            referenceData={referenceData}
                        />
                    )}
                    {state.activeTab === 'Notation' && state.krithi.id && (
                        <NotationTab
                            krithiId={state.krithi.id}
                            musicalForm={state.krithi.musicalForm}
                        />
                    )}
                </div>
            </div>
        </>
    );
};

import { ErrorBoundary } from '../components/ErrorBoundary';

const KrithiEditorWithBoundary: React.FC = () => (
    <ErrorBoundary>
        <KrithiEditor />
    </ErrorBoundary>
);

export default KrithiEditorWithBoundary;
