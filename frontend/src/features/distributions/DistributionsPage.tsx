import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Plus, Search, HandHeart } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { listDistributions } from "@/api/distributions";
import type { DistributionStatus, PriorityLevel } from "@/types/enums";
import { PRIORITY_LABELS } from "@/types/enums";
import { formatDate, toTitleCase } from "@/lib/utils";
import { NewDistributionDialog } from "@/features/distributions/NewDistributionDialog";

const STATUS_TONE: Record<DistributionStatus, BadgeTone> = {
  PENDING: "warning",
  APPROVED: "info",
  COMPLETED: "success",
  REJECTED: "danger",
  CANCELLED: "neutral",
};
const PRIORITY_TONE: Record<PriorityLevel, BadgeTone> = { HIGH: "danger", MEDIUM: "warning", LOW: "neutral" };

export default function DistributionsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [status, setStatus] = useState("");
  const [priority, setPriority] = useState("");
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  const newFor = searchParams.get("newFor");

  useEffect(() => {
    if (newFor) setDialogOpen(true);
  }, [newFor]);

  const closeDialog = () => {
    setDialogOpen(false);
    if (newFor) {
      const next = new URLSearchParams(searchParams);
      next.delete("newFor");
      setSearchParams(next, { replace: true });
    }
  };

  const { data, isLoading } = useQuery({
    queryKey: ["distributions", { search: debouncedSearch, status, priority, page }],
    queryFn: () =>
      listDistributions({
        search: debouncedSearch || undefined,
        status: (status as DistributionStatus) || undefined,
        priority: (priority as PriorityLevel) || undefined,
        page,
        size: 10,
        sort: "createdAt,desc",
      }),
  });

  return (
    <div>
      <PageHeader
        title="Distributions"
        description="Requests for aid, from submission through allocation, approval and hand-over."
        actions={
          <Button onClick={() => setDialogOpen(true)}>
            <Plus className="h-4 w-4" />
            New request
          </Button>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search reference or beneficiary…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            />
          </div>
          <Select className="w-40" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="COMPLETED">Completed</option>
            <option value="REJECTED">Rejected</option>
            <option value="CANCELLED">Cancelled</option>
          </Select>
          <Select className="w-40" value={priority} onChange={(e) => { setPriority(e.target.value); setPage(0); }}>
            <option value="">All priorities</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={7} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={HandHeart} title="No distribution requests found" description="Create a new request to get started." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Reference</TH>
                <TH>Beneficiary</TH>
                <TH>Date</TH>
                <TH>Items</TH>
                <TH>Priority</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {data.items.map((d) => (
                <TR key={d.id} className="cursor-pointer" onClick={() => navigate(`/distributions/${d.id}`)}>
                  <TD className="font-medium text-ink-900">{d.requestReference}</TD>
                  <TD className="text-ink-600">{d.beneficiaryName}</TD>
                  <TD className="text-ink-500">{formatDate(d.requestDate)}</TD>
                  <TD className="text-ink-600">{d.totalItems}</TD>
                  <TD>
                    <Badge tone={PRIORITY_TONE[d.priority]} dot>
                      {PRIORITY_LABELS[d.priority]}
                    </Badge>
                  </TD>
                  <TD>
                    <Badge tone={STATUS_TONE[d.status]} dot>
                      {toTitleCase(d.status)}
                    </Badge>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <NewDistributionDialog
        open={dialogOpen}
        onClose={closeDialog}
        presetBeneficiaryId={newFor ? Number(newFor) : undefined}
      />
    </div>
  );
}
