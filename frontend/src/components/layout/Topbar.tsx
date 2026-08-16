import { Menu, LogOut, User as UserIcon, ChevronDown, Sun, Moon } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/store/auth";
import { useThemeStore } from "@/store/theme";
import { initials, toTitleCase } from "@/lib/utils";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { AlertsBell } from "@/components/layout/AlertsBell";
import { Badge } from "@/components/ui/Badge";

export function Topbar({ onOpenMobile }: { onOpenMobile: () => void }) {
  const user = useAuthStore((s) => s.user);
  const clearSession = useAuthStore((s) => s.clearSession);
  const { mode, toggle } = useThemeStore();
  const navigate = useNavigate();
  const canSeeAlerts = user?.role === "ADMIN" || user?.role === "INVENTORY_STAFF";

  const handleLogout = () => {
    clearSession();
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-30 flex h-14 shrink-0 items-center gap-3 border-b border-ink-200/80 bg-[rgba(243,246,244,0.85)] px-3 backdrop-blur-[12px] sm:h-16 sm:px-6 dark:bg-[rgba(11,18,32,0.85)]">
      <button
        onClick={onOpenMobile}
        className="rounded-[8px] p-2 text-ink-500 hover:bg-ink-100 lg:hidden"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      <div className="min-w-0">
        <p className="hidden text-xs font-medium text-ink-500 sm:block">Inventory operations</p>
        <p className="truncate text-sm font-bold tracking-[-0.02em] text-ink-900 sm:text-base">
          Charity Inventory Management System
        </p>
      </div>

      <div className="flex-1" />

      {user?.role && (
        <Badge tone="brand" className="hidden md:inline-flex">
          {toTitleCase(user.role)}
        </Badge>
      )}

      {canSeeAlerts && <AlertsBell />}

      <button
        onClick={toggle}
        className="rounded-[8px] p-2 text-ink-500 hover:bg-ink-100"
        aria-label={mode === "dark" ? "Switch to light mode" : "Switch to dark mode"}
      >
        {mode === "dark" ? <Sun className="h-5 w-5" /> : <Moon className="h-5 w-5" />}
      </button>

      <DropdownMenu
        trigger={
          <button className="flex items-center gap-2 rounded-[10px] py-1.5 pl-1.5 pr-2 hover:bg-ink-100">
            <div className="flex h-[34px] w-[34px] items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white dark:text-brand-950">
              {initials(user?.name)}
            </div>
            <div className="hidden text-left sm:block">
              <p className="text-sm font-semibold leading-tight text-ink-800">{user?.name}</p>
              <p className="text-xs leading-tight text-ink-400">{toTitleCase(user?.role)}</p>
            </div>
            <ChevronDown className="h-3.5 w-3.5 text-ink-400" />
          </button>
        }
      >
        <DropdownItem icon={<UserIcon className="h-4 w-4" />} onClick={() => navigate("/profile")}>
          My profile
        </DropdownItem>
        <div className="my-1 border-t border-ink-200/80" />
        <DropdownItem icon={<LogOut className="h-4 w-4" />} danger onClick={handleLogout}>
          Sign out
        </DropdownItem>
      </DropdownMenu>
    </header>
  );
}
