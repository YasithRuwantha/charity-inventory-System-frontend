import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeftRight } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Select, Input, Label } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { InventoryItemPicker } from "@/components/shared/InventoryItemPicker";
import { listAllTransactions } from "@/api/inventoryTransactions";
import type { InventoryItem } from "@/api/inventory";
import type { TransactionType } from "@/types/enums";
import { formatDateTime, toTitleCase } from "@/lib/utils";

export default function LedgerPage() {
  const [item, setItem] = useState<InventoryItem | null>(null);
  const [transactionType, setTransactionType] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["inventory-transactions", { itemId: item?.id, transactionType, startDate, endDate, page }],
    queryFn: () =>
      listAllTransactions({
        itemId: item?.id,
        transactionType: (transactionType as TransactionType) || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 15,
        sort: "createdAt,desc",
      }),
  });

  return (
    <div>
      <PageHeader title="Stock ledger" description="Immutable, chronological record of every inventory movement." />

      <Card>
        <div className="grid grid-cols-1 gap-3 border-b border-ink-100 px-5 py-4 sm:grid-cols-2 lg:grid-cols-4">
          <div>
            <Label>Item</Label>
            <InventoryItemPicker value={item} onChange={(v) => { setItem(v); setPage(0); }} />
          </div>
          <div>
            <Label>Movement type</Label>
            <Select
              value={transactionType}
              onChange={(e) => {
                setTransactionType(e.target.value);
                setPage(0);
              }}
            >
              <option value="">All types</option>
              <option value="DONATION_IN">Donation in</option>
              <option value="DISTRIBUTION_OUT">Distribution out</option>
              <option value="MANUAL_ADJUSTMENT">Manual adjustment</option>
              <option value="CORRECTION">Correction</option>
            </Select>
          </div>
          <div>
            <Label>From</Label>
            <Input type="date" value={startDate} onChange={(e) => { setStartDate(e.target.value); setPage(0); }} />
          </div>
          <div>
            <Label>To</Label>
            <Input type="date" value={endDate} onChange={(e) => { setEndDate(e.target.value); setPage(0); }} />
          </div>
        </div>

        {isLoading ? (
          <TableSkeleton cols={6} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={ArrowLeftRight} title="No movements found" description="Try adjusting your filters." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Date</TH>
                <TH>Item</TH>
                <TH>Type</TH>
                <TH>Change</TH>
                <TH>Before → After</TH>
                <TH>By</TH>
              </TR>
            </THead>
            <TBody>
              {data.items.map((tx) => (
                <TR key={tx.id}>
                  <TD className="text-ink-500">{formatDateTime(tx.createdAt)}</TD>
                  <TD>
                    <p className="font-medium text-ink-800">{tx.itemName}</p>
                    <p className="text-xs text-ink-400">{tx.itemCode}</p>
                  </TD>
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
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>
    </div>
  );
}
