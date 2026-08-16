import { create } from "zustand";
import { persist } from "zustand/middleware";

export type Role = "ADMIN" | "INVENTORY_STAFF" | "VOLUNTEER";

export interface AuthUser {
  id?: number;
  name: string;
  email: string;
  role: Role;
}

interface AuthState {
  token: string | null;
  user: AuthUser | null;
  isAuthenticated: boolean;
  setSession: (token: string, user: AuthUser) => void;
  updateUser: (user: Partial<AuthUser>) => void;
  clearSession: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      setSession: (token, user) => set({ token, user, isAuthenticated: true }),
      updateUser: (partial) => {
        const current = get().user;
        if (!current) return;
        set({ user: { ...current, ...partial } });
      },
      clearSession: () => set({ token: null, user: null, isAuthenticated: false }),
    }),
    { name: "charity-auth" }
  )
);

export function hasRole(role: Role | undefined, allowed: Role[]): boolean {
  if (!role) return false;
  return allowed.includes(role);
}
