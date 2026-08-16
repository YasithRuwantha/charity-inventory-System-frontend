import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Search, ChevronDown, X } from "lucide-react";
import { listInventory, type InventoryItem } from "@/api/inventory";
import { useDebouncedValue } from "@/hooks/useDebouncedValue";
import { cn } from "@/lib/utils";
import { UNIT_LABELS } from "@/types/enums";

interface InventoryItemPickerProps {
  value: InventoryItem | null;
  onChange: (item: InventoryItem | null) => void;
  placeholder?: string;
  invalid?: boolean;
  excludeIds?: number[];
  onlyInStock?: boolean;
}

export function InventoryItemPicker({
  value,
  onChange,
  placeholder = "Search for an item…",
  invalid,
  excludeIds = [],
  onlyInStock = false,
}: InventoryItemPickerProps) {
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
    queryKey: ["inventory", "picker", debouncedQuery],
    queryFn: () => listInventory({ search: debouncedQuery || undefined, size: 20, sort: "itemName,asc" }),
    enabled: open,
  });

  const results = (data?.items ?? []).filter((i) => !excludeIds.includes(i.id) && (!onlyInStock || i.quantity > 0));

  return (
    <div className="relative" ref={ref}>
      {value ? (
        <div
          className={cn(
            "flex h-9.5 w-full items-center justify-between rounded-lg border bg-white px-3 text-sm",
            invalid ? "border-rose-400" : "border-ink-300"
          )}
        >
          <div className="min-w-0">
            <span className="font-medium text-ink-800">{value.itemName}</span>
            <span className="ml-1.5 text-ink-400">{value.itemCode}</span>
          </div>
          <button
            type="button"
            onClick={() => onChange(null)}
            className="ml-2 shrink-0 rounded-md p-1 text-ink-400 hover:bg-ink-100 hover:text-ink-700"
          >
            <X className="h-3.5 w-3.5" />
          </button>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => setOpen(true)}
          className={cn(
            "flex h-9.5 w-full items-center justify-between rounded-lg border bg-white px-3 text-left text-sm text-ink-400",
            invalid ? "border-rose-400" : "border-ink-300"
          )}
        >
          {placeholder}
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
                placeholder="Type to search…"
                className="h-8 w-full rounded-md border border-ink-200 pl-8 pr-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand-500/40"
              />
            </div>
          </div>
          <div className="max-h-64 overflow-y-auto py-1">
            {isFetching ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">Searching…</p>
            ) : results.length === 0 ? (
              <p className="px-3 py-4 text-center text-sm text-ink-400">No items found.</p>
            ) : (
              results.map((item) => (
                <button
                  key={item.id}
                  type="button"
                  onClick={() => {
                    onChange(item);
                    setOpen(false);
                    setQuery("");
                  }}
                  className="flex w-full items-center justify-between gap-2 px-3 py-2 text-left text-sm hover:bg-ink-50"
                >
                  <div className="min-w-0">
                    <p className="truncate font-medium text-ink-800">{item.itemName}</p>
                    <p className="text-xs text-ink-400">
                      {item.itemCode} · {item.categoryName ?? "Uncategorized"}
                    </p>
                  </div>
                  <span className="shrink-0 text-xs text-ink-500">
                    {item.quantity} {UNIT_LABELS[item.unit]}
                  </span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
