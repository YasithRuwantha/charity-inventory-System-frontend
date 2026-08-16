import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import {
  Package,
  Layers,
  TriangleAlert,
  PackageX,
  Gift,
  HandHeart,
  Users,
  Clock,
} from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatCard } from "@/components/ui/StatCard";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { TrendChart } from "@/components/charts/TrendChart";
import { BreakdownChart } from "@/components/charts/BreakdownChart";
import { EmptyState } from "@/components/ui/EmptyState";
import {
  getDashboardSummary,
  getDashboardAlerts,
  getDonationTrend,
  getDistributionTrend,
  getInventoryByCategory,
  getBeneficiaryPriority,
  getRecentActivity,
  type TrendPeriod,
} from "@/api/dashboard";
import { formatDateTime, formatNumber, toTitleCase } from "@/lib/utils";
import { useAuthStore } from "@/store/auth";

const PERIODS: { key: TrendPeriod; label: string }[] = [
  { key: "3months", label: "3 months" },
  { key: "6months", label: "6 months" },
  { key: "12months", label: "12 months" },
];

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const [period, setPeriod] = useState<TrendPeriod>("6months");

  const summary = useQuery({ queryKey: ["dashboard", "summary"], queryFn: getDashboardSummary });
  const alerts = useQuery({ queryKey: ["dashboard", "alerts"], queryFn: getDashboardAlerts });
  const donationTrend = useQuery({ queryKey: ["dashboard", "donation-trend", period], queryFn: () => getDonationTrend(period) });
  const distributionTrend = useQuery({ queryKey: ["dashboard", "distribution-trend", period], queryFn: () => getDistributionTrend(period) });
  const byCategory = useQuery({ queryKey: ["dashboard", "by-category"], queryFn: getInventoryByCategory });
  const byPriority = useQuery({ queryKey: ["dashboard", "by-priority"], queryFn: getBeneficiaryPriority });
  const recentActivity = useQuery({ queryKey: ["dashboard", "recent-activity"], queryFn: getRecentActivity });

  const s = summary.data;

  return (
    <div>
      <PageHeader
        title={`Welcome back, ${user?.name?.split(" ")[0] ?? ""}`}
        description="Here's what's happening across your organization today."
      />

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Inventory items" value={formatNumber(s?.totalInventoryItems)} icon={Package} tone="brand" loading={summary.isLoading} />
        <StatCard label="Stock units on hand" value={formatNumber(s?.totalStockUnits)} icon={Layers} tone="sky" loading={summary.isLoading} />
        <StatCard label="Low stock items" value={formatNumber(s?.lowStockItems)} icon={TriangleAlert} tone="amber" loading={summary.isLoading} />
        <StatCard label="Out of stock" value={formatNumber(s?.outOfStockItems)} icon={PackageX} tone="rose" loading={summary.isLoading} />
        <StatCard label="Donations this month" value={formatNumber(s?.donationsThisMonth)} icon={Gift} tone="brand" loading={summary.isLoading} />
        <StatCard label="Distributions this month" value={formatNumber(s?.distributionsThisMonth)} icon={HandHeart} tone="sky" loading={summary.isLoading} />
        <StatCard label="Pending requests" value={formatNumber(s?.pendingDistributionRequests)} icon={Clock} tone="amber" loading={summary.isLoading} />
        <StatCard label="Beneficiaries" value={formatNumber(s?.totalBeneficiaries)} icon={Users} tone="slate" loading={summary.isLoading} />
      </div>

      <div className="mt-6 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card className="xl:col-span-2">
          <CardHeader
            title="Donation & distribution trend"
            description="Quantity moved per month"
            actions={
              <div className="flex items-center gap-1 rounded-lg bg-ink-100 p-0.5">
                {PERIODS.map((p) => (
                  <button
                    key={p.key}
                    onClick={() => setPeriod(p.key)}
                    className={`rounded-md px-2.5 py-1 text-xs font-medium transition-colors ${
                      period === p.key ? "bg-white text-ink-800 shadow-sm" : "text-ink-500 hover:text-ink-700"
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
              </div>
            }
          />
          <CardBody>
            <div className="mb-2 flex items-center gap-4 text-xs font-medium text-ink-500">
              <span className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-brand-600" /> Donations
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-sky-500" /> Distributions
              </span>
            </div>
            {donationTrend.isLoading || distributionTrend.isLoading ? (
              <div className="h-[220px] animate-pulse rounded-lg bg-ink-100" />
            ) : (
              <TrendChart
                series={[
                  { key: "donations", label: "Donations", color: "#059669", data: donationTrend.data ?? [] },
                  { key: "distributions", label: "Distributions", color: "#0ea5e9", data: distributionTrend.data ?? [] },
                ]}
              />
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Alerts" description="Needs attention" />
          <CardBody className="max-h-[300px] overflow-y-auto p-0">
            {alerts.isLoading ? (
              <div className="space-y-2 p-5">
                <div className="h-10 animate-pulse rounded-lg bg-ink-100" />
                <div className="h-10 animate-pulse rounded-lg bg-ink-100" />
                <div className="h-10 animate-pulse rounded-lg bg-ink-100" />
              </div>
            ) : !alerts.data || alerts.data.length === 0 ? (
              <EmptyState icon={TriangleAlert} title="All clear" description="No alerts right now." />
            ) : (
              <ul className="divide-y divide-ink-100">
                {alerts.data.map((alert, i) => (
                  <li key={i} className="flex items-start gap-3 px-5 py-3">
                    <Badge tone={alert.severity === "CRITICAL" ? "danger" : alert.severity === "WARNING" ? "warning" : "info"} dot>
                      {toTitleCase(alert.severity)}
                    </Badge>
                    <p className="flex-1 text-sm text-ink-700">{alert.message}</p>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Card>
          <CardHeader title="Inventory by category" description="Item count per category" />
          <CardBody>
            {byCategory.isLoading ? (
              <div className="h-[220px] animate-pulse rounded-lg bg-ink-100" />
            ) : !byCategory.data || byCategory.data.length === 0 ? (
              <EmptyState title="No categories yet" />
            ) : (
              <BreakdownChart data={byCategory.data} valueKey="count" />
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Beneficiaries by priority" />
          <CardBody>
            {byPriority.isLoading ? (
              <div className="h-[220px] animate-pulse rounded-lg bg-ink-100" />
            ) : !byPriority.data || byPriority.data.length === 0 ? (
              <EmptyState title="No beneficiaries yet" />
            ) : (
              <BreakdownChart data={byPriority.data} valueKey="count" />
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Recent activity" />
          <CardBody className="max-h-[280px] overflow-y-auto p-0">
            {recentActivity.isLoading ? (
              <div className="space-y-2 p-5">
                <div className="h-8 animate-pulse rounded-lg bg-ink-100" />
                <div className="h-8 animate-pulse rounded-lg bg-ink-100" />
              </div>
            ) : !recentActivity.data || recentActivity.data.length === 0 ? (
              <EmptyState title="No recent activity" />
            ) : (
              <ul className="divide-y divide-ink-100">
                {recentActivity.data.map((a, i) => (
                  <li key={i} className="px-5 py-3">
                    <p className="text-sm text-ink-700">{a.description}</p>
                    <p className="mt-0.5 text-xs text-ink-400">
                      {a.performedBy ?? "System"} · {formatDateTime(a.occurredAt)}
                    </p>
                  </li>
                ))}
              </ul>
            )}
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
