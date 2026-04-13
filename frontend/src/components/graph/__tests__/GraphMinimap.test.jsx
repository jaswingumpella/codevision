import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import GraphMinimap from '../GraphMinimap';

describe('GraphMinimap', () => {
  it('renders nothing when not visible', () => {
    const { container } = render(<GraphMinimap visible={false} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when graph is null', () => {
    const { container } = render(<GraphMinimap graph={null} visible={true} />);
    expect(container.firstChild).toBeNull();
  });

  it('renders nothing when sigma is null', () => {
    const mockGraph = {
      forEachNode: () => {},
    };
    const { container } = render(
      <GraphMinimap graph={mockGraph} sigma={null} visible={true} />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders container when visible with graph and sigma', () => {
    const mockGraph = {
      forEachNode: (fn) => {
        fn('n1', { x: 0, y: 0, color: '#999' });
        fn('n2', { x: 10, y: 10, color: '#999' });
      },
    };
    const mockCamera = {
      getState: () => ({ x: 0, y: 0, ratio: 1, angle: 0 }),
      on: () => {},
      removeListener: () => {},
    };
    const mockSigma = { getCamera: () => mockCamera };

    const { container } = render(
      <GraphMinimap graph={mockGraph} sigma={mockSigma} visible={true} />
    );
    expect(container.firstChild).toBeTruthy();
    expect(container.querySelector('canvas')).toBeTruthy();
  });
});
