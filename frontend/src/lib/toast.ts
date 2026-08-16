import { toast } from "sonner";
import { ApiRequestError } from "@/api/types";

export function notifySuccess(message: string) {
  toast.success(message);
}

export function notifyError(error: unknown, fallback = "Something went wrong. Please try again.") {
  if (error instanceof ApiRequestError) {
    toast.error(error.message || fallback);
    return;
  }
  if (error instanceof Error) {
    toast.error(error.message || fallback);
    return;
  }
  toast.error(fallback);
}

export { toast };
