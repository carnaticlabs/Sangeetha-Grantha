import { afterEach, describe, expect, it, vi } from 'vitest';
import { getKrithi, searchKrithis } from './client';

describe('admin catalogue read routes', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
        vi.restoreAllMocks();
    });

    it('searchKrithis uses authorized admin search', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            status: 200,
            json: async () => ({ items: [], total: 0, page: 0, pageSize: 25 }),
        });
        vi.stubGlobal('fetch', fetchMock);

        await searchKrithis({ query: 'Vatapi', page: 0, pageSize: 25 });

        expect(fetchMock).toHaveBeenCalledTimes(1);
        const url = String(fetchMock.mock.calls[0]?.[0]);
        expect(url).toMatch(/\/admin\/krithis\/search\?/);
        expect(url).not.toMatch(/\/v1\/krithis\/search/);
        expect(url).toContain('query=Vatapi');
    });

    it('getKrithi uses authorized admin detail', async () => {
        const fetchMock = vi.fn().mockResolvedValue({
            ok: true,
            status: 200,
            json: async () => ({ id: 'k-1', title: 'Vatapi Ganapatim' }),
        });
        vi.stubGlobal('fetch', fetchMock);

        await getKrithi('k-1');

        expect(fetchMock).toHaveBeenCalledTimes(1);
        expect(String(fetchMock.mock.calls[0]?.[0])).toMatch(/\/admin\/krithis\/k-1$/);
        expect(String(fetchMock.mock.calls[0]?.[0])).not.toMatch(/\/v1\/krithis\/k-1$/);
    });
});
