import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import GraphDiffOverlay from '../GraphDiffOverlay';

describe('GraphDiffOverlay', () => {
  it('renders nothing when not visible', () => {
    const { container } = render(
      <GraphDiffOverlay diffData={{ added: ['a'] }} visible={false} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when diffData is null', () => {
    const { container } = render(<GraphDiffOverlay diffData={null} visible={true} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders change counts when diff data provided', () => {
    const diffData = {
      added: ['node1', 'node2'],
      removed: ['node3'],
      modified: ['node4', 'node5', 'node6'],
    };
    const { container } = render(
      <GraphDiffOverlay diffData={diffData} visible={true} />
    );
    expect(container.textContent).toContain('2 added');
    expect(container.textContent).toContain('1 removed');
    expect(container.textContent).toContain('3 modified');
  });

  it('renders no changes message when all counts are zero', () => {
    const diffData = { added: [], removed: [], modified: [] };
    const { container } = render(
      <GraphDiffOverlay diffData={diffData} visible={true} />
    );
    expect(container.textContent).toContain('No changes');
  });
});
