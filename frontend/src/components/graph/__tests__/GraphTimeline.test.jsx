import { describe, it, expect, vi } from 'vitest';
import { render, fireEvent } from '@testing-library/react';
import GraphTimeline from '../GraphTimeline';

describe('GraphTimeline', () => {
  it('renders nothing when not visible', () => {
    const { container } = render(
      <GraphTimeline snapshots={[{ label: 'v1' }]} visible={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when snapshots are empty', () => {
    const { container } = render(<GraphTimeline snapshots={[]} visible={true} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders timeline with snapshots', () => {
    const snapshots = [
      { label: 'Initial' },
      { label: 'Refactor' },
      { label: 'Final' },
    ];
    const { container } = render(
      <GraphTimeline snapshots={snapshots} currentIndex={1} visible={true} />
    );
    expect(container.firstChild).toBeTruthy();
    expect(container.textContent).toContain('2 / 3');
  });

  it('calls onSelect when a snapshot is clicked', () => {
    const onSelect = vi.fn();
    const snapshots = [{ label: 'A' }, { label: 'B' }];
    const { container } = render(
      <GraphTimeline snapshots={snapshots} currentIndex={0} onSelect={onSelect} visible={true} />
    );
    // Click the second snapshot dot
    const dots = container.querySelectorAll('[style*="cursor: pointer"]');
    expect(dots.length).toBe(2);
    fireEvent.click(dots[1]);
    expect(onSelect).toHaveBeenCalledWith(1);
  });
});
