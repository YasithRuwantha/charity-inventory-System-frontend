import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { PriorityLevel, RecordStatus } from "@/types/enums";
import type { Distribution } from "@/api/distributions";

export interface Beneficiary {
  id: number;
  beneficiaryCode: string;
  beneficiaryName: string;
  identificationNumber: string | null;
  familySize: number;
  contactNumber: string | null;
  address: string | null;
  priorityLevel: PriorityLevel;
  status: RecordStatus;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface BeneficiaryListParams {
  search?: string;
  priority?: PriorityLevel;
  status?: RecordStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface BeneficiaryPayload {
  beneficiaryName: string;
  identificationNumber?: string;
  familySize: number;
  contactNumber?: string;
  address?: string;
  priorityLevel: PriorityLevel;
  notes?: string;
  status?: RecordStatus;
}

export interface BeneficiaryStatistics {
  beneficiaryId: number;
  beneficiaryCode: string;
  beneficiaryName: string;
  priorityLevel: PriorityLevel;
  familySize: number;
  totalDistributions: number;
  totalItemsReceived: number;
  totalQuantityReceived: number;
  lastDistributionDate: string | null;
  pendingRequests: number;
  approvedRequests: number;
  recentDistributions: Distribution[];
}

export async function listBeneficiaries(params: BeneficiaryListParams = {}): Promise<Paged<Beneficiary>> {
  const res = await httpClient.get(`/api/beneficiaries${buildQuery(params)}`);
  return unwrapPaged<Beneficiary>(res);
}

export async function getBeneficiary(id: number): Promise<Beneficiary> {
  const res = await httpClient.get(`/api/beneficiaries/${id}`);
  return unwrap(res);
}

export async function createBeneficiary(payload: BeneficiaryPayload): Promise<Beneficiary> {
  const res = await httpClient.post("/api/beneficiaries", payload);
  return unwrap(res);
}

export async function updateBeneficiary(id: number, payload: Omit<BeneficiaryPayload, "status">): Promise<Beneficiary> {
  const res = await httpClient.put(`/api/beneficiaries/${id}`, payload);
  return unwrap(res);
}

export async function updateBeneficiaryStatus(id: number, status: RecordStatus): Promise<Beneficiary> {
  const res = await httpClient.patch(`/api/beneficiaries/${id}/status`, { status });
  return unwrap(res);
}

export async function getBeneficiaryDistributions(
  id: number,
  params: { status?: string; startDate?: string; endDate?: string; page?: number; size?: number } = {}
): Promise<Paged<Distribution>> {
  const res = await httpClient.get(`/api/beneficiaries/${id}/distributions${buildQuery(params)}`);
  return unwrapPaged<Distribution>(res);
}

export async function getBeneficiaryStatistics(id: number): Promise<BeneficiaryStatistics> {
  const res = await httpClient.get(`/api/beneficiaries/${id}/statistics`);
  return unwrap(res);
}
