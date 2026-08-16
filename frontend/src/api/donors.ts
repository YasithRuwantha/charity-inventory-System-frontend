import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { DonorType, RecordStatus } from "@/types/enums";
import type { Donation } from "@/api/donations";

export interface Donor {
  id: number;
  donorCode: string;
  donorName: string;
  donorType: DonorType;
  phone: string | null;
  email: string | null;
  address: string | null;
  notes: string | null;
  status: RecordStatus;
  createdAt: string;
  updatedAt: string;
}

export interface DonorListParams {
  search?: string;
  donorType?: DonorType;
  status?: RecordStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DonorPayload {
  donorName: string;
  donorType: DonorType;
  phone?: string;
  email?: string;
  address?: string;
  notes?: string;
  status?: RecordStatus;
}

export interface DonorStatistics {
  donorId: number;
  donorCode: string;
  donorName: string;
  totalDonations: number;
  totalItemLines: number;
  totalQuantityDonated: number;
  lastDonationDate: string | null;
  recentDonations: Donation[];
}

export async function listDonors(params: DonorListParams = {}): Promise<Paged<Donor>> {
  const res = await httpClient.get(`/api/donors${buildQuery(params)}`);
  return unwrapPaged<Donor>(res);
}

export async function getDonor(id: number): Promise<Donor> {
  const res = await httpClient.get(`/api/donors/${id}`);
  return unwrap(res);
}

export async function createDonor(payload: DonorPayload): Promise<Donor> {
  const res = await httpClient.post("/api/donors", payload);
  return unwrap(res);
}

export async function updateDonor(id: number, payload: Omit<DonorPayload, "status">): Promise<Donor> {
  const res = await httpClient.put(`/api/donors/${id}`, payload);
  return unwrap(res);
}

export async function updateDonorStatus(id: number, status: RecordStatus): Promise<Donor> {
  const res = await httpClient.patch(`/api/donors/${id}/status`, { status });
  return unwrap(res);
}

export async function getDonorDonations(
  id: number,
  params: { status?: string; startDate?: string; endDate?: string; page?: number; size?: number } = {}
): Promise<Paged<Donation>> {
  const res = await httpClient.get(`/api/donors/${id}/donations${buildQuery(params)}`);
  return unwrapPaged<Donation>(res);
}

export async function getDonorStatistics(id: number): Promise<DonorStatistics> {
  const res = await httpClient.get(`/api/donors/${id}/statistics`);
  return unwrap(res);
}
