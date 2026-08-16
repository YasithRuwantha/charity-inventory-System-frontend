import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Search, HardHat, MoreVertical, Pencil, Ban, CheckCircle2 } from "lucide-react";
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
import { useAuthStore } from "@/store/auth";
import { listVolunteers, updateVolunteerStatus, type Volunteer } from "@/api/volunteers";
import { toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { VolunteerFormDialog } from "@/features/volunteers/VolunteerFormDialog";

export default function VolunteersPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const role = useAuthStore((s) => s.user?.role);
  const isAdmin = role === "ADMIN";

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Volunteer | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["volunteers", { search: debouncedSearch, status, page }],
    queryFn: () =>
      listVolunteers({
        search: debouncedSearch || undefined,
        status: (status as "ACTIVE" | "INACTIVE") || undefined,
        page,
        size: 10,
      }),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: "ACTIVE" | "INACTIVE" }) => updateVolunteerStatus(id, next),
    onSuccess: () => {
      notifySuccess("Status updated");
      queryClient.invalidateQueries({ queryKey: ["volunteers"] });
    },
    onError: (err) => notifyError(err),
  });

  return (
    <div>
      <PageHeader
        title="Volunteers"
        description="People helping coordinate donations and distributions."
        actions={
          isAdmin && (
            <Button
              onClick={() => {
                setEditing(null);
                setFormOpen(true);
              }}
            >
              <Plus className="h-4 w-4" />
              Register volunteer
            </Button>
          )
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search name, code, email…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            />
          </div>
          <Select className="w-40" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={5} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={HardHat} title="No volunteers found" />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Volunteer</TH>
                <TH>Phone</TH>
                <TH>Email</TH>
                <TH>Status</TH>
                {isAdmin && <TH className="w-10" />}
              </TR>
            </THead>
            <TBody>
              {data.items.map((v) => (
                <TR key={v.id} className="cursor-pointer" onClick={() => navigate(`/volunteers/${v.id}`)}>
                  <TD>
                    <p className="font-medium text-ink-900">{v.volunteerName}</p>
                    <p className="text-xs text-ink-400">{v.volunteerCode}</p>
                  </TD>
                  <TD className="text-ink-500">{v.phone || "—"}</TD>
                  <TD className="text-ink-500">{v.email || "—"}</TD>
                  <TD>
                    <Badge tone={v.status === "ACTIVE" ? "success" : "neutral"} dot>
                      {toTitleCase(v.status)}
                    </Badge>
                  </TD>
                  {isAdmin && (
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
                            setEditing(v);
                            setFormOpen(true);
                          }}
                        >
                          Edit
                        </DropdownItem>
                        {v.status === "ACTIVE" ? (
                          <DropdownItem icon={<Ban className="h-4 w-4" />} danger onClick={() => statusMutation.mutate({ id: v.id, next: "INACTIVE" })}>
                            Deactivate
                          </DropdownItem>
                        ) : (
                          <DropdownItem icon={<CheckCircle2 className="h-4 w-4" />} onClick={() => statusMutation.mutate({ id: v.id, next: "ACTIVE" })}>
                            Activate
                          </DropdownItem>
                        )}
                      </DropdownMenu>
                    </TD>
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <VolunteerFormDialog open={formOpen} onClose={() => setFormOpen(false)} volunteer={editing} />
    </div>
  );
}
