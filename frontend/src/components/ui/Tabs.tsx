import { cn } from "@/lib/utils";

export interface TabItem {
  key: string;
  label: string;
  count?: number;
}

interface TabsProps {
  items: TabItem[];
  active: string;
  onChange: (key: string) => void;
}

export function Tabs({ items, active, onChange }: TabsProps) {
  return (
    <div className="flex items-center gap-1 border-b border-ink-200/80">
      {items.map((item) => {
        const isActive = item.key === active;
        return (
          <button
            key={item.key}
            onClick={() => onChange(item.key)}
            className={cn(
              "relative flex items-center gap-2 px-3.5 py-2.5 text-sm transition-colors",
              isActive ? "font-bold text-brand-700 dark:text-brand-400" : "font-medium text-ink-500 hover:text-ink-800"
            )}
          >
            {item.label}
            {item.count !== undefined && (
              <span
                className={cn(
                  "rounded-md px-1.5 py-0.5 text-xs font-semibold",
                  isActive ? "bg-brand-50 text-brand-700 dark:bg-brand-100 dark:text-brand-400" : "bg-ink-100 text-ink-500"
                )}
              >
                {item.count}
              </span>
            )}
            {isActive && <span className="absolute inset-x-0 -bottom-px h-0.5 rounded-full bg-brand-600" />}
          </button>
        );
      })}
    </div>
  );
}
