/**
 * A tiny external store that records when the editor last saved, so a finished search can say "edits saved since
 * this search" without re-running it (TRACK-158). Module-level on purpose: the editor and the preview are in
 * different component trees.
 */
let lastSavedAt = 0;
const listeners = new Set<() => void>();

export const markEdited = (now: number = Date.now()): void => {
    lastSavedAt = now;
    listeners.forEach((l) => l());
};

export const subscribeEdits = (listener: () => void): (() => void) => {
    listeners.add(listener);
    return () => { listeners.delete(listener); };
};

export const lastEditedAt = (): number => lastSavedAt;
