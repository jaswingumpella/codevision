import { describe, expect, it, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchResultsPanel from './SearchResultsPanel';

describe('SearchResultsPanel', () => {
  it('returns null when the query is empty', () => {
    const { container } = render(<SearchResultsPanel query="" matches={[]} onNavigate={vi.fn()} />);
    expect(container.firstChild).toBeNull();
  });

  it('shows helpful message when no matches exist', () => {
    render(<SearchResultsPanel query="demo" matches={[]} onNavigate={vi.fn()} />);
    expect(screen.getByText(/No classes, endpoints, logs, or PII findings match "demo"/i)).toBeInTheDocument();
  });

  it('renders matches and navigates on click or keyboard interaction', async () => {
    const user = userEvent.setup();
    const handleNavigate = vi.fn();
    const matches = [
      {
        id: 'class-demo',
        type: 'class',
        title: 'DemoClass',
        subtitle: 'Controller',
        description: 'src/DemoClass.java',
        tabValue: 'overview'
      }
    ];
    render(<SearchResultsPanel query="demo" matches={matches} onNavigate={handleNavigate} />);

    await user.click(screen.getByRole('button', { name: /View Overview/i }));
    expect(handleNavigate).toHaveBeenCalledWith(matches[0]);

    const row = screen.getByRole('listitem', { name: /Class result/i });
    fireEvent.keyDown(row, { key: 'Enter', code: 'Enter', charCode: 13 });
    fireEvent.keyDown(row, { key: ' ', code: 'Space', charCode: 32 });
    expect(handleNavigate).toHaveBeenCalledTimes(3);
  });
});
