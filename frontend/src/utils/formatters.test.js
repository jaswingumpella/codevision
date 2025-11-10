import { describe, expect, it, vi } from 'vitest';
import { deriveProjectName, formatDate, textMatches } from './formatters';

describe('formatters', () => {
  it('derives project names independent of git suffixes', () => {
    expect(deriveProjectName('https://github.com/cv/demo.git')).toBe('demo');
    expect(deriveProjectName('https://github.com/cv/demo/')).toBe('demo');
    expect(deriveProjectName('')).toBe('');
  });

  it('formats dates and falls back to placeholder when parsing fails', () => {
    const spy = vi.spyOn(Date.prototype, 'toLocaleString').mockReturnValue('May 01, 2024');
    expect(formatDate('2024-05-01T00:00:00Z')).toBe('May 01, 2024');
    spy.mockRestore();
    expect(formatDate('')).toBe('—');
  });

  it('performs case-insensitive text matching across arbitrary fields', () => {
    expect(textMatches('demo', 'DemoClass', '')).toBe(true);
    expect(textMatches('demo', null, undefined)).toBe(false);
    expect(textMatches('', null)).toBe(true);
  });
});
