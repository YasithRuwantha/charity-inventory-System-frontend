import axios, { AxiosError } from "axios";
import { useAuthStore } from "@/store/auth";
import { ApiRequestError, type ApiEnvelope } from "@/api/types";

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

export const httpClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { "Content-Type": "application/json" },
});

httpClient.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiEnvelope<unknown>>) => {
    const status = error.response?.status ?? 0;
    const body = error.response?.data;

    if (status === 401) {
      const wasAuthenticated = useAuthStore.getState().isAuthenticated;
      useAuthStore.getState().clearSession();
      if (wasAuthenticated && !window.location.pathname.startsWith("/login")) {
        window.location.assign("/login?expired=1");
      }
    }

    if (body && body.success === false) {
      return Promise.reject(
        new ApiRequestError(body.message, body.errorCode, status, body.errors, body.data)
      );
    }

    // /api/v1/auth/** endpoints return a different, non-enveloped shape: { error: "..." }
    const authErrorMessage = (body as unknown as { error?: string } | undefined)?.error;
    if (authErrorMessage) {
      return Promise.reject(new ApiRequestError(authErrorMessage, "UNAUTHORIZED", status));
    }

    return Promise.reject(
      new ApiRequestError(
        error.message || "Network error — the server could not be reached.",
        "NETWORK_ERROR",
        status
      )
    );
  }
);
