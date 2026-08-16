import { useEffect, useState } from "react";
import { useForm, useFieldArray, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Trash2 } from "lucide-react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Textarea } from "@/components/ui/Input";
import { DonorPicker } from "@/components/shared/DonorPicker";
import { InventoryItemPicker } from "@/components/shared/InventoryItemPicker";
import type { Donor } from "@/api/donors";
import type { InventoryItem } from "@/api/inventory";
import { createDonation } from "@/api/donations";
import { notifyError, notifySuccess } from "@/lib/toast";
import { todayIso } from "@/lib/utils";

const lineSchema = z.object({
  item: z.custom<InventoryItem>((v) => !!v, "Select an item"),
  quantity: z.coerce.number().positive("Quantity must be greater than zero"),
  expiryDate: z.string().optional(),
  notes: z.string().max(500).optional(),
});

const schema = z.object({
  donor: z.custom<Donor>((v) => !!v, "Select a donor"),
  donationDate: z.string().min(1, "Date is required"),
  notes: z.string().max(1000).optional(),
  items: z.array(lineSchema).min(1, "Add at least one item"),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function NewDonationDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [navigateOnSuccess, setNavigateOnSuccess] = useState(true);

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { donor: undefined, donationDate: todayIso(), notes: "", items: [{ item: undefined, quantity: 1 }] },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  useEffect(() => {
    if (open) {
      reset({ donor: undefined, donationDate: todayIso(), notes: "", items: [{ item: undefined, quantity: 1 }] });
    }
  }, [open, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) =>
      createDonation({
        donorId: values.donor.id,
        donationDate: values.donationDate,
        notes: values.notes || undefined,
        items: values.items.map((line) => ({
          inventoryItemId: line.item.id,
          quantity: line.quantity,
          expiryDate: line.expiryDate || undefined,
          notes: line.notes || undefined,
        })),
      }),
    onSuccess: (donation) => {
      notifySuccess(`Donation ${donation.donationReference} recorded`);
      queryClient.invalidateQueries({ queryKey: ["donations"] });
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      onClose();
      if (navigateOnSuccess) navigate(`/donations/${donation.id}`);
    },
    onError: (err) => notifyError(err),
  });

  const usedIds = fields.map((f) => f.item?.id).filter(Boolean) as number[];

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title="Record a donation"
      description="Increases stock for each item and generates a reference number."
      size="xl"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit((v) => {
              setNavigateOnSuccess(true);
              mutation.mutate(v);
            })}
            loading={isSubmitting || mutation.isPending}
          >
            Save donation
          </Button>
        </>
      }
    >
      <form className="space-y-5" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <Label required>Donor</Label>
            <Controller
              control={control}
              name="donor"
              render={({ field }) => <DonorPicker value={field.value ?? null} onChange={field.onChange} invalid={!!errors.donor} />}
            />
            <FieldError>{errors.donor?.message as string}</FieldError>
          </div>
          <div>
            <Label htmlFor="donationDate" required>
              Donation date
            </Label>
            <Input id="donationDate" type="date" max={todayIso()} invalid={!!errors.donationDate} {...register("donationDate")} />
            <FieldError>{errors.donationDate?.message}</FieldError>
          </div>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <Label className="mb-0">Items</Label>
            <Button type="button" size="sm" variant="outline" onClick={() => append({ item: undefined as unknown as InventoryItem, quantity: 1 })}>
              <Plus className="h-3.5 w-3.5" />
              Add line
            </Button>
          </div>
          <FieldError>{errors.items?.message as string}</FieldError>

          <div className="space-y-3">
            {fields.map((field, index) => (
              <div key={field.id} className="rounded-lg border border-ink-200 p-3">
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-[2fr_1fr_1fr_auto]">
                  <div>
                    <Label className="mb-1 text-xs">Item</Label>
                    <Controller
                      control={control}
                      name={`items.${index}.item`}
                      render={({ field: itemField }) => (
                        <InventoryItemPicker
                          value={itemField.value ?? null}
                          onChange={itemField.onChange}
                          excludeIds={usedIds.filter((id) => id !== itemField.value?.id)}
                          invalid={!!errors.items?.[index]?.item}
                        />
                      )}
                    />
                    <FieldError>{errors.items?.[index]?.item?.message as string}</FieldError>
                  </div>
                  <div>
                    <Label className="mb-1 text-xs">Quantity</Label>
                    <Input type="number" min={1} invalid={!!errors.items?.[index]?.quantity} {...register(`items.${index}.quantity`)} />
                    <FieldError>{errors.items?.[index]?.quantity?.message}</FieldError>
                  </div>
                  <div>
                    <Label className="mb-1 text-xs">Batch expiry</Label>
                    <Input type="date" min={todayIso()} {...register(`items.${index}.expiryDate`)} />
                  </div>
                  <div className="flex items-end">
                    <button
                      type="button"
                      onClick={() => remove(index)}
                      disabled={fields.length === 1}
                      className="flex h-9.5 w-9.5 items-center justify-center rounded-lg text-ink-400 hover:bg-rose-50 hover:text-rose-600 disabled:opacity-30"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div>
          <Label htmlFor="notes">Notes</Label>
          <Textarea id="notes" rows={2} {...register("notes")} />
        </div>
      </form>
    </Dialog>
  );
}
