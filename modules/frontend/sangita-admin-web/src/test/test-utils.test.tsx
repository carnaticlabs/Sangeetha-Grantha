/**
 * Smoke test for the shared test harness itself: proves jest-dom matchers are
 * registered (setup.ts), the render wrapper provides router + query contexts,
 * and user-event interactions work under jsdom.
 */
import { describe, it, expect } from 'vitest';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { render, screen, waitFor } from './test-utils';

const RouterAndQueryConsumer = () => {
    const { data, isLoading } = useQuery({
        queryKey: ['smoke'],
        queryFn: () => Promise.resolve('query-data'),
    });
    return (
        <div>
            <Link to="/somewhere">a link</Link>
            {isLoading ? <span>loading</span> : <span>{data}</span>}
        </div>
    );
};

const Counter = () => {
    const [count, setCount] = useState(0);
    return <button onClick={() => setCount((c) => c + 1)}>count: {count}</button>;
};

describe('test-utils render harness', () => {
    it('provides router and query contexts and registers jest-dom matchers', async () => {
        render(<RouterAndQueryConsumer />);
        expect(screen.getByRole('link', { name: 'a link' })).toBeInTheDocument();
        await waitFor(() => expect(screen.getByText('query-data')).toBeInTheDocument());
    });

    it('supports user-event interactions', async () => {
        const { user } = render(<Counter />);
        await user.click(screen.getByRole('button'));
        expect(screen.getByRole('button')).toHaveTextContent('count: 1');
    });
});
