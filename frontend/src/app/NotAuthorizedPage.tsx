import { Link } from "react-router-dom";
import { ShieldAlert } from "lucide-react";
import { Button } from "@/components/ui/Button";

export function NotAuthorizedPage() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center gap-4 px-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-rose-50">
        <ShieldAlert className="h-6 w-6 text-rose-500" />
      </div>
      <div>
        <h1 className="font-display text-2xl font-bold text-ink-900">Access restricted</h1>
        <p className="mt-1.5 max-w-sm text-sm text-ink-500">
          Your role doesn't have permission to view this page. Contact an administrator if you believe this is a mistake.
        </p>
      </div>
      <Link to="/">
        <Button variant="outline">Back to home</Button>
      </Link>
    </div>
  );
}
