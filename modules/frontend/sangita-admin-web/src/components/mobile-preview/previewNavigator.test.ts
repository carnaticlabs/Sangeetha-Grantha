import { describe, it, expect } from 'vitest';
import * as nav from './previewNavigator';

const reader = (n: number) => ({ kind: 'KrithiReader' as const, krithiId: `k${n}` });

describe('previewNavigator (port of RasikaNavigator)', () => {
    it('starts every tab at its root and Home selected', () => {
        const s = nav.initialNavigator();
        expect(s.selectedTab).toBe('Home');
        expect(nav.current(s)).toEqual({ kind: 'Home' });
        expect(nav.canGoBack(s)).toBe(false);
    });

    it('open pushes, ignores a repeat of the top destination, and back pops', () => {
        let s = nav.open(nav.initialNavigator(), reader(1));
        s = nav.open(s, reader(1));
        expect(s.stacks.Home).toHaveLength(2);
        expect(nav.current(nav.back(s))).toEqual({ kind: 'Home' });
    });

    it('caps the stack at 8 and flags it instead of pushing a 9th', () => {
        let s = nav.initialNavigator();
        for (let i = 1; i <= 7; i++) s = nav.open(s, reader(i));
        expect(s.stacks.Home).toHaveLength(nav.MAX_STACK_DEPTH);
        s = nav.open(s, reader(8));
        expect(s.stacks.Home).toHaveLength(nav.MAX_STACK_DEPTH);
        expect(s.stackLimitReached).toBe(true);
        expect(nav.acknowledgeStackLimit(s).stackLimitReached).toBe(false);
    });

    it('replace swaps the top without growing the stack, but pushes from a root', () => {
        const atRoot = nav.replace(nav.initialNavigator(), reader(1));
        expect(atRoot.stacks.Home).toHaveLength(2);
        const swapped = nav.replace(atRoot, reader(2));
        expect(swapped.stacks.Home).toHaveLength(2);
        expect(nav.current(swapped)).toEqual(reader(2));
    });

    it('keeps one stack per tab and returnToRoot clears the limit flag', () => {
        let s = nav.openOn(nav.initialNavigator(), 'Explore', reader(1));
        expect(s.selectedTab).toBe('Explore');
        expect(s.stacks.Home).toHaveLength(1);
        s = nav.returnToRoot(s);
        expect(nav.current(s)).toEqual({ kind: 'Explore' });
        expect(s.stackLimitReached).toBe(false);
    });
});
