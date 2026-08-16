import { useParams, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, Printer, HandHeart } from "lucide-react";
import { Card, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { getDistributionReport } from "@/api/distributions";
import { UNIT_LABELS } from "@/types/enums";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";

export default function DistributionReportPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const distId = Number(id);

  const { data: report, isLoading } = useQuery({
    queryKey: ["distributions", distId, "report"],
    queryFn: () => getDistributionReport(distId),
    enabled: Number.isFinite(distId),
  });

  if (isLoading) return <PageSpinner />;
  if (!report) return <EmptyState icon={HandHeart} title="Report not available" />;

  const d = report.distribution;

  return (
    <div>
      <div className="mb-4 flex items-center justify-between print:hidden">
        <button onClick={() => navigate(-1)} className="inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800">
          <ArrowLeft className="h-4 w-4" /> Back
        </button>
        <Button variant="outline" onClick={() => window.print()}>
          <Printer className="h-4 w-4" />
          Print
        </Button>
      </div>

      <Card>
        <CardBody>
          <div className="border-b border-ink-100 pb-4 text-center">
            <h1 className="font-display text-lg font-bold text-ink-900">{report.organizationName}</h1>
            {report.organizationAddress && <p className="text-sm text-ink-500">{report.organizationAddress}</p>}
            {report.organizationContact && <p className="text-sm text-ink-500">{report.organizationContact}</p>}
            <p className="mt-2 text-sm font-medium text-ink-700">Distribution Report — {d.requestReference}</p>
          </div>

          <div className="grid grid-cols-1 gap-6 py-5 sm:grid-cols-2">
            <div>
              <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-ink-400">Beneficiary</p>
              <p className="text-sm text-ink-800">{d.beneficiaryName} ({d.beneficiaryCode})</p>
              {report.beneficiaryIdentificationNumber && <p className="text-sm text-ink-500">ID: {report.beneficiaryIdentificationNumber}</p>}
              {report.beneficiaryContactNumber && <p className="text-sm text-ink-500">{report.beneficiaryContactNumber}</p>}
              {report.beneficiaryAddress && <p className="text-sm text-ink-500">{report.beneficiaryAddress}</p>}
              {report.beneficiaryFamilySize && <p className="text-sm text-ink-500">Family size: {report.beneficiaryFamilySize}</p>}
            </div>
            <div>
              <p className="mb-1.5 text-xs font-semibold uppercase tracking-wide text-ink-400">Distribution</p>
              <p className="text-sm text-ink-800">Status: {toTitleCase(d.status)}</p>
              <p className="text-sm text-ink-500">Requested: {formatDate(d.requestDate)}</p>
              {d.completedAt && <p className="text-sm text-ink-500">Completed: {formatDateTime(d.completedAt)}</p>}
              {d.completedBy && <p className="text-sm text-ink-500">Handed over by: {d.completedBy.name}</p>}
            </div>
          </div>

          <Table>
            <THead>
              <TR>
                <TH>Item</TH>
                <TH>Requested</TH>
                <TH>Allocated</TH>
                <TH>Distributed</TH>
              </TR>
            </THead>
            <TBody>
              {d.items.map((item) => (
                <TR key={item.id}>
                  <TD>{item.itemName}</TD>
                  <TD>
                    {item.requestedQuantity} {UNIT_LABELS[item.unit]}
                  </TD>
                  <TD>{item.allocatedQuantity ?? "—"}</TD>
                  <TD>{item.distributedQuantity ?? "—"}</TD>
                </TR>
              ))}
            </TBody>
          </Table>

          {report.overrides.length > 0 && (
            <div className="mt-5">
              <p className="mb-2 text-sm font-semibold text-ink-800">Duplicate overrides</p>
              <Table>
                <THead>
                  <TR>
                    <TH>Item</TH>
                    <TH>Reason</TH>
                    <TH>By</TH>
                    <TH>Date</TH>
                  </TR>
                </THead>
                <TBody>
                  {report.overrides.map((o) => (
                    <TR key={o.id}>
                      <TD>{o.itemName}</TD>
                      <TD className="max-w-xs">{o.overrideReason}</TD>
                      <TD>{o.overriddenBy?.name ?? "—"}</TD>
                      <TD>{formatDateTime(o.createdAt)}</TD>
                    </TR>
                  ))}
                </TBody>
              </Table>
            </div>
          )}

          <p className="mt-6 text-center text-xs text-ink-400">Generated {formatDateTime(report.generatedAt)}</p>
        </CardBody>
      </Card>
    </div>
  );
}
