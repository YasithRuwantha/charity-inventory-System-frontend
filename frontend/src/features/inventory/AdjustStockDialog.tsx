import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Minus } from "lucide-react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Textarea } from "@/components/ui/Input";
import { adjustStock, type InventoryItem } from "@/api/inventory";
import { notifyError, notifySuccess } from "@/lib/toast";
import { cn } from "@/lib/utils";

const schema = z.object({
  direction: z.enum(["ADD", "REMOVE"]),
  amount: z.coerce.number().int().positive("Enter a quantity greater than zero"),
  notes: z.string().max(500).optional(),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function AdjustStockDialog({
  open,
  onClose,
  item,
}: {
  open: boolean;
  onClose: () => void;
  item: InventoryItem | null;
}) {
  const queryClient = useQueryClient();
  const {
    register,
    handleSubmit,
    control,
    reset,
    watch,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema), defaultValues: { direction: "ADD" } });

  useEffect(() => {
    if (open) reset({ direction: "ADD", amount: undefined, notes: "" });
  }, [open, reset]);

  const direction = watch("direction");

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      adjustStock(item!.id, {
        adjustment: values.direction === "ADD" ? values.amount : -values.amount,
        transactionType: "MANUAL_ADJUSTMENT",
        notes: values.notes || undefined,
      }),
    onSuccess: (updated) => {
      notifySuccess(`Stock updated — new quantity is ${updated.quantity}`);
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  if (!item) return null;

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Adjust stock"
      description={`${item.itemName} · currently ${item.quantity} ${item.unit.toLowerCase()}`}
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit((v) => mutation.mutate(v))} loading={isSubmitting || mutation.isPending}>
            Apply adjustment
          </Button>
        </>
      }
    >
      <form className="space-y-4" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div>
          <Label required>Direction</Label>
          <Controller
            control={control}
            name="direction"
            render={({ field }) => (
              <div className="grid grid-cols-2 gap-2">
                <button
                  type="button"
                  onClick={() => field.onChange("ADD")}
                  className={cn(
                    "flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors",
                    field.value === "ADD"
                      ? "border-brand-500 bg-brand-50 text-brand-700"
                      : "border-ink-300 text-ink-600 hover:bg-ink-50"
                  )}
                >
                  <Plus className="h-4 w-4" /> Add stock
                </button>
                <button
                  type="button"
                  onClick={() => field.onChange("REMOVE")}
                  className={cn(
                    "flex items-center justify-center gap-2 rounded-lg border px-3 py-2 text-sm font-medium transition-colors",
                    field.value === "REMOVE"
                      ? "border-rose-500 bg-rose-50 text-rose-700"
                      : "border-ink-300 text-ink-600 hover:bg-ink-50"
                  )}
                >
                  <Minus className="h-4 w-4" /> Remove stock
                </button>
              </div>
            )}
          />
        </div>
        <div>
          <Label htmlFor="amount" required>
            Quantity to {direction === "ADD" ? "add" : "remove"}
          </Label>
          <Input id="amount" type="number" min={1} invalid={!!errors.amount} {...register("amount")} />
          <FieldError>{errors.amount?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="notes">Reason / notes</Label>
          <Textarea id="notes" rows={3} placeholder="e.g. Damaged in storage, stock count correction…" {...register("notes")} />
        </div>
      </form>
    </Dialog>
  );
}
