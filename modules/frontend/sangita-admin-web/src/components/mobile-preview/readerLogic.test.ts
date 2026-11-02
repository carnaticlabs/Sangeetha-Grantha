import { describe, it, expect } from 'vitest';
import golden from './fixtures/readerRules.golden.json';
import * as r from './readerLogic';
import type { RagaRef, VariantRef } from '../../types/mobilePreview.types';

describe('readerLogic against the golden fixture', () => {
    it.each(golden.tamil)('Tamil notation: $name', ({ in: input, out }) => {
        expect(r.formatTamilNotation(input)).toBe(out);
    });

    it('leaves the four-line legend as stored', () => {
        const legend = ['க,ச,ட,த,ப - 2', 'a 3', 'b 3', 'c 3', 'ப் 3 ர'].join('\n');
        expect(r.formatTamilNotation(legend)).toBe(['க,ச,ட,த,ப - 2', 'a 3', 'b 3', 'c 3', 'ப்³ர'].join('\n'));
    });

    it('only formats Tamil script', () => {
        expect(r.displayLyrics('ப் 3 ர', 'DEVANAGARI')).toBe('ப் 3 ர');
        expect(r.displayLyrics('ப் 3 ர', 'TAMIL')).toBe('ப்³ர');
    });

    it.each(golden.headings)('heading for $type', ({ type, label, out }) => {
        expect(r.sectionHeading(type, label)).toBe(out);
    });

    it.each(golden.captions)('caption for $type in $form', ({ type, form, out }) => {
        expect(r.sectionCaption(type, form as 'VARNAM' | 'KRITHI')).toBe(out);
    });

    it('marks swara sections preformatted', () => {
        golden.preformatted.yes.forEach((t) => expect(r.isPreformattedSwara(t)).toBe(true));
        golden.preformatted.no.forEach((t) => expect(r.isPreformattedSwara(t)).toBe(false));
    });

    it.each(golden.vibhakti)('vibhakti $in', ({ in: input, out }) => {
        expect(r.vibhaktiLabel(input)).toBe(out);
    });
});

describe('ragaNamesForSection (M1)', () => {
    const ragas: RagaRef[] = [
        { id: 'r2', name: 'Kalyani', orderIndex: 1, sectionId: 's1' },
        { id: 'r1', name: 'Todi', orderIndex: 0, sectionId: 's1' },
        { id: 'r3', name: 'Bhairavi', orderIndex: 2, sectionId: null },
    ];
    it('joins only ragas bound to the section, in order', () => {
        expect(r.ragaNamesForSection(ragas, 's1')).toBe('Todi · Kalyani');
    });
    it('returns null when nothing is bound', () => {
        expect(r.ragaNamesForSection(ragas, 's9')).toBeNull();
    });
});

describe('chooseVariant', () => {
    const v = (id: string, script: VariantRef['script'], isPrimary = false): VariantRef =>
        ({ id, language: 'SA', script, isPrimary });
    it('prefers the preferred script, then default, then primary, then lowest id', () => {
        const variants = [v('b', 'TAMIL'), v('a', 'TAMIL'), v('c', 'LATIN', true)];
        expect(r.chooseVariant({ variants, defaultVariantId: 'c' }, 'TAMIL')).toBe('a');
        expect(r.chooseVariant({ variants, defaultVariantId: 'b' }, null)).toBe('b');
        expect(r.chooseVariant({ variants, defaultVariantId: null }, null)).toBe('c');
        expect(r.chooseVariant({ variants: [v('z', 'LATIN'), v('y', 'LATIN')], defaultVariantId: null }, null)).toBe('y');
        expect(r.chooseVariant({ variants: [], defaultVariantId: null }, null)).toBeNull();
    });
});

describe('collection copy', () => {
    it('shows a position only when the source attests it', () => {
        expect(r.positionLabel({ orderBasis: 'SOURCE', position: 6, coreCount: 9 })).toBe('6 of 9');
        expect(r.positionLabel({ orderBasis: 'PERFORMANCE_CONVENTION', position: 6, coreCount: 9 })).toBeNull();
    });
    it('describes ordering honestly', () => {
        expect(r.orderNote('SOURCE')).toBeNull();
        expect(r.orderNote('PERFORMANCE_CONVENTION', 'Kamalamba')).toBe('Traditional performance order · Kamalamba');
        expect(r.orderNote('UNSPECIFIED')).toContain('No fixed sequence');
    });
});
