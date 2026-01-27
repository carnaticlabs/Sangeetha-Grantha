import {
    KrithiDetail,
    Composer,
    Raga,
    Tala,
    Deity,
    Temple,
    Tag,
    Sampradaya,
    MusicalForm
} from '../types';

export interface KrithiEditorState {
    krithi: Partial<KrithiDetail>;
    isLoading: boolean;
    isSaving: boolean;
    activeTab: 'Metadata' | 'Structure' | 'Lyrics' | 'Notation' | 'Tags' | 'Audit';
    sectionsLoaded: boolean;
    lyricVariantsLoaded: boolean;
}

export type KrithiEditorAction =
    | { type: 'SET_KRITHI'; payload: Partial<KrithiDetail> }
    | { type: 'UPDATE_FIELD'; field: keyof KrithiDetail; value: any }
    | { type: 'SET_LOADING'; payload: boolean }
    | { type: 'SET_SAVING'; payload: boolean }
    | { type: 'SET_ACTIVE_TAB'; payload: KrithiEditorState['activeTab'] }
    | { type: 'SET_SECTIONS_LOADED'; payload: boolean }
    | { type: 'SET_VARIANTS_LOADED'; payload: boolean }
    | { type: 'RESET' };

export interface ReferenceDataState {
    composers: Composer[];
    ragas: Raga[];
    talas: Tala[];
    deities: Deity[];
    temples: Temple[];
    tags: Tag[];
    sampradayas: Sampradaya[];
    loading: boolean;
    error: string | null;
    loadTags: () => Promise<void>;
}

export interface TabProps {
    krithi: Partial<KrithiDetail>;
    onChange: (field: keyof KrithiDetail, value: any) => void;
    referenceData: ReferenceDataState;
    readOnly?: boolean;
}
