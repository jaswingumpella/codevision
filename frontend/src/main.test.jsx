import { afterEach, describe, expect, it, vi } from 'vitest';

const renderMock = vi.fn();

vi.mock('react-dom/client', () => {
  const createRoot = vi.fn(() => ({
    render: renderMock
  }));
  return {
    __esModule: true,
    createRoot,
    default: {
      createRoot
    }
  };
});

describe('main entrypoint', () => {
  afterEach(() => {
    vi.resetModules();
    renderMock.mockReset();
  });

  it('mounts the React application', async () => {
    document.body.innerHTML = '<div id="root"></div>';
    const { createRoot, default: client } = await import('react-dom/client');
    await import('./main');
    expect(createRoot).toHaveBeenCalledWith(document.getElementById('root'));
    expect(renderMock).toHaveBeenCalled();
    expect(client.createRoot).toHaveBeenCalled();
  });
});
