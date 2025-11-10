import axios from 'axios';

const resolveBaseUrl = (rawValue) => {
  if (typeof rawValue !== 'string') {
    return '';
  }
  const trimmed = rawValue.trim();
  if (trimmed === '') {
    return '';
  }
  return trimmed.replace(/\/+$/, '');
};

const envBaseUrl =
  typeof import.meta !== 'undefined' && import.meta.env && typeof import.meta.env.VITE_API_BASE_URL === 'string'
    ? import.meta.env.VITE_API_BASE_URL
    : '';

const apiClient = axios.create({
  baseURL: resolveBaseUrl(envBaseUrl)
});

export { resolveBaseUrl };
export default apiClient;
