import { describe, expect, it } from 'vitest';
import { ERROR_SUGGESTIONS, buildFriendlyError } from './errors';

describe('buildFriendlyError', () => {
  const repoUrl = 'https://example.com/org/repo.git';

  const scenarios = [
    {
      label: 'invalidUrl',
      error: { response: { status: 400, data: 'Invalid repo URL' } },
      expectedMessage: 'The repository URL looks invalid or unsupported.'
    },
    {
      label: 'auth',
      error: { response: { status: 401, data: 'API key missing' } },
      expectedMessage: 'Authentication failed. Check your API key and Git credentials.'
    },
    {
      label: 'clone',
      error: { message: 'Git clone failed due to permissions' },
      expectedMessage: 'Git clone failed before the analyzer could start.'
    },
    {
      label: 'timeout',
      error: { message: 'Analysis timed out after 10m' },
      expectedMessage: 'The analysis timed out before finishing.'
    },
    {
      label: 'oom',
      error: { message: 'OutOfMemoryError occurred' },
      expectedMessage: 'The analyzer ran out of memory while processing this repository.'
    },
    {
      label: 'generic',
      error: {},
      expectedMessage: 'The analysis could not be completed.'
    }
  ];

  scenarios.forEach(({ label, error, expectedMessage }) => {
    it(`builds friendly copy for ${label} issues`, () => {
      const friendly = buildFriendlyError(error, repoUrl);
      expect(friendly.message).toBe(expectedMessage);
      expect(friendly.repoUrl).toBe(repoUrl);
      expect(friendly.suggestions).toBe(ERROR_SUGGESTIONS[label]);
    });
  });
});
