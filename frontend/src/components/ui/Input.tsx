import { forwardRef, type InputHTMLAttributes, type ReactNode, type TextareaHTMLAttributes, type SelectHTMLAttributes } from "react";
import { cn } from "@/lib/utils";

const fieldBase =
  "w-full rounded-[10px] border bg-white text-sm text-ink-900 placeholder:text-ink-400 " +
  "focus:outline-none focus:ring-2 focus:ring-brand-500/30 focus:border-brand-600 " +
  "disabled:bg-ink-100 disabled:text-ink-400 disabled:cursor-not-allowed " +
  "dark:bg-ink-100 dark:text-ink-900";

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
              fieldBase,
              "h-10 pl-9 pr-3",
              invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-200",
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
          fieldBase,
          "h-10 px-3",
          invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-200",
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
        fieldBase,
        "px-3 py-2",
        invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-200",
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
        fieldBase,
        "h-10 px-3",
        invalid ? "border-rose-400 focus:ring-rose-500/30 focus:border-rose-500" : "border-ink-200",
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
    <label className={cn("mb-1.5 block text-sm font-semibold text-ink-700", className)} {...props}>
      {children}
      {required && <span className="ml-0.5 text-[var(--color-rose-accent)]">*</span>}
    </label>
  );
}

export function FieldError({ children }: { children?: string }) {
  if (!children) return null;
  return <p className="mt-1.5 text-xs font-medium text-[var(--color-rose-accent)]">{children}</p>;
}

export function FieldHint({ children }: { children?: ReactNode }) {
  if (!children) return null;
  return <p className="mt-1.5 text-xs text-ink-500">{children}</p>;
}
