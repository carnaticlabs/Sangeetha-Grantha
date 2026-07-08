import { describe, it, expect } from 'vitest';
import {
    formatWorkflowState,
    getWorkflowStateColor,
    formatLanguageCode,
    formatScriptCode,
    formatMusicalForm,
    formatSectionType,
    formatTagCategory,
} from './enums';

// Baseline unit coverage for the enum display formatters (TRACK-118).
// These pure mappers back workflow badges, language/script pickers and
// section labels across the admin UI; the fallback behaviour matters because
// the backend can emit both lower- and upper-case enum spellings.
describe('enum formatters', () => {
    it('maps workflow states in both casings to friendly labels', () => {
        expect(formatWorkflowState('in_review')).toBe('In Review');
        expect(formatWorkflowState('IN_REVIEW')).toBe('In Review');
        expect(formatWorkflowState('PUBLISHED')).toBe('Published');
    });

    it('falls back to the raw workflow state when unknown', () => {
        expect(formatWorkflowState('mystery')).toBe('mystery');
    });

    it('returns colour classes with a safe default for unknown states', () => {
        expect(getWorkflowStateColor('published')).toContain('green');
        expect(getWorkflowStateColor('nope')).toBe(
            'bg-slate-100 text-slate-600 border-slate-200',
        );
    });

    it('formats known language codes and upper-cases unknown ones', () => {
        expect(formatLanguageCode('ta')).toBe('Tamil');
        expect(formatLanguageCode('xx')).toBe('XX');
    });

    it('formats script codes with passthrough fallback', () => {
        expect(formatScriptCode('devanagari')).toBe('Devanagari');
        expect(formatScriptCode('braille')).toBe('braille');
    });

    it('formats musical forms and section/tag types', () => {
        expect(formatMusicalForm('KRITHI')).toBe('Krithi');
        expect(formatSectionType('SAMASHTI_CHARANAM')).toBe('Samashti Charanam');
        expect(formatTagCategory('KSHETRA')).toBe('Kshetra');
    });
});
