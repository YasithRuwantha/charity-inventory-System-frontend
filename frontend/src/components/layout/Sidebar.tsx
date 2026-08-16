import { NavLink } from "react-router-dom";
import { HeartHandshake, X } from "lucide-react";
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
    <div className="flex h-full flex-col">
      <div className="flex h-16 shrink-0 items-center gap-2.5 px-5">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-brand-600 text-white">
          <HeartHandshake className="h-4.5 w-4.5" />
        </div>
        <div className="leading-tight">
          <p className="font-display text-[15px] font-bold text-white">Charity IMS</p>
          <p className="text-[11px] text-ink-400">Inventory Management</p>
        </div>
        <button
          onClick={onCloseMobile}
          className="ml-auto rounded-md p-1 text-ink-400 hover:bg-ink-800 hover:text-white lg:hidden"
        >
          <X className="h-4.5 w-4.5" />
        </button>
      </div>

      <nav className="scrollbar-thin flex-1 space-y-5 overflow-y-auto px-3 pb-6 pt-2">
        {navSections.map((section, idx) => {
          const items = section.items.filter((item) => !role || item.roles.includes(role));
          if (items.length === 0) return null;
          return (
            <div key={idx}>
              {section.label && (
                <p className="mb-1.5 px-2.5 text-[11px] font-semibold uppercase tracking-wider text-ink-500">
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
                        "flex items-center gap-2.5 rounded-lg px-2.5 py-2 text-sm font-medium transition-colors",
                        isActive
                          ? "bg-brand-600/15 text-brand-400"
                          : "text-ink-300 hover:bg-white/5 hover:text-white"
                      )
                    }
                  >
                    <item.icon className="h-4.5 w-4.5 shrink-0" />
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
      <aside className="hidden w-64 shrink-0 border-r border-ink-800 bg-ink-950 lg:block">
        {content}
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-40 lg:hidden">
          <div className="absolute inset-0 bg-ink-950/60" onClick={onCloseMobile} />
          <aside className="absolute inset-y-0 left-0 w-64 border-r border-ink-800 bg-ink-950 animate-slide-up">
            {content}
          </aside>
        </div>
      )}
    </>
  );
}
