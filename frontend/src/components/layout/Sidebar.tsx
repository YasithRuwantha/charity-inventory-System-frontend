import { NavLink } from "react-router-dom";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";
import { navSections } from "@/components/layout/nav";
import { useAuthStore } from "@/store/auth";

interface SidebarProps {
  mobileOpen: boolean;
  onCloseMobile: () => void;
}

export function Sidebar({ mobileOpen, onCloseMobile }: SidebarProps) {
  const role = useAuthStore((s) => s.user?.role);

  const content = (
    <div className="flex h-full flex-col bg-gradient-to-b from-white to-[#F8FAF9] dark:from-ink-100 dark:to-ink-50">
      <div className="flex h-16 shrink-0 items-center gap-2.5 px-5">
        <div className="min-w-0 leading-tight">
          <p className="font-brand text-[1.15rem] font-bold leading-none text-brand-600">Charity IMS</p>
          <p className="mt-1 text-[11px] font-medium text-ink-500">Inventory Management</p>
        </div>
        <button
          onClick={onCloseMobile}
          className="ml-auto rounded-[8px] p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700 lg:hidden"
          aria-label="Close menu"
        >
          <X className="h-4.5 w-4.5" />
        </button>
      </div>

      <nav className="scrollbar-thin flex-1 space-y-5 overflow-y-auto px-3 pb-6 pt-1">
        {navSections.map((section, idx) => {
          const items = section.items.filter((item) => !role || item.roles.includes(role));
          if (items.length === 0) return null;
          return (
            <div key={idx}>
              {section.label && (
                <p className="mb-1.5 px-2.5 text-[11px] font-semibold uppercase tracking-[0.08em] text-ink-500">
                  {section.label}
                </p>
              )}
              <div className="space-y-0.5">
                {items.map((item) => (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    end={item.to === "/"}
                    onClick={onCloseMobile}
                    className={({ isActive }) =>
                      cn(
                        "flex min-h-11 items-center gap-2.5 rounded-[8px] px-2.5 text-[0.92rem] transition-colors",
                        isActive
                          ? "bg-brand-600/12 font-bold text-brand-700 dark:text-brand-400"
                          : "font-medium text-ink-600 hover:bg-brand-600/10 hover:text-brand-700"
                      )
                    }
                  >
                    <item.icon className="h-[19px] w-[19px] shrink-0 opacity-90" />
                    {item.label}
                  </NavLink>
                ))}
              </div>
            </div>
          );
        })}
      </nav>
    </div>
  );

  return (
    <>
      <aside className="hidden w-[248px] shrink-0 border-r border-ink-200/80 lg:block">{content}</aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink-950/50" onClick={onCloseMobile} />
          <aside className="absolute inset-y-0 left-0 w-[248px] border-r border-ink-200/80 shadow-popover animate-slide-up">
            {content}
          </aside>
        </div>
      )}
    </>
  );
}
