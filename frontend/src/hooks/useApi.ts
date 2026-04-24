import { useAuth0 } from "@auth0/auth0-react";
import axios from "axios";
import { useMemo } from "react";

export const useApi = () => {
  const { getAccessTokenSilently } = useAuth0();

  const api = useMemo(() => {
    const instance = axios.create({
      baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
    });

    instance.interceptors.request.use(async (config) => {
      try {
        const token = await getAccessTokenSilently();
        config.headers.Authorization = `Bearer ${token}`;
      } catch (e) {
        console.error("Not authenticated", e);
      }
      return config;
    });

    return instance;
  }, [getAccessTokenSilently]);

  return api;
};
