import { lazy, Suspense } from "react";
import { Route, Routes } from "react-router-dom";
import { AppShell } from "@/components/layout/AppShell";
import { ProtectedRoute, GuestRoute } from "@/app/ProtectedRoute";
import { RoleRoute } from "@/app/RoleRoute";
import { HomeRedirect } from "@/app/HomeRedirect";
import { NotFoundPage } from "@/app/NotFoundPage";
import { NotAuthorizedPage } from "@/app/NotAuthorizedPage";
import { PageSpinner } from "@/components/ui/Skeleton";

const LoginPage = lazy(() => import("@/features/auth/LoginPage").then((m) => ({ default: m.LoginPage })));
const RegisterPage = lazy(() => import("@/features/auth/RegisterPage").then((m) => ({ default: m.RegisterPage })));

const DashboardPage = lazy(() => import("@/features/dashboard/DashboardPage"));
const InventoryListPage = lazy(() => import("@/features/inventory/InventoryListPage"));
const InventoryDetailPage = lazy(() => import("@/features/inventory/InventoryDetailPage"));
const CategoriesPage = lazy(() => import("@/features/inventory/CategoriesPage"));
const LedgerPage = lazy(() => import("@/features/inventory/LedgerPage"));

const DonorsPage = lazy(() => import("@/features/donations/DonorsPage"));
const DonorDetailPage = lazy(() => import("@/features/donations/DonorDetailPage"));
const DonationsPage = lazy(() => import("@/features/donations/DonationsPage"));
const DonationDetailPage = lazy(() => import("@/features/donations/DonationDetailPage"));
const BulkImportPage = lazy(() => import("@/features/donations/BulkImportPage"));

const BeneficiariesPage = lazy(() => import("@/features/beneficiaries/BeneficiariesPage"));
const BeneficiaryDetailPage = lazy(() => import("@/features/beneficiaries/BeneficiaryDetailPage"));

const DistributionsPage = lazy(() => import("@/features/distributions/DistributionsPage"));
const DistributionDetailPage = lazy(() => import("@/features/distributions/DistributionDetailPage"));
const DistributionReportPage = lazy(() => import("@/features/distributions/DistributionReportPage"));

const VolunteersPage = lazy(() => import("@/features/volunteers/VolunteersPage"));
const VolunteerDetailPage = lazy(() => import("@/features/volunteers/VolunteerDetailPage"));
const MyTasksPage = lazy(() => import("@/features/volunteers/MyTasksPage"));

const ReportsPage = lazy(() => import("@/features/reports/ReportsPage"));
const UsersPage = lazy(() => import("@/features/users/UsersPage"));
const AuditLogPage = lazy(() => import("@/features/audit/AuditLogPage"));
const ProfilePage = lazy(() => import("@/features/profile/ProfilePage"));

const STAFF: Array<"ADMIN" | "INVENTORY_STAFF"> = ["ADMIN", "INVENTORY_STAFF"];

export function App() {
  return (
    <Suspense fallback={<PageSpinner />}>
      <Routes>
        <Route element={<GuestRoute />}>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Route>

        <Route element={<ProtectedRoute />}>
          <Route element={<AppShell />}>
            <Route path="/" element={<HomeRedirect />} />
            <Route path="/not-authorized" element={<NotAuthorizedPage />} />
            <Route path="/profile" element={<ProfilePage />} />

            <Route element={<RoleRoute allow={STAFF} />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/inventory/transactions" element={<LedgerPage />} />
              <Route path="/donors" element={<DonorsPage />} />
              <Route path="/donors/:id" element={<DonorDetailPage />} />
              <Route path="/donations" element={<DonationsPage />} />
              <Route path="/donations/bulk" element={<BulkImportPage />} />
              <Route path="/donations/:id" element={<DonationDetailPage />} />
              <Route path="/beneficiaries" element={<BeneficiariesPage />} />
              <Route path="/beneficiaries/:id" element={<BeneficiaryDetailPage />} />
              <Route path="/distributions" element={<DistributionsPage />} />
              <Route path="/distributions/:id" element={<DistributionDetailPage />} />
              <Route path="/distributions/:id/report" element={<DistributionReportPage />} />
              <Route path="/volunteers" element={<VolunteersPage />} />
              <Route path="/volunteers/:id" element={<VolunteerDetailPage />} />
              <Route path="/reports" element={<ReportsPage />} />
            </Route>

            <Route path="/inventory" element={<InventoryListPage />} />
            <Route path="/inventory/categories" element={<CategoriesPage />} />
            <Route path="/inventory/:id" element={<InventoryDetailPage />} />

            <Route element={<RoleRoute allow={["VOLUNTEER"]} />}>
              <Route path="/my-tasks" element={<MyTasksPage />} />
            </Route>

            <Route element={<RoleRoute allow={["ADMIN"]} />}>
              <Route path="/admin/users" element={<UsersPage />} />
              <Route path="/admin/audit-log" element={<AuditLogPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </Suspense>
  );
}
