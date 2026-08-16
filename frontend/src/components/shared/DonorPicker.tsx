import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, ChevronDown, X } from "lucide-react";
import { listDonors, type Donor } from "@/api/donors";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn } from "@/lib/utils";

interface DonorPickerProps {
  value: Donor | null;
  onChange: (donor: Donor | null) => void;
  invalid?: boolean;
}

export function DonorPicker({ value, onChange, invalid }: DonorPickerProps) {
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
    queryKey: ["donors", "picker", debouncedQuery],
    queryFn: () => listDonors({ search: debouncedQuery || undefined, status: "ACTIVE", size: 20 }),
    enabled: open,
  });

  return (
    <div className="relative" ref={ref}>
      {value ? (
        <div
          className={cn(
            "flex h-10 w-full items-center justify-between rounded-[10px] border bg-white px-3 text-sm dark:bg-ink-100",
            invalid ? "border-rose-500" : "border-ink-200"
          )}
        >
          <div className="min-w-0">
            <span className="font-medium text-ink-800">{value.donorName}</span>
            <span className="ml-1.5 font-mono text-xs text-ink-400">{value.donorCode}</span>
          </div>
          <button type="button" onClick={() => onChange(null)} className="ml-2 shrink-0 p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className={cn(
            "flex h-10 w-full items-center justify-between rounded-[10px] border bg-white px-3 text-left text-sm text-ink-400 dark:bg-ink-100",
            invalid ? "border-rose-500" : "border-ink-200"
          )}
        >
          Select a donor…
          <ChevronDown className="h-4 w-4" />
        </button>
      )}

      {open && !value && (
        <div className="absolute z-40 mt-1 w-full rounded-[10px] border border-ink-200/80 bg-white shadow-popover animate-slide-up dark:bg-ink-100">
          <div className="border-b border-ink-200 p-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-ink-400" />
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Search donors…"
                className="h-8 w-full rounded-[10px] border border-ink-200 bg-white pl-8 pr-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/30 dark:bg-ink-100"
              />
            </div>
          </div>
          <div className="max-h-64 overflow-y-auto py-1">
            {isFetching ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">Searching…</p>
            ) : !data || data.items.length === 0 ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">No donors found.</p>
            ) : (
              data.items.map((donor) => (
                <button
                  key={donor.id}
                  type="button"
                  onClick={() => {
                    onChange(donor);
                    setOpen(false);
                    setQuery("");
                  }}
                  className="flex w-full flex-col items-start px-3 py-2 text-left text-sm hover:bg-ink-50"
                >
                  <span className="font-medium text-ink-800">{donor.donorName}</span>
                  <span className="text-xs text-ink-400">{donor.donorCode}</span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
