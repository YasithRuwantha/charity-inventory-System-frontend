import { Link } from "react-router-dom";
import { CompassIcon } from "lucide-react";
import { Button } from "@/components/ui/Button";

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 bg-ink-50 px-6 text-center">
      <div className="flex h-14 w-14 items-center justify-center rounded-full bg-ink-100">
        <CompassIcon className="h-6 w-6 text-ink-400" />
      </div>
      <div>
        <h1 className="font-display text-2xl font-bold text-ink-900">Page not found</h1>
        <p className="mt-1.5 text-sm text-ink-500">The page you're looking for doesn't exist or was moved.</p>
      </div>
      <Link to="/">
        <Button>Back to home</Button>
      </Link>
    </div>
  );
}
