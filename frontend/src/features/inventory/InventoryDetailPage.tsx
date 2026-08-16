import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Pencil, SlidersHorizontal, Package } from "lucide-react";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { getInventoryItem, listItemTransactions } from "@/api/inventory";
import { useAuthStore } from "@/store/auth";
import { UNIT_LABELS } from "@/types/enums";
import type { InventoryStatus } from "@/types/enums";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";
import { InventoryFormDialog } from "@/features/inventory/InventoryFormDialog";
import { AdjustStockDialog } from "@/features/inventory/AdjustStockDialog";

const STATUS_TONE: Record<InventoryStatus, BadgeTone> = {
  IN_STOCK: "success",
  LOW_STOCK: "warning",
  OUT_OF_STOCK: "danger",
  EXPIRED: "neutral",
};

export default function InventoryDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const role = useAuthStore((s) => s.user?.role);
  const canManage = role === "ADMIN" || role === "INVENTORY_STAFF";
  const itemId = Number(id);

  const [formOpen, setFormOpen] = useState(false);
  const [adjustOpen, setAdjustOpen] = useState(false);

  const { data: item, isLoading } = useQuery({
    queryKey: ["inventory", itemId],
    queryFn: () => getInventoryItem(itemId),
    enabled: Number.isFinite(itemId),
  });

  const { data: transactions, isLoading: txLoading } = useQuery({
    queryKey: ["inventory", itemId, "transactions"],
    queryFn: () => listItemTransactions(itemId, { size: 20, sort: "createdAt,desc" }),
    enabled: Number.isFinite(itemId) && canManage,
  });

  if (isLoading) return <PageSpinner />;
  if (!item) return <EmptyState icon={Package} title="Item not found" />;

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
            <h1 className="font-display text-xl font-bold text-ink-900">{item.itemName}</h1>
            <Badge tone={STATUS_TONE[item.status]} dot>
              {toTitleCase(item.status)}
            </Badge>
            {item.archived && <Badge tone="neutral">Archived</Badge>}
          </div>
          <p className="mt-1 text-sm text-ink-500">
            {item.itemCode} · {item.categoryName ?? "Uncategorized"}
          </p>
        </div>
        {canManage && (
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => setAdjustOpen(true)}>
              <SlidersHorizontal className="h-4 w-4" />
              Adjust stock
            </Button>
            <Button onClick={() => setFormOpen(true)}>
              <Pencil className="h-4 w-4" />
              Edit
            </Button>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <div className="grid grid-cols-2 gap-4 lg:col-span-1 lg:grid-cols-1">
          <Card>
            <CardBody className="text-center">
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Current stock</p>
              <p className="mt-1 font-display text-3xl font-bold text-ink-900">{item.quantity}</p>
              <p className="text-sm text-ink-500">{UNIT_LABELS[item.unit]}</p>
            </CardBody>
          </Card>
          <Card>
            <CardBody className="text-center">
              <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Minimum level</p>
              <p className="mt-1 font-display text-3xl font-bold text-ink-900">{item.minimumStockLevel}</p>
              <p className="text-sm text-ink-500">reorder threshold</p>
            </CardBody>
          </Card>
        </div>

        <div className="lg:col-span-2">
          <Card>
            <CardHeader title="Item details" />
            <CardBody>
              <dl className="grid grid-cols-1 gap-x-6 gap-y-4 sm:grid-cols-2">
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Category</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{item.categoryName ?? "—"}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Unit of measure</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{UNIT_LABELS[item.unit]}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Expiry date</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">
                    {formatDate(item.expiryDate)}
                    {item.daysUntilExpiry !== null && (
                      <span className="ml-1.5 text-xs text-ink-400">
                        ({item.daysUntilExpiry >= 0 ? `${item.daysUntilExpiry} days left` : "expired"})
                      </span>
                    )}
                  </dd>
                </div>
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Added by</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{item.createdBy?.name ?? "—"}</dd>
                </div>
                <div className="sm:col-span-2">
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Description</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{item.description || "No description provided."}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Created</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{formatDateTime(item.createdAt)}</dd>
                </div>
                <div>
                  <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Last updated</dt>
                  <dd className="mt-0.5 text-sm text-ink-800">{formatDateTime(item.updatedAt)}</dd>
                </div>
              </dl>
            </CardBody>
          </Card>
        </div>
      </div>

      {canManage && (
        <Card className="mt-4">
          <CardHeader title="Stock movement history" description="Every change to this item's quantity, oldest to newest reversed." />
          {txLoading ? (
            <CardBody>
              <div className="h-32 animate-pulse rounded-lg bg-ink-100" />
            </CardBody>
          ) : !transactions || transactions.items.length === 0 ? (
            <EmptyState title="No movements yet" description="Stock changes will appear here." />
          ) : (
            <Table>
              <THead>
                <TR>
                  <TH>Date</TH>
                  <TH>Type</TH>
                  <TH>Change</TH>
                  <TH>Before → After</TH>
                  <TH>By</TH>
                  <TH>Notes</TH>
                </TR>
              </THead>
              <TBody>
                {transactions.items.map((tx) => (
                  <TR key={tx.id}>
                    <TD className="text-ink-500">{formatDateTime(tx.createdAt)}</TD>
                    <TD>
                      <Badge tone={tx.quantity >= 0 ? "success" : "danger"}>{toTitleCase(tx.transactionType)}</Badge>
                    </TD>
                    <TD className={tx.quantity >= 0 ? "font-medium text-emerald-600" : "font-medium text-rose-600"}>
                      {tx.quantity >= 0 ? `+${tx.quantity}` : tx.quantity}
                    </TD>
                    <TD className="text-ink-500">
                      {tx.quantityBefore} → {tx.quantityAfter}
                    </TD>
                    <TD className="text-ink-600">{tx.performedBy?.name ?? "System"}</TD>
                    <TD className="max-w-[200px] truncate text-ink-500">{tx.notes || "—"}</TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </Card>
      )}

      {!canManage && (
        <p className="mt-4 text-center text-sm text-ink-400">
          Need to report an issue with this item? <Link to="/my-tasks" className="text-brand-700 hover:underline">Check your tasks</Link> or contact your coordinator.
        </p>
      )}

      <InventoryFormDialog open={formOpen} onClose={() => setFormOpen(false)} item={item} />
      <AdjustStockDialog open={adjustOpen} onClose={() => setAdjustOpen(false)} item={item} />
    </div>
  );
}
