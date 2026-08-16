import { Fragment, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { ShieldCheck, ChevronDown, ChevronUp } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card } from "@/components/ui/Card";
import { Input, Select } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { Pagination } from "@/components/ui/Pagination";
import { EmptyState } from "@/components/ui/EmptyState";
import { TableSkeleton } from "@/components/ui/Skeleton";
import { listAuditLogs, listAuditActions } from "@/api/auditLogs";
import { formatDateTime, toTitleCase } from "@/lib/utils";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";

export default function AuditLogPage() {
  const [action, setAction] = useState("");
  const [entityType, setEntityType] = useState("");
  const debouncedEntityType = useDebouncedValue(entityType);
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [page, setPage] = useState(0);
  const [expanded, setExpanded] = useState<number | null>(null);

  const { data: actions } = useQuery({ queryKey: ["audit-logs", "actions"], queryFn: listAuditActions });

  const { data, isLoading } = useQuery({
    queryKey: ["audit-logs", { action, entityType: debouncedEntityType, startDate, endDate, page }],
    queryFn: () =>
      listAuditLogs({
        action: action || undefined,
        entityType: debouncedEntityType || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        page,
        size: 20,
        sort: "createdAt,desc",
      }),
  });

  return (
    <div>
      <PageHeader title="Audit log" description="Every significant action, with the acting user and what changed." />

      <Card>
        <div className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-5 py-3.5">
          <Select className="w-56" value={action} onChange={(e) => { setAction(e.target.value); setPage(0); }}>
            <option value="">All actions</option>
            {actions?.map((a) => (
              <option key={a} value={a}>
                {toTitleCase(a)}
              </option>
            ))}
          </Select>
          <Input
            className="w-44"
            placeholder="Entity type…"
            value={entityType}
            onChange={(e) => { setEntityType(e.target.value); setPage(0); }}
          />
          <Input type="date" className="w-40" value={startDate} onChange={(e) => { setStartDate(e.target.value); setPage(0); }} />
          <span className="text-sm text-ink-400">to</span>
          <Input type="date" className="w-40" value={endDate} onChange={(e) => { setEndDate(e.target.value); setPage(0); }} />
        </div>

        {isLoading ? (
          <TableSkeleton cols={5} />
        ) : !data || data.items.length === 0 ? (
          <EmptyState icon={ShieldCheck} title="No audit entries found" />
        ) : (
          <Table>
            <THead>
              <TR>
                <TH>When</TH>
                <TH>Action</TH>
                <TH>Entity</TH>
                <TH>User</TH>
                <TH>Description</TH>
                <TH className="w-8" />
              </TR>
            </THead>
            <TBody>
              {data.items.map((log) => (
                <Fragment key={log.id}>
                  <TR className="cursor-pointer" onClick={() => setExpanded(expanded === log.id ? null : log.id)}>
                    <TD className="whitespace-nowrap text-ink-500">{formatDateTime(log.createdAt)}</TD>
                    <TD>
                      <Badge tone="info">{toTitleCase(log.action)}</Badge>
                    </TD>
                    <TD className="text-ink-600">
                      {log.entityType ?? "—"} {log.entityId && <span className="text-ink-400">#{log.entityId}</span>}
                    </TD>
                    <TD className="text-ink-600">{log.userEmail ?? "System"}</TD>
                    <TD className="max-w-xs truncate text-ink-500">{log.description ?? "—"}</TD>
                    <TD>{expanded === log.id ? <ChevronUp className="h-4 w-4 text-ink-400" /> : <ChevronDown className="h-4 w-4 text-ink-400" />}</TD>
                  </TR>
                  {expanded === log.id && (log.oldValues || log.newValues || log.ipAddress) && (
                    <TR>
                      <TD colSpan={6} className="bg-ink-50">
                        <div className="grid grid-cols-1 gap-3 py-2 sm:grid-cols-2">
                          {log.oldValues && (
                            <div>
                              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-ink-400">Before</p>
                              <pre className="scrollbar-thin max-h-40 overflow-auto rounded-md bg-white p-2 text-xs text-ink-600">{log.oldValues}</pre>
                            </div>
                          )}
                          {log.newValues && (
                            <div>
                              <p className="mb-1 text-xs font-semibold uppercase tracking-wide text-ink-400">After</p>
                              <pre className="scrollbar-thin max-h-40 overflow-auto rounded-md bg-white p-2 text-xs text-ink-600">{log.newValues}</pre>
                            </div>
                          )}
                          {log.ipAddress && (
                            <p className="text-xs text-ink-400 sm:col-span-2">IP address: {log.ipAddress}</p>
                          )}
                        </div>
                      </TD>
                    </TR>
                  )}
                </Fragment>
              ))}
            </TBody>
          </Table>
        )}

        {data && <Pagination meta={data.pagination} onPageChange={setPage} />}
      </Card>
    </div>
  );
}
