import { describe, expect, it } from 'vitest';
import { mapKrithiDtoToDetail } from './krithi-mapper';
import { MusicalForm } from '../types';
import { formatMusicalForm } from './enums';

const references = { composers: [], ragas: [], talas: [], deities: [], temples: [] };
describe('musical classification boundary', () => {
    it('does not invent a form for an omitted value', () => {
        expect(mapKrithiDtoToDetail({ title: 'Fixture' }, references).musicalForm)
            .toBe(MusicalForm.UNESTABLISHED);
        expect(formatMusicalForm(MusicalForm.UNESTABLISHED)).toBe('Not established');
    });
    it.each(Object.values(MusicalForm))('preserves an explicit %s', musicalForm => {
        expect(mapKrithiDtoToDetail({ title: 'Fixture', musicalForm }, references).musicalForm)
            .toBe(musicalForm);
    });
});
