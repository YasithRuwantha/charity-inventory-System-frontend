import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged, UserSummary } from "@/api/types";
import type { DonationStatus, UnitOfMeasure } from "@/types/enums";

export interface DonationItem {
  id: number;
  inventoryItemId: number;
  itemCode: string;
  itemName: string;
  categoryName: string | null;
  quantity: number;
  unit: UnitOfMeasure;
  expiryDate: string | null;
  notes: string | null;
}

export interface Donation {
  id: number;
  donationReference: string;
  donorId: number;
  donorCode: string;
  donorName: string;
  donationDate: string;
  notes: string | null;
  receivedBy: UserSummary | null;
  status: DonationStatus;
  totalItems: number;
  totalQuantity: number;
  items: DonationItem[];
  createdAt: string;
  updatedAt: string;
}

export interface DonationListParams {
  search?: string;
  donorId?: number;
  status?: DonationStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DonationItemPayload {
  inventoryItemId: number;
  quantity: number;
  expiryDate?: string | null;
  notes?: string;
}

export interface CreateDonationPayload {
  donorId: number;
  donationDate: string;
  notes?: string;
  items: DonationItemPayload[];
}

export async function listDonations(params: DonationListParams = {}): Promise<Paged<Donation>> {
  const res = await httpClient.get(`/api/donations${buildQuery(params)}`);
  return unwrapPaged<Donation>(res);
}

export async function getDonation(id: number): Promise<Donation> {
  const res = await httpClient.get(`/api/donations/${id}`);
  return unwrap(res);
}

export async function getDonationByReference(reference: string): Promise<Donation> {
  const res = await httpClient.get(`/api/donations/reference/${encodeURIComponent(reference)}`);
  return unwrap(res);
}

export async function createDonation(payload: CreateDonationPayload): Promise<Donation> {
  const res = await httpClient.post("/api/donations", payload);
  return unwrap(res);
}

export interface DonationReceipt {
  organizationName: string;
  organizationAddress: string | null;
  organizationContact: string | null;
  donationReference: string;
  donationDate: string;
  donorCode: string;
  donorName: string;
  donorType: string;
  donorPhone: string | null;
  donorEmail: string | null;
  donorAddress: string | null;
  items: DonationItem[];
  totalItems: number;
  totalQuantity: number;
  receivedByName: string | null;
  notes: string | null;
  status: string;
  generatedAt: string;
}

export async function getDonationReceipt(id: number): Promise<DonationReceipt> {
  const res = await httpClient.get(`/api/donations/${id}/receipt`);
  return unwrap(res);
}

export interface BulkRowResult {
  rowNumber: number;
  valid: boolean;
  donor: string;
  item: string;
  category: string;
  quantity: string;
  unit: string;
  expiryDate: string;
  donationDate: string;
  errors: string[];
}

export interface BulkDonationPreview {
  fileName: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  donationsToCreate: number;
  rows: BulkRowResult[];
}

export interface BulkDonationImportResult {
  fileName: string;
  totalRows: number;
  successfulRows: number;
  failedRows: number;
  skippedRows: number;
  createdDonations: number;
  totalQuantityImported: number;
  createdDonationReferences: string[];
  errors: BulkRowResult[];
  message: string;
}

export async function previewBulkDonations(file: File): Promise<BulkDonationPreview> {
  const form = new FormData();
  form.append("file", file);
  const res = await httpClient.post("/api/donations/bulk/preview", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return unwrap(res);
}

export async function importBulkDonations(file: File, allowPartial: boolean): Promise<BulkDonationImportResult> {
  const form = new FormData();
  form.append("file", file);
  const res = await httpClient.post(`/api/donations/bulk/import${buildQuery({ allowPartial })}`, form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return unwrap(res);
}

export async function downloadBulkTemplate(): Promise<Blob> {
  const res = await httpClient.get("/api/donations/bulk/template", { responseType: "blob" });
  return res.data;
}
