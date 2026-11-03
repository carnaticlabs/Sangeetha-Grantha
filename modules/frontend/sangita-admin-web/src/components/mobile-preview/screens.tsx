import React from 'react';
import type {
    CollectionDetail, CollectionSummary, ComposerDetail, ComposerSummary, Discovery, Facet, FacetKey, KrithiSummary,
    RagaDetail, RagaSummary, TempleDetail, TempleSummary,
} from '../../types/mobilePreview.types';
import { Card, Chip, KrithiCard, SectionLabel } from './PreviewParts';
import { usePalette } from './previewPalette';
import * as rules from './readerLogic';

const count = (n: number): string => (n === 1 ? '1 in this library' : `${n} in this library`);
const kritiCount = (n: number): string => (n === 1 ? '1 kriti' : `${n} kritis`);

export const HomeView: React.FC<{
    discovery: Discovery;
    onOpenKrithi: (id: string) => void;
    onOpenRagas: () => void;
    onOpenComposers: () => void;
    onOpenCollections: () => void;
    onOpenKshetras: () => void;
}> = ({ discovery, onOpenKrithi, onOpenRagas, onOpenComposers, onOpenCollections, onOpenKshetras }) => {
    const p = usePalette();
    const f = discovery.feature;
    return (
        <div className="p-5">
            {f ? (
                <section aria-label="From the collection" className="rounded-xl p-4" style={{ background: p.card, border: `1px solid ${p.hairline}` }}>
                    <p className="text-xs font-semibold" style={{ color: p.primaryDeep }}>{f.heading.toUpperCase()}</p>
                    <h3 className="mt-1 font-display text-xl">{f.krithi.title}</h3>
                    <p className="text-xs" style={{ color: p.inkSoft }}>{f.krithi.composer.name}</p>
                    <p className="mt-2 text-sm">{f.summary}</p>
                    <button type="button" onClick={() => onOpenKrithi(f.krithi.id)} className="mt-3 rounded px-3 py-1 text-sm text-white" style={{ background: p.primary }}>
                        Read composition
                    </button>
                </section>
            ) : null /* The featured composition is a bonus: when it cannot load, show nothing in its place (as the app does). */}
            <p className="mt-6 text-xs font-semibold" style={{ color: p.primaryDeep }}>Explore the tradition</p>
            <div className="mt-2 flex flex-wrap gap-2">
                <Chip label="Ragas" onClick={onOpenRagas} />
                <Chip label="Composers" onClick={onOpenComposers} />
                <Chip label="Collections" onClick={onOpenCollections} />
                <Chip label="Kshetras" onClick={onOpenKshetras} />
            </div>
        </div>
    );
};

export const RagaDirectoryView: React.FC<{ ragas: RagaSummary[]; total: number; onOpen: (id: string) => void }> = ({ ragas, total, onOpen }) => {
    const p = usePalette();
    const melakartas = ragas.filter((r) => r.melakartaNumber != null && !r.parentRagaName?.trim())
        .sort((a, b) => (a.melakartaNumber ?? 0) - (b.melakartaNumber ?? 0) || a.name.localeCompare(b.name));
    const rest = ragas.filter((r) => !(r.melakartaNumber != null && !r.parentRagaName?.trim()));
    const janyas = rest.filter((r) => r.parentRagaName?.trim());
    const other = rest.filter((r) => !r.parentRagaName?.trim());
    const group = (title: string, items: RagaSummary[]) => items.length === 0 ? null : (
        <div key={title}>
            <SectionLabel>{title}</SectionLabel>
            <ul className="space-y-2 px-5">
                {items.map((r) => (
                    <li key={r.id}>
                        <Card onClick={() => onOpen(r.id)} label={r.name}>
                            <span className="block font-semibold">{r.name}</span>
                            <span className="block text-xs" style={{ color: p.inkSoft }}>
                                {count(r.publishedCompositionCount)}
                                {r.parentRagaName ? ` · Janya of ${r.parentRagaName}${r.parentMelakartaNumber != null ? ` (Mela ${r.parentMelakartaNumber})` : ''}` : r.melakartaNumber != null ? ` · Melakarta ${r.melakartaNumber}` : ''}
                            </span>
                        </Card>
                    </li>
                ))}
            </ul>
        </div>
    );
    return (
        <div className="pb-6">
            <p className="px-5 pt-3 text-xs" style={{ color: p.inkSoft }}>{total} ragas</p>
            {group('Melakartas', melakartas)}{group('Janya ragas', janyas)}{group('Other ragas', other)}
        </div>
    );
};

export const ComposerDirectoryView: React.FC<{ composers: ComposerSummary[]; onOpen: (id: string) => void }> = ({ composers, onOpen }) => {
    const p = usePalette();
    return (
        <ul className="space-y-2 p-5">
            {composers.map((c) => (
                <li key={c.id}>
                    <Card onClick={() => onOpen(c.id)} label={c.name}>
                        <span className="block font-semibold">{c.name}</span>
                        <span className="block text-xs" style={{ color: p.inkSoft }}>{count(c.publishedCompositionCount)}</span>
                    </Card>
                </li>
            ))}
        </ul>
    );
};

export const KrithiListView: React.FC<{ krithis: KrithiSummary[]; onOpen: (id: string) => void }> = ({ krithis, onOpen }) => (
    <ul className="space-y-2 p-5">
        {krithis.map((k) => <li key={k.id}><KrithiCard krithi={k} onOpen={() => onOpen(k.id)} /></li>)}
    </ul>
);

export const RagaDetailView: React.FC<{ raga: RagaDetail; krithis: KrithiSummary[]; onOpenKrithi: (id: string) => void }> = ({ raga, krithis, onOpenKrithi }) => {
    const p = usePalette();
    const relation = rules.ragaRelationshipCaption(raga.melakartaNumber, raga.parentRagaName, raga.parentMelakartaNumber);
    return (
        <div className="pb-6">
            <div className="px-5 py-3">
                <h3 className="font-display text-xl">{raga.name}</h3>
                {relation ? <p className="text-xs" style={{ color: p.teal }}>{relation}</p> : null}
                <p className="text-xs" style={{ color: p.inkSoft }}>{count(raga.publishedCompositionCount)}</p>
                {raga.aliases.length > 0 ? <p className="mt-1 text-xs">Also: {raga.aliases.join(', ')}</p> : null}
                {raga.arohanam ? <p className="mt-2 text-sm"><span className="font-semibold">Arohanam </span>{raga.arohanam}</p> : null}
                {raga.avarohanam ? <p className="text-sm"><span className="font-semibold">Avarohanam </span>{raga.avarohanam}</p> : null}
                {raga.nomenclatureLinks.map((l) => <p key={l.relatedRagaId} className="text-xs" style={{ color: p.inkSoft }}>{l.relationLabel}: {l.relatedRagaName}</p>)}
            </div>
            <SectionLabel>Works in this library</SectionLabel>
            <KrithiListView krithis={krithis} onOpen={onOpenKrithi} />
        </div>
    );
};

export const ComposerDetailView: React.FC<{ composer: ComposerDetail; krithis: KrithiSummary[]; onOpenKrithi: (id: string) => void }> = ({ composer, krithis, onOpenKrithi }) => {
    const p = usePalette();
    const years = [composer.birthYear, composer.deathYear].some((y) => y != null) ? `${composer.birthYear ?? '?'}–${composer.deathYear ?? '?'}` : null;
    return (
        <div className="pb-6">
            <div className="px-5 py-3">
                <h3 className="font-display text-xl">{composer.name}</h3>
                {years ? <p className="text-xs" style={{ color: p.inkSoft }}>{years}{composer.place ? ` · ${composer.place}` : ''}</p> : null}
                <p className="text-xs" style={{ color: p.inkSoft }}>{count(composer.publishedCompositionCount)}</p>
                {composer.aliases.length > 0 ? <p className="mt-1 text-xs">Also: {composer.aliases.join(', ')}</p> : null}
            </div>
            <SectionLabel>Works in this library</SectionLabel>
            <KrithiListView krithis={krithis} onOpen={onOpenKrithi} />
        </div>
    );
};

export const CollectionsView: React.FC<{ collections: CollectionSummary[]; onOpen: (slug: string) => void }> = ({ collections, onOpen }) => {
    const p = usePalette();
    return (
        <ul className="space-y-2 p-5">
            {collections.map((c) => (
                <li key={c.id}>
                    <Card onClick={() => onOpen(c.slug)} label={c.name}>
                        <span className="block font-semibold">{c.name}</span>
                        <span className="block text-xs" style={{ color: p.inkSoft }}>{[c.composer?.name, kritiCount(c.memberCount)].filter(Boolean).join(' · ')}</span>
                    </Card>
                </li>
            ))}
        </ul>
    );
};

export const CollectionDetailView: React.FC<{ collection: CollectionDetail; onOpenKrithi: (id: string) => void }> = ({ collection: c, onOpenKrithi }) => {
    const p = usePalette();
    const note = rules.orderNote(c.orderBasis, c.orderConvention);
    return (
        <div className="pb-6">
            <div className="px-5 py-3">
                <h3 className="font-display text-xl">{c.name}</h3>
                {c.composer ? <p className="text-xs" style={{ color: p.inkSoft }}>{c.composer.name}</p> : null}
                {c.overview ? <p className="mt-2 text-sm">{c.overview}</p> : null}
                {note ? <p className="mt-2 text-xs" style={{ color: p.inkSoft }}>{note}</p> : null}
                {!c.isComplete ? <p className="mt-1 text-xs" style={{ color: p.inkSoft }}>An open set. This list is not claimed to be complete.</p> : null}
            </div>
            <ul className="space-y-2 px-5">
                {[...c.members].sort((a, b) => a.sequenceOrder - b.sequenceOrder).map((m) => (
                    <li key={m.krithi.id}>
                        <KrithiCard krithi={m.krithi} onOpen={() => onOpenKrithi(m.krithi.id)} />
                        {[m.label, rules.roleLabel(m.role), m.axisValue].filter(Boolean).length > 0
                            ? <p className="px-1 pt-1 text-xs" style={{ color: p.inkSoft }}>{[m.label, rules.roleLabel(m.role), m.axisValue].filter(Boolean).join(' · ')}</p> : null}
                    </li>
                ))}
            </ul>
        </div>
    );
};

export const KshetrasView: React.FC<{ temples: TempleSummary[]; onOpen: (id: string) => void }> = ({ temples, onOpen }) => {
    const p = usePalette();
    const groups = new Map<string, TempleSummary[]>();
    temples.forEach((t) => {
        const key = t.mandalam ? rules.humanise(t.mandalam) : 'Other places';
        groups.set(key, [...(groups.get(key) ?? []), t]);
    });
    return (
        <div className="pb-6">
            <p className="px-5 pt-3 text-xs" style={{ color: p.inkSoft }}>Places recorded in the catalogue, grouped by region. Only places with published kritis or a reviewed note appear.</p>
            {[...groups.entries()].map(([region, items]) => (
                <div key={region}>
                    <SectionLabel>{region.toUpperCase()}</SectionLabel>
                    <ul className="space-y-2 px-5">
                        {items.map((t) => (
                            <li key={t.id}>
                                <Card onClick={() => onOpen(t.id)} label={t.name}>
                                    <span className="block font-semibold">{t.name}</span>
                                    <span className="block text-xs" style={{ color: p.inkSoft }}>
                                        {[t.city, t.recordedGap ? 'Recorded gap' : kritiCount(t.publishedKrithiCount)].filter(Boolean).join(' · ')}
                                    </span>
                                </Card>
                            </li>
                        ))}
                    </ul>
                </div>
            ))}
        </div>
    );
};

export const KshetraDetailView: React.FC<{ temple: TempleDetail; onOpenKrithi: (id: string) => void; onOpenTemple: (id: string) => void }> = ({ temple: t, onOpenKrithi, onOpenTemple }) => {
    const p = usePalette();
    const facts: [string, string | null | undefined][] = [
        ['REGION', t.mandalam ? rules.humanise(t.mandalam) : null],
        ['ELEMENT', t.element ? rules.humanise(t.element) : null],
        ['STHALA VRIKSHA', t.sthalaVriksha], ['TIRTHA', t.sthalaTirtha], ['NADI TIRTHA', t.nadiTirtha],
    ];
    return (
        <div className="pb-6">
            <div className="px-5 py-3">
                <h3 className="font-display text-xl">{t.name}</h3>
                <p className="text-xs" style={{ color: p.inkSoft }}>{[t.city, t.state].filter(Boolean).join(', ')}</p>
                {t.parent ? <p className="text-xs" style={{ color: p.inkSoft }}>PART OF {t.parent.name}</p> : null}
                {t.recordedGap ? <p className="mt-2 text-xs font-semibold" style={{ color: p.primaryDeep }}>Recorded gap</p> : null}
                {t.overview ? <><SectionLabel>ABOUT</SectionLabel><p className="text-sm">{t.overview}</p></> : null}
                <dl className="mt-2">
                    {facts.filter(([, v]) => v).map(([k, v]) => (
                        <div key={k} className="flex py-1" style={{ borderBottom: `1px solid ${p.hairline}` }}>
                            <dt className="w-32 text-xs" style={{ color: p.inkSoft }}>{k}</dt><dd className="font-semibold">{v}</dd>
                        </div>
                    ))}
                </dl>
            </div>
            {t.children.length > 0 ? <><SectionLabel>SHRINES HERE</SectionLabel><div className="space-y-2 px-5">
                {t.children.map((c) => <Card key={c.id} onClick={() => onOpenTemple(c.id)} label={c.name}><span className="font-semibold">{c.name}</span></Card>)}
            </div></> : null}
            {t.krithis.length > 0 ? <><SectionLabel>SUNG HERE</SectionLabel><KrithiListView krithis={t.krithis} onOpen={onOpenKrithi} /></> : null}
        </div>
    );
};

export const BrowseFacetsView: React.FC<{ facets: Facet[]; selected: FacetKey | null; onSelect: (key: FacetKey | null) => void }> = ({ facets, selected, onSelect }) => {
    const p = usePalette();
    return (
        <div className="p-5">
            <p className="text-xs font-semibold" style={{ color: p.primaryDeep }}>Filter by</p>
            <div className="mt-2 flex flex-wrap gap-2">
                {facets.map((f) => <Chip key={f.key} label={rules.facetName(f.key)} selected={selected === f.key} onClick={() => onSelect(selected === f.key ? null : f.key)} />)}
            </div>
            {facets.filter((f) => f.key === selected).map((f) => (
                <ul key={f.key} className="mt-3 space-y-1 text-sm">
                    {f.values.map((v) => <li key={v.value}>{v.count != null ? `${rules.facetLabel(f.key, v.value, v.label)} (${v.count})` : rules.facetLabel(f.key, v.value, v.label)}</li>)}
                </ul>
            ))}
            <p className="mt-4 text-xs" style={{ color: p.inkSoft }}>Filters cover only the compositions that have reviewed data for them.</p>
        </div>
    );
};
