import type { HTMLAttributes } from "react";
import { cn } from "@/lib/utils";

export type BadgeTone = "neutral" | "brand" | "success" | "warning" | "danger" | "info";

const toneClasses: Record<BadgeTone, string> = {
  neutral: "bg-ink-100 text-ink-600",
  brand: "bg-brand-50 text-brand-700 dark:bg-brand-100 dark:text-brand-400",
  success: "bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-300",
  warning: "bg-amber-50 text-amber-800 dark:bg-amber-950/40 dark:text-amber-300",
  danger: "bg-rose-50 text-rose-700 dark:bg-rose-950/40 dark:text-rose-300",
  info: "bg-sky-50 text-sky-800 dark:bg-sky-950/40 dark:text-sky-300",
};

const dotClasses: Record<BadgeTone, string> = {
  neutral: "bg-ink-400",
  brand: "bg-brand-600",
  success: "bg-emerald-600",
  warning: "bg-amber-500",
  danger: "bg-rose-500",
  info: "bg-sky-600",
};

interface BadgeProps extends HTMLAttributes<HTMLSpanElement> {
  tone?: BadgeTone;
  dot?: boolean;
}

export function Badge({ className, tone = "neutral", dot = false, children, ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-md px-2 py-0.5 text-xs font-semibold",
        toneClasses[tone],
        className
      )}
      {...props}
    >
      {dot && <span className={cn("h-1.5 w-1.5 rounded-full", dotClasses[tone])} />}
      {children}
    </span>
  );
}
