import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { TriangleAlert } from "lucide-react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, Textarea } from "@/components/ui/Input";
import { completeDistribution, type Distribution, type DuplicateDistribution } from "@/api/distributions";
import { UNIT_LABELS } from "@/types/enums";
import { ApiRequestError } from "@/api/types";
import { notifyError, notifySuccess } from "@/lib/toast";
import { useAuthStore } from "@/store/auth";

export function CompleteDialog({ open, onClose, distribution }: { open: boolean; onClose: () => void; distribution: Distribution | null }) {
  const queryClient = useQueryClient();
  const role = useAuthStore((s) => s.user?.role);
  const [amounts, setAmounts] = useState<Record<number, number>>({});
  const [duplicate, setDuplicate] = useState<DuplicateDistribution | null>(null);
  const [overrideReason, setOverrideReason] = useState("");

  useEffect(() => {
    if (open && distribution) {
      const initial: Record<number, number> = {};
      distribution.items.forEach((item) => {
        initial[item.id] = item.allocatedQuantity ?? item.requestedQuantity;
      });
      setAmounts(initial);
      setDuplicate(null);
      setOverrideReason("");
    }
  }, [open, distribution]);

  const mutation = useMutation({
    mutationFn: (withOverride: boolean) =>
      completeDistribution(distribution!.id, {
        items: distribution!.items.map((item) => ({ distributionItemId: item.id, distributedQuantity: amounts[item.id] ?? 0 })),
        overrideDuplicates: withOverride || undefined,
        overrideReason: withOverride ? overrideReason : undefined,
      }),
    onSuccess: () => {
      notifySuccess("Distribution completed — stock updated");
      queryClient.invalidateQueries({ queryKey: ["distributions"] });
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      onClose();
    },
    onError: (err) => {
      if (err instanceof ApiRequestError && err.errorCode === "DUPLICATE_DISTRIBUTION" && err.data) {
        setDuplicate(err.data as DuplicateDistribution);
        return;
      }
      notifyError(err);
    },
  });

  if (!distribution) return null;
  const isAdmin = role === "ADMIN";

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Complete distribution"
      description="Confirms hand-over and deducts stock. This is the only step that changes inventory."
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          {duplicate && isAdmin ? (
            <Button
              variant="danger"
              onClick={() => mutation.mutate(true)}
              loading={mutation.isPending}
              disabled={overrideReason.trim().length === 0}
            >
              Override & complete
            </Button>
          ) : (
            <Button onClick={() => mutation.mutate(false)} loading={mutation.isPending}>
              Complete distribution
            </Button>
          )}
        </>
      }
    >
      <div className="space-y-3">
        {distribution.items.map((item) => (
          <div key={item.id} className="rounded-lg border border-ink-200 p-3">
            <p className="font-medium text-ink-900">{item.itemName}</p>
            <p className="text-xs text-ink-400">
              Allocated {item.allocatedQuantity ?? 0} {UNIT_LABELS[item.unit]} · Available now {item.availableQuantity}
            </p>
            <div className="mt-2">
              <Label className="mb-1 text-xs">Quantity to hand over</Label>
              <Input
                type="number"
                min={0}
                max={item.allocatedQuantity ?? undefined}
                value={amounts[item.id] ?? 0}
                onChange={(e) => setAmounts((prev) => ({ ...prev, [item.id]: Number(e.target.value) }))}
              />
            </div>
          </div>
        ))}

        {duplicate && (
          <div className="rounded-lg border border-amber-200 bg-amber-50 p-3.5">
            <div className="flex items-start gap-2.5">
              <TriangleAlert className="mt-0.5 h-4.5 w-4.5 shrink-0 text-amber-600" />
              <div className="text-sm text-amber-800">
                <p className="font-medium">Possible duplicate distribution</p>
                <p className="mt-0.5">{duplicate.message}</p>
                <p className="mt-1 text-xs text-amber-700">
                  {duplicate.beneficiaryName} received {duplicate.quantityPreviouslyReceived} of {duplicate.itemName}{" "}
                  {duplicate.daysSinceLastDistribution} day(s) ago (window: {duplicate.duplicateWindowDays} days).
                </p>
              </div>
            </div>
            {isAdmin ? (
              <div className="mt-3">
                <Label htmlFor="overrideReason" required className="text-xs">
                  Override reason
                </Label>
                <Textarea
                  id="overrideReason"
                  rows={2}
                  value={overrideReason}
                  onChange={(e) => setOverrideReason(e.target.value)}
                  placeholder="Explain why this override is justified…"
                />
              </div>
            ) : (
              <p className="mt-2 text-xs font-medium text-amber-700">Only an administrator can override this.</p>
            )}
          </div>
        )}
      </div>
    </Dialog>
  );
}
