import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged, UserSummary } from "@/api/types";
import type { TaskStatus } from "@/types/enums";

export interface VolunteerTask {
  id: number;
  volunteerId: number;
  volunteerCode: string;
  volunteerName: string;
  title: string;
  description: string | null;
  taskDate: string;
  dueDate: string | null;
  status: TaskStatus;
  assignedBy: UserSummary | null;
  relatedDistributionId: number | null;
  startedAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  overdue: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VolunteerTaskListParams {
  search?: string;
  volunteerId?: number;
  status?: TaskStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}

export interface CreateVolunteerTaskPayload {
  volunteerId: number;
  title: string;
  description?: string;
  taskDate: string;
  dueDate?: string;
  relatedDistributionId?: number;
}

export interface UpdateVolunteerTaskPayload {
  volunteerId?: number;
  title: string;
  description?: string;
  taskDate: string;
  dueDate?: string;
  relatedDistributionId?: number;
}

export async function listVolunteerTasks(params: VolunteerTaskListParams = {}): Promise<Paged<VolunteerTask>> {
  const res = await httpClient.get(`/api/volunteer-tasks${buildQuery(params)}`);
  return unwrapPaged<VolunteerTask>(res);
}

export async function getVolunteerTask(id: number): Promise<VolunteerTask> {
  const res = await httpClient.get(`/api/volunteer-tasks/${id}`);
  return unwrap(res);
}

export async function createVolunteerTask(payload: CreateVolunteerTaskPayload): Promise<VolunteerTask> {
  const res = await httpClient.post("/api/volunteer-tasks", payload);
  return unwrap(res);
}

export async function updateVolunteerTask(id: number, payload: UpdateVolunteerTaskPayload): Promise<VolunteerTask> {
  const res = await httpClient.put(`/api/volunteer-tasks/${id}`, payload);
  return unwrap(res);
}

export async function updateVolunteerTaskStatus(id: number, status: TaskStatus, reason?: string): Promise<VolunteerTask> {
  const res = await httpClient.patch(`/api/volunteer-tasks/${id}/status`, { status, reason });
  return unwrap(res);
}
