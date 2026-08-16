import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged, UserSummary } from "@/api/types";
import type { DistributionStatus, InventoryStatus, PriorityLevel, UnitOfMeasure } from "@/types/enums";

export interface DistributionItem {
  id: number;
  inventoryItemId: number;
  itemCode: string;
  itemName: string;
  categoryName: string | null;
  unit: UnitOfMeasure;
  requestedQuantity: number;
  allocatedQuantity: number | null;
  distributedQuantity: number | null;
  notes: string | null;
  availableQuantity: number;
  inventoryStatus: InventoryStatus;
  expiryDate: string | null;
  expired: boolean;
}

export interface Distribution {
  id: number;
  requestReference: string;
  beneficiaryId: number;
  beneficiaryCode: string;
  beneficiaryName: string;
  requestDate: string;
  priority: PriorityLevel;
  reason: string | null;
  notes: string | null;
  status: DistributionStatus;
  requestedBy: UserSummary | null;
  approvedBy: UserSummary | null;
  approvedAt: string | null;
  rejectedBy: UserSummary | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  cancelledBy: UserSummary | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  completedBy: UserSummary | null;
  completedAt: string | null;
  totalItems: number;
  totalRequestedQuantity: number;
  totalAllocatedQuantity: number;
  totalDistributedQuantity: number;
  items: DistributionItem[];
  createdAt: string;
  updatedAt: string;
}

export interface DistributionListParams {
  search?: string;
  status?: DistributionStatus;
  priority?: PriorityLevel;
  beneficiaryId?: number;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface DistributionItemPayload {
  inventoryItemId: number;
  requestedQuantity: number;
  notes?: string;
}

export interface CreateDistributionPayload {
  beneficiaryId: number;
  requestDate: string;
  priority?: PriorityLevel;
  reason?: string;
  notes?: string;
  items: DistributionItemPayload[];
}

export interface UpdateDistributionPayload {
  requestDate: string;
  priority: PriorityLevel;
  reason?: string;
  notes?: string;
  items: DistributionItemPayload[];
}

export interface AllocationLine {
  distributionItemId: number;
  allocatedQuantity: number;
}

export interface CompletionLine {
  distributionItemId: number;
  distributedQuantity: number;
}

export interface CompleteDistributionPayload {
  items?: CompletionLine[];
  overrideDuplicates?: boolean;
  overrideReason?: string;
  notes?: string;
}

export interface DuplicateDistribution {
  duplicateWarning: boolean;
  message: string;
  beneficiaryId: number;
  beneficiaryName: string;
  inventoryItemId: number;
  itemName: string;
  previousDistributionId: number;
  previousDistributionReference: string;
  lastDistributionDate: string;
  daysSinceLastDistribution: number;
  quantityPreviouslyReceived: number;
  duplicateWindowDays: number;
}

export async function listDistributions(params: DistributionListParams = {}): Promise<Paged<Distribution>> {
  const res = await httpClient.get(`/api/distributions${buildQuery(params)}`);
  return unwrapPaged<Distribution>(res);
}

export async function getDistribution(id: number): Promise<Distribution> {
  const res = await httpClient.get(`/api/distributions/${id}`);
  return unwrap(res);
}

export async function checkDuplicate(beneficiaryId: number, inventoryItemId: number): Promise<DuplicateDistribution> {
  const res = await httpClient.get(`/api/distributions/check-duplicate${buildQuery({ beneficiaryId, inventoryItemId })}`);
  return unwrap(res);
}

export async function createDistribution(payload: CreateDistributionPayload): Promise<Distribution> {
  const res = await httpClient.post("/api/distributions", payload);
  return unwrap(res);
}

export async function updateDistribution(id: number, payload: UpdateDistributionPayload): Promise<Distribution> {
  const res = await httpClient.put(`/api/distributions/${id}`, payload);
  return unwrap(res);
}

export async function allocateDistribution(id: number, items: AllocationLine[]): Promise<Distribution> {
  const res = await httpClient.post(`/api/distributions/${id}/allocate`, { items });
  return unwrap(res);
}

export async function approveDistribution(id: number, notes?: string): Promise<Distribution> {
  const res = await httpClient.post(`/api/distributions/${id}/approve`, notes ? { notes } : {});
  return unwrap(res);
}

export async function rejectDistribution(id: number, reason: string): Promise<Distribution> {
  const res = await httpClient.post(`/api/distributions/${id}/reject`, { reason });
  return unwrap(res);
}

export async function cancelDistribution(id: number, reason: string): Promise<Distribution> {
  const res = await httpClient.post(`/api/distributions/${id}/cancel`, { reason });
  return unwrap(res);
}

export async function completeDistribution(id: number, payload: CompleteDistributionPayload = {}): Promise<Distribution> {
  const res = await httpClient.post(`/api/distributions/${id}/complete`, payload);
  return unwrap(res);
}

export async function listBeneficiaryDistributions(
  beneficiaryId: number,
  params: { status?: string; startDate?: string; endDate?: string; page?: number; size?: number } = {}
): Promise<Paged<Distribution>> {
  const res = await httpClient.get(`/api/distributions/beneficiary/${beneficiaryId}${buildQuery(params)}`);
  return unwrapPaged<Distribution>(res);
}

export interface DistributionOverride {
  id: number;
  distributionRequestId: number;
  distributionReference: string;
  beneficiaryId: number;
  beneficiaryName: string;
  inventoryItemId: number;
  itemName: string;
  previousDistributionId: number;
  daysSinceLastDistribution: number;
  overrideReason: string;
  overriddenBy: UserSummary | null;
  createdAt: string;
}

export interface DistributionReport {
  organizationName: string;
  organizationAddress: string | null;
  organizationContact: string | null;
  distribution: Distribution;
  beneficiaryIdentificationNumber: string | null;
  beneficiaryFamilySize: number;
  beneficiaryContactNumber: string | null;
  beneficiaryAddress: string | null;
  overrides: DistributionOverride[];
  generatedAt: string;
}

export async function getDistributionReport(id: number): Promise<DistributionReport> {
  const res = await httpClient.get(`/api/distributions/${id}/report`);
  return unwrap(res);
}
