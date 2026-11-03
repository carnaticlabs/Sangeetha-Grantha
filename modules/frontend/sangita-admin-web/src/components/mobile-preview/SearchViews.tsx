import React, { useState } from 'react';
import type { SemanticSearchResultItem } from '../../types';
import type {
    ExploreCategory, MacroHit, PreviewSearchExplain, RagaSummary, ComposerSummary, SearchMode, SearchResponse, PreviewScope,
} from '../../types/mobilePreview.types';
import { Card, Chip, SectionLabel } from './PreviewParts';
import { usePalette } from './previewPalette';
import * as s from './searchLogic';

export interface AppliedFilters { ragaId?: string; ragaLabel?: string; composerId?: string; composerLabel?: string }

interface ControlsProps {
    text: string;
    onText: (v: string) => void;
    onSubmit: () => void;
    mode: SearchMode;
    onMode: (m: SearchMode) => void;
    optionsOpen: boolean;
    onToggleOptions: () => void;
    category: ExploreCategory;
    onCategory: (c: ExploreCategory) => void;
    scope: PreviewScope;
    onScope: (v: PreviewScope) => void;
    applied: AppliedFilters;
    onOpenFilters: () => void;
    onRemoveRaga: () => void;
    onRemoveComposer: () => void;
}

/** The query box, category tabs, "Search options" disclosure, applied filter chips and the preview scope toggle. */
export const SearchControls: React.FC<ControlsProps> = (p) => {
    const pal = usePalette();
    const krithis = p.category === 'Krithis';
    return (
        <div className="px-5 pt-4">
            <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); p.onSubmit(); }}>
                <input
                    aria-label="Search"
                    value={p.text}
                    onChange={(e) => p.onText(e.target.value)}
                    placeholder="Title, incipit or a line of sahitya"
                    className="min-w-0 flex-1 rounded border px-2 py-1 text-sm"
                    style={{ borderColor: pal.hairline, background: pal.card, color: pal.ink }}
                />
                <button type="submit" className="rounded px-3 py-1 text-sm text-white" style={{ background: pal.primary }}>Search</button>
            </form>
            <div className="mt-3 flex gap-2">
                {(['Krithis', 'Ragas', 'Composers'] as const).map((c) => <Chip key={c} label={c} selected={p.category === c} onClick={() => p.onCategory(c)} />)}
            </div>
            {krithis ? (
                <div className="mt-3">
                    <p className="text-xs" style={{ color: pal.inkSoft }}>{s.MODE_COPY[p.mode].hint}</p>
                    <button type="button" onClick={p.onToggleOptions} aria-expanded={p.optionsOpen} className="mt-1 text-xs font-semibold underline" style={{ color: pal.primary }}>
                        {p.optionsOpen ? 'Hide options' : 'Search options'}
                    </button>
                    {p.optionsOpen ? (
                        <div className="mt-2 flex flex-wrap gap-2" role="group" aria-label="Search mode">
                            {(['Hybrid', 'Semantic', 'Lexical'] as const).map((m) => <Chip key={m} label={s.MODE_COPY[m].label} selected={p.mode === m} onClick={() => p.onMode(m)} />)}
                        </div>
                    ) : null}
                    {p.mode === 'Lexical' ? (
                        <div className="mt-2 flex flex-wrap items-center gap-2">
                            <button type="button" onClick={p.onOpenFilters} className="rounded border px-2 py-1 text-xs" style={{ borderColor: pal.hairline }}>+ Rāga or composer</button>
                            {p.applied.ragaId ? <Chip label={`Rāga: ${p.applied.ragaLabel ?? ''} ×`} onClick={p.onRemoveRaga} /> : null}
                            {p.applied.composerId ? <Chip label={`Composer: ${p.applied.composerLabel ?? ''} ×`} onClick={p.onRemoveComposer} /> : null}
                        </div>
                    ) : (p.applied.ragaId || p.applied.composerId) ? <p className="mt-2 text-xs" style={{ color: pal.inkSoft }}>{s.DISCOVERY_SAVED_FILTERS}</p> : null}
                </div>
            ) : null}
            <label className="mt-3 flex items-center gap-2 text-xs" style={{ color: pal.inkSoft }}>
                <input type="checkbox" checked={p.scope === 'INCLUDE_UNPUBLISHED'} onChange={(e) => p.onScope(e.target.checked ? 'INCLUDE_UNPUBLISHED' : 'PUBLISHED_ONLY')} />
                Include unpublished (curator only; mobile never shows these)
            </label>
        </div>
    );
};

/**
 * The rāga and composer picker (mirrors the mobile filter sheet after TRACK-159 D8): a search box and chips, a
 * "Load more" while more exist, and the chosen rāga kept visible even when the current search does not list it.
 */
export const FilterSheet: React.FC<{
    ragas: RagaSummary[]; ragaTotal: number; ragaText: string; ragaQuery: string;
    composers: ComposerSummary[]; composerTotal: number;
    draft: AppliedFilters;
    onRagaText: (v: string) => void; onRagaSearch: () => void;
    onLoadMoreRagas: () => void; onLoadMoreComposers: () => void;
    onDraftRaga: (id: string | null, label: string | null) => void;
    onDraftComposer: (id: string | null, label: string | null) => void;
    onApply: () => void; onCancel: () => void; onReset: () => void;
}> = (p) => {
    const pal = usePalette();
    const chosenMissing = p.draft.ragaId && p.draft.ragaLabel && !p.ragas.some((r) => r.id === p.draft.ragaId);
    return (
        <div role="dialog" aria-label="Filters" className="m-5 rounded-xl p-4" style={{ background: pal.card, border: `1px solid ${pal.hairline}` }}>
            <p className="text-sm font-semibold">Filters</p>
            <p className="text-xs" style={{ color: pal.inkSoft }}>Choose one rāga and one composer. Apply commits both. Cancel discards the draft.</p>

            <p className="mt-3 text-xs font-semibold" style={{ color: pal.primary }}>Ragas</p>
            <form className="mt-1 flex gap-2" onSubmit={(e) => { e.preventDefault(); p.onRagaSearch(); }}>
                <input
                    aria-label="Find a rāga"
                    value={p.ragaText}
                    onChange={(e) => p.onRagaText(e.target.value)}
                    placeholder="Find a rāga"
                    className="min-w-0 flex-1 rounded border px-2 py-1 text-sm"
                    style={{ borderColor: pal.hairline, background: pal.card, color: pal.ink }}
                />
                <button type="submit" className="rounded border px-2 py-1 text-xs" style={{ borderColor: pal.hairline }}>Find</button>
            </form>
            <div className="mt-2 flex flex-wrap gap-1">
                <Chip label="All ragas" selected={!p.draft.ragaId} onClick={() => p.onDraftRaga(null, null)} />
                {chosenMissing ? <Chip label={p.draft.ragaLabel as string} selected onClick={() => p.onDraftRaga(null, null)} /> : null}
                {p.ragas.map((r) => <Chip key={r.id} label={r.name} selected={p.draft.ragaId === r.id} onClick={() => p.onDraftRaga(r.id, r.name)} />)}
            </div>
            {p.ragaQuery && p.ragas.length === 0 ? (
                <p className="mt-1 text-xs" style={{ color: pal.inkSoft }}>No rāga matches that name.</p>
            ) : p.ragaTotal > 0 ? (
                <p className="mt-1 text-xs" style={{ color: pal.inkSoft }}>Showing {p.ragas.length} of {p.ragaTotal}</p>
            ) : null}
            {p.ragas.length < p.ragaTotal ? (
                <button type="button" onClick={p.onLoadMoreRagas} className="mt-1 text-xs font-semibold underline" style={{ color: pal.primary }}>Load more</button>
            ) : null}

            <p className="mt-4 text-xs font-semibold" style={{ color: pal.primary }}>Composers</p>
            <div className="mt-2 flex flex-wrap gap-1">
                <Chip label="All composers" selected={!p.draft.composerId} onClick={() => p.onDraftComposer(null, null)} />
                {p.composers.map((c) => <Chip key={c.id} label={c.name} selected={p.draft.composerId === c.id} onClick={() => p.onDraftComposer(c.id, c.name)} />)}
            </div>
            {p.composers.length < p.composerTotal ? (
                <button type="button" onClick={p.onLoadMoreComposers} className="mt-1 text-xs font-semibold underline" style={{ color: pal.primary }}>Load more composers</button>
            ) : null}

            <div className="mt-4 flex gap-2">
                <button type="button" onClick={p.onApply} className="rounded px-3 py-1 text-sm text-white" style={{ background: pal.primary }}>Apply filters</button>
                <button type="button" onClick={p.onCancel} className="rounded border px-3 py-1 text-sm" style={{ borderColor: pal.hairline }}>Cancel</button>
                <button type="button" onClick={p.onReset} className="rounded border px-3 py-1 text-sm" style={{ borderColor: pal.hairline }}>Reset</button>
            </div>
        </div>
    );
};

/** Why a kriti hit matched: the stored passage and the three scores the server returned. */
const WhyMatched: React.FC<{ item: SemanticSearchResultItem; mode: SearchMode }> = ({ item, mode }) => {
    const pal = usePalette();
    const [open, setOpen] = useState(false);
    return (
        <div className="mt-1">
            <button type="button" aria-expanded={open} onClick={() => setOpen(!open)} className="text-xs underline" style={{ color: pal.primary }}>
                {open ? 'Hide why' : 'Why this matched'}
            </button>
            {open ? (
                <dl className="mt-1 rounded p-2 text-xs" style={{ background: pal.creamDeep }}>
                    <div><dt className="inline font-semibold">Matched in </dt><dd className="inline">{item.documentKind === 'SECTION_PASSAGE' ? 'a lyric section' : item.documentKind === 'COMPOSITION_OVERVIEW' ? 'the composition overview' : item.documentKind}</dd></div>
                    <div><dt className="inline font-semibold">Meaning score </dt><dd className="inline">{s.fmtScore(item.similarityScore)}</dd>{mode === 'Hybrid' ? <> · <dt className="inline font-semibold">Text score </dt><dd className="inline">{s.fmtScore(item.lexicalScore)}</dd> · <dt className="inline font-semibold">Combined rank score </dt><dd className="inline">{s.fmtScore(item.rrfScore)}</dd></> : null}</div>
                    <blockquote className="mt-1 whitespace-pre-wrap opacity-90">{item.matchedContent.length > 240 ? `${item.matchedContent.slice(0, 239)}…` : item.matchedContent}</blockquote>
                </dl>
            ) : null}
        </div>
    );
};

const MacroCard: React.FC<{ hit: MacroHit; onOpen: () => void }> = ({ hit, onOpen }) => {
    const pal = usePalette();
    return (
        <Card onClick={onOpen} label={`${s.macroKindLabel(hit)}: ${hit.title}${hit.subtitle ? `, ${hit.subtitle}` : ''}${hit.recordedGap ? ', recorded gap' : ''}`}>
            <span className="block text-xs font-semibold" style={{ color: pal.primaryDeep }}>{s.macroKindLabel(hit).toUpperCase()}{hit.recordedGap ? ' · Recorded gap' : ''}</span>
            <span className="block font-semibold">{hit.title}</span>
            {hit.subtitle ? <span className="block text-xs" style={{ color: pal.inkSoft }}>{hit.subtitle}</span> : null}
            <span className="mt-1 block text-xs">{hit.snippet}</span>
        </Card>
    );
};

export const ExplainPanel: React.FC<{ explain: PreviewSearchExplain }> = ({ explain }) => {
    const pal = usePalette();
    const f = explain.floor;
    return (
        <section aria-label="Why cards appeared" className="m-5 rounded-xl p-3 text-xs" style={{ background: pal.creamDeep }}>
            <h4 className="font-semibold">Why collections and kshetras appear (curator view)</h4>
            <p className="mt-1">
                Profile: {explain.profileModel ? `${explain.profileModel} / ${explain.profileDimensions}` : 'none active (text only)'}
                {' · '}Mode: {explain.mode.toLowerCase()}
            </p>
            {!explain.floorCalibrated ? <p role="alert" className="mt-1">No relevance floor is calibrated for this profile, so no card can show.</p> : null}
            {f ? <p className="mt-1">A card needs {f.minSimilarity != null ? `similarity ≥ ${f.minSimilarity} and gap ≥ ${f.minGap}` : 'no vector route'}{f.minLexical != null ? `${f.minSimilarity != null ? ', or' : ''} lexical ≥ ${f.minLexical}` : ''}.</p> : null}
            {explain.macroCandidates.length === 0 ? <p className="mt-1">No collection or kshetra candidates were returned.</p> : (
                <table className="mt-2 w-full text-left">
                    <thead><tr><th>Candidate</th><th>Sim</th><th>Gap</th><th>Lex</th><th>Result</th></tr></thead>
                    <tbody>
                        {explain.macroCandidates.map((c) => (
                            <tr key={`${c.kind}:${c.title}`} className="align-top">
                                <td>{c.title}</td><td>{s.fmtScore(c.similarity)}</td><td>{s.fmtScore(c.gap)}</td><td>{s.fmtScore(c.lexicalScore)}</td>
                                <td>{c.cleared ? 'Shown' : `Hidden: ${c.reason ?? ''}`}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            )}
        </section>
    );
};

export const DiscoveryResults: React.FC<{
    response: SearchResponse;
    explain?: PreviewSearchExplain | null;
    mode: SearchMode;
    /** Result krithis that are not published, so mobile can never show them (from the server). */
    unpublished: ReadonlySet<string>;
    scope: PreviewScope;
    onOpenKrithi: (id: string) => void;
    onOpenMacro: (hit: MacroHit) => void;
}> = ({ response, explain, mode, unpublished, scope, onOpenKrithi, onOpenMacro }) => {
    const pal = usePalette();
    const macro = s.displayableMacroHits(response);
    const { exact, rest } = s.splitExactTitle(response.items, response.exactTitleMatchId);
    const row = (item: SemanticSearchResultItem) => (
        <li key={item.krithiId}>
            <Card onClick={() => onOpenKrithi(item.krithiId)} label={`${item.title}, ${item.composerName}`}>
                <span className="block font-semibold">{item.title}</span>
                <span className="block text-xs" style={{ color: pal.inkSoft }}>{item.composerName}</span>
                {item.ragaName ? <span className="block text-xs" style={{ color: pal.teal }}>{item.ragaName}</span> : null}
                {unpublished.has(item.krithiId) ? <span className="mt-1 inline-block rounded bg-amber-100 px-1 text-xs text-amber-900">Not visible on mobile</span> : null}
            </Card>
            <WhyMatched item={item} mode={mode} />
        </li>
    );
    return (
        <div>
            {scope === 'INCLUDE_UNPUBLISHED' ? (
                <p role="note" className="mx-5 mt-3 rounded p-2 text-xs" style={{ background: pal.creamDeep }}>
                    Including unpublished krithis. Their documents also take part in ranking, so the order can differ slightly from what mobile shows.
                </p>
            ) : null}
            {exact ? <ul className="space-y-2 px-5 pt-4">{row(exact)}</ul> : null}
            {macro.length > 0 ? (
                <>
                    <SectionLabel>Collections and kshetras</SectionLabel>
                    <ul className="space-y-2 px-5">{macro.map((h) => <li key={`${h.kind}:${h.title}`}><MacroCard hit={h} onOpen={() => onOpenMacro(h)} /></li>)}</ul>
                </>
            ) : null}
            {rest.length > 0 ? <><SectionLabel>Top matches</SectionLabel><ul className="space-y-2 px-5">{rest.map(row)}</ul></> : null}
            {explain ? <ExplainPanel explain={explain} /> : null}
        </div>
    );
};
