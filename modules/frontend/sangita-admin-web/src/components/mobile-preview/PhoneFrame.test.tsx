import { describe, it, expect } from 'vitest';
import { render, screen } from '../../test/test-utils';
import PhoneFrame from './PhoneFrame';
import { TEXT_SIZE_PX } from './previewTokens';

describe('PhoneFrame text size', () => {
    it.each(['small', 'medium', 'large'] as const)('sets the text-size variable the scaled utilities read for %s', (size) => {
        render(
            <PhoneFrame preferences={{ appearance: 'light', textSize: size, preferredScript: null }} selectedTab="Home" onSelectTab={() => undefined} onBack={null} title="Home">
                <p className="text-sm">body</p>
            </PhoneFrame>,
        );
        const frame = screen.getByTestId('phone-frame');
        expect(frame).toHaveClass('preview-scale');
        expect(frame.style.getPropertyValue('--preview-px')).toBe(`${TEXT_SIZE_PX[size]}px`);
    });
});
