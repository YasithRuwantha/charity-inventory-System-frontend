import { useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearchParams } from "react-router-dom";
import { Plus, Search, Package, MoreVertical, Pencil, Archive, ArchiveRestore, SlidersHorizontal } from "lucide-react";
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
import { useAuthStore } from "@/store/auth";
import { listCategories } from "@/api/categories";
import { archiveInventoryItem, listInventory, restoreInventoryItem, type InventoryItem } from "@/api/inventory";
import type { InventoryStatus } from "@/types/enums";
import { UNIT_LABELS } from "@/types/enums";
import { formatDate, toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { InventoryFormDialog } from "@/features/inventory/InventoryFormDialog";
import { AdjustStockDialog } from "@/features/inventory/AdjustStockDialog";
import { ConfirmDialog } from "@/components/ui/Dialog";

const STATUS_TONE: Record<InventoryStatus, BadgeTone> = {
  IN_STOCK: "success",
  LOW_STOCK: "warning",
  OUT_OF_STOCK: "danger",
  EXPIRED: "neutral",
};

export default function InventoryListPage() {
  const role = useAuthStore((s) => s.user?.role);
  const canManage = role === "ADMIN" || role === "INVENTORY_STAFF";
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams, setSearchParams] = useSearchParams();

  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [categoryId, setCategoryId] = useState("");
  const [status, setStatus] = useState(searchParams.get("status") ?? "");
  const [includeArchived, setIncludeArchived] = useState(false);
  const [page, setPage] = useState(0);

  const [formOpen, setFormOpen] = useState(false);
  const [editingItem, setEditingItem] = useState<InventoryItem | null>(null);
  const [adjustItem, setAdjustItem] = useState<InventoryItem | null>(null);
  const [archiveTarget, setArchiveTarget] = useState<InventoryItem | null>(null);

  const { data: categories } = useQuery({
    queryKey: ["categories", "all-active"],
    queryFn: () => listCategories({ status: "ACTIVE", size: 100 }),
  });

  const { data, isLoading } = useQuery({
    queryKey: ["inventory", { search: debouncedSearch, categoryId, status, includeArchived, page }],
    queryFn: () =>
      listInventory({
        search: debouncedSearch || undefined,
        categoryId: categoryId ? Number(categoryId) : undefined,
        status: (status as InventoryStatus) || undefined,
        includeArchived,
        page,
        size: 10,
      }),
  });

  const archiveMutation = useMutation({
    mutationFn: (item: InventoryItem) => (item.archived ? restoreInventoryItem(item.id) : archiveInventoryItem(item.id)),
    onSuccess: (_res, item) => {
      notifySuccess(item.archived ? "Item restored" : "Item archived");
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      setArchiveTarget(null);
    },
    onError: (err) => notifyError(err),
  });

  const updateStatusFilter = (value: string) => {
    setStatus(value);
    setPage(0);
    const next = new URLSearchParams(searchParams);
    if (value) next.set("status", value);
    else next.delete("status");
    setSearchParams(next, { replace: true });
  };

  return (
    <div>
      <PageHeader
        title="Inventory"
        description="All items currently tracked across categories, with live stock and expiry status."
        actions={
          canManage && (
            <Button
              onClick={() => {
                setEditingItem(null);
                setFormOpen(true);
              }}
            >
              <Plus className="h-4 w-4" />
              New item
            </Button>
          )
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search by name or item code…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
            />
          </div>
          <Select
            className="w-44"
            value={categoryId}
            onChange={(e) => {
              setCategoryId(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All categories</option>
            {categories?.items.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
          <Select className="w-40" value={status} onChange={(e) => updateStatusFilter(e.target.value)}>
            <option value="">All statuses</option>
            <option value="IN_STOCK">In stock</option>
            <option value="LOW_STOCK">Low stock</option>
            <option value="OUT_OF_STOCK">Out of stock</option>
            <option value="EXPIRED">Expired</option>
          </Select>
          {canManage && (
            <label className="ml-auto flex items-center gap-2 text-sm text-ink-600">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
                checked={includeArchived}
                onChange={(e) => {
                  setIncludeArchived(e.target.checked);
                  setPage(0);
                }}
              />
              Show archived
            </label>
          )}
        </div>

        {isLoading ? (
          <TableSkeleton cols={7} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState
            icon={Package}
            title="No inventory items found"
            description="Try adjusting your filters, or add a new item to get started."
          />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Item</TH>
                <TH>Category</TH>
                <TH>Quantity</TH>
                <TH>Min. level</TH>
                <TH>Expiry</TH>
                <TH>Status</TH>
                <TH className="w-10" />
              </TR>
            </THead>
            <TBody>
              {data.items.map((item) => (
                <TR key={item.id} className="cursor-pointer" onClick={() => navigate(`/inventory/${item.id}`)}>
                  <TD>
                    <p className="font-medium text-ink-900">{item.itemName}</p>
                    <p className="text-xs text-ink-400">{item.itemCode}</p>
                  </TD>
                  <TD className="text-ink-600">{item.categoryName ?? "—"}</TD>
                  <TD className="text-ink-700">
                    {item.quantity} <span className="text-ink-400">{UNIT_LABELS[item.unit]}</span>
                  </TD>
                  <TD className="text-ink-500">{item.minimumStockLevel}</TD>
                  <TD className="text-ink-500">
                    {formatDate(item.expiryDate)}
                    {item.daysUntilExpiry !== null && item.daysUntilExpiry >= 0 && item.daysUntilExpiry <= 30 && (
                      <span className="ml-1.5 text-xs text-amber-600">({item.daysUntilExpiry}d)</span>
                    )}
                  </TD>
                  <TD>
                    <div className="flex items-center gap-1.5">
                      <Badge tone={STATUS_TONE[item.status]} dot>
                        {toTitleCase(item.status)}
                      </Badge>
                      {item.archived && <Badge tone="neutral">Archived</Badge>}
                    </div>
                  </TD>
                  {canManage ? (
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
                            setEditingItem(item);
                            setFormOpen(true);
                          }}
                        >
                          Edit details
                        </DropdownItem>
                        <DropdownItem icon={<SlidersHorizontal className="h-4 w-4" />} onClick={() => setAdjustItem(item)}>
                          Adjust stock
                        </DropdownItem>
                        {item.archived ? (
                          <DropdownItem icon={<ArchiveRestore className="h-4 w-4" />} onClick={() => setArchiveTarget(item)}>
                            Restore item
                          </DropdownItem>
                        ) : (
                          <DropdownItem icon={<Archive className="h-4 w-4" />} danger onClick={() => setArchiveTarget(item)}>
                            Archive item
                          </DropdownItem>
                        )}
                      </DropdownMenu>
                    </TD>
                  ) : (
                    <TD />
                  )}
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <InventoryFormDialog open={formOpen} onClose={() => setFormOpen(false)} item={editingItem} />
      <AdjustStockDialog open={!!adjustItem} onClose={() => setAdjustItem(null)} item={adjustItem} />
      <ConfirmDialog
        open={!!archiveTarget}
        onClose={() => setArchiveTarget(null)}
        onConfirm={() => archiveTarget && archiveMutation.mutate(archiveTarget)}
        title={archiveTarget?.archived ? "Restore this item?" : "Archive this item?"}
        description={
          archiveTarget?.archived
            ? "The item will reappear in active inventory lists."
            : "Archived items are hidden from active lists but their history is preserved. You can restore it later."
        }
        confirmLabel={archiveTarget?.archived ? "Restore" : "Archive"}
        danger={!archiveTarget?.archived}
        loading={archiveMutation.isPending}
      />
    </div>
  );
}
