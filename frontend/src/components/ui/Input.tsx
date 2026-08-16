import { forwardRef, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  leadingIcon?: ReactNode;
  invalid?: boolean;
}

export const Input = forwardRef<HTMLInputElement, InputProps>(
  ({ className, leadingIcon, invalid, ...props }, ref) => {
    if (leadingIcon) {
      return (
        <div className="relative">
          <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400">
            {leadingIcon}
          </span>
          <input
            ref={ref}
            className={cn(
              "h-9.5 w-full rounded-lg border bg-white pl-9 pr-3 text-sm text-ink-800 placeholder:text-ink-400",
              "focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:border-brand-500",
              "disabled:bg-ink-100 disabled:text-ink-400 disabled:cursor-not-allowed",
              invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-300",
              className
            )}
            {...props}
          />
        </div>
      );
    }
    return (
      <input
        ref={ref}
        className={cn(
          "h-9.5 w-full rounded-lg border bg-white px-3 text-sm text-ink-800 placeholder:text-ink-400",
          "focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:border-brand-500",
          "disabled:bg-ink-100 disabled:text-ink-400 disabled:cursor-not-allowed",
          invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-300",
          className
        )}
        {...props}
      />
    );
  }
);
Input.displayName = "Input";

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  invalid?: boolean;
}

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({ className, invalid, ...props }, ref) => (
    <textarea
      ref={ref}
      className={cn(
        "w-full rounded-lg border bg-white px-3 py-2 text-sm text-ink-800 placeholder:text-ink-400",
        "focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:border-brand-500",
        "disabled:bg-ink-100 disabled:text-ink-400 disabled:cursor-not-allowed",
        invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-300",
        className
      )}
      {...props}
    />
  )
);
Textarea.displayName = "Textarea";

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  invalid?: boolean;
}

export const Select = forwardRef<HTMLSelectElement, SelectProps>(
  ({ className, invalid, children, ...props }, ref) => (
    <select
      ref={ref}
      className={cn(
        "h-9.5 w-full rounded-lg border bg-white px-3 text-sm text-ink-800",
        "focus:outline-none focus:ring-2 focus:ring-brand-500/40 focus:border-brand-500",
        "disabled:bg-ink-100 disabled:text-ink-400 disabled:cursor-not-allowed",
        invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-300",
        className
      )}
      {...props}
    >
      {children}
    </select>
  )
);
Select.displayName = "Select";

export function Label({ className, children, required, ...props }: React.LabelHTMLAttributes<HTMLLabelElement> & { required?: boolean }) {
  return (
    <label className={cn("mb-1.5 block text-sm font-medium text-ink-700", className)} {...props}>
      {children}
      {required && <span className="ml-0.5 text-rose-500">*</span>}
    </label>
  );
}

export function FieldError({ children }: { children?: string }) {
  if (!children) return null;
  return <p className="mt-1.5 text-xs font-medium text-rose-600">{children}</p>;
}

export function FieldHint({ children }: { children?: ReactNode }) {
  if (!children) return null;
  return <p className="mt-1.5 text-xs text-ink-400">{children}</p>;
}
