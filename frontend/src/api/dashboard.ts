import { httpClient } from "@/api/client";
import { buildQuery, unwrap } from "@/api/helpers";
import type { ReportBreakdownEntry, TrendPoint } from "@/api/reports";

export interface DashboardAlert {
  severity: "CRITICAL" | "WARNING" | "INFO" | string;
  type: string;
  message: string;
  count: number;
  entityType?: string;
  entityId?: number;
}

export interface DashboardSummary {
  totalInventoryItems: number;
  totalStockUnits: number;
  lowStockItems: number;
  outOfStockItems: number;
  expiredItems: number;
  expiringSoonItems: number;
  totalCategories: number;
  totalDonors: number;
  totalBeneficiaries: number;
  totalVolunteers: number;
  donationsThisMonth: number;
  distributionsThisMonth: number;
  pendingDistributionRequests: number;
  approvedDistributionRequests: number;
  pendingVolunteerTasks: number;
  generatedAt: string;
}

export interface RecentActivity {
  action: string;
  description: string;
  entityType: string | null;
  entityId: string | null;
  performedBy: string | null;
  occurredAt: string;
}

export type TrendPeriod = "3months" | "6months" | "12months";

export async function getDashboardSummary(): Promise<DashboardSummary> {
  const res = await httpClient.get("/api/dashboard/summary");
  return unwrap(res);
}

export async function getDashboardAlerts(): Promise<DashboardAlert[]> {
  const res = await httpClient.get("/api/dashboard/alerts");
  return unwrap(res);
}

export async function getDonationTrend(period: TrendPeriod = "6months"): Promise<TrendPoint[]> {
  const res = await httpClient.get(`/api/dashboard/donation-trend${buildQuery({ period })}`);
  return unwrap(res);
}

export async function getDistributionTrend(period: TrendPeriod = "6months"): Promise<TrendPoint[]> {
  const res = await httpClient.get(`/api/dashboard/distribution-trend${buildQuery({ period })}`);
  return unwrap(res);
}

export async function getInventoryByCategory(): Promise<ReportBreakdownEntry[]> {
  const res = await httpClient.get("/api/dashboard/inventory-by-category");
  return unwrap(res);
}

export async function getBeneficiaryPriority(): Promise<ReportBreakdownEntry[]> {
  const res = await httpClient.get("/api/dashboard/beneficiary-priority");
  return unwrap(res);
}

export async function getRecentActivity(): Promise<RecentActivity[]> {
  const res = await httpClient.get("/api/dashboard/recent-activity");
  return unwrap(res);
}
