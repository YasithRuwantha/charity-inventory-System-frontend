import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Search, Gift, MoreVertical, Pencil, Ban, CheckCircle2 } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { listDonors, updateDonorStatus, type Donor } from "@/api/donors";
import type { DonorType } from "@/types/enums";
import { toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { DonorFormDialog } from "@/features/donations/DonorFormDialog";

export default function DonorsPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [donorType, setDonorType] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Donor | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["donors", { search: debouncedSearch, donorType, status, page }],
    queryFn: () =>
      listDonors({
        search: debouncedSearch || undefined,
        donorType: (donorType as DonorType) || undefined,
        status: (status as "ACTIVE" | "INACTIVE") || undefined,
        page,
        size: 10,
      }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: "ACTIVE" | "INACTIVE" }) => updateDonorStatus(id, next),
    onSuccess: () => {
      notifySuccess("Status updated");
      queryClient.invalidateQueries({ queryKey: ["donors"] });
    },
    onError: (err) => notifyError(err),
  });

  return (
    <div>
      <PageHeader
        title="Donors"
        description="Individuals and organizations that contribute items to your inventory."
        actions={
          <Button
            onClick={() => {
              setEditing(null);
              setFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" />
            New donor
          </Button>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search name, code, email…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <Select className="w-44" value={donorType} onChange={(e) => { setDonorType(e.target.value); setPage(0); }}>
            <option value="">All types</option>
            <option value="INDIVIDUAL">Individual</option>
            <option value="ORGANIZATION">Organization</option>
            <option value="ANONYMOUS">Anonymous</option>
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
          <EmptyState icon={Gift} title="No donors found" description="Try adjusting your search or filters." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Donor</TH>
                <TH>Type</TH>
                <TH>Phone</TH>
                <TH>Email</TH>
                <TH>Status</TH>
                <TH className="w-10" />
              </TR>
            </THead>
            <TBody>
              {data.items.map((donor) => (
                <TR key={donor.id} className="cursor-pointer" onClick={() => navigate(`/donors/${donor.id}`)}>
                  <TD>
                    <p className="font-medium text-ink-900">{donor.donorName}</p>
                    <p className="text-xs text-ink-400">{donor.donorCode}</p>
                  </TD>
                  <TD className="text-ink-600">{toTitleCase(donor.donorType)}</TD>
                  <TD className="text-ink-500">{donor.phone || "—"}</TD>
                  <TD className="text-ink-500">{donor.email || "—"}</TD>
                  <TD>
                    <Badge tone={donor.status === "ACTIVE" ? "success" : "neutral"} dot>
                      {toTitleCase(donor.status)}
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
                          setEditing(donor);
                          setFormOpen(true);
                        }}
                      >
                        Edit
                      </DropdownItem>
                      {donor.status === "ACTIVE" ? (
                        <DropdownItem
                          icon={<Ban className="h-4 w-4" />}
                          danger
                          onClick={() => statusMutation.mutate({ id: donor.id, next: "INACTIVE" })}
                        >
                          Deactivate
                        </DropdownItem>
                      ) : (
                        <DropdownItem
                          icon={<CheckCircle2 className="h-4 w-4" />}
                          onClick={() => statusMutation.mutate({ id: donor.id, next: "ACTIVE" })}
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

      <DonorFormDialog open={formOpen} onClose={() => setFormOpen(false)} donor={editing} />
    </div>
  );
}
