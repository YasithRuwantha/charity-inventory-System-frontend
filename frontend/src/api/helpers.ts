import type { AxiosResponse } from "axios";
import type { ApiSuccess, Paged, PaginationMeta } from "@/api/types";

export function unwrap<T>(response: AxiosResponse<ApiSuccess<T>>): T {
  return response.data.data;
}

export function unwrapPaged<T>(response: AxiosResponse<ApiSuccess<T[]>>): Paged<T> {
  const fallback: PaginationMeta = { page: 0, size: response.data.data.length, totalElements: response.data.data.length, totalPages: 1 };
  return {
    items: response.data.data,
    pagination: response.data.pagination ?? fallback,
  };
}

export function buildQuery(params: object): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params as Record<string, unknown>)) {
    if (typeof value !== "string" && typeof value !== "number" && typeof value !== "boolean") continue;
    if (value === undefined || value === null || value === "") continue;
    search.set(key, String(value));
  }
  const qs = search.toString();
  return qs ? `?${qs}` : "";
}
