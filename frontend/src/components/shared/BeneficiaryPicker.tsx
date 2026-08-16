import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, ChevronDown, X } from "lucide-react";
import { listBeneficiaries, type Beneficiary } from "@/api/beneficiaries";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn } from "@/lib/utils";
import { PRIORITY_LABELS } from "@/types/enums";

interface BeneficiaryPickerProps {
  value: Beneficiary | null;
  onChange: (beneficiary: Beneficiary | null) => void;
  invalid?: boolean;
  disabled?: boolean;
}

export function BeneficiaryPicker({ value, onChange, invalid, disabled }: BeneficiaryPickerProps) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const debouncedQuery = useDebouncedValue(query, 250);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const { data, isFetching } = useQuery({
    queryKey: ["beneficiaries", "picker", debouncedQuery],
    queryFn: () => listBeneficiaries({ search: debouncedQuery || undefined, status: "ACTIVE", size: 20 }),
    enabled: open,
  });

  return (
    <div className="relative" ref={ref}>
      {value ? (
        <div
          className={cn(
            "flex h-9.5 w-full items-center justify-between rounded-lg border bg-white px-3 text-sm",
            disabled && "bg-ink-100",
            invalid ? "border-rose-400" : "border-ink-300"
          )}
        >
          <div className="min-w-0">
            <span className="font-medium text-ink-800">{value.beneficiaryName}</span>
            <span className="ml-1.5 text-ink-400">{value.beneficiaryCode}</span>
          </div>
          {!disabled && (
            <button type="button" onClick={() => onChange(null)} className="ml-2 shrink-0 rounded-md p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      ) : (
        <button
          type="button"
          disabled={disabled}
          onClick={() => setOpen(true)}
          className={cn(
            "flex h-9.5 w-full items-center justify-between rounded-lg border bg-white px-3 text-left text-sm text-ink-400 disabled:bg-ink-100",
            invalid ? "border-rose-400" : "border-ink-300"
          )}
        >
          Select a beneficiary…
          <ChevronDown className="h-4 w-4" />
        </button>
      )}

      {open && !value && (
        <div className="absolute z-40 mt-1.5 w-full rounded-lg border border-ink-200 bg-white shadow-popover animate-slide-up">
          <div className="border-b border-ink-100 p-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-400" />
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search beneficiaries…"
                className="h-8 w-full rounded-md border border-ink-200 pl-8 pr-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
              />
            </div>
          </div>
          <div className="max-h-64 overflow-y-auto py-1">
            {isFetching ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">Searching…</p>
            ) : !data || data.items.length === 0 ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">No beneficiaries found.</p>
            ) : (
              data.items.map((b) => (
                <button
                  key={b.id}
                  type="button"
                  onClick={() => {
                    onChange(b);
                    setOpen(false);
                    setQuery("");
                  }}
                  className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-ink-50"
                >
                  <div>
                    <span className="font-medium text-ink-800">{b.beneficiaryName}</span>
                    <span className="ml-1.5 text-xs text-ink-400">{b.beneficiaryCode}</span>
                  </div>
                  <span className="text-xs text-ink-500">{PRIORITY_LABELS[b.priorityLevel]}</span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
