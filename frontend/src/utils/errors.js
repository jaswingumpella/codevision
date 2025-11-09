const ERROR_SUGGESTIONS = {
  invalidUrl: [
    'Confirm the repository URL includes the full https:// or SSH path.',
    'If the repository is private, ensure the configured Git credentials have read access.'
  ],
  auth: [
    'Double-check the API key you entered matches the backend configuration.',
    'Verify the Git credentials stored on the server are still valid.'
  ],
  clone: [
    'Confirm the repository URL is reachable from the analysis environment.',
    'If credentials recently rotated, update the Git token or username and retry.'
  ],
  timeout: [
    'Retry the analysis during a quieter time or limit the repository size.',
    'Exclude large binary directories so the analyzer can finish faster.'
  ],
  oom: [
    'Limit the repository scope (modules or folders) to keep memory usage in check.',
    'Increase the JVM heap size or run the backend on a host with more RAM.'
  ],
  generic: [
    'Retry the analysis; if it persists, inspect the backend logs for more detail.',
    'Share the request ID or stack trace with the platform team for further help.'
  ]
};

const detectErrorCategory = (message, status) => {
  const normalized = (message || '').toLowerCase();
  if (status === 400 || normalized.includes('invalid') || normalized.includes('malformed')) {
    return 'invalidUrl';
  }
  if (status === 401 || status === 403 || normalized.includes('api key') || normalized.includes('unauthorized')) {
    return 'auth';
  }
  if (normalized.includes('clone') || normalized.includes('git fetch')) {
    return 'clone';
  }
  if (normalized.includes('timeout') || normalized.includes('timed out')) {
    return 'timeout';
  }
  if (normalized.includes('outofmemory') || normalized.includes('out of memory') || normalized.includes('heap')) {
    return 'oom';
  }
  return 'generic';
};

const buildFriendlyError = (error, repoUrl) => {
  const rawMessage =
    typeof error?.response?.data === 'string'
      ? error.response.data
      : typeof error?.message === 'string'
        ? error.message
        : 'The analysis failed for an unknown reason.';
  const category = detectErrorCategory(rawMessage, error?.response?.status);
  let message = 'The analysis could not be completed.';
  if (category === 'invalidUrl') {
    message = 'The repository URL looks invalid or unsupported.';
  } else if (category === 'auth') {
    message = 'Authentication failed. Check your API key and Git credentials.';
  } else if (category === 'clone') {
    message = 'Git clone failed before the analyzer could start.';
  } else if (category === 'timeout') {
    message = 'The analysis timed out before finishing.';
  } else if (category === 'oom') {
    message = 'The analyzer ran out of memory while processing this repository.';
  }
  return {
    message,
    raw: rawMessage,
    suggestions: ERROR_SUGGESTIONS[category] || ERROR_SUGGESTIONS.generic,
    repoUrl
  };
};

export { ERROR_SUGGESTIONS, buildFriendlyError };
