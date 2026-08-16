import { useParams, useNavigate, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Printer, Gift } from "lucide-react";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { getDonation, getDonationReceipt } from "@/api/donations";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";
import { UNIT_LABELS } from "@/types/enums";

export default function DonationDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const donationId = Number(id);

  const { data: donation, isLoading } = useQuery({
    queryKey: ["donations", donationId],
    queryFn: () => getDonation(donationId),
    enabled: Number.isFinite(donationId),
  });

  const { data: receipt } = useQuery({
    queryKey: ["donations", donationId, "receipt"],
    queryFn: () => getDonationReceipt(donationId),
    enabled: Number.isFinite(donationId),
  });

  if (isLoading) return <PageSpinner />;
  if (!donation) return <EmptyState icon={Gift} title="Donation not found" />;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between print:hidden">
        <button
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800"
        >
          <ArrowLeft className="h-4 w-4" /> Back
        </button>
        <Button variant="outline" onClick={() => window.print()}>
          <Printer className="h-4 w-4" />
          Print receipt
        </Button>
      </div>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="font-display text-xl font-bold text-ink-900">{donation.donationReference}</h1>
            <Badge tone={donation.status === "RECEIVED" ? "success" : "neutral"} dot>
              {toTitleCase(donation.status)}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-ink-500">Received {formatDate(donation.donationDate)}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card>
          <CardHeader title="Donor" />
          <CardBody>
            <Link to={`/donors/${donation.donorId}`} className="font-medium text-brand-700 hover:underline">
              {donation.donorName}
            </Link>
            <p className="mt-0.5 text-sm text-ink-400">{donation.donorCode}</p>
            {receipt && (
              <dl className="mt-4 space-y-2 text-sm">
                {receipt.donorPhone && (
                  <div className="flex justify-between">
                    <dt className="text-ink-400">Phone</dt>
                    <dd className="text-ink-700">{receipt.donorPhone}</dd>
                  </div>
                )}
                {receipt.donorEmail && (
                  <div className="flex justify-between">
                    <dt className="text-ink-400">Email</dt>
                    <dd className="text-ink-700">{receipt.donorEmail}</dd>
                  </div>
                )}
              </dl>
            )}
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Summary" />
          <CardBody>
            <dl className="space-y-2 text-sm">
              <div className="flex justify-between">
                <dt className="text-ink-400">Total items</dt>
                <dd className="font-medium text-ink-800">{donation.totalItems}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-400">Total quantity</dt>
                <dd className="font-medium text-ink-800">{donation.totalQuantity}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-400">Received by</dt>
                <dd className="font-medium text-ink-800">{donation.receivedBy?.name ?? "—"}</dd>
              </div>
              <div className="flex justify-between">
                <dt className="text-ink-400">Recorded</dt>
                <dd className="font-medium text-ink-800">{formatDateTime(donation.createdAt)}</dd>
              </div>
            </dl>
          </CardBody>
        </Card>

        <Card>
          <CardHeader title="Notes" />
          <CardBody>
            <p className="text-sm text-ink-700">{donation.notes || "No notes recorded."}</p>
          </CardBody>
        </Card>
      </div>

      <Card className="mt-4">
        <CardHeader title="Donated items" />
        <Table>
          <THead>
            <TR>
              <TH>Item</TH>
              <TH>Category</TH>
              <TH>Quantity</TH>
              <TH>Batch expiry</TH>
              <TH>Notes</TH>
            </TR>
          </THead>
          <TBody>
            {donation.items.map((item) => (
              <TR key={item.id}>
                <TD>
                  <p className="font-medium text-ink-900">{item.itemName}</p>
                  <p className="text-xs text-ink-400">{item.itemCode}</p>
                </TD>
                <TD className="text-ink-600">{item.categoryName ?? "—"}</TD>
                <TD className="text-ink-700">
                  {item.quantity} {UNIT_LABELS[item.unit]}
                </TD>
                <TD className="text-ink-500">{formatDate(item.expiryDate)}</TD>
                <TD className="max-w-[220px] truncate text-ink-500">{item.notes || "—"}</TD>
              </TR>
            ))}
          </TBody>
        </Table>
      </Card>
    </div>
  );
}
