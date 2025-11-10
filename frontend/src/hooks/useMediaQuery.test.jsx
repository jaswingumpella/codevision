import { describe, expect, it, vi } from 'vitest';
import { act, renderHook } from '@testing-library/react';
import useMediaQuery from './useMediaQuery';

describe('useMediaQuery', () => {
  it('subscribes to matchMedia and updates its value on change events', () => {
    const listeners = new Set();
    const mockMatchMedia = vi.fn().mockImplementation(() => ({
      matches: true,
      addEventListener: (_, cb) => listeners.add(cb),
      removeEventListener: (_, cb) => listeners.delete(cb)
    }));
    window.matchMedia = mockMatchMedia;

    const { result, unmount } = renderHook(() => useMediaQuery('(min-width: 768px)'));
    expect(mockMatchMedia).toHaveBeenCalledWith('(min-width: 768px)');
    expect(result.current).toBe(true);

    act(() => {
      listeners.forEach((listener) => listener({ matches: false }));
    });
    expect(result.current).toBe(false);
    unmount();
  });

  it('returns false when matchMedia is unavailable', () => {
    const original = window.matchMedia;
    window.matchMedia = undefined;
    const { result } = renderHook(() => useMediaQuery('(min-width: 1024px)'));
    expect(result.current).toBe(false);
    window.matchMedia = original;
  });
});
