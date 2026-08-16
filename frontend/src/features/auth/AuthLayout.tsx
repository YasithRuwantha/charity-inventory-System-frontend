import type { ReactNode } from "react";
import { HeartHandshake } from "lucide-react";

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-ink-50">
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden bg-ink-950 p-10 text-white lg:flex">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(16,185,129,0.25),transparent_45%),radial-gradient(circle_at_80%_80%,rgba(16,185,129,0.15),transparent_40%)]" />
        <div className="relative flex items-center gap-2.5">
          <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600">
            <HeartHandshake className="h-5 w-5" />
          </div>
          <span className="font-display text-lg font-bold">Charity IMS</span>
        </div>
        <div className="relative space-y-4">
          <h2 className="font-display text-3xl font-bold leading-tight">
            Every donation tracked.
            <br />
            Every distribution accounted for.
          </h2>
          <p className="max-w-md text-sm text-ink-300">
            A single system for donations, inventory, beneficiaries and distributions —
            with a full audit trail behind every stock movement.
          </p>
        </div>
        <p className="relative text-xs text-ink-500">
          © {new Date().getFullYear()} Charity Inventory Management System
        </p>
      </div>

      <div className="flex flex-1 items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-sm">
          <div className="mb-8 flex items-center gap-2.5 lg:hidden">
            <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-brand-600 text-white">
              <HeartHandshake className="h-5 w-5" />
            </div>
            <span className="font-display text-lg font-bold text-ink-900">Charity IMS</span>
          </div>
          <h1 className="font-display text-2xl font-bold text-ink-900">{title}</h1>
          <p className="mt-1.5 text-sm text-ink-500">{subtitle}</p>
          <div className="mt-7">{children}</div>
        </div>
      </div>
    </div>
  );
}
