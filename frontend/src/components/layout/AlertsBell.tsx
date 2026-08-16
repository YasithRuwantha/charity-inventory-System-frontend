import { Bell, TriangleAlert, PackageX, CalendarClock } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { DropdownMenu } from "@/components/ui/DropdownMenu";
import { getDashboardAlerts } from "@/api/dashboard";

export function AlertsBell() {
  const { data } = useQuery({
    queryKey: ["dashboard", "alerts"],
    queryFn: getDashboardAlerts,
    refetchInterval: 60_000,
  });

  const alerts = data ?? [];
  const count = alerts.length;

  return (
    <DropdownMenu
      trigger={
        <button
          className="relative rounded-lg p-2 text-ink-500 hover:bg-ink-100"
          aria-label="Alerts"
        >
          <Bell className="h-5 w-5" />
          {count > 0 && (
            <span className="absolute right-1.5 top-1.5 flex h-2 w-2 items-center justify-center rounded-full bg-rose-500" />
          )}
        </button>
      }
    >
      <div className="w-80">
        <div className="border-b border-ink-100 px-3.5 py-2.5">
          <p className="text-sm font-semibold text-ink-800">Alerts</p>
          <p className="text-xs text-ink-400">Low stock, out of stock and expiring items</p>
        </div>
        <div className="max-h-80 overflow-y-auto">
          {alerts.length === 0 ? (
            <p className="px-3.5 py-6 text-center text-sm text-ink-400">You're all caught up.</p>
          ) : (
            alerts.slice(0, 8).map((alert, i) => {
              const isCritical = alert.severity === "CRITICAL";
              return (
                <div key={i} className="flex items-start gap-2.5 px-3.5 py-2.5 hover:bg-ink-50">
                  <span
                    className={
                      "mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full " +
                      (isCritical ? "bg-rose-50 text-rose-600" : "bg-amber-50 text-amber-600")
                    }
                  >
                    {alert.type === "OUT_OF_STOCK" ? (
                      <PackageX className="h-3.5 w-3.5" />
                    ) : alert.type === "EXPIRING_SOON" || alert.type === "EXPIRED_STOCK" ? (
                      <CalendarClock className="h-3.5 w-3.5" />
                    ) : (
                      <TriangleAlert className="h-3.5 w-3.5" />
                    )}
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-ink-800">{alert.message}</p>
                    {alert.count > 1 && <p className="text-xs text-ink-500">{alert.count} affected</p>}
                  </div>
                </div>
              );
            })
          )}
        </div>
        {alerts.length > 0 && (
          <Link
            to="/inventory?status=LOW_STOCK"
            className="block border-t border-ink-100 px-3.5 py-2.5 text-center text-sm font-medium text-brand-700 hover:bg-brand-50"
          >
            View inventory
          </Link>
        )}
      </div>
    </DropdownMenu>
  );
}
