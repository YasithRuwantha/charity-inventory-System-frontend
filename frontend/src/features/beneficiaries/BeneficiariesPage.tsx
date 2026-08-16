import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Search, Users, MoreVertical, Pencil, Ban, CheckCircle2 } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { listBeneficiaries, updateBeneficiaryStatus, type Beneficiary } from "@/api/beneficiaries";
import type { PriorityLevel } from "@/types/enums";
import { PRIORITY_LABELS } from "@/types/enums";
import { toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { BeneficiaryFormDialog } from "@/features/beneficiaries/BeneficiaryFormDialog";

const PRIORITY_TONE: Record<PriorityLevel, BadgeTone> = {
  HIGH: "danger",
  MEDIUM: "warning",
  LOW: "neutral",
};

export default function BeneficiariesPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [priority, setPriority] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Beneficiary | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["beneficiaries", { search: debouncedSearch, priority, status, page }],
    queryFn: () =>
      listBeneficiaries({
        search: debouncedSearch || undefined,
        priority: (priority as PriorityLevel) || undefined,
        status: (status as "ACTIVE" | "INACTIVE") || undefined,
        page,
        size: 10,
      }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: "ACTIVE" | "INACTIVE" }) => updateBeneficiaryStatus(id, next),
    onSuccess: () => {
      notifySuccess("Status updated");
      queryClient.invalidateQueries({ queryKey: ["beneficiaries"] });
    },
    onError: (err) => notifyError(err),
  });

  return (
    <div>
      <PageHeader
        title="Beneficiaries"
        description="People and families registered to receive aid, with distribution history and priority."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" />
            Register beneficiary
          </Button>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search name, code, ID, contact…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            />
          </div>
          <Select className="w-40" value={priority} onChange={(e) => { setPriority(e.target.value); setPage(0); }}>
            <option value="">All priorities</option>
            <option value="HIGH">High</option>
            <option value="MEDIUM">Medium</option>
            <option value="LOW">Low</option>
          </Select>
          <Select className="w-40" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={6} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={Users} title="No beneficiaries found" description="Try adjusting your search or filters." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Beneficiary</TH>
                <TH>Family size</TH>
                <TH>Contact</TH>
                <TH>Priority</TH>
                <TH>Status</TH>
                <TH className="w-10" />
              </TR>
            </THead>
            <TBody>
              {data.items.map((b) => (
                <TR key={b.id} className="cursor-pointer" onClick={() => navigate(`/beneficiaries/${b.id}`)}>
                  <TD>
                    <p className="font-medium text-ink-900">{b.beneficiaryName}</p>
                    <p className="text-xs text-ink-400">{b.beneficiaryCode}</p>
                  </TD>
                  <TD className="text-ink-600">{b.familySize}</TD>
                  <TD className="text-ink-500">{b.contactNumber || "—"}</TD>
                  <TD>
                    <Badge tone={PRIORITY_TONE[b.priorityLevel]} dot>
                      {PRIORITY_LABELS[b.priorityLevel]}
                    </Badge>
                  </TD>
                  <TD>
                    <Badge tone={b.status === "ACTIVE" ? "success" : "neutral"} dot>
                      {toTitleCase(b.status)}
                    </Badge>
                  </TD>
                  <TD onClick={(e) => e.stopPropagation()}>
                    <DropdownMenu
                      trigger={
                        <button className="rounded-md p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
                          <MoreVertical className="h-4 w-4" />
                        </button>
                      }
                    >
                      <DropdownItem
                        icon={<Pencil className="h-4 w-4" />}
                        onClick={() => {
                          setEditing(b);
                          setFormOpen(true);
                        }}
                      >
                        Edit
                      </DropdownItem>
                      {b.status === "ACTIVE" ? (
                        <DropdownItem
                          icon={<Ban className="h-4 w-4" />}
                          danger
                          onClick={() => statusMutation.mutate({ id: b.id, next: "INACTIVE" })}
                        >
                          Deactivate
                        </DropdownItem>
                      ) : (
                        <DropdownItem
                          icon={<CheckCircle2 className="h-4 w-4" />}
                          onClick={() => statusMutation.mutate({ id: b.id, next: "ACTIVE" })}
                        >
                          Activate
                        </DropdownItem>
                      )}
                    </DropdownMenu>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <BeneficiaryFormDialog open={formOpen} onClose={() => setFormOpen(false)} beneficiary={editing} />
    </div>
  );
}
