import type { ReactNode } from "react";

export function AuthLayout({ title, subtitle, children }: { title: string; subtitle: string; children: ReactNode }) {
  return (
    <div className="flex min-h-screen bg-gradient-to-b from-[#F8FAF9] to-[#EEF4F1] dark:from-ink-50 dark:to-ink-100">
      <div className="relative hidden w-[45%] flex-col justify-between overflow-hidden auth-brand-panel p-10 lg:flex">
        <div className="relative">
          <p className="font-brand text-[2.5rem] font-bold leading-none tracking-[-0.03em]">
            Charity Inventory Management System
          </p>
          <p className="mt-3 text-sm text-[#ECFDF5]/80">
            Every donation tracked. Every distribution accounted for.
          </p>
        </div>
        <div className="relative max-w-md space-y-3">
          <h2 className="text-2xl font-bold tracking-[-0.02em]">
            A single workspace for stock, donors, and aid.
          </h2>
          <p className="text-sm leading-relaxed text-[#ECFDF5]/75">
            Manage donations, inventory, beneficiaries and distributions — with a full audit trail
            behind every stock movement.
          </p>
        </div>
        <p className="relative text-xs text-[#ECFDF5]/55">
          © {new Date().getFullYear()} Charity Inventory Management System
        </p>
      </div>

      <div className="flex flex-1 items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-sm rounded-[14px] border border-ink-200/80 bg-white p-6 shadow-none sm:p-8 dark:bg-ink-100">
          <div className="mb-6 lg:hidden">
            <p className="font-brand text-xl font-bold text-brand-600">Charity IMS</p>
            <p className="text-xs text-ink-500">Inventory Management</p>
          </div>
          <h1 className="text-2xl font-bold tracking-[-0.02em] text-ink-900">{title}</h1>
          <p className="mt-1.5 text-sm text-ink-500">{subtitle}</p>
          <div className="mt-7">{children}</div>
        </div>
      </div>
    </div>
  );
}
