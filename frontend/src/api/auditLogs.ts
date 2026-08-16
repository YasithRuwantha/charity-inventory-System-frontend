import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { AuditAction } from "@/types/enums";

export interface AuditLog {
  id: number;
  userId: number | null;
  userEmail: string | null;
  action: AuditAction | string;
  entityType: string | null;
  entityId: string | null;
  description: string | null;
  oldValues: string | null;
  newValues: string | null;
  ipAddress: string | null;
  createdAt: string;
}

export interface AuditLogListParams {
  userId?: number;
  action?: string;
  entityType?: string;
  entityId?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export async function listAuditLogs(params: AuditLogListParams = {}): Promise<Paged<AuditLog>> {
  const res = await httpClient.get(`/api/audit-logs${buildQuery(params)}`);
  return unwrapPaged<AuditLog>(res);
}

export async function listAuditActions(): Promise<string[]> {
  const res = await httpClient.get("/api/audit-logs/actions");
  return unwrap(res);
}
