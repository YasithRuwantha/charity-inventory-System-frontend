import { useRef, useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, Download, Upload, FileSpreadsheet, CheckCircle2, XCircle } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import {
  downloadBulkTemplate,
  previewBulkDonations,
  importBulkDonations,
  type BulkDonationPreview,
  type BulkDonationImportResult,
} from "@/api/donations";
import { notifyError, notifySuccess } from "@/lib/toast";

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}

export default function BulkImportPage() {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<BulkDonationPreview | null>(null);
  const [result, setResult] = useState<BulkDonationImportResult | null>(null);
  const [allowPartial, setAllowPartial] = useState(false);

  const templateMutation = useMutation({
    mutationFn: downloadBulkTemplate,
    onSuccess: (blob) => downloadBlob(blob, "donation-bulk-template.csv"),
    onError: (err) => notifyError(err),
  });

  const previewMutation = useMutation({
    mutationFn: (f: File) => previewBulkDonations(f),
    onSuccess: (data) => {
      setPreview(data);
      setResult(null);
    },
    onError: (err) => notifyError(err),
  });

  const importMutation = useMutation({
    mutationFn: (f: File) => importBulkDonations(f, allowPartial),
    onSuccess: (data) => {
      setResult(data);
      notifySuccess(data.message || `Imported ${data.createdDonations} donation(s)`);
    },
    onError: (err) => notifyError(err),
  });

  const handleFileChange = (f: File | null) => {
    setFile(f);
    setPreview(null);
    setResult(null);
    if (f) previewMutation.mutate(f);
  };

  return (
    <div>
      <button
        onClick={() => navigate("/donations")}
        className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800"
      >
        <ArrowLeft className="h-4 w-4" /> Back to donations
      </button>

      <PageHeader
        title="Bulk import donations"
        description="Upload a CSV of donations to validate and import them in one batch."
        actions={
          <Button variant="outline" onClick={() => templateMutation.mutate()} loading={templateMutation.isPending}>
            <Download className="h-4 w-4" />
            Download template
          </Button>
        }
      />

      <Card>
        <CardHeader
          title="1. Choose a file"
          description="Columns: Donor, Item, Category, Quantity, Unit, Expiry Date, Donation Date"
        />
        <CardBody>
          <input
            ref={fileInputRef}
            type="file"
            accept=".csv"
            className="hidden"
            onChange={(e) => handleFileChange(e.target.files?.[0] ?? null)}
          />
          <button
            onClick={() => fileInputRef.current?.click()}
            className="flex w-full flex-col items-center gap-2 rounded-xl border-2 border-dashed border-ink-300 py-10 text-center hover:border-brand-400 hover:bg-brand-50/40"
          >
            <FileSpreadsheet className="h-8 w-8 text-ink-400" />
            <p className="text-sm font-medium text-ink-700">{file ? file.name : "Click to select a CSV file"}</p>
            <p className="text-xs text-ink-400">Max 5 MB</p>
          </button>
        </CardBody>
      </Card>

      {previewMutation.isPending && (
        <Card className="mt-4">
          <CardBody>
            <div className="h-24 animate-pulse rounded-lg bg-ink-100" />
          </CardBody>
        </Card>
      )}

      {preview && !result && (
        <Card className="mt-4">
          <CardHeader
            title="2. Review"
            description={`${preview.validRows} of ${preview.totalRows} rows are valid — ${preview.donationsToCreate} donation(s) will be created.`}
            actions={
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-sm text-ink-600">
                  <input
                    type="checkbox"
                    className="h-4 w-4 rounded border-ink-300 text-brand-600 focus:ring-brand-500"
                    checked={allowPartial}
                    onChange={(e) => setAllowPartial(e.target.checked)}
                  />
                  Import valid rows even if some fail
                </label>
                <Button
                  onClick={() => file && importMutation.mutate(file)}
                  loading={importMutation.isPending}
                  disabled={preview.validRows === 0}
                >
                  <Upload className="h-4 w-4" />
                  Import
                </Button>
              </div>
            }
          />
          <Table>
            <THead>
              <TR>
                <TH>Row</TH>
                <TH>Donor</TH>
                <TH>Item</TH>
                <TH>Category</TH>
                <TH>Qty</TH>
                <TH>Unit</TH>
                <TH>Donation date</TH>
                <TH>Status</TH>
              </TR>
            </THead>
            <TBody>
              {preview.rows.map((row) => (
                <TR key={row.rowNumber}>
                  <TD className="text-ink-500">{row.rowNumber}</TD>
                  <TD>{row.donor}</TD>
                  <TD>{row.item}</TD>
                  <TD className="text-ink-500">{row.category}</TD>
                  <TD>{row.quantity}</TD>
                  <TD className="text-ink-500">{row.unit}</TD>
                  <TD className="text-ink-500">{row.donationDate}</TD>
                  <TD>
                    {row.valid ? (
                      <Badge tone="success" dot>
                        Valid
                      </Badge>
                    ) : (
                      <div>
                        <Badge tone="danger" dot>
                          Invalid
                        </Badge>
                        <p className="mt-1 max-w-[220px] text-xs text-rose-600">{row.errors.join("; ")}</p>
                      </div>
                    )}
                  </TD>
                </TR>
              ))}
            </TBody>
          </Table>
        </Card>
      )}

      {result && (
        <Card className="mt-4">
          <CardHeader title="3. Import result" />
          <CardBody>
            <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
              <div className="rounded-lg bg-emerald-50 p-4 text-center">
                <CheckCircle2 className="mx-auto h-5 w-5 text-emerald-600" />
                <p className="mt-1 text-xl font-bold text-emerald-700">{result.successfulRows}</p>
                <p className="text-xs text-emerald-600">Rows imported</p>
              </div>
              <div className="rounded-lg bg-rose-50 p-4 text-center">
                <XCircle className="mx-auto h-5 w-5 text-rose-600" />
                <p className="mt-1 text-xl font-bold text-rose-700">{result.failedRows}</p>
                <p className="text-xs text-rose-600">Rows failed</p>
              </div>
              <div className="rounded-lg bg-ink-100 p-4 text-center">
                <p className="mt-1 text-xl font-bold text-ink-800">{result.createdDonations}</p>
                <p className="text-xs text-ink-500">Donations created</p>
              </div>
              <div className="rounded-lg bg-ink-100 p-4 text-center">
                <p className="mt-1 text-xl font-bold text-ink-800">{result.totalQuantityImported}</p>
                <p className="text-xs text-ink-500">Total quantity</p>
              </div>
            </div>

            {result.errors.length > 0 && (
              <div className="mt-4">
                <p className="mb-2 text-sm font-medium text-ink-700">Rows that failed</p>
                <Table>
                  <THead>
                    <TR>
                      <TH>Row</TH>
                      <TH>Donor</TH>
                      <TH>Item</TH>
                      <TH>Errors</TH>
                    </TR>
                  </THead>
                  <TBody>
                    {result.errors.map((row) => (
                      <TR key={row.rowNumber}>
                        <TD>{row.rowNumber}</TD>
                        <TD>{row.donor}</TD>
                        <TD>{row.item}</TD>
                        <TD className="text-rose-600">{row.errors.join("; ")}</TD>
                      </TR>
                    ))}
                  </TBody>
                </Table>
              </div>
            )}

            <div className="mt-5 flex justify-end">
              <Button onClick={() => navigate("/donations")}>Go to donations</Button>
            </div>
          </CardBody>
        </Card>
      )}
    </div>
  );
}
