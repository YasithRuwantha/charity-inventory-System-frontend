import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Select, Textarea } from "@/components/ui/Input";
import { listCategories } from "@/api/categories";
import { createInventoryItem, updateInventoryItem, type InventoryItem } from "@/api/inventory";
import { UNIT_OF_MEASURE, UNIT_LABELS } from "@/types/enums";
import { notifyError, notifySuccess } from "@/lib/toast";
import { todayIso } from "@/lib/utils";

const schema = z.object({
  itemName: z.string().min(1, "Item name is required").max(200),
  categoryId: z.coerce.number({ error: "Select a category" }).positive("Select a category"),
  description: z.string().max(1000).optional(),
  quantity: z.coerce.number().int().min(0, "Must be zero or more").optional(),
  unit: z.enum(UNIT_OF_MEASURE),
  minimumStockLevel: z.coerce.number().int().min(0, "Must be zero or more"),
  expiryDate: z.string().optional(),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

interface InventoryFormDialogProps {
  open: boolean;
  onClose: () => void;
  item: InventoryItem | null;
}

export function InventoryFormDialog({ open, onClose, item }: InventoryFormDialogProps) {
  const queryClient = useQueryClient();
  const isEditing = !!item;

  const { data: categories } = useQuery({
    queryKey: ["categories", "all-active"],
    queryFn: () => listCategories({ status: "ACTIVE", size: 100 }),
    enabled: open,
  });

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    if (item) {
      reset({
        itemName: item.itemName,
        categoryId: item.categoryId ?? undefined,
        description: item.description ?? "",
        unit: item.unit,
        minimumStockLevel: item.minimumStockLevel,
        expiryDate: item.expiryDate ?? "",
      });
    } else {
      reset({
        itemName: "",
        categoryId: undefined,
        description: "",
        quantity: 0,
        unit: "ITEM",
        minimumStockLevel: 5,
        expiryDate: "",
      });
    }
  }, [open, item, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const expiryDate = values.expiryDate || undefined;
      if (isEditing) {
        return updateInventoryItem(item!.id, {
          itemName: values.itemName,
          categoryId: values.categoryId,
          description: values.description || undefined,
          unit: values.unit,
          minimumStockLevel: values.minimumStockLevel,
          expiryDate,
        });
      }
      return createInventoryItem({
        itemName: values.itemName,
        categoryId: values.categoryId,
        description: values.description || undefined,
        quantity: values.quantity ?? 0,
        unit: values.unit,
        minimumStockLevel: values.minimumStockLevel,
        expiryDate,
      });
    },
    onSuccess: () => {
      notifySuccess(isEditing ? "Item updated" : "Item created");
      queryClient.invalidateQueries({ queryKey: ["inventory"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  const onSubmit = (values: FormValues) => mutation.mutate(values);

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit item" : "New inventory item"}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit(onSubmit)} loading={isSubmitting || mutation.isPending}>
            {isEditing ? "Save changes" : "Create item"}
          </Button>
        </>
      }
    >
      <form className="grid grid-cols-1 gap-4 sm:grid-cols-2" onSubmit={handleSubmit(onSubmit)}>
        <div className="sm:col-span-2">
          <Label htmlFor="itemName" required>
            Item name
          </Label>
          <Input id="itemName" invalid={!!errors.itemName} {...register("itemName")} />
          <FieldError>{errors.itemName?.message}</FieldError>
        </div>

        <div>
          <Label htmlFor="categoryId" required>
            Category
          </Label>
          <Select id="categoryId" invalid={!!errors.categoryId} {...register("categoryId")}>
            <option value="">Select a category</option>
            {categories?.items.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </Select>
          <FieldError>{errors.categoryId?.message}</FieldError>
        </div>

        <div>
          <Label htmlFor="unit" required>
            Unit of measure
          </Label>
          <Controller
            control={control}
            name="unit"
            render={({ field }) => (
              <Select id="unit" {...field}>
                {UNIT_OF_MEASURE.map((u) => (
                  <option key={u} value={u}>
                    {UNIT_LABELS[u]}
                  </option>
                ))}
              </Select>
            )}
          />
        </div>

        {!isEditing && (
          <div>
            <Label htmlFor="quantity">Opening quantity</Label>
            <Input id="quantity" type="number" min={0} invalid={!!errors.quantity} {...register("quantity")} />
            <FieldError>{errors.quantity?.message}</FieldError>
          </div>
        )}

        <div>
          <Label htmlFor="minimumStockLevel" required>
            Minimum stock level
          </Label>
          <Input
            id="minimumStockLevel"
            type="number"
            min={0}
            invalid={!!errors.minimumStockLevel}
            {...register("minimumStockLevel")}
          />
          <FieldError>{errors.minimumStockLevel?.message}</FieldError>
          <p className="mt-1.5 text-xs text-ink-400">Triggers a low-stock alert when quantity falls to or below this.</p>
        </div>

        <div>
          <Label htmlFor="expiryDate">Expiry date</Label>
          <Input id="expiryDate" type="date" min={todayIso()} {...register("expiryDate")} />
        </div>

        <div className="sm:col-span-2">
          <Label htmlFor="description">Description</Label>
          <Textarea id="description" rows={3} invalid={!!errors.description} {...register("description")} />
          <FieldError>{errors.description?.message}</FieldError>
        </div>
      </form>
    </Dialog>
  );
}
