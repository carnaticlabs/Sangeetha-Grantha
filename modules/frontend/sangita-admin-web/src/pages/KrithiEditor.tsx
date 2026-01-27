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
        loadKrithi,
        saveKrithi,
        loadSections,
        loadVariants,
        loading: dataLoading,
        saving
    } = useKrithiData(referenceData); // Pass refs to allow mapping inside the hook if needed

    // Load Krithi on mount (or when refs are ready)
    useEffect(() => {
        // Wait for refs to load first to ensure mapping is correct
        if (!referenceData.loading && !isNew && krithiId) {
            // Check if we already loaded it to avoid double fetch if refs re-render
            // Actually, we should just let the hook handle it or use a flag
            // However, the hook uses callback, so we can call it here.

            const fetchKrithi = async () => {
                dispatch({ type: 'SET_LOADING', payload: true });
                const detail = await loadKrithi(krithiId);
                if (detail) {
                    dispatch({ type: 'SET_KRITHI', payload: detail });
                }
                dispatch({ type: 'SET_LOADING', payload: false });
            };
            fetchKrithi();
        } else if (isNew) {
            // Initialize new krithi defaults if needed
            // The reducer already has defaults
        }
    }, [krithiId, isNew, referenceData.loading, loadKrithi]);

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

        if (tab === 'Tags') {
            referenceData.loadTags();
        }
    };

    const handleFieldChange = (field: keyof KrithiDetail, value: any) => {
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
                // Determine if we need to reload based on save result? 
                // Currently saveKrithi returns ID but mapping might need refresh.
                // Ideally, we re-fetch the krithi to get server-normalized data.
                const updated = await loadKrithi(savedId);
                if (updated) {
                    dispatch({ type: 'SET_KRITHI', payload: updated });
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

    if (referenceData.loading || (state.isLoading && !state.krithi.id && !isNew)) {
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
                        <NotationTab krithiId={state.krithi.id} />
                    )}
                </div>
            </div>
        </>
    );
};

export default KrithiEditor;
