import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Pencil, Users, HandHeart } from "lucide-react";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { getBeneficiary, getBeneficiaryStatistics } from "@/api/beneficiaries";
import { PRIORITY_LABELS, type PriorityLevel, type DistributionStatus } from "@/types/enums";
import { formatDate, formatNumber, toTitleCase } from "@/lib/utils";
import { BeneficiaryFormDialog } from "@/features/beneficiaries/BeneficiaryFormDialog";

const PRIORITY_TONE: Record<PriorityLevel, BadgeTone> = { HIGH: "danger", MEDIUM: "warning", LOW: "neutral" };
const DIST_STATUS_TONE: Record<DistributionStatus, BadgeTone> = {
  PENDING: "warning",
  APPROVED: "info",
  COMPLETED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
};

export default function BeneficiaryDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const beneficiaryId = Number(id);
  const [formOpen, setFormOpen] = useState(false);

  const { data: beneficiary, isLoading } = useQuery({
    queryKey: ["beneficiaries", beneficiaryId],
    queryFn: () => getBeneficiary(beneficiaryId),
    enabled: Number.isFinite(beneficiaryId),
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ["beneficiaries", beneficiaryId, "statistics"],
    queryFn: () => getBeneficiaryStatistics(beneficiaryId),
    enabled: Number.isFinite(beneficiaryId),
  });

  if (isLoading) return <PageSpinner />;
  if (!beneficiary) return <EmptyState icon={Users} title="Beneficiary not found" />;

  return (
    <div>
      <button
        onClick={() => navigate(-1)}
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800"
      >
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="font-display text-xl font-bold text-ink-900">{beneficiary.beneficiaryName}</h1>
            <Badge tone={PRIORITY_TONE[beneficiary.priorityLevel]} dot>
              {PRIORITY_LABELS[beneficiary.priorityLevel]} priority
            </Badge>
            <Badge tone={beneficiary.status === "ACTIVE" ? "success" : "neutral"} dot>
              {toTitleCase(beneficiary.status)}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-ink-500">{beneficiary.beneficiaryCode}</p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => navigate(`/distributions?newFor=${beneficiary.id}`)}>
            <HandHeart className="h-4 w-4" />
            New distribution
          </Button>
          <Button onClick={() => setFormOpen(true)}>
            <Pencil className="h-4 w-4" />
            Edit
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Distributions</p>
            <p className="mt-1 font-display text-2xl font-bold text-ink-900">
              {statsLoading ? "—" : formatNumber(stats?.totalDistributions)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Items received</p>
            <p className="mt-1 font-display text-2xl font-bold text-ink-900">
              {statsLoading ? "—" : formatNumber(stats?.totalQuantityReceived)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Pending requests</p>
            <p className="mt-1 font-display text-2xl font-bold text-amber-600">
              {statsLoading ? "—" : formatNumber(stats?.pendingRequests)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Last received</p>
            <p className="mt-1 font-display text-lg font-bold text-ink-900">
              {statsLoading ? "—" : formatDate(stats?.lastDistributionDate)}
            </p>
          </CardBody>
        </Card>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader title="Details" />
          <CardBody>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Family size</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{beneficiary.familySize}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">ID number</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{beneficiary.identificationNumber || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Contact</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{beneficiary.contactNumber || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Address</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{beneficiary.address || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Notes</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{beneficiary.notes || "—"}</dd>
              </div>
            </dl>
          </CardBody>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Recent distributions" />
          {!stats || stats.recentDistributions.length === 0 ? (
            <EmptyState icon={HandHeart} title="No distributions yet" />
          ) : (
            <Table>
              <THead>
                <TR>
                  <TH>Reference</TH>
                  <TH>Date</TH>
                  <TH>Items</TH>
                  <TH>Status</TH>
                </TR>
              </THead>
              <TBody>
                {stats.recentDistributions.map((d) => (
                  <TR key={d.id} className="cursor-pointer" onClick={() => navigate(`/distributions/${d.id}`)}>
                    <TD className="font-medium text-brand-700">{d.requestReference}</TD>
                    <TD className="text-ink-500">{formatDate(d.requestDate)}</TD>
                    <TD className="text-ink-600">{d.totalItems}</TD>
                    <TD>
                      <Badge tone={DIST_STATUS_TONE[d.status]} dot>
                        {toTitleCase(d.status)}
                      </Badge>
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </Card>
      </div>

      <BeneficiaryFormDialog open={formOpen} onClose={() => setFormOpen(false)} beneficiary={beneficiary} />
    </div>
  );
}
