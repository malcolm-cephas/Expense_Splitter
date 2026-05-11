import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
});

let getTokenSilently: (() => Promise<string>) | null = null;

export const setTokenFetcher = (fetcher: () => Promise<string>) => {
  getTokenSilently = fetcher;
};

api.interceptors.request.use(async (config) => {
  if (getTokenSilently) {
    try {
      const token = await getTokenSilently();
      config.headers.Authorization = `Bearer ${token}`;
    } catch (error) {
      console.error('Error fetching Auth0 token:', error);
    }
  }
  return config;
});

export default api;
