import { afterEach, describe, expect, it, vi } from 'vitest';
import { resolveBaseUrl } from './apiClient';

describe('apiClient', () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.resetModules();
  });

  it('normalizes base URLs by trimming whitespace and trailing slashes', () => {
    expect(resolveBaseUrl(' https://api.example.com/ ')).toBe('https://api.example.com');
    expect(resolveBaseUrl('')).toBe('');
    expect(resolveBaseUrl(null)).toBe('');
  });

  it('creates an axios instance using the configured VITE_API_BASE_URL', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/base/');
    vi.resetModules();
    const module = await import('./apiClient');
    expect(module.default.defaults.baseURL).toBe('https://api.example.com/base');
  });

  it('falls back to relative requests when no env override is present', async () => {
    vi.stubEnv('VITE_API_BASE_URL', '');
    vi.resetModules();
    const module = await import('./apiClient');
    expect(module.default.defaults.baseURL).toBe('');
  });
});
