import { httpClient } from "@/api/client";
import { buildQuery, unwrap } from "@/api/helpers";
import type { PaginationMeta } from "@/api/types";
import type { InventoryItem, InventoryTransaction } from "@/api/inventory";
import type { ReferenceType, TransactionType } from "@/types/enums";

export interface ReportBreakdownEntry {
  id?: number;
  code?: string;
  label: string;
  count: number;
  quantity: number;
}

export interface TrendPoint {
  period: string;
  year: number;
  month: number;
  count: number;
  quantity: number;
}

export interface InventoryReport {
  totalItems: number;
  totalQuantity: number;
  lowStockItems: number;
  outOfStockItems: number;
  expiredItems: number;
  expiringSoonItems: number;
  expiringSoonHorizonDays: number;
  categoryBreakdown: ReportBreakdownEntry[];
  statusBreakdown: ReportBreakdownEntry[];
  generatedAt: string;
}

export interface DonationReport {
  startDate: string | null;
  endDate: string | null;
  totalDonations: number;
  totalItemLines: number;
  totalQuantityDonated: number;
  donorContributions: ReportBreakdownEntry[];
  monthlyTrend: TrendPoint[];
  generatedAt: string;
}

export interface DistributionReportSummary {
  startDate: string | null;
  endDate: string | null;
  completedDistributions: number;
  totalItemLines: number;
  totalQuantityDistributed: number;
  statusBreakdown: ReportBreakdownEntry[];
  priorityBreakdown: ReportBreakdownEntry[];
  beneficiaryBreakdown: ReportBreakdownEntry[];
  itemBreakdown: ReportBreakdownEntry[];
  monthlyTrend: TrendPoint[];
  generatedAt: string;
}

export interface BeneficiaryReport {
  startDate: string | null;
  endDate: string | null;
  totalBeneficiaries: number;
  activeBeneficiaries: number;
  inactiveBeneficiaries: number;
  statusBreakdown: ReportBreakdownEntry[];
  priorityBreakdown: ReportBreakdownEntry[];
  distributionBreakdown: ReportBreakdownEntry[];
  generatedAt: string;
}

export interface VolunteerReport {
  startDate: string | null;
  endDate: string | null;
  totalVolunteers: number;
  activeVolunteers: number;
  inactiveVolunteers: number;
  totalTasks: number;
  pendingTasks: number;
  inProgressTasks: number;
  completedTasks: number;
  cancelledTasks: number;
  overdueTasks: number;
  taskStatusBreakdown: ReportBreakdownEntry[];
  volunteerBreakdown: ReportBreakdownEntry[];
  generatedAt: string;
}

export interface ExpiryReport {
  expiredCount: number;
  expiringWithin7DaysCount: number;
  expiringWithin30DaysCount: number;
  expired: InventoryItem[];
  expiringWithin7Days: InventoryItem[];
  expiringWithin30Days: InventoryItem[];
  generatedAt: string;
}

export interface StockMovementReport {
  startDate: string | null;
  endDate: string | null;
  totalMovements: number;
  totalQuantityIn: number;
  totalQuantityOut: number;
  typeBreakdown: ReportBreakdownEntry[];
  movements: InventoryTransaction[];
  pagination: PaginationMeta;
  generatedAt: string;
}

export async function getInventoryReport(categoryId?: number): Promise<InventoryReport> {
  const res = await httpClient.get(`/api/reports/inventory${buildQuery({ categoryId })}`);
  return unwrap(res);
}

export async function getDonationReport(params: { startDate?: string; endDate?: string; donorId?: number } = {}): Promise<DonationReport> {
  const res = await httpClient.get(`/api/reports/donations${buildQuery(params)}`);
  return unwrap(res);
}

export async function getDistributionReportSummary(
  params: { startDate?: string; endDate?: string; beneficiaryId?: number; itemId?: number } = {}
): Promise<DistributionReportSummary> {
  const res = await httpClient.get(`/api/reports/distributions${buildQuery(params)}`);
  return unwrap(res);
}

export async function getBeneficiaryReport(params: { startDate?: string; endDate?: string; beneficiaryId?: number } = {}): Promise<BeneficiaryReport> {
  const res = await httpClient.get(`/api/reports/beneficiaries${buildQuery(params)}`);
  return unwrap(res);
}

export async function getVolunteerReport(params: { startDate?: string; endDate?: string; volunteerId?: number } = {}): Promise<VolunteerReport> {
  const res = await httpClient.get(`/api/reports/volunteers${buildQuery(params)}`);
  return unwrap(res);
}

export async function getExpiryReport(): Promise<ExpiryReport> {
  const res = await httpClient.get("/api/reports/expiry");
  return unwrap(res);
}

export async function getStockMovementReport(
  params: {
    itemId?: number;
    transactionType?: TransactionType;
    referenceType?: ReferenceType;
    performedBy?: number;
    startDate?: string;
    endDate?: string;
    page?: number;
    size?: number;
  } = {}
): Promise<StockMovementReport> {
  const res = await httpClient.get(`/api/reports/stock-movements${buildQuery(params)}`);
  return unwrap(res);
}

export type ReportKind = "inventory" | "donations" | "distributions" | "beneficiaries" | "volunteers" | "expiry" | "stock-movements";

export async function exportReport(kind: ReportKind, params: Record<string, string | number | boolean | undefined> = {}): Promise<Blob> {
  const res = await httpClient.get(`/api/reports/${kind}/export${buildQuery(params)}`, { responseType: "blob" });
  return res.data;
}
