import { forwardRef, type ButtonHTMLAttributes } from "react";
import { Loader2 } from "lucide-react";
import { cn } from "@/lib/utils";

type Variant = "primary" | "secondary" | "outline" | "ghost" | "danger" | "danger-outline";
type Size = "sm" | "md" | "lg" | "icon";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variantClasses: Record<Variant, string> = {
  primary:
    "bg-brand-600 text-white hover:bg-brand-700 active:bg-brand-800 disabled:bg-brand-300 dark:text-brand-950 dark:disabled:bg-brand-200",
  secondary:
    "bg-[var(--color-amber-accent)] text-white hover:opacity-90 disabled:opacity-40 dark:text-ink-950",
  outline:
    "border border-ink-200 bg-white text-ink-800 hover:bg-ink-50 disabled:text-ink-300 dark:bg-ink-100 dark:text-ink-800 dark:hover:bg-ink-200",
  ghost: "text-ink-600 hover:bg-ink-100 hover:text-ink-900 disabled:text-ink-300",
  danger: "bg-[var(--color-rose-accent)] text-white hover:opacity-90 disabled:opacity-40",
  "danger-outline":
    "border border-rose-200 bg-white text-[var(--color-rose-accent)] hover:bg-rose-50 disabled:opacity-40 dark:bg-ink-100 dark:border-rose-400/40",
};

const sizeClasses: Record<Size, string> = {
  sm: "h-8 px-3 text-sm gap-1.5",
  md: "h-10 px-[18px] text-sm gap-2",
  lg: "h-11 px-5 text-[15px] gap-2",
  icon: "h-10 w-10 p-0",
};

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "primary", size = "md", loading, disabled, children, ...props }, ref) => {
    return (
      <button
        ref={ref}
        disabled={disabled || loading}
        className={cn(
          "inline-flex items-center justify-center whitespace-nowrap rounded-[10px] font-semibold normal-case shadow-none transition-colors duration-150",
          "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40 focus-visible:ring-offset-2 focus-visible:ring-offset-ink-50",
          "disabled:cursor-not-allowed",
          variantClasses[variant],
          sizeClasses[size],
          className
        )}
        {...props}
      >
        {loading && <Loader2 className="h-4 w-4 animate-spin" />}
        {children}
      </button>
    );
  }
);
Button.displayName = "Button";
