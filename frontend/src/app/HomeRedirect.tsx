import { Navigate } from "react-router-dom";
import { useAuthStore } from "@/store/auth";

export function HomeRedirect() {
  const role = useAuthStore((s) => s.user?.role);
  if (role === "VOLUNTEER") {
    return <Navigate to="/my-tasks" replace />;
  }
  return <Navigate to="/dashboard" replace />;
}
