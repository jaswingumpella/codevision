const deriveProjectName = (repoUrl) => {
  if (!repoUrl) {
    return '';
  }
  let sanitized = repoUrl.trim();
  if (sanitized.endsWith('/')) {
    sanitized = sanitized.slice(0, -1);
  }
  const lastSlash = sanitized.lastIndexOf('/') + 1;
  let name = sanitized.slice(lastSlash);
  if (name.endsWith('.git')) {
    name = name.slice(0, -4);
  }
  return name;
};

const formatDate = (value) => {
  if (!value) {
    return '—';
  }
  try {
    return new Date(value).toLocaleString();
  } catch {
    return value;
  }
};

const textMatches = (query, ...fields) => {
  if (!query) {
    return true;
  }
  const normalized = query.trim().toLowerCase();
  if (!normalized) {
    return true;
  }
  return fields.some((field) => (field || '').toString().toLowerCase().includes(normalized));
};

export { deriveProjectName, formatDate, textMatches };
