import { Navigate, Outlet } from "react-router-dom";
import { useAuthStore, type Role } from "@/store/auth";

export function RoleRoute({ allow }: { allow: Role[] }) {
  const role = useAuthStore((s) => s.user?.role);
  if (!role || !allow.includes(role)) {
    return <Navigate to="/not-authorized" replace />;
  }
  return <Outlet />;
}
