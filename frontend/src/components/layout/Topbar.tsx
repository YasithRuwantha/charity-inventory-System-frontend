import { Menu, LogOut, User as UserIcon, ChevronDown } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/store/auth";
import { initials, toTitleCase } from "@/lib/utils";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { AlertsBell } from "@/components/layout/AlertsBell";

export function Topbar({ onOpenMobile }: { onOpenMobile: () => void }) {
  const user = useAuthStore((s) => s.user);
  const clearSession = useAuthStore((s) => s.clearSession);
  const navigate = useNavigate();
  const canSeeAlerts = user?.role === "ADMIN" || user?.role === "INVENTORY_STAFF";

  const handleLogout = () => {
    clearSession();
    navigate("/login");
  };

  return (
    <header className="sticky top-0 z-30 flex h-16 shrink-0 items-center gap-3 border-b border-ink-200 bg-white/85 px-4 backdrop-blur-sm sm:px-6">
      <button
        onClick={onOpenMobile}
        className="rounded-lg p-2 text-ink-500 hover:bg-ink-100 lg:hidden"
        aria-label="Open menu"
      >
        <Menu className="h-5 w-5" />
      </button>

      <div className="flex-1" />

      {canSeeAlerts && <AlertsBell />}

      <DropdownMenu
        trigger={
          <button className="flex items-center gap-2 rounded-lg py-1.5 pl-1.5 pr-2 hover:bg-ink-100">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-brand-600 text-xs font-semibold text-white">
              {initials(user?.name)}
            </div>
            <div className="hidden text-left sm:block">
              <p className="text-sm font-medium leading-tight text-ink-800">{user?.name}</p>
              <p className="text-xs leading-tight text-ink-400">{toTitleCase(user?.role)}</p>
            </div>
            <ChevronDown className="h-3.5 w-3.5 text-ink-400" />
          </button>
        }
      >
        <DropdownItem icon={<UserIcon className="h-4 w-4" />} onClick={() => navigate("/profile")}>
          My profile
        </DropdownItem>
        <div className="my-1 border-t border-ink-100" />
        <DropdownItem icon={<LogOut className="h-4 w-4" />} danger onClick={handleLogout}>
          Sign out
        </DropdownItem>
      </DropdownMenu>
    </header>
  );
}
