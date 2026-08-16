import { useEffect, useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label } from "@/components/ui/Input";
import { Badge } from "@/components/ui/Badge";
import { allocateDistribution, type Distribution } from "@/api/distributions";
import { UNIT_LABELS } from "@/types/enums";
import { notifyError, notifySuccess } from "@/lib/toast";

export function AllocateDialog({ open, onClose, distribution }: { open: boolean; onClose: () => void; distribution: Distribution | null }) {
  const queryClient = useQueryClient();
  const [amounts, setAmounts] = useState<Record<number, number>>({});

  useEffect(() => {
    if (open && distribution) {
      const initial: Record<number, number> = {};
      distribution.items.forEach((item) => {
        initial[item.id] = item.allocatedQuantity
          ? item.allocatedQuantity
          : Math.min(item.requestedQuantity, item.availableQuantity);
      });
      setAmounts(initial);
    }
  }, [open, distribution]);

  const mutation = useMutation({
    mutationFn: () =>
      allocateDistribution(
        distribution!.id,
        distribution!.items.map((item) => ({ distributionItemId: item.id, allocatedQuantity: amounts[item.id] ?? 0 }))
      ),
    onSuccess: () => {
      notifySuccess("Items allocated");
      queryClient.invalidateQueries({ queryKey: ["distributions"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  if (!distribution) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Allocate inventory"
      description="Set how much of each requested item to reserve. This does not deduct stock yet."
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={() => mutation.mutate()} loading={mutation.isPending}>
            Save allocation
          </Button>
        </>
      }
    >
      <div className="space-y-3">
        {distribution.items.map((item) => {
          const insufficient = item.availableQuantity < item.requestedQuantity;
          return (
            <div key={item.id} className="rounded-lg border border-ink-200 p-3">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-medium text-ink-900">{item.itemName}</p>
                  <p className="text-xs text-ink-400">
                    Requested {item.requestedQuantity} {UNIT_LABELS[item.unit]} · Available {item.availableQuantity}
                  </p>
                </div>
                {item.expired && <Badge tone="danger">Expired</Badge>}
                {!item.expired && insufficient && <Badge tone="warning">Low stock</Badge>}
              </div>
              <div className="mt-2">
                <Label className="mb-1 text-xs">Allocated quantity</Label>
                <Input
                  type="number"
                  min={0}
                  max={item.availableQuantity}
                  value={amounts[item.id] ?? 0}
                  onChange={(e) => setAmounts((prev) => ({ ...prev, [item.id]: Number(e.target.value) }))}
                />
              </div>
            </div>
          );
        })}
      </div>
    </Dialog>
  );
}
