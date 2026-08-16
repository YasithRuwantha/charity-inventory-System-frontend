import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { RecordStatus, TaskStatus } from "@/types/enums";
import type { VolunteerTask } from "@/api/volunteerTasks";

export interface Volunteer {
  id: number;
  volunteerCode: string;
  volunteerName: string;
  phone: string | null;
  email: string | null;
  address: string | null;
  status: RecordStatus;
  joinedDate: string | null;
  notes: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface VolunteerListParams {
  search?: string;
  status?: RecordStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export interface VolunteerPayload {
  volunteerName: string;
  phone?: string;
  email?: string;
  address?: string;
  joinedDate?: string;
  notes?: string;
  status?: RecordStatus;
}

export interface VolunteerActivity {
  activityType: "TASK_ASSIGNED" | "TASK_STARTED" | "TASK_COMPLETED" | "TASK_CANCELLED" | string;
  description: string;
  taskId: number;
  taskTitle: string;
  relatedDistributionId: number | null;
  occurredAt: string;
}

export async function listVolunteers(params: VolunteerListParams = {}): Promise<Paged<Volunteer>> {
  const res = await httpClient.get(`/api/volunteers${buildQuery(params)}`);
  return unwrapPaged<Volunteer>(res);
}

export async function getVolunteer(id: number): Promise<Volunteer> {
  const res = await httpClient.get(`/api/volunteers/${id}`);
  return unwrap(res);
}

export async function createVolunteer(payload: VolunteerPayload): Promise<Volunteer> {
  const res = await httpClient.post("/api/volunteers", payload);
  return unwrap(res);
}

export async function updateVolunteer(id: number, payload: Omit<VolunteerPayload, "status">): Promise<Volunteer> {
  const res = await httpClient.put(`/api/volunteers/${id}`, payload);
  return unwrap(res);
}

export async function updateVolunteerStatus(id: number, status: RecordStatus): Promise<Volunteer> {
  const res = await httpClient.patch(`/api/volunteers/${id}/status`, { status });
  return unwrap(res);
}

export async function getVolunteerTasks(
  id: number,
  params: { status?: TaskStatus; startDate?: string; endDate?: string; page?: number; size?: number; sort?: string } = {}
): Promise<Paged<VolunteerTask>> {
  const res = await httpClient.get(`/api/volunteers/${id}/tasks${buildQuery(params)}`);
  return unwrapPaged<VolunteerTask>(res);
}

export async function getVolunteerActivity(id: number): Promise<VolunteerActivity[]> {
  const res = await httpClient.get(`/api/volunteers/${id}/activity`);
  return unwrap(res);
}
