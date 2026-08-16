import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Download } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Label } from "@/components/ui/Input";
import { Tabs } from "@/components/ui/Tabs";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { BreakdownChart } from "@/components/charts/BreakdownChart";
import { TrendChart } from "@/components/charts/TrendChart";
import {
  getInventoryReport,
  getDonationReport,
  getDistributionReportSummary,
  getBeneficiaryReport,
  getVolunteerReport,
  getExpiryReport,
  exportReport,
  type ReportKind,
} from "@/api/reports";
import { formatDate, formatNumber } from "@/lib/utils";
import { notifyError } from "@/lib/toast";

type ReportTab = "inventory" | "donations" | "distributions" | "beneficiaries" | "volunteers" | "expiry";

const TABS: { key: ReportTab; label: string }[] = [
  { key: "inventory", label: "Inventory" },
  { key: "donations", label: "Donations" },
  { key: "distributions", label: "Distributions" },
  { key: "beneficiaries", label: "Beneficiaries" },
  { key: "volunteers", label: "Volunteers" },
  { key: "expiry", label: "Expiry" },
];

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

function StatTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg bg-ink-50 px-4 py-3">
      <p className="text-xs font-medium text-ink-500">{label}</p>
      <p className="mt-1 font-display text-xl font-bold text-ink-900">{value}</p>
    </div>
  );
}

export default function ReportsPage() {
  const [tab, setTab] = useState<ReportTab>("inventory");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [exporting, setExporting] = useState(false);

  const dateParams = { startDate: startDate || undefined, endDate: endDate || undefined };

  const inventoryQuery = useQuery({ queryKey: ["reports", "inventory"], queryFn: () => getInventoryReport(), enabled: tab === "inventory" });
  const donationQuery = useQuery({ queryKey: ["reports", "donations", dateParams], queryFn: () => getDonationReport(dateParams), enabled: tab === "donations" });
  const distributionQuery = useQuery({
    queryKey: ["reports", "distributions", dateParams],
    queryFn: () => getDistributionReportSummary(dateParams),
    enabled: tab === "distributions",
  });
  const beneficiaryQuery = useQuery({
    queryKey: ["reports", "beneficiaries", dateParams],
    queryFn: () => getBeneficiaryReport(dateParams),
    enabled: tab === "beneficiaries",
  });
  const volunteerQuery = useQuery({
    queryKey: ["reports", "volunteers", dateParams],
    queryFn: () => getVolunteerReport(dateParams),
    enabled: tab === "volunteers",
  });
  const expiryQuery = useQuery({ queryKey: ["reports", "expiry"], queryFn: getExpiryReport, enabled: tab === "expiry" });

  const handleExport = async () => {
    setExporting(true);
    try {
      const kind: ReportKind = tab as ReportKind;
      const blob = await exportReport(kind, tab === "inventory" || tab === "expiry" ? {} : dateParams);
      downloadBlob(blob, `${tab}-report-${formatDate(new Date().toISOString())}.csv`);
    } catch (err) {
      notifyError(err);
    } finally {
      setExporting(false);
    }
  };

  const showDateFilter = tab === "donations" || tab === "distributions" || tab === "beneficiaries" || tab === "volunteers";

  return (
    <div>
      <PageHeader title="Reports" description="Aggregate figures computed live from your data, with CSV export." />

      <Card>
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-100 px-5 pt-4">
          <Tabs items={TABS} active={tab} onChange={(k) => setTab(k as ReportTab)} />
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-ink-100 px-5 py-3.5">
          {showDateFilter ? (
            <div className="flex items-center gap-2">
              <Label className="mb-0 text-xs">From</Label>
              <Input type="date" className="w-40" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
              <Label className="mb-0 text-xs">To</Label>
              <Input type="date" className="w-40" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
            </div>
          ) : (
            <div />
          )}
          <Button variant="outline" onClick={handleExport} loading={exporting}>
            <Download className="h-4 w-4" />
            Export CSV
          </Button>
        </div>

        <CardBody>
          {tab === "inventory" && (
            <InventoryReportView loading={inventoryQuery.isLoading} data={inventoryQuery.data} />
          )}
          {tab === "donations" && <DonationReportView loading={donationQuery.isLoading} data={donationQuery.data} />}
          {tab === "distributions" && <DistributionReportView loading={distributionQuery.isLoading} data={distributionQuery.data} />}
          {tab === "beneficiaries" && <BeneficiaryReportView loading={beneficiaryQuery.isLoading} data={beneficiaryQuery.data} />}
          {tab === "volunteers" && <VolunteerReportView loading={volunteerQuery.isLoading} data={volunteerQuery.data} />}
          {tab === "expiry" && <ExpiryReportView loading={expiryQuery.isLoading} data={expiryQuery.data} />}
        </CardBody>
      </Card>
    </div>
  );
}

function ReportLoading() {
  return (
    <div className="space-y-4">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {[0, 1, 2, 3].map((i) => (
          <div key={i} className="h-20 animate-pulse rounded-lg bg-ink-100" />
        ))}
      </div>
      <div className="h-56 animate-pulse rounded-lg bg-ink-100" />
    </div>
  );
}

function InventoryReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getInventoryReport>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatTile label="Total items" value={formatNumber(data.totalItems)} />
        <StatTile label="Total quantity" value={formatNumber(data.totalQuantity)} />
        <StatTile label="Low stock" value={formatNumber(data.lowStockItems)} />
        <StatTile label="Out of stock" value={formatNumber(data.outOfStockItems)} />
        <StatTile label="Expired" value={formatNumber(data.expiredItems)} />
        <StatTile label="Expiring soon" value={formatNumber(data.expiringSoonItems)} />
      </div>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">By category</p>
          {data.categoryBreakdown.length === 0 ? <EmptyState title="No categories" /> : <BreakdownChart data={data.categoryBreakdown} valueKey="quantity" />}
        </div>
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">By status</p>
          {data.statusBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.statusBreakdown} valueKey="count" />}
        </div>
      </div>
    </div>
  );
}

function DonationReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getDonationReport>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatTile label="Total donations" value={formatNumber(data.totalDonations)} />
        <StatTile label="Item lines" value={formatNumber(data.totalItemLines)} />
        <StatTile label="Quantity donated" value={formatNumber(data.totalQuantityDonated)} />
      </div>
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">Monthly trend</p>
        {data.monthlyTrend.length === 0 ? (
          <EmptyState title="No trend data" />
        ) : (
          <TrendChart series={[{ key: "donations", label: "Donations", color: "#059669", data: data.monthlyTrend }]} />
        )}
      </div>
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">Top donors</p>
        {data.donorContributions.length === 0 ? <EmptyState title="No donors" /> : <BreakdownChart data={data.donorContributions} valueKey="quantity" />}
      </div>
    </div>
  );
}

function DistributionReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getDistributionReportSummary>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatTile label="Completed" value={formatNumber(data.completedDistributions)} />
        <StatTile label="Item lines" value={formatNumber(data.totalItemLines)} />
        <StatTile label="Quantity distributed" value={formatNumber(data.totalQuantityDistributed)} />
      </div>
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">Monthly trend</p>
        {data.monthlyTrend.length === 0 ? (
          <EmptyState title="No trend data" />
        ) : (
          <TrendChart series={[{ key: "distributions", label: "Distributions", color: "#0ea5e9", data: data.monthlyTrend }]} />
        )}
      </div>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">By priority</p>
          {data.priorityBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.priorityBreakdown} valueKey="count" />}
        </div>
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">Top items distributed</p>
          {data.itemBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.itemBreakdown} valueKey="quantity" />}
        </div>
      </div>
    </div>
  );
}

function BeneficiaryReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getBeneficiaryReport>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
        <StatTile label="Total beneficiaries" value={formatNumber(data.totalBeneficiaries)} />
        <StatTile label="Active" value={formatNumber(data.activeBeneficiaries)} />
        <StatTile label="Inactive" value={formatNumber(data.inactiveBeneficiaries)} />
      </div>
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">By priority</p>
          {data.priorityBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.priorityBreakdown} valueKey="count" />}
        </div>
        <div>
          <p className="mb-2 text-sm font-semibold text-ink-800">Most aid received</p>
          {data.distributionBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.distributionBreakdown} valueKey="quantity" />}
        </div>
      </div>
    </div>
  );
}

function VolunteerReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getVolunteerReport>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <StatTile label="Volunteers" value={formatNumber(data.totalVolunteers)} />
        <StatTile label="Total tasks" value={formatNumber(data.totalTasks)} />
        <StatTile label="Completed" value={formatNumber(data.completedTasks)} />
        <StatTile label="Overdue" value={formatNumber(data.overdueTasks)} />
      </div>
      <div>
        <p className="mb-2 text-sm font-semibold text-ink-800">Most active volunteers</p>
        {data.volunteerBreakdown.length === 0 ? <EmptyState title="No data" /> : <BreakdownChart data={data.volunteerBreakdown} valueKey="count" />}
      </div>
    </div>
  );
}

function ExpiryReportView({ loading, data }: { loading: boolean; data: Awaited<ReturnType<typeof getExpiryReport>> | undefined }) {
  if (loading) return <ReportLoading />;
  if (!data) return <EmptyState title="No data" />;
  const rows = [...data.expired, ...data.expiringWithin30Days];
  return (
    <div className="space-y-6">
      <div className="grid grid-cols-3 gap-4">
        <StatTile label="Expired" value={formatNumber(data.expiredCount)} />
        <StatTile label="Expiring within 7 days" value={formatNumber(data.expiringWithin7DaysCount)} />
        <StatTile label="Expiring within 30 days" value={formatNumber(data.expiringWithin30DaysCount)} />
      </div>
      {rows.length === 0 ? (
        <EmptyState title="Nothing expired or expiring soon" />
      ) : (
        <Table>
          <THead>
            <TR>
              <TH>Item</TH>
              <TH>Category</TH>
              <TH>Quantity</TH>
              <TH>Expiry date</TH>
              <TH>Status</TH>
            </TR>
          </THead>
          <TBody>
            {rows.map((item) => (
              <TR key={item.id}>
                <TD className="font-medium text-ink-900">{item.itemName}</TD>
                <TD className="text-ink-500">{item.categoryName ?? "—"}</TD>
                <TD className="text-ink-600">{item.quantity}</TD>
                <TD className="text-ink-500">{formatDate(item.expiryDate)}</TD>
                <TD>
                  <Badge tone={item.expired ? "danger" : "warning"} dot>
                    {item.expired ? "Expired" : "Expiring soon"}
                  </Badge>
                </TD>
              </TR>
            ))}
          </TBody>
        </Table>
      )}
    </div>
  );
}
