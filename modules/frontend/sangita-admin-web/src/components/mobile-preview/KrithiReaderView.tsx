import React, { useState } from 'react';
import type {
    FacetChip, KrithiReader, Lyrics, PreviewPreferences, VariantRef,
} from '../../types/mobilePreview.types';
import { paletteFor } from './previewTokens';
import * as rules from './readerLogic';

export interface LyricsState {
    status: 'idle' | 'loading' | 'error';
    message?: string;
}

interface KrithiReaderViewProps {
    reader: KrithiReader;
    lyrics: Lyrics | undefined;
    lyricsState: LyricsState;
    selectedVariantId: string | null;
    preferences: PreviewPreferences;
    onSelectVariant: (variantId: string) => void;
    onRetryLyrics: () => void;
    onOpenCollection: (slug: string) => void;
    onOpenNeighbour: (krithiId: string) => void;
    onOpenFacet: (chip: FacetChip) => void;
}

type ReaderTab = 'Lyrics' | 'Details';

const Chip: React.FC<{ label: string; selected?: boolean; onClick?: () => void; primary: string; hairline: string }> = (
    { label, selected = false, onClick, primary, hairline },
) => (
    <button
        type="button"
        onClick={onClick}
        aria-pressed={selected}
        className="rounded-full px-3 py-1 text-xs font-semibold"
        style={{ border: `1px solid ${selected ? primary : hairline}`, background: selected ? primary : 'transparent', color: selected ? '#fff' : primary }}
    >
        {label}
    </button>
);

/** The mobile reader: header facts, Lyrics and Details panes. Prop-driven; data comes from the page hook. */
const KrithiReaderView: React.FC<KrithiReaderViewProps> = ({
    reader, lyrics, lyricsState, selectedVariantId, preferences, onSelectVariant, onRetryLyrics,
    onOpenCollection, onOpenNeighbour, onOpenFacet,
}) => {
    const p = paletteFor(preferences.appearance);
    const [tab, setTab] = useState<ReaderTab>('Lyrics');
    const selected: VariantRef | undefined = reader.variants.find((v) => v.id === selectedVariantId);
    const scripts = Array.from(new Set(reader.variants.map((v) => v.script)));
    const readings = selected ? reader.variants.filter((v) => v.script === selected.script) : reader.variants;
    const sections = [...(lyrics?.sections ?? [])].sort((a, b) => a.orderIndex - b.orderIndex);
    const ragas = [...reader.ragas].sort((a, b) => a.orderIndex - b.orderIndex);
    const form = rules.musicalFormLabel(reader.musicalForm);
    const chip = { primary: p.primary, hairline: p.hairline };

    return (
        <article className="pb-6">
            <div className="px-5 py-2 space-y-1" style={{ color: p.teal }}>
                <p className="font-semibold">{ragas.map((r) => r.name).join(' · ')}</p>
                {form ? <p className="text-xs">{form}</p> : null}
                {reader.temple ? <p className="text-xs" style={{ color: p.inkSoft }}>{rules.templeChipLabel(reader.temple.name)}</p> : null}
                {reader.cycleContext?.collections.map((c) => (
                    <div key={c.collectionId}>
                        <Chip label={`Part of ${rules.collectionChipLabel(c)}`} onClick={() => setTab('Details')} {...chip} />
                    </div>
                ))}
            </div>

            <div role="tablist" className="flex" style={{ background: p.creamDeep }}>
                {(['Lyrics', 'Details'] as const).map((t) => (
                    <button
                        key={t}
                        role="tab"
                        type="button"
                        aria-selected={tab === t}
                        onClick={() => setTab(t)}
                        className="flex-1 py-2 text-sm font-semibold"
                        style={{ color: tab === t ? p.primaryDeep : p.inkSoft, borderBottom: tab === t ? `2px solid ${p.primary}` : '2px solid transparent' }}
                    >
                        {t}
                    </button>
                ))}
            </div>

            {tab === 'Lyrics' ? (
                <div className="px-5 py-4">
                    {scripts.length > 1 ? (
                        <>
                            <p className="text-xs font-semibold" style={{ color: p.primary }}>Script</p>
                            <div className="mt-1 mb-3 flex flex-wrap gap-1">
                                {scripts.map((s) => (
                                    <Chip
                                        key={s}
                                        label={rules.scriptLabel(s)}
                                        selected={selected?.script === s}
                                        onClick={() => {
                                            const target = [...reader.variants].filter((v) => v.script === s).sort((a, b) => (a.id < b.id ? -1 : 1))[0];
                                            if (target) onSelectVariant(target.id);
                                        }}
                                        {...chip}
                                    />
                                ))}
                            </div>
                        </>
                    ) : null}
                    {readings.length > 1 || scripts.length === 1 ? (
                        <>
                            <p className="text-xs font-semibold" style={{ color: p.primary }}>Source reading</p>
                            <div className="mt-1 flex flex-wrap gap-1">
                                {readings.map((v) => (
                                    <Chip key={v.id} label={rules.sourceLabel(v)} selected={v.id === selectedVariantId} onClick={() => onSelectVariant(v.id)} {...chip} />
                                ))}
                            </div>
                        </>
                    ) : null}
                    {selected ? <p className="mt-3 text-xs" style={{ color: p.inkSoft }}>{rules.readingIdentity(selected)}</p> : null}

                    {lyricsState.status === 'loading' ? <p role="status" className="mt-3 text-sm" style={{ color: p.inkSoft }}>Loading the selected reading…</p> : null}
                    {lyricsState.status === 'error' ? (
                        <div role="alert" className="mt-3 text-sm" style={{ color: p.inkSoft }}>
                            <p>That reading could not be loaded.</p>
                            <button type="button" onClick={onRetryLyrics} className="mt-2 rounded px-3 py-1 text-white" style={{ background: p.primary }}>Retry</button>
                        </div>
                    ) : null}

                    {sections.map((s) => {
                        const caption = rules.sectionCaption(s.sectionType, reader.musicalForm);
                        const ragaLabel = rules.ragaNamesForSection(reader.ragas, s.sectionId);
                        const preformatted = rules.isPreformattedSwara(s.sectionType);
                        return (
                            <section key={s.sectionId} className="mt-6" aria-label={rules.sectionHeading(s.sectionType, s.label)}>
                                <h3 className="flex items-center gap-2 text-xs font-semibold" style={{ color: p.primaryDeep }}>
                                    {rules.sectionHeading(s.sectionType, s.label)}
                                    <span className="h-0.5 flex-1" style={{ background: p.hairline }} />
                                </h3>
                                {caption ? <p className="text-xs" style={{ color: p.inkSoft }}>{caption}</p> : null}
                                {ragaLabel ? <p className="text-xs" style={{ color: p.teal }}>{ragaLabel}</p> : null}
                                <p className={`mt-2 ${preformatted ? 'whitespace-pre overflow-x-auto' : 'whitespace-pre-wrap'}`}>
                                    {rules.displayLyrics(s.text, selected?.script)}
                                </p>
                            </section>
                        );
                    })}
                    {sections.length === 0 && lyrics?.unsegmentedText?.trim() ? (
                        <p className="mt-4 whitespace-pre-wrap">{rules.displayLyrics(lyrics.unsegmentedText, selected?.script)}</p>
                    ) : null}
                    {lyrics && lyrics.completeness !== 'UNKNOWN' ? (
                        <p className="mt-4 rounded p-2 text-xs" style={{ background: p.creamDeep, color: p.inkSoft }}>
                            {lyrics.completeness === 'COMPLETE'
                                ? 'This stored reading is complete for the selected source.'
                                : 'Some sections are not present in the stored source reading, so completeness is partial.'}
                        </p>
                    ) : null}
                </div>
            ) : (
                <dl className="px-5 py-2">
                    {reader.facetChips && reader.facetChips.length > 0 ? (
                        <div className="py-2 flex flex-wrap gap-1">
                            {reader.facetChips.map((c) => (
                                <Chip
                                    key={`${c.key}:${c.value}`}
                                    label={`${rules.facetName(c.key)}: ${rules.facetLabel(c.key, c.value, c.label)}`}
                                    onClick={() => onOpenFacet(c)}
                                    {...chip}
                                />
                            ))}
                        </div>
                    ) : null}
                    <Row k="RĀGA" v={ragas.map((r) => r.name).join(' · ')} sub={reader.isRagamalika ? 'Ragamalika' : undefined} p={p} />
                    {reader.tala ? <Row k="TĀLA" v={reader.tala.name} p={p} /> : null}
                    {form ? <Row k="FORM" v={form} p={p} /> : null}
                    <Row k="LANGUAGE" v={rules.languageLabel(reader.originalLanguage)} p={p} />
                    {selected ? <Row k="SOURCE" v={rules.sourceLabel(selected)} p={p} /> : null}
                    {reader.cycleContext?.vibhaktiCase ? <Row k="VIBHAKTI" v={rules.vibhaktiLabel(reader.cycleContext.vibhaktiCase)} p={p} /> : null}
                    {reader.cycleContext?.occasionNote?.trim() ? <Row k="OCCASION" v={reader.cycleContext.occasionNote} p={p} /> : null}
                    {reader.cycleContext?.collections.map((c) => {
                        const line = [rules.positionLabel(c), rules.roleLabel(c.role), c.axisValue].filter(Boolean).join(' · ');
                        const note = c.orderBasis === 'UNSPECIFIED' ? null : rules.orderNote(c.orderBasis, c.orderConvention);
                        return (
                            <div key={c.collectionId} className="py-3">
                                <p className="text-xs" style={{ color: p.inkSoft }}>PART OF</p>
                                <button type="button" onClick={() => onOpenCollection(c.slug)} className="mt-1 w-full rounded-lg p-3 text-left" style={{ background: p.card, border: `1px solid ${p.hairline}` }}>
                                    <span className="block font-semibold">{c.name}</span>
                                    {line ? <span className="block text-xs" style={{ color: p.inkSoft }}>{line}</span> : null}
                                    {note ? <span className="block text-xs" style={{ color: p.inkSoft }}>{note}</span> : null}
                                </button>
                                {c.previous ? <Neighbour label="Previous" title={c.previous.title} raga={c.previous.ragaName} onOpen={() => onOpenNeighbour(c.previous!.krithiId)} p={p} /> : null}
                                {c.next ? <Neighbour label="Next" title={c.next.title} raga={c.next.ragaName} onOpen={() => onOpenNeighbour(c.next!.krithiId)} p={p} /> : null}
                            </div>
                        );
                    })}
                    {reader.isRagamalika && ragas.length > 0 ? (
                        <div className="py-3">
                            <p className="text-xs font-semibold" style={{ color: p.primaryDeep }}>MEMBERSHIP</p>
                            <ol className="mt-1 flex flex-wrap gap-x-2" aria-label="Raga sequence">
                                {ragas.map((r) => <li key={`${r.id}-${r.orderIndex}`} style={{ color: p.teal }}>{r.name}</li>)}
                            </ol>
                        </div>
                    ) : null}
                    <p className="py-3 text-sm" style={{ color: p.inkSoft }}>
                        {lyrics ? (lyrics.completeness === 'UNKNOWN' ? 'Completeness not established' : null) : 'Completeness not established'}
                    </p>
                </dl>
            )}
        </article>
    );
};

const Row: React.FC<{ k: string; v: string; sub?: string; p: ReturnType<typeof paletteFor> }> = ({ k, v, sub, p }) => (
    <div className="flex py-2" style={{ borderBottom: `1px solid ${p.hairline}` }}>
        <dt className="w-24 pt-1 text-xs" style={{ color: p.inkSoft }}>{k}</dt>
        <dd className="flex-1"><span className="block font-semibold">{v}</span>{sub ? <span className="block text-xs" style={{ color: p.inkSoft }}>{sub}</span> : null}</dd>
    </div>
);

const Neighbour: React.FC<{ label: string; title: string; raga?: string | null; onOpen: () => void; p: ReturnType<typeof paletteFor> }> = ({ label, title, raga, onOpen, p }) => (
    <button type="button" onClick={onOpen} aria-label={`${label}: ${title}`} className="mt-2 w-full rounded-lg p-3 text-left" style={{ background: p.card, border: `1px solid ${p.hairline}` }}>
        <span className="block text-xs" style={{ color: p.inkSoft }}>{label.toUpperCase()}</span>
        <span className="block font-semibold">{title}</span>
        {raga ? <span className="block text-xs" style={{ color: p.inkSoft }}>{raga}</span> : null}
    </button>
);

export default KrithiReaderView;
