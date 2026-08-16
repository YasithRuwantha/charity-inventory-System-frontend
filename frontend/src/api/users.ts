import { httpClient } from "@/api/client";
import { buildQuery, unwrap, unwrapPaged } from "@/api/helpers";
import type { Paged } from "@/api/types";
import type { UserRole, UserStatus } from "@/types/enums";

export interface AppUser {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  status: UserStatus;
}

export interface UserListParams {
  search?: string;
  role?: UserRole;
  status?: UserStatus;
  page?: number;
  size?: number;
  sort?: string;
}

export async function getMe(): Promise<AppUser> {
  const res = await httpClient.get("/api/users/me");
  return unwrap(res);
}

export async function listUsers(params: UserListParams = {}): Promise<Paged<AppUser>> {
  const res = await httpClient.get(`/api/users${buildQuery(params)}`);
  return unwrapPaged<AppUser>(res);
}

export async function getUser(id: number): Promise<AppUser> {
  const res = await httpClient.get(`/api/users/${id}`);
  return unwrap(res);
}

export async function updateUserRole(id: number, role: UserRole): Promise<AppUser> {
  const res = await httpClient.patch(`/api/users/${id}/role`, { role });
  return unwrap(res);
}

export async function updateUserStatus(id: number, status: UserStatus): Promise<AppUser> {
  const res = await httpClient.patch(`/api/users/${id}/status`, { status });
  return unwrap(res);
}
