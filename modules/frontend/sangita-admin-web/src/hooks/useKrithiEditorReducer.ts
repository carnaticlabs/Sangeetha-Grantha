import { useReducer } from 'react';
import { KrithiEditorState, KrithiEditorAction } from '../types/krithi-editor.types';
import { MusicalForm } from '../types';

const initialState: KrithiEditorState = {
    krithi: {
        title: '',
        status: 'DRAFT',
        primaryLanguage: 'te',
        musicalForm: MusicalForm.KRITHI,
        ragas: [],
        sections: [],
        lyricVariants: [],
        tags: []
    },
    isLoading: false,
    isSaving: false,
    activeTab: 'Metadata',
    sectionsLoaded: false,
    lyricVariantsLoaded: false,
};

function krithiEditorReducer(
    state: KrithiEditorState,
    action: KrithiEditorAction
): KrithiEditorState {
    switch (action.type) {
        case 'SET_KRITHI':
            return { ...state, krithi: { ...state.krithi, ...action.payload } };
        case 'UPDATE_FIELD':
            return {
                ...state,
                krithi: { ...state.krithi, [action.field]: action.value }
            };
        case 'SET_LOADING':
            return { ...state, isLoading: action.payload };
        case 'SET_SAVING':
            return { ...state, isSaving: action.payload };
        case 'SET_ACTIVE_TAB':
            return { ...state, activeTab: action.payload };
        case 'SET_SECTIONS_LOADED':
            return { ...state, sectionsLoaded: action.payload };
        case 'SET_VARIANTS_LOADED':
            return { ...state, lyricVariantsLoaded: action.payload };
        case 'RESET':
            return initialState;
        default:
            return state;
    }
}

export const useKrithiEditorReducer = () => {
    const [state, dispatch] = useReducer(krithiEditorReducer, initialState);
    return { state, dispatch };
};
