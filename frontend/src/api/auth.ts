import { httpClient } from "@/api/client";
import type { Role } from "@/store/auth";

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  name: string;
  email: string;
  password: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  name: string;
  email: string;
  role: Role;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const res = await httpClient.post<AuthResponse>("/api/v1/auth/login", payload);
  return res.data;
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const res = await httpClient.post<AuthResponse>("/api/v1/auth/register", payload);
  return res.data;
}
