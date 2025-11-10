import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import GlobalSearchBar from './GlobalSearchBar';

describe('GlobalSearchBar', () => {
  it('captures user input and exposes clear action', async () => {
    const user = userEvent.setup();
    const handleChange = vi.fn();
    const handleClear = vi.fn();
    render(<GlobalSearchBar value="log" onChange={handleChange} onClear={handleClear} resultCount={3} />);
    await user.type(screen.getByPlaceholderText(/Filter classes/i), 's');
    expect(handleChange).toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: /Clear search input/i }));
    expect(handleClear).toHaveBeenCalled();
    expect(screen.getByText(/3 matches/i)).toBeInTheDocument();
  });

  it('shows guidance when no search performed yet', () => {
    render(<GlobalSearchBar value="" onChange={() => {}} onClear={() => {}} resultCount={null} />);
    expect(screen.getByText(/Type to search across tabs/i)).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Clear search input/i })).not.toBeInTheDocument();
  });
});
