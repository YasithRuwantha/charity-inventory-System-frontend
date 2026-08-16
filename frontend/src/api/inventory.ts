import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged, UserSummary } from "@/api/types";
import type { ExpiryFilter, InventoryStatus, TransactionType, UnitOfMeasure } from "@/types/enums";

export interface InventoryItem {
  id: number;
  itemCode: string;
  itemName: string;
  categoryId: number | null;
  categoryName: string | null;
  description: string | null;
  quantity: number;
  unit: UnitOfMeasure;
  minimumStockLevel: number;
  expiryDate: string | null;
  status: InventoryStatus;
  expired: boolean;
  daysUntilExpiry: number | null;
  archived: boolean;
  createdBy: UserSummary | null;
  createdAt: string;
  updatedAt: string;
}

export interface InventoryListParams {
  search?: string;
  categoryId?: number;
  status?: InventoryStatus;
  expiryStatus?: ExpiryFilter;
  includeArchived?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateInventoryPayload {
  itemName: string;
  categoryId: number;
  description?: string;
  quantity: number;
  unit: UnitOfMeasure;
  minimumStockLevel: number;
  expiryDate?: string | null;
}

export interface UpdateInventoryPayload {
  itemName: string;
  categoryId: number;
  description?: string;
  unit: UnitOfMeasure;
  minimumStockLevel: number;
  expiryDate?: string | null;
}

export interface AdjustStockPayload {
  adjustment: number;
  transactionType?: Extract<TransactionType, "MANUAL_ADJUSTMENT" | "CORRECTION">;
  notes?: string;
}

export async function listInventory(params: InventoryListParams = {}): Promise<Paged<InventoryItem>> {
  const res = await httpClient.get(`/api/inventory${buildQuery(params)}`);
  return unwrapPaged<InventoryItem>(res);
}

export async function listLowStock(params: { page?: number; size?: number } = {}): Promise<Paged<InventoryItem>> {
  const res = await httpClient.get(`/api/inventory/low-stock${buildQuery(params)}`);
  return unwrapPaged<InventoryItem>(res);
}

export async function listOutOfStock(params: { page?: number; size?: number } = {}): Promise<Paged<InventoryItem>> {
  const res = await httpClient.get(`/api/inventory/out-of-stock${buildQuery(params)}`);
  return unwrapPaged<InventoryItem>(res);
}

export async function listExpired(params: { page?: number; size?: number } = {}): Promise<Paged<InventoryItem>> {
  const res = await httpClient.get(`/api/inventory/expired${buildQuery(params)}`);
  return unwrapPaged<InventoryItem>(res);
}

export async function listExpiringSoon(days?: number): Promise<InventoryItem[]> {
  const res = await httpClient.get(`/api/inventory/expiring-soon${buildQuery({ days })}`);
  return unwrap(res);
}

export async function getInventoryItem(id: number): Promise<InventoryItem> {
  const res = await httpClient.get(`/api/inventory/${id}`);
  return unwrap(res);
}

export async function createInventoryItem(payload: CreateInventoryPayload): Promise<InventoryItem> {
  const res = await httpClient.post("/api/inventory", payload);
  return unwrap(res);
}

export async function updateInventoryItem(id: number, payload: UpdateInventoryPayload): Promise<InventoryItem> {
  const res = await httpClient.put(`/api/inventory/${id}`, payload);
  return unwrap(res);
}

export async function archiveInventoryItem(id: number): Promise<InventoryItem> {
  const res = await httpClient.delete(`/api/inventory/${id}`);
  return unwrap(res);
}

export async function restoreInventoryItem(id: number): Promise<InventoryItem> {
  const res = await httpClient.post(`/api/inventory/${id}/restore`);
  return unwrap(res);
}

export async function adjustStock(id: number, payload: AdjustStockPayload): Promise<InventoryItem> {
  const res = await httpClient.post(`/api/inventory/${id}/adjust-stock`, payload);
  return unwrap(res);
}

export interface InventoryTransaction {
  id: number;
  inventoryItemId: number;
  itemCode: string;
  itemName: string;
  transactionType: TransactionType;
  quantity: number;
  quantityBefore: number;
  quantityAfter: number;
  referenceType: string;
  referenceId: number | null;
  notes: string | null;
  performedBy: UserSummary | null;
  createdAt: string;
}

export async function listItemTransactions(
  id: number,
  params: { page?: number; size?: number; sort?: string } = {}
): Promise<Paged<InventoryTransaction>> {
  const res = await httpClient.get(`/api/inventory/${id}/transactions${buildQuery(params)}`);
  return unwrapPaged<InventoryTransaction>(res);
}
