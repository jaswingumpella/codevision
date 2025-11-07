import axios from 'axios';

const envBaseUrl =
  typeof import.meta !== 'undefined' && import.meta.env && typeof import.meta.env.VITE_API_BASE_URL === 'string'
    ? import.meta.env.VITE_API_BASE_URL
    : '';

const trimmedBaseUrl = envBaseUrl.trim();
const normalizedBaseUrl = trimmedBaseUrl === '' ? '' : trimmedBaseUrl.replace(/\/+$/, '');

const apiClient = axios.create({
  baseURL: normalizedBaseUrl
});

export default apiClient;
