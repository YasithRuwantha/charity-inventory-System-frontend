import type { LucideIcon } from "lucide-react";
import { ArrowDownRight, ArrowUpRight } from "lucide-react";
import { cn } from "@/lib/utils";

interface StatCardProps {
  label: string;
  value: string;
  icon: LucideIcon;
  tone?: "brand" | "amber" | "rose" | "sky" | "slate";
  trend?: { value: number; label?: string };
  loading?: boolean;
}

const toneClasses = {
  brand: "bg-brand-50 text-brand-600 dark:bg-brand-100 dark:text-brand-400",
  amber: "bg-amber-50 text-amber-700 dark:bg-amber-950/40 dark:text-amber-300",
  rose: "bg-rose-50 text-rose-600 dark:bg-rose-950/40 dark:text-rose-300",
  sky: "bg-sky-50 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300",
  slate: "bg-ink-100 text-ink-600",
};

export function StatCard({ label, value, icon: Icon, tone = "brand", trend, loading }: StatCardProps) {
  return (
    <div className="rounded-[14px] border border-ink-200/80 bg-white p-5 shadow-none transition-[border-color,box-shadow] duration-150 hover:border-brand-300 hover:shadow-card-hover dark:bg-ink-100">
      <div className="flex items-center justify-between">
        <p className="text-sm font-medium text-ink-500">{label}</p>
        <div className={cn("flex h-9 w-9 items-center justify-center rounded-[10px] opacity-80", toneClasses[tone])}>
          <Icon className="h-4.5 w-4.5" />
        </div>
      </div>
      {loading ? (
        <div className="mt-3 h-8 w-24 animate-pulse rounded-md bg-ink-200/70" />
      ) : (
        <p className="mt-2 text-[1.35rem] font-bold tracking-[-0.02em] text-ink-900">{value}</p>
      )}
      {trend && !loading && (
        <div className="mt-2 flex items-center gap-1 text-xs font-medium">
          {trend.value >= 0 ? (
            <ArrowUpRight className="h-3.5 w-3.5 text-emerald-600" />
          ) : (
            <ArrowDownRight className="h-3.5 w-3.5 text-rose-600" />
          )}
          <span className={trend.value >= 0 ? "text-emerald-600" : "text-rose-600"}>
            {Math.abs(trend.value)}%
          </span>
          {trend.label && <span className="text-ink-400">{trend.label}</span>}
        </div>
      )}
    </div>
  );
}
