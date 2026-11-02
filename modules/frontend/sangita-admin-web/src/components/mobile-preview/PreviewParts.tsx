import React from 'react';
import { usePalette } from './previewPalette';
import type { KrithiSummary } from '../../types/mobilePreview.types';
import { musicalFormLabel } from './readerLogic';

export interface LoadStateProps {
    isLoading: boolean;
    error: unknown;
    isEmpty: boolean;
    emptyTitle: string;
    emptyBody?: string;
    onRetry?: () => void;
    children: React.ReactNode;
}

/** Loading, error-with-retry and empty states shared by every preview screen (mirrors `LoadStateContent`). */
export const LoadStateView: React.FC<LoadStateProps> = ({ isLoading, error, isEmpty, emptyTitle, emptyBody, onRetry, children }) => {
    const p = usePalette();
    if (isLoading) return <p role="status" className="p-5 text-sm" style={{ color: p.inkSoft }}>Loading the catalogue…</p>;
    if (error) {
        return (
            <div role="alert" className="p-5 text-sm">
                <p className="font-semibold">Could not reach the catalogue</p>
                <p style={{ color: p.inkSoft }}>{(error as Error).message}</p>
                {onRetry ? <button type="button" onClick={onRetry} className="mt-2 rounded px-3 py-1 text-white" style={{ background: p.primary }}>Retry</button> : null}
            </div>
        );
    }
    if (isEmpty) {
        return (
            <div className="p-5 text-sm">
                <p className="font-semibold">{emptyTitle}</p>
                {emptyBody ? <p style={{ color: p.inkSoft }}>{emptyBody}</p> : null}
            </div>
        );
    }
    return <>{children}</>;
};

export const Chip: React.FC<{ label: string; selected?: boolean; onClick?: () => void }> = ({ label, selected = false, onClick }) => {
    const p = usePalette();
    return (
        <button
            type="button"
            onClick={onClick}
            aria-pressed={selected}
            className="rounded-full px-3 py-1 text-xs font-semibold"
            style={{ border: `1px solid ${selected ? p.primary : p.hairline}`, background: selected ? p.primary : 'transparent', color: selected ? '#fff' : p.primary }}
        >
            {label}
        </button>
    );
};

export const Card: React.FC<{ onClick?: () => void; label: string; children: React.ReactNode }> = ({ onClick, label, children }) => {
    const p = usePalette();
    return (
        <button
            type="button"
            onClick={onClick}
            aria-label={label}
            className="w-full rounded-xl p-3 text-left"
            style={{ background: p.card, border: `1px solid ${p.hairline}` }}
        >
            {children}
        </button>
    );
};

export const SectionLabel: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const p = usePalette();
    return <h3 className="px-5 pt-5 pb-1 text-xs font-semibold tracking-wide" style={{ color: p.primaryDeep }}>{children}</h3>;
};

/** A krithi row: title, composer, raga sequence, form. Mirrors `KrithiCard`. */
export const KrithiCard: React.FC<{ krithi: KrithiSummary; onOpen: () => void }> = ({ krithi, onOpen }) => {
    const p = usePalette();
    const ragas = [...krithi.ragas].sort((a, b) => a.orderIndex - b.orderIndex).map((r) => r.name);
    const form = musicalFormLabel(krithi.musicalForm);
    return (
        <Card onClick={onOpen} label={`${krithi.title}, ${krithi.composer.name}`}>
            <span className="block font-semibold">{krithi.title}</span>
            <span className="block text-xs" style={{ color: p.inkSoft }}>{krithi.composer.name}</span>
            {ragas.length > 0 ? <span className="block text-xs" style={{ color: p.teal }}>{ragas.join(' · ')}{krithi.isRagamalika ? ' · Ragamalika' : ''}</span> : null}
            {form ? <span className="block text-xs" style={{ color: p.inkSoft }}>{form}</span> : null}
        </Card>
    );
};
