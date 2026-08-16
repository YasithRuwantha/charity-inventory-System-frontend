import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { RecordStatus } from "@/types/enums";

export interface Category {
  id: number;
  name: string;
  description: string | null;
  status: RecordStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CategoryListParams {
  search?: string;
  status?: RecordStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CategoryPayload {
  name: string;
  description?: string;
  status?: RecordStatus;
}

export async function listCategories(params: CategoryListParams = {}): Promise<Paged<Category>> {
  const res = await httpClient.get(`/api/categories${buildQuery(params)}`);
  return unwrapPaged<Category>(res);
}

export async function getCategory(id: number): Promise<Category> {
  const res = await httpClient.get(`/api/categories/${id}`);
  return unwrap(res);
}

export async function createCategory(payload: CategoryPayload): Promise<Category> {
  const res = await httpClient.post("/api/categories", payload);
  return unwrap(res);
}

export async function updateCategory(id: number, payload: CategoryPayload): Promise<Category> {
  const res = await httpClient.put(`/api/categories/${id}`, payload);
  return unwrap(res);
}

export async function updateCategoryStatus(id: number, status: RecordStatus): Promise<Category> {
  const res = await httpClient.patch(`/api/categories/${id}/status`, { status });
  return unwrap(res);
}
