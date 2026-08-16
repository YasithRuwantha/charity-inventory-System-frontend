import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { LogOut, ShieldCheck } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardHeader, CardBody } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { PageSpinner } from "@/components/ui/Skeleton";
import { getMe } from "@/api/users";
import { useAuthStore } from "@/store/auth";
import { initials, toTitleCase } from "@/lib/utils";

const ROLE_DESCRIPTIONS: Record<string, string> = {
  ADMIN: "Full access: user administration, approvals, duplicate overrides, volunteer management and audit logs, plus everything Inventory Staff can do.",
  INVENTORY_STAFF: "Day-to-day operations: categories, inventory, donors, donations, beneficiaries, distribution requests, allocation, completion, reports and the dashboard.",
  VOLUNTEER: "Read-only inventory and category lookups, plus your own assigned tasks and activity history.",
};

export default function ProfilePage() {
  const navigate = useNavigate();
  const clearSession = useAuthStore((s) => s.clearSession);
  const { data: me, isLoading } = useQuery({ queryKey: ["users", "me"], queryFn: getMe });

  if (isLoading) return <PageSpinner />;
  if (!me) return null;

  const handleLogout = () => {
    clearSession();
    navigate("/login");
  };

  return (
    <div>
      <PageHeader title="My profile" description="Your account details and access level." />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardBody className="flex flex-col items-center text-center">
            <div className="flex h-16 w-16 items-center justify-center rounded-full bg-brand-600 text-xl font-semibold text-white">
              {initials(me.name)}
            </div>
            <p className="mt-3 font-display text-lg font-bold text-ink-900">{me.name}</p>
            <p className="text-sm text-ink-500">{me.email}</p>
            <div className="mt-3 flex items-center gap-2">
              <Badge tone="brand" dot>
                {toTitleCase(me.role)}
              </Badge>
              <Badge tone={me.status === "ACTIVE" ? "success" : "neutral"} dot>
                {toTitleCase(me.status)}
              </Badge>
            </div>
            <Button variant="outline" className="mt-5 w-full" onClick={handleLogout}>
              <LogOut className="h-4 w-4" />
              Sign out
            </Button>
          </CardBody>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader title="Access level" description="What your role can do in this system" />
          <CardBody>
            <div className="flex items-start gap-3 rounded-lg bg-brand-50 p-4">
              <ShieldCheck className="mt-0.5 h-5 w-5 shrink-0 text-brand-600" />
              <p className="text-sm text-brand-800">{ROLE_DESCRIPTIONS[me.role]}</p>
            </div>
            <p className="mt-4 text-sm text-ink-500">
              Need a different role or access level? Contact your organization's administrator — role changes are
              managed from the Users page.
            </p>
          </CardBody>
        </Card>
      </div>
    </div>
  );
}
