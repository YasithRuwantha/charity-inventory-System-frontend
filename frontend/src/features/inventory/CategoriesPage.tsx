import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Search, Tags, MoreVertical, Pencil, Ban, CheckCircle2 } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Select, Textarea } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { Dialog } from "@/components/ui/Dialog";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { useAuthStore } from "@/store/auth";
import {
  listCategories,
  createCategory,
  updateCategory,
  updateCategoryStatus,
  type Category,
  type CategoryPayload,
} from "@/api/categories";
import { formatDate } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";

const schema = z.object({
  name: z.string().min(1, "Name is required").max(120, "Max 120 characters"),
  description: z.string().max(500, "Max 500 characters").optional(),
});
type FormValues = z.infer<typeof schema>;

export default function CategoriesPage() {
  const role = useAuthStore((s) => s.user?.role);
  const canManage = role === "ADMIN" || role === "INVENTORY_STAFF";

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [status, setStatus] = useState("");
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editing, setEditing] = useState<Category | null>(null);

  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["categories", { search: debouncedSearch, status, page }],
    queryFn: () =>
      listCategories({
        search: debouncedSearch || undefined,
        status: (status as "ACTIVE" | "INACTIVE") || undefined,
        page,
        size: 10,
      }),
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  const openCreate = () => {
    setEditing(null);
    reset({ name: "", description: "" });
    setDialogOpen(true);
  };

  const openEdit = (category: Category) => {
    setEditing(category);
    reset({ name: category.name, description: category.description ?? "" });
    setDialogOpen(true);
  };

  const saveMutation = useMutation({
    mutationFn: (payload: CategoryPayload) =>
      editing ? updateCategory(editing.id, payload) : createCategory(payload),
    onSuccess: () => {
      notifySuccess(editing ? "Category updated" : "Category created");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      setDialogOpen(false);
    },
    onError: (err) => notifyError(err),
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, next }: { id: number; next: "ACTIVE" | "INACTIVE" }) => updateCategoryStatus(id, next),
    onSuccess: () => {
      notifySuccess("Status updated");
      queryClient.invalidateQueries({ queryKey: ["categories"] });
    },
    onError: (err) => notifyError(err),
  });

  const onSubmit = (values: FormValues) => {
    saveMutation.mutate({ name: values.name, description: values.description || undefined });
  };

  return (
    <div>
      <PageHeader
        title="Categories"
        description="Organize inventory items into groups such as Food, Clothing or Medicine."
        actions={
          canManage && (
            <Button onClick={openCreate}>
              <Plus className="h-4 w-4" />
              New category
            </Button>
          )
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search categories…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <Select
            className="w-40"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
        </div>

        {isLoading ? (
          <TableSkeleton cols={4} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={Tags} title="No categories found" description="Try adjusting your search or filters." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Name</TH>
                <TH>Description</TH>
                <TH>Status</TH>
                <TH>Created</TH>
                {canManage && <TH className="w-10" />}
              </TR>
            </THead>
            <TBody>
              {data.items.map((category) => (
                <TR key={category.id}>
                  <TD className="font-medium text-ink-900">{category.name}</TD>
                  <TD className="max-w-xs truncate text-ink-500">{category.description || "—"}</TD>
                  <TD>
                    <Badge tone={category.status === "ACTIVE" ? "success" : "neutral"} dot>
                      {category.status === "ACTIVE" ? "Active" : "Inactive"}
                    </Badge>
                  </TD>
                  <TD className="text-ink-500">{formatDate(category.createdAt)}</TD>
                  {canManage && (
                    <TD>
                      <DropdownMenu
                        trigger={
                          <button className="rounded-md p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
                            <MoreVertical className="h-4 w-4" />
                          </button>
                        }
                      >
                        <DropdownItem icon={<Pencil className="h-4 w-4" />} onClick={() => openEdit(category)}>
                          Edit
                        </DropdownItem>
                        {category.status === "ACTIVE" ? (
                          <DropdownItem
                            icon={<Ban className="h-4 w-4" />}
                            danger
                            onClick={() => statusMutation.mutate({ id: category.id, next: "INACTIVE" })}
                          >
                            Deactivate
                          </DropdownItem>
                        ) : (
                          <DropdownItem
                            icon={<CheckCircle2 className="h-4 w-4" />}
                            onClick={() => statusMutation.mutate({ id: category.id, next: "ACTIVE" })}
                          >
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

      <Dialog
        open={dialogOpen}
        onClose={() => setDialogOpen(false)}
        title={editing ? "Edit category" : "New category"}
        footer={
          <>
            <Button variant="outline" onClick={() => setDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSubmit(onSubmit)} loading={isSubmitting || saveMutation.isPending}>
              {editing ? "Save changes" : "Create category"}
            </Button>
          </>
        }
      >
        <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
          <div>
            <Label htmlFor="name" required>
              Name
            </Label>
            <Input id="name" invalid={!!errors.name} {...register("name")} />
            <FieldError>{errors.name?.message}</FieldError>
          </div>
          <div>
            <Label htmlFor="description">Description</Label>
            <Textarea id="description" rows={3} invalid={!!errors.description} {...register("description")} />
            <FieldError>{errors.description?.message}</FieldError>
          </div>
        </form>
      </Dialog>
    </div>
  );
}
