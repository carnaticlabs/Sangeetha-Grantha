/**
 * Design tokens ported from `RasikaTokens` and the dark scheme in
 * `modules/shared/presentation/.../theme/RasikaTheme.kt`. The phone frame is themed by these alone,
 * so the admin console's own light/dark mode never changes how the preview looks.
 */
import type { PreviewPreferences } from '../../types/mobilePreview.types';

export interface PreviewPalette {
    ground: string;
    card: string;
    creamDeep: string;
    ink: string;
    inkSoft: string;
    primary: string;
    primaryDeep: string;
    teal: string;
    hairline: string;
    onDark: string;
}

export const LIGHT: PreviewPalette = {
    ground: '#FDF3E3', card: '#FFFFFF', creamDeep: '#FBE8CF', ink: '#2A1A12', inkSoft: '#7A5C4A',
    primary: '#C1350F', primaryDeep: '#A92815', teal: '#1F6B70', hairline: 'rgba(169,40,21,0.20)', onDark: '#FFF6E8',
};

export const DARK: PreviewPalette = {
    ground: '#161210', card: '#231C18', creamDeep: '#3A2418', ink: '#F3EBE0', inkSoft: '#B6A598',
    primary: '#E08A4C', primaryDeep: '#E08A4C', teal: '#7FB3C3', hairline: 'rgba(224,138,76,0.20)', onDark: '#161210',
};

export const paletteFor = (appearance: PreviewPreferences['appearance']): PreviewPalette =>
    appearance === 'dark' ? DARK : LIGHT;

/** Root font size per text-size preference, in px (mobile body is 16sp at Medium). */
export const TEXT_SIZE_PX: Record<PreviewPreferences['textSize'], number> = { small: 14, medium: 16, large: 19 };

/** Screen geometry of the frame: iPhone-class logical points, matching the simulator checks in the Plan. */
export const FRAME = { width: 375, height: 812, radius: 40 } as const;
