import { useEffect } from "react";
import { useForm, useFieldArray, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { Plus, Trash2 } from "lucide-react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Select, Textarea } from "@/components/ui/Input";
import { BeneficiaryPicker } from "@/components/shared/BeneficiaryPicker";
import { InventoryItemPicker } from "@/components/shared/InventoryItemPicker";
import { getBeneficiary, type Beneficiary } from "@/api/beneficiaries";
import type { InventoryItem } from "@/api/inventory";
import { createDistribution, updateDistribution, type Distribution } from "@/api/distributions";
import { PRIORITY_LEVEL, PRIORITY_LABELS } from "@/types/enums";
import { notifyError, notifySuccess } from "@/lib/toast";
import { todayIso } from "@/lib/utils";

const lineSchema = z.object({
  item: z.custom<InventoryItem>((v) => !!v, "Select an item"),
  requestedQuantity: z.coerce.number().positive("Quantity must be greater than zero"),
  notes: z.string().max(500).optional(),
});

const schema = z.object({
  beneficiary: z.custom<Beneficiary>((v) => !!v, "Select a beneficiary"),
  requestDate: z.string().min(1, "Date is required"),
  priority: z.enum(PRIORITY_LEVEL).optional(),
  reason: z.string().max(1000).optional(),
  notes: z.string().max(1000).optional(),
  items: z.array(lineSchema).min(1, "Add at least one item"),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function NewDistributionDialog({
  open,
  onClose,
  presetBeneficiaryId,
  editingDistribution,
}: {
  open: boolean;
  onClose: () => void;
  presetBeneficiaryId?: number;
  editingDistribution?: Distribution | null;
}) {
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const isEditing = !!editingDistribution;

  const { data: presetBeneficiary } = useQuery({
    queryKey: ["beneficiaries", presetBeneficiaryId],
    queryFn: () => getBeneficiary(presetBeneficiaryId!),
    enabled: open && !!presetBeneficiaryId && !isEditing,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({
    resolver: zodResolver(schema),
    defaultValues: { requestDate: todayIso(), items: [{ item: undefined, requestedQuantity: 1 }] },
  });

  const { fields, append, remove } = useFieldArray({ control, name: "items" });

  useEffect(() => {
    if (!open) return;
    if (editingDistribution) {
      reset({
        beneficiary: {
          id: editingDistribution.beneficiaryId,
          beneficiaryCode: editingDistribution.beneficiaryCode,
          beneficiaryName: editingDistribution.beneficiaryName,
        } as Beneficiary,
        requestDate: editingDistribution.requestDate,
        priority: editingDistribution.priority,
        reason: editingDistribution.reason ?? "",
        notes: editingDistribution.notes ?? "",
        items: editingDistribution.items.map((item) => ({
          item: {
            id: item.inventoryItemId,
            itemCode: item.itemCode,
            itemName: item.itemName,
            categoryName: item.categoryName,
            quantity: item.availableQuantity,
            unit: item.unit,
          } as InventoryItem,
          requestedQuantity: item.requestedQuantity,
          notes: item.notes ?? "",
        })),
      });
    } else {
      reset({
        beneficiary: undefined,
        requestDate: todayIso(),
        priority: undefined,
        reason: "",
        notes: "",
        items: [{ item: undefined, requestedQuantity: 1 }],
      });
    }
  }, [open, editingDistribution, reset]);

  useEffect(() => {
    if (presetBeneficiary && !isEditing) setValue("beneficiary", presetBeneficiary);
  }, [presetBeneficiary, isEditing, setValue]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const items = values.items.map((line) => ({
        inventoryItemId: line.item.id,
        requestedQuantity: line.requestedQuantity,
        notes: line.notes || undefined,
      }));
      if (isEditing) {
        return updateDistribution(editingDistribution!.id, {
          requestDate: values.requestDate,
          priority: values.priority ?? editingDistribution!.priority,
          reason: values.reason || undefined,
          notes: values.notes || undefined,
          items,
        });
      }
      return createDistribution({
        beneficiaryId: values.beneficiary.id,
        requestDate: values.requestDate,
        priority: values.priority || undefined,
        reason: values.reason || undefined,
        notes: values.notes || undefined,
        items,
      });
    },
    onSuccess: (dist) => {
      notifySuccess(isEditing ? "Distribution request updated" : `Distribution request ${dist.requestReference} created`);
      queryClient.invalidateQueries({ queryKey: ["distributions"] });
      onClose();
      if (!isEditing) navigate(`/distributions/${dist.id}`);
    },
    onError: (err) => notifyError(err),
  });

  const usedIds = fields.map((f) => f.item?.id).filter(Boolean) as number[];

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit distribution request" : "New distribution request"}
      description={
        isEditing
          ? "Only pending requests can be edited. The item list is replaced with what you save here."
          : "Creates a pending request. Inventory is untouched until it's allocated, approved and completed."
      }
      size="xl"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit((v) => mutation.mutate(v))} loading={isSubmitting || mutation.isPending}>
            {isEditing ? "Save changes" : "Create request"}
          </Button>
        </>
      }
    >
      <form className="space-y-5" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="sm:col-span-2">
            <Label required>Beneficiary</Label>
            <Controller
              control={control}
              name="beneficiary"
              render={({ field }) => (
                <BeneficiaryPicker value={field.value ?? null} onChange={field.onChange} invalid={!!errors.beneficiary} disabled={!!presetBeneficiaryId || isEditing} />
              )}
            />
            <FieldError>{errors.beneficiary?.message as string}</FieldError>
          </div>
          <div>
            <Label htmlFor="requestDate" required>
              Request date
            </Label>
            <Input id="requestDate" type="date" max={todayIso()} invalid={!!errors.requestDate} {...register("requestDate")} />
            <FieldError>{errors.requestDate?.message}</FieldError>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <Label htmlFor="priority">Priority override</Label>
            <Controller
              control={control}
              name="priority"
              render={({ field }) => (
                <Select id="priority" value={field.value ?? ""} onChange={(e) => field.onChange(e.target.value || undefined)}>
                  <option value="">Use beneficiary's priority</option>
                  {PRIORITY_LEVEL.map((p) => (
                    <option key={p} value={p}>
                      {PRIORITY_LABELS[p]}
                    </option>
                  ))}
                </Select>
              )}
            />
          </div>
          <div>
            <Label htmlFor="reason">Reason</Label>
            <Input id="reason" placeholder="e.g. Monthly food aid" {...register("reason")} />
          </div>
        </div>

        <div>
          <div className="mb-2 flex items-center justify-between">
            <Label className="mb-0">Requested items</Label>
            <Button type="button" size="sm" variant="outline" onClick={() => append({ item: undefined as unknown as InventoryItem, requestedQuantity: 1 })}>
              <Plus className="h-3.5 w-3.5" />
              Add line
            </Button>
          </div>
          <FieldError>{errors.items?.message as string}</FieldError>

          <div className="space-y-3">
            {fields.map((field, index) => (
              <div key={field.id} className="rounded-lg border border-ink-200 p-3">
                <div className="grid grid-cols-1 gap-3 sm:grid-cols-[2fr_1fr_2fr_auto]">
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
                    <Input type="number" min={1} invalid={!!errors.items?.[index]?.requestedQuantity} {...register(`items.${index}.requestedQuantity`)} />
                    <FieldError>{errors.items?.[index]?.requestedQuantity?.message}</FieldError>
                  </div>
                  <div>
                    <Label className="mb-1 text-xs">Notes</Label>
                    <Input {...register(`items.${index}.notes`)} />
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
