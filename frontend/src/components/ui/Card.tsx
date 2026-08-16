import type { HTMLAttributes, ReactNode } from "react";
import { cn } from "@/lib/utils";

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn("rounded-xl border border-ink-200 bg-white shadow-card", className)}
      {...props}
    />
  );
}

export function CardHeader({
  className,
  title,
  description,
  actions,
  ...props
}: HTMLAttributes<HTMLDivElement> & { title?: ReactNode; description?: ReactNode; actions?: ReactNode }) {
  return (
    <div className={cn("flex items-start justify-between gap-4 border-b border-ink-100 px-5 py-4", className)} {...props}>
      <div>
        {title && <h3 className="text-sm font-semibold text-ink-900">{title}</h3>}
        {description && <p className="mt-0.5 text-sm text-ink-500">{description}</p>}
      </div>
      {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
    </div>
  );
}

export function CardBody({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("px-5 py-4", className)} {...props} />;
}
