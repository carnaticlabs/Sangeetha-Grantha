import { describe, it, expect } from 'vitest';
import golden from './fixtures/searchRules.golden.json';
import * as s from './searchLogic';
import type { MacroHit, SearchResponse } from '../../types/mobilePreview.types';
import type { SemanticSearchResultItem } from '../../types';

const item = (id: string): SemanticSearchResultItem => ({
    krithiId: id, title: id, composerName: 'c', ragaName: null, talaName: null, documentKind: 'COMPOSITION_OVERVIEW', matchedContent: '', similarityScore: 0.5,
});
const hit = (kind: MacroHit['kind'], over: Partial<MacroHit> = {}): MacroHit => ({
    kind, title: kind, snippet: '', recordedGap: false, similarityScore: 0.9,
    collectionSlug: kind === 'COLLECTION' ? 'slug' : null, templeId: kind === 'KSHETRA' ? 't1' : null, ...over,
});
const response = (hits: MacroHit[]): SearchResponse => ({ query: 'q', totalMatches: 0, macroHits: hits, items: [] });

describe('searchLogic against the golden fixture', () => {
    it('renders the exact-title kriti first and keeps server order for the rest', () => {
        const g = golden.exactTitle;
        const { exact, rest } = s.splitExactTitle(g.ids.map(item), g.exact);
        expect(exact?.krithiId).toBe(g.expectFirst);
        expect(rest.map((i) => i.krithiId)).toEqual(g.expectRest);
    });

    it('leaves order alone without an exact-title match, or when the id is not among the items', () => {
        const g = golden.noExactTitle;
        expect(s.splitExactTitle(g.ids.map(item), g.exact).rest.map((i) => i.krithiId)).toEqual(g.expectRest);
        expect(s.splitExactTitle(g.ids.map(item), 'zzz').exact).toBeNull();
    });

    it('drops KSHETRA cards only when the build has no kshetra screen', () => {
        const r = response([hit('COLLECTION'), hit('KSHETRA')]);
        expect(s.displayableMacroHits(r, true).map((h) => h.kind)).toEqual(golden.macroKinds.available);
        expect(s.displayableMacroHits(r, false).map((h) => h.kind)).toEqual(golden.macroKinds.kshetraDropped);
    });

    it('uses the same paging constants and mode copy as mobile', () => {
        expect({ pageSize: s.PAGE_SIZE, maxPages: s.MAX_PAGES, discoveryLimit: s.DISCOVERY_LIMIT }).toEqual(golden.paging);
        expect(Object.fromEntries(Object.entries(s.MODE_COPY).map(([k, v]) => [k, v.label]))).toEqual(golden.modeLabels);
    });
});

describe('macro destinations and paging', () => {
    it('opens collections by slug and kshetras by temple id, and nothing without a target', () => {
        expect(s.macroDestination(hit('COLLECTION'))).toEqual({ kind: 'CollectionDetail', slug: 'slug' });
        expect(s.macroDestination(hit('KSHETRA'))).toEqual({ kind: 'KshetraDetail', templeId: 't1' });
        expect(s.macroDestination(hit('COLLECTION', { collectionSlug: null }))).toBeNull();
    });

    it('stops lexical paging at the cap but not for an uncapped directory', () => {
        expect(s.hasMorePages(90, 500, 3, 3)).toBe(false);
        expect(s.hasMorePages(60, 500, 2, 3)).toBe(true);
        expect(s.hasMorePages(300, 1014, 10, null)).toBe(true);
        expect(s.hasMorePages(1014, 1014, 34, null)).toBe(false);
    });
});
