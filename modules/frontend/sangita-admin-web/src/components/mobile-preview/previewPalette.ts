import { createContext, useContext } from 'react';
import { LIGHT, type PreviewPalette } from './previewTokens';

export const PaletteContext = createContext<PreviewPalette>(LIGHT);
export const usePalette = (): PreviewPalette => useContext(PaletteContext);
