import { createContext, useContext, useEffect } from 'react';

export interface ScreenHeader { title: string; subtitle?: string | null }

/** Lets a screen pane put its own title (the krithi, raga, collection…) in the phone's top bar, as mobile does. */
export const HeaderContext = createContext<(header: ScreenHeader | null) => void>(() => undefined);

export const useScreenHeader = (title: string | undefined, subtitle?: string | null): void => {
    const setHeader = useContext(HeaderContext);
    useEffect(() => {
        setHeader(title ? { title, subtitle } : null);
        return () => setHeader(null);
    }, [setHeader, title, subtitle]);
};
