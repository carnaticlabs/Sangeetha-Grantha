/**
 * Reader presentation rules ported from the Rasika mobile app (TRACK-158). Each function names the Kotlin
 * source it mirrors; `fixtures/readerRules.golden.json` pins the behaviour and is meant to be read by a Kotlin
 * test as well (Plan step C1), so the two cannot drift silently. Pure functions, no React.
 */
import type {
    CollectionRef, FacetKey, KrithiReader, MusicalFormWire, OrderBasis, CollectionRole, RagaRef, ScriptCode, VariantRef,
} from '../../types/mobilePreview.types';

// -- reader/SectionLabels.kt -------------------------------------------------------------------------------

const SECTION_HEADINGS: Record<string, string> = {
    PALLAVI: 'Pallavi', ANUPALLAVI: 'Anupallavi', CHARANAM: 'Charanam', SAMASHTI_CHARANAM: 'Samashti Charanam',
    CHITTASWARAM: 'Chittaswaram', SWARA_SAHITYA: 'Swara Sahitya', MADHYAMA_KALA: 'Madhyamakala',
    SOLKATTU_SWARA: 'Solkattu Swaram', ANUBANDHA: 'Anubandha', MUKTAYI_SWARA: 'Muktayi Swaram',
    ETTUGADA_SWARA: 'Ettugada Swaram', ETTUGADA_SAHITYA: 'Ettugada Sahitya', VILOMA_CHITTASWARAM: 'Viloma Chittaswaram',
};

/** Stored labels win. Fallbacks never title-case the enum token (M4). */
export const sectionHeading = (sectionType: string, storedLabel?: string | null): string => {
    const trimmed = storedLabel?.trim() ?? '';
    return trimmed || SECTION_HEADINGS[sectionType] || sectionType;
};

export const VARNAM_CHARANAM_CAPTION = 'pallavi of the charanam';

export const sectionCaption = (sectionType: string, form: MusicalFormWire): string | null =>
    form === 'VARNAM' && sectionType === 'CHARANAM' ? VARNAM_CHARANAM_CAPTION : null;

const PREFORMATTED = new Set(['MUKTAYI_SWARA', 'SOLKATTU_SWARA', 'ETTUGADA_SWARA', 'CHITTASWARAM', 'VILOMA_CHITTASWARAM']);
export const isPreformattedSwara = (sectionType: string): boolean => PREFORMATTED.has(sectionType);

/** M1: only an explicit sectionId binding may label a lyric section. */
export const ragaNamesForSection = (ragas: RagaRef[], sectionId: string): string | null => {
    const bound = ragas.filter((r) => r.sectionId === sectionId).sort((a, b) => a.orderIndex - b.orderIndex);
    return bound.length === 0 ? null : bound.map((r) => r.name).join(' · ');
};

// -- reader/TamilNotation.kt -------------------------------------------------------------------------------

const TAMIL_MARKER = /(ரு|[கசடதபஸஜ][ா-்]?) ?([1-4])(?![0-9])( ?)/g;
const VIRAMA = '்';
const LEGEND_START = 'க,ச,ட,த,ப';
const LEGEND_LINES = 4;
const SUPERSCRIPTS = '¹²³⁴';

const accepts = (base: string, digit: string): boolean => {
    if (base === 'ரு') return digit === '2';
    if (base[0] === 'ஸ') return digit === '1';
    if (base[0] === 'ஜ') return digit === '2';
    return digit >= '2' && digit <= '4';
};

/** Shows the source's Tamil digit notation as attached superscripts; stored text is never rewritten (Domain Model §6.5). */
export const formatTamilNotation = (text: string): string => {
    if (!/[1-4]/.test(text)) return text;
    let legendLeft = 0;
    return text.split('\n').map((line) => {
        if (line.startsWith(LEGEND_START)) legendLeft = LEGEND_LINES;
        if (legendLeft > 0) {
            legendLeft -= 1;
            return line;
        }
        return line.replace(TAMIL_MARKER, (whole: string, base: string, digit: string, space: string) => {
            if (!accepts(base, digit)) return whole;
            const mark = SUPERSCRIPTS[Number(digit) - 1];
            return base.endsWith(VIRAMA) ? base + mark : base + mark + space;
        });
    }).join('\n');
};

export const displayLyrics = (text: string, script: ScriptCode | null | undefined): string =>
    script === 'TAMIL' ? formatTamilNotation(text) : text;

// -- RasikaCopy.kt / reader screen labels ------------------------------------------------------------------

export const scriptLabel = (script: ScriptCode): string =>
    script === 'LATIN' ? 'English' : script.charAt(0) + script.slice(1).toLowerCase();

export const sourceLabel = (v: VariantRef): string =>
    v.label?.trim() || v.sourceReference?.trim() || scriptLabel(v.script);

export const readingIdentity = (v: VariantRef): string =>
    scriptLabel(v.script) + (v.sourceReference?.trim() ? ` · ${v.sourceReference.trim()}` : '');

export const musicalFormLabel = (form: MusicalFormWire): string | null =>
    ({ KRITHI: 'Krithi', VARNAM: 'Varnam', SWARAJATHI: 'Swarajathi', UNESTABLISHED: null } as const)[form];

export const languageLabel = (code: string): string => code.charAt(0).toUpperCase() + code.slice(1).toLowerCase();

const FACET_NAMES: Record<FacetKey, string> = {
    vibhakti: 'Vibhakti', collection: 'Cycle', tune: 'Tune', mandalam: 'Region', element: 'Element',
};
export const facetName = (key: FacetKey): string => FACET_NAMES[key];

export const humanise = (raw: string): string => {
    const s = raw.toLowerCase().replace(/_/g, ' ');
    return s.charAt(0).toUpperCase() + s.slice(1);
};

const VIBHAKTI: Record<string, string> = {
    PRATHAMA: 'Prathama · nominative', DVITIYA: 'Dvitiya · accusative', TRITIYA: 'Tritiya · instrumental',
    CHATURTHI: 'Chaturthi · dative', PANCHAMI: 'Panchami · ablative', SHASHTHI: 'Shashthi · genitive',
    SAPTAMI: 'Saptami · locative', SAMBODHANA: 'Sambodhana · vocative', SARVA_VIBHAKTI: 'All cases',
};
export const vibhaktiLabel = (raw: string): string => VIBHAKTI[raw] ?? humanise(raw);

export const facetLabel = (key: FacetKey, value: string, label?: string | null): string =>
    label?.trim() ? label : key === 'vibhakti' ? vibhaktiLabel(value) : humanise(value);

export const templeChipLabel = (name: string): string => `Sung at ${name}`;

// -- explore/ExploreFilters.kt ragaRelationshipCaption (M5: never treat a janya's inherited number as its own)

export const ragaRelationshipCaption = (
    own: number | null | undefined,
    parentName: string | null | undefined,
    parentMela: number | null | undefined,
): string | null => {
    const parent = parentName?.trim() ?? '';
    if (parent) return parentMela != null ? `Janya of ${parent} (Mela ${parentMela})` : `Janya of ${parent}`;
    return own != null ? `Melakarta ${own}` : null;
};

// -- collections/CollectionCopy.kt -------------------------------------------------------------------------

export const positionLabel = (ref: Pick<CollectionRef, 'orderBasis' | 'position' | 'coreCount'>): string | null =>
    ref.orderBasis === 'SOURCE' && ref.position != null ? `${ref.position} of ${ref.coreCount}` : null;

export const collectionChipLabel = (ref: CollectionRef): string =>
    [ref.name, positionLabel(ref)].filter(Boolean).join(' · ');

export const roleLabel = (role: CollectionRole): string | null => ({
    CORE: null, DHYANA: 'Dhyana · invocation', MANGALAM: 'Mangalam · closing', OPTIONAL: 'Optional',
    DISPUTED_CONJECTURE: 'Attribution disputed',
} as const)[role];

export const orderNote = (basis: OrderBasis, convention?: string | null): string | null => {
    if (basis === 'SOURCE') return null;
    if (basis === 'PERFORMANCE_CONVENTION') {
        return 'Traditional performance order' + (convention?.trim() ? ` · ${convention}` : '');
    }
    return 'Shown for reading. No fixed sequence is claimed.';
};

// -- reader/KrithiReaderPresenter.kt chooseVariant ---------------------------------------------------------

const byId = (a: VariantRef, b: VariantRef): number => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0);

/** The reading shown first: preferred script, then the server default, then primary, then lowest id. */
export const chooseVariant = (reader: Pick<KrithiReader, 'variants' | 'defaultVariantId'>, preferred: ScriptCode | null): string | null => {
    const matching = reader.variants.filter((v) => preferred !== null && v.script === preferred);
    const primaries = matching.filter((v) => v.isPrimary);
    const fromPreference =
        matching.length === 1 ? matching[0]
            : primaries.length === 1 ? primaries[0]
                : [...matching].sort(byId)[0];
    return fromPreference?.id
        ?? reader.defaultVariantId
        ?? reader.variants.find((v) => v.isPrimary)?.id
        ?? [...reader.variants].sort(byId)[0]?.id
        ?? null;
};
