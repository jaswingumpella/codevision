import { describe, expect, it } from 'vitest';
import { render, screen } from '@testing-library/react';
import LoadingTimeline from './LoadingTimeline';

describe('LoadingTimeline', () => {
  it('renders nothing when no steps exist', () => {
    const { container } = render(<LoadingTimeline steps={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it('computes progress and surfaces step statuses', () => {
    const steps = [
      { id: 'clone', label: 'Clone repo', status: 'complete' },
      { id: 'scan', label: 'Scan source', status: 'active' },
      { id: 'pii', label: 'PII Scan', status: 'pending' }
    ];
    render(<LoadingTimeline steps={steps} />);
    expect(screen.getByText(/Analysis progress/i)).toBeInTheDocument();
    const progress = screen.getByRole('progressbar');
    expect(progress).toHaveAttribute('aria-valuenow', '33');
    expect(screen.getByText('Clone repo')).toBeInTheDocument();
    expect(screen.getByText('PII Scan')).toBeInTheDocument();
  });
});
