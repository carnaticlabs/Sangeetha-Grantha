import { describe, it, expect } from 'vitest';
import { EDITOR_TABS, isEditorTab } from './krithi-editor.types';

describe('editor tabs', () => {
    it('includes the Mobile Preview tab and accepts it as a deep link', () => {
        expect(EDITOR_TABS).toContain('Mobile Preview');
        expect(isEditorTab('Mobile Preview')).toBe(true);
    });
});
