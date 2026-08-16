import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Search, UserCog, MoreVertical, Ban, CheckCircle2, ShieldCheck } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Input, Select } from "@/components/ui/Input";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { ConfirmDialog } from "@/components/ui/Dialog";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { useAuthStore } from "@/store/auth";
import { listUsers, updateUserRole, updateUserStatus, type AppUser } from "@/api/users";
import type { UserRole } from "@/types/enums";
import { toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { ApiRequestError } from "@/api/types";

const ROLE_TONE: Record<UserRole, BadgeTone> = { ADMIN: "brand", INVENTORY_STAFF: "info", VOLUNTEER: "neutral" };

export default function UsersPage() {
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((s) => s.user);
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [statusTarget, setStatusTarget] = useState<AppUser | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ["users", { search: debouncedSearch, role, status, page }],
    queryFn: () =>
      listUsers({
        search: debouncedSearch || undefined,
        role: (role as UserRole) || undefined,
        status: (status as "ACTIVE" | "INACTIVE") || undefined,
        page,
        size: 10,
      }),
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: UserRole }) => updateUserRole(id, next),
    onSuccess: () => {
      notifySuccess("Role updated");
      queryClient.invalidateQueries({ queryKey: ["users"] });
    },
    onError: (err) => {
      if (err instanceof ApiRequestError && err.errorCode === "LAST_ADMIN_PROTECTED") {
        notifyError(err, "This is the last administrator — assign another admin first.");
      } else {
        notifyError(err);
      }
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: "ACTIVE" | "INACTIVE" }) => updateUserStatus(id, next),
    onSuccess: () => {
      notifySuccess("Status updated");
      queryClient.invalidateQueries({ queryKey: ["users"] });
      setStatusTarget(null);
    },
    onError: (err) => {
      if (err instanceof ApiRequestError && err.errorCode === "LAST_ADMIN_PROTECTED") {
        notifyError(err, "This is the last administrator — assign another admin first.");
      } else {
        notifyError(err);
      }
      setStatusTarget(null);
    },
  });

  return (
    <div>
      <PageHeader title="Users" description="Everyone with a login to this system, and their assigned role." />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search name or email…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            />
          </div>
          <Select className="w-44" value={role} onChange={(e) => { setRole(e.target.value); setPage(0); }}>
            <option value="">All roles</option>
            <option value="ADMIN">Administrator</option>
            <option value="INVENTORY_STAFF">Inventory staff</option>
            <option value="VOLUNTEER">Volunteer</option>
          </Select>
          <Select className="w-40" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={4} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={UserCog} title="No users found" />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>User</TH>
                <TH>Role</TH>
                <TH>Status</TH>
                <TH className="w-10" />
              </TR>
            </THead>
            <TBody>
              {data.items.map((user) => {
                const isSelf = user.email === currentUser?.email;
                return (
                  <TR key={user.id}>
                    <TD>
                      <p className="font-medium text-ink-900">
                        {user.name} {isSelf && <span className="text-xs font-normal text-ink-400">(you)</span>}
                      </p>
                      <p className="text-xs text-ink-400">{user.email}</p>
                    </TD>
                    <TD>
                      <Badge tone={ROLE_TONE[user.role]} dot>
                        {toTitleCase(user.role)}
                      </Badge>
                    </TD>
                    <TD>
                      <Badge tone={user.status === "ACTIVE" ? "success" : "neutral"} dot>
                        {toTitleCase(user.status)}
                      </Badge>
                    </TD>
                    <TD>
                      <DropdownMenu
                        trigger={
                          <button className="rounded-md p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
                            <MoreVertical className="h-4 w-4" />
                          </button>
                        }
                      >
                        {(["ADMIN", "INVENTORY_STAFF", "VOLUNTEER"] as UserRole[])
                          .filter((r) => r !== user.role)
                          .map((r) => (
                            <DropdownItem
                              key={r}
                              icon={<ShieldCheck className="h-4 w-4" />}
                              onClick={() => roleMutation.mutate({ id: user.id, next: r })}
                            >
                              Make {toTitleCase(r)}
                            </DropdownItem>
                          ))}
                        <div className="my-1 border-t border-ink-100" />
                        {user.status === "ACTIVE" ? (
                          <DropdownItem icon={<Ban className="h-4 w-4" />} danger onClick={() => setStatusTarget(user)}>
                            Deactivate
                          </DropdownItem>
                        ) : (
                          <DropdownItem
                            icon={<CheckCircle2 className="h-4 w-4" />}
                            onClick={() => statusMutation.mutate({ id: user.id, next: "ACTIVE" })}
                          >
                            Activate
                          </DropdownItem>
                        )}
                      </DropdownMenu>
                    </TD>
                  </TR>
                );
              })}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <ConfirmDialog
        open={!!statusTarget}
        onClose={() => setStatusTarget(null)}
        onConfirm={() => statusTarget && statusMutation.mutate({ id: statusTarget.id, next: "INACTIVE" })}
        title="Deactivate this user?"
        description={`${statusTarget?.name} will no longer be able to sign in.`}
        confirmLabel="Deactivate"
        danger
        loading={statusMutation.isPending}
      />
    </div>
  );
}
