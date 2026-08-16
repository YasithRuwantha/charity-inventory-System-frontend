import { useState } from "react";
import { useParams, useNavigate, Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Pencil, Gift, Package } from "lucide-react";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { getDonor, getDonorStatistics } from "@/api/donors";
import { formatDate, formatNumber, toTitleCase } from "@/lib/utils";
import { DonorFormDialog } from "@/features/donations/DonorFormDialog";

export default function DonorDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const donorId = Number(id);
  const [formOpen, setFormOpen] = useState(false);

  const { data: donor, isLoading } = useQuery({
    queryKey: ["donors", donorId],
    queryFn: () => getDonor(donorId),
    enabled: Number.isFinite(donorId),
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ["donors", donorId, "statistics"],
    queryFn: () => getDonorStatistics(donorId),
    enabled: Number.isFinite(donorId),
  });

  if (isLoading) return <PageSpinner />;
  if (!donor) return <EmptyState icon={Gift} title="Donor not found" />;

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
            <h1 className="font-display text-xl font-bold text-ink-900">{donor.donorName}</h1>
            <Badge tone={donor.status === "ACTIVE" ? "success" : "neutral"} dot>
              {toTitleCase(donor.status)}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-ink-500">
            {donor.donorCode} · {toTitleCase(donor.donorType)}
          </p>
        </div>
        <Button onClick={() => setFormOpen(true)}>
          <Pencil className="h-4 w-4" />
          Edit
        </Button>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Donations</p>
            <p className="mt-1 font-display text-2xl font-bold text-ink-900">
              {statsLoading ? "—" : formatNumber(stats?.totalDonations)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Item lines</p>
            <p className="mt-1 font-display text-2xl font-bold text-ink-900">
              {statsLoading ? "—" : formatNumber(stats?.totalItemLines)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Quantity donated</p>
            <p className="mt-1 font-display text-2xl font-bold text-ink-900">
              {statsLoading ? "—" : formatNumber(stats?.totalQuantityDonated)}
            </p>
          </CardBody>
        </Card>
        <Card>
          <CardBody className="text-center">
            <p className="text-xs font-medium uppercase tracking-wide text-ink-400">Last donation</p>
            <p className="mt-1 font-display text-lg font-bold text-ink-900">
              {statsLoading ? "—" : formatDate(stats?.lastDonationDate)}
            </p>
          </CardBody>
        </Card>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader title="Contact details" />
          <CardBody>
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Phone</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{donor.phone || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Email</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{donor.email || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Address</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{donor.address || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Notes</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{donor.notes || "—"}</dd>
              </div>
            </dl>
          </CardBody>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Recent donations" />
          {!stats || stats.recentDonations.length === 0 ? (
            <EmptyState icon={Package} title="No donations yet" description="Donations from this donor will appear here." />
          ) : (
            <Table>
              <THead>
                <TR>
                  <TH>Reference</TH>
                  <TH>Date</TH>
                  <TH>Items</TH>
                  <TH>Quantity</TH>
                </TR>
              </THead>
              <TBody>
                {stats.recentDonations.map((d) => (
                  <TR key={d.id} className="cursor-pointer" onClick={() => navigate(`/donations/${d.id}`)}>
                    <TD>
                      <Link to={`/donations/${d.id}`} className="font-medium text-brand-700 hover:underline">
                        {d.donationReference}
                      </Link>
                    </TD>
                    <TD className="text-ink-500">{formatDate(d.donationDate)}</TD>
                    <TD className="text-ink-600">{d.totalItems}</TD>
                    <TD className="text-ink-600">{d.totalQuantity}</TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </Card>
      </div>

      <DonorFormDialog open={formOpen} onClose={() => setFormOpen(false)} donor={donor} />
    </div>
  );
}
