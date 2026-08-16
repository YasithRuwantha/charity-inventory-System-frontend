import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Pencil, Boxes, ThumbsUp, ThumbsDown, Ban, CheckCheck, Printer, HandHeart } from "lucide-react";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { ReasonDialog } from "@/components/ui/ReasonDialog";
import {
  getDistribution,
  approveDistribution,
  rejectDistribution,
  cancelDistribution,
  type Distribution,
} from "@/api/distributions";
import { useAuthStore } from "@/store/auth";
import { PRIORITY_LABELS, UNIT_LABELS, type DistributionStatus, type PriorityLevel } from "@/types/enums";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { NewDistributionDialog } from "@/features/distributions/NewDistributionDialog";
import { AllocateDialog } from "@/features/distributions/AllocateDialog";
import { CompleteDialog } from "@/features/distributions/CompleteDialog";

const STATUS_TONE: Record<DistributionStatus, BadgeTone> = {
  PENDING: "warning",
  APPROVED: "info",
  COMPLETED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
};
const PRIORITY_TONE: Record<PriorityLevel, BadgeTone> = { HIGH: "danger", MEDIUM: "warning", LOW: "neutral" };

export default function DistributionDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const role = useAuthStore((s) => s.user?.role);
  const isAdmin = role === "ADMIN";
  const distId = Number(id);

  const [editOpen, setEditOpen] = useState(false);
  const [allocateOpen, setAllocateOpen] = useState(false);
  const [completeOpen, setCompleteOpen] = useState(false);
  const [rejectOpen, setRejectOpen] = useState(false);
  const [cancelOpen, setCancelOpen] = useState(false);

  const { data: distribution, isLoading } = useQuery({
    queryKey: ["distributions", distId],
    queryFn: () => getDistribution(distId),
    enabled: Number.isFinite(distId),
  });

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ["distributions"] });
  };

  const approveMutation = useMutation({
    mutationFn: () => approveDistribution(distId),
    onSuccess: () => {
      notifySuccess("Distribution approved");
      invalidate();
    },
    onError: (err) => notifyError(err),
  });

  const rejectMutation = useMutation({
    mutationFn: (reason: string) => rejectDistribution(distId, reason),
    onSuccess: () => {
      notifySuccess("Distribution rejected");
      invalidate();
      setRejectOpen(false);
    },
    onError: (err) => notifyError(err),
  });

  const cancelMutation = useMutation({
    mutationFn: (reason: string) => cancelDistribution(distId, reason),
    onSuccess: () => {
      notifySuccess("Distribution cancelled");
      invalidate();
      setCancelOpen(false);
    },
    onError: (err) => notifyError(err),
  });

  if (isLoading) return <PageSpinner />;
  if (!distribution) return <EmptyState icon={HandHeart} title="Distribution not found" />;

  const d: Distribution = distribution;
  const isPending = d.status === "PENDING";
  const isApproved = d.status === "APPROVED";
  const hasAllocation = d.items.every((item) => (item.allocatedQuantity ?? 0) > 0);

  return (
    <div>
      <div className="mb-4 flex items-center justify-between print:hidden">
        <button onClick={() => navigate(-1)} className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800">
          <ArrowLeft className="h-4 w-4" /> Back
        </button>
        <Button variant="outline" onClick={() => navigate(`/distributions/${distId}/report`)}>
          <Printer className="h-4 w-4" />
          View report
        </Button>
      </div>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="font-display text-xl font-bold text-ink-900">{d.requestReference}</h1>
            <Badge tone={STATUS_TONE[d.status]} dot>
              {toTitleCase(d.status)}
            </Badge>
            <Badge tone={PRIORITY_TONE[d.priority]} dot>
              {PRIORITY_LABELS[d.priority]} priority
            </Badge>
          </div>
          <p className="mt-1 text-sm text-ink-500">
            <Link to={`/beneficiaries/${d.beneficiaryId}`} className="text-brand-700 hover:underline">
              {d.beneficiaryName}
            </Link>{" "}
            · Requested {formatDate(d.requestDate)}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2 print:hidden">
          {isPending && (
            <>
              <Button variant="outline" onClick={() => setEditOpen(true)}>
                <Pencil className="h-4 w-4" />
                Edit
              </Button>
              <Button variant="outline" onClick={() => setAllocateOpen(true)}>
                <Boxes className="h-4 w-4" />
                Allocate
              </Button>
              {isAdmin && (
                <>
                  <Button onClick={() => approveMutation.mutate()} loading={approveMutation.isPending} disabled={!hasAllocation}>
                    <ThumbsUp className="h-4 w-4" />
                    Approve
                  </Button>
                  <Button variant="danger-outline" onClick={() => setRejectOpen(true)}>
                    <ThumbsDown className="h-4 w-4" />
                    Reject
                  </Button>
                </>
              )}
              <Button variant="danger-outline" onClick={() => setCancelOpen(true)}>
                <Ban className="h-4 w-4" />
                Cancel
              </Button>
            </>
          )}
          {isApproved && (
            <>
              <Button onClick={() => setCompleteOpen(true)}>
                <CheckCheck className="h-4 w-4" />
                Complete
              </Button>
              <Button variant="danger-outline" onClick={() => setCancelOpen(true)}>
                <Ban className="h-4 w-4" />
                Cancel
              </Button>
            </>
          )}
        </div>
      </div>

      {isPending && !hasAllocation && (
        <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 px-3.5 py-2.5 text-sm text-amber-700">
          Allocate quantities for every line before this request can be approved.
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader title="Requested items" />
          <Table>
            <THead>
              <TR>
                <TH>Item</TH>
                <TH>Requested</TH>
                <TH>Allocated</TH>
                <TH>Distributed</TH>
                <TH>Available now</TH>
              </TR>
            </THead>
            <TBody>
              {d.items.map((item) => (
                <TR key={item.id}>
                  <TD>
                    <p className="font-medium text-ink-900">{item.itemName}</p>
                    <p className="text-xs text-ink-400">{item.itemCode}</p>
                  </TD>
                  <TD className="text-ink-700">
                    {item.requestedQuantity} {UNIT_LABELS[item.unit]}
                  </TD>
                  <TD className="text-ink-700">{item.allocatedQuantity ?? "—"}</TD>
                  <TD className="text-ink-700">{item.distributedQuantity ?? "—"}</TD>
                  <TD className="text-ink-500">
                    {item.availableQuantity}
                    {item.expired && (
                      <Badge tone="danger" className="ml-1.5">
                        Expired
                      </Badge>
                    )}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>

        <Card>
          <CardHeader title="Timeline" />
          <CardBody>
            <ol className="space-y-4">
              <TimelineEntry label="Requested" by={d.requestedBy?.name} at={d.createdAt} />
              {d.approvedAt && <TimelineEntry label="Approved" by={d.approvedBy?.name} at={d.approvedAt} />}
              {d.rejectedAt && <TimelineEntry label="Rejected" by={d.rejectedBy?.name} at={d.rejectedAt} note={d.rejectionReason} tone="rose" />}
              {d.cancelledAt && <TimelineEntry label="Cancelled" by={d.cancelledBy?.name} at={d.cancelledAt} note={d.cancellationReason} tone="rose" />}
              {d.completedAt && <TimelineEntry label="Completed" by={d.completedBy?.name} at={d.completedAt} tone="emerald" />}
            </ol>
            {(d.reason || d.notes) && (
              <div className="mt-4 space-y-2 border-t border-ink-100 pt-4">
                {d.reason && (
                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Reason</p>
                    <p className="text-sm text-ink-700">{d.reason}</p>
                  </div>
                )}
                {d.notes && (
                  <div>
                    <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Notes</p>
                    <p className="text-sm text-ink-700">{d.notes}</p>
                  </div>
                )}
              </div>
            )}
          </CardBody>
        </Card>
      </div>

      <NewDistributionDialog open={editOpen} onClose={() => setEditOpen(false)} editingDistribution={d} />
      <AllocateDialog open={allocateOpen} onClose={() => setAllocateOpen(false)} distribution={d} />
      <CompleteDialog open={completeOpen} onClose={() => setCompleteOpen(false)} distribution={d} />
      <ReasonDialog
        open={rejectOpen}
        onClose={() => setRejectOpen(false)}
        onConfirm={(reason) => rejectMutation.mutate(reason)}
        title="Reject this request?"
        description="The beneficiary will not receive these items."
        confirmLabel="Reject"
        danger
        loading={rejectMutation.isPending}
      />
      <ReasonDialog
        open={cancelOpen}
        onClose={() => setCancelOpen(false)}
        onConfirm={(reason) => cancelMutation.mutate(reason)}
        title="Cancel this request?"
        description="This request will be closed without distributing any items."
        confirmLabel="Cancel request"
        danger
        loading={cancelMutation.isPending}
      />
    </div>
  );
}

function TimelineEntry({
  label,
  by,
  at,
  note,
  tone = "brand",
}: {
  label: string;
  by?: string | null;
  at: string;
  note?: string | null;
  tone?: "brand" | "rose" | "emerald";
}) {
  const dotClass = tone === "rose" ? "bg-rose-500" : tone === "emerald" ? "bg-emerald-500" : "bg-brand-500";
  return (
    <li className="flex gap-3">
      <span className={`mt-1.5 h-2 w-2 shrink-0 rounded-full ${dotClass}`} />
      <div>
        <p className="text-sm font-medium text-ink-800">{label}</p>
        <p className="text-xs text-ink-400">
          {by ?? "System"} · {formatDateTime(at)}
        </p>
        {note && <p className="mt-0.5 text-xs text-ink-500">{note}</p>}
      </div>
    </li>
  );
}
