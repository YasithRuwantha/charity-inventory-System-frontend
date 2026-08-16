import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Search, Gift, Upload } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Select } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { listDonations } from "@/api/donations";
import type { DonationStatus } from "@/types/enums";
import { formatDate, toTitleCase } from "@/lib/utils";
import { NewDonationDialog } from "@/features/donations/NewDonationDialog";

export default function DonationsPage() {
  const navigate = useNavigate();
  const [search, setSearch] = useState("");
  const debouncedSearch = useDebouncedValue(search);
  const [status, setStatus] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);

  const { data, isLoading } = useQuery({
    queryKey: ["donations", { search: debouncedSearch, status, startDate, endDate, page }],
    queryFn: () =>
      listDonations({
        search: debouncedSearch || undefined,
        status: (status as DonationStatus) || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 10,
        sort: "donationDate,desc",
      }),
  });

  return (
    <div>
      <PageHeader
        title="Donations"
        description="Every donation received, with the items and quantities recorded against it."
        actions={
          <>
            <Button variant="outline" onClick={() => navigate("/donations/bulk")}>
              <Upload className="h-4 w-4" />
              Bulk import
            </Button>
            <Button onClick={() => setDialogOpen(true)}>
              <Plus className="h-4 w-4" />
              Record donation
            </Button>
          </>
        }
      />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <div className="w-full max-w-xs">
            <Input
              placeholder="Search reference or donor…"
              leadingIcon={<Search className="h-4 w-4" />}
              value={search}
              onChange={(e) => { setSearch(e.target.value); setPage(0); }}
            />
          </div>
          <Select className="w-40" value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
            <option value="">All statuses</option>
            <option value="RECEIVED">Received</option>
            <option value="CANCELLED">Cancelled</option>
          </Select>
          <Input type="date" className="w-40" value={startDate} onChange={(e) => { setStartDate(e.target.value); setPage(0); }} />
          <span className="text-sm text-ink-400">to</span>
          <Input type="date" className="w-40" value={endDate} onChange={(e) => { setEndDate(e.target.value); setPage(0); }} />
        </div>

        {isLoading ? (
          <TableSkeleton cols={6} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={Gift} title="No donations found" description="Record your first donation to get started." />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>Reference</TH>
                <TH>Donor</TH>
                <TH>Date</TH>
                <TH>Items</TH>
                <TH>Quantity</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {data.items.map((donation) => (
                <TR key={donation.id} className="cursor-pointer" onClick={() => navigate(`/donations/${donation.id}`)}>
                  <TD className="font-medium text-ink-900">{donation.donationReference}</TD>
                  <TD className="text-ink-600">{donation.donorName}</TD>
                  <TD className="text-ink-500">{formatDate(donation.donationDate)}</TD>
                  <TD className="text-ink-600">{donation.totalItems}</TD>
                  <TD className="text-ink-600">{donation.totalQuantity}</TD>
                  <TD>
                    <Badge tone={donation.status === "RECEIVED" ? "success" : "neutral"} dot>
                      {toTitleCase(donation.status)}
                    </Badge>
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>

      <NewDonationDialog open={dialogOpen} onClose={() => setDialogOpen(false)} />
    </div>
  );
}
