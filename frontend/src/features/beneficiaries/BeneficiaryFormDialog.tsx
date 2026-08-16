import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Select, Textarea } from "@/components/ui/Input";
import { createBeneficiary, updateBeneficiary, type Beneficiary } from "@/api/beneficiaries";
import { PRIORITY_LEVEL, PRIORITY_LABELS } from "@/types/enums";
import { notifyError, notifySuccess } from "@/lib/toast";

const schema = z.object({
  beneficiaryName: z.string().min(1, "Name is required").max(200),
  identificationNumber: z.string().max(64).optional(),
  familySize: z.coerce.number().int().min(1, "Must be at least 1"),
  contactNumber: z.string().max(40).optional(),
  address: z.string().max(500).optional(),
  priorityLevel: z.enum(PRIORITY_LEVEL),
  notes: z.string().max(1000).optional(),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function BeneficiaryFormDialog({
  open,
  onClose,
  beneficiary,
}: {
  open: boolean;
  onClose: () => void;
  beneficiary: Beneficiary | null;
}) {
  const queryClient = useQueryClient();
  const isEditing = !!beneficiary;

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset(
      beneficiary
        ? {
            beneficiaryName: beneficiary.beneficiaryName,
            identificationNumber: beneficiary.identificationNumber ?? "",
            familySize: beneficiary.familySize,
            contactNumber: beneficiary.contactNumber ?? "",
            address: beneficiary.address ?? "",
            priorityLevel: beneficiary.priorityLevel,
            notes: beneficiary.notes ?? "",
          }
        : {
            beneficiaryName: "",
            identificationNumber: "",
            familySize: 1,
            contactNumber: "",
            address: "",
            priorityLevel: "MEDIUM",
            notes: "",
          }
    );
  }, [open, beneficiary, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        beneficiaryName: values.beneficiaryName,
        identificationNumber: values.identificationNumber || undefined,
        familySize: values.familySize,
        contactNumber: values.contactNumber || undefined,
        address: values.address || undefined,
        priorityLevel: values.priorityLevel,
        notes: values.notes || undefined,
      };
      return isEditing ? updateBeneficiary(beneficiary!.id, payload) : createBeneficiary(payload);
    },
    onSuccess: () => {
      notifySuccess(isEditing ? "Beneficiary updated" : "Beneficiary registered");
      queryClient.invalidateQueries({ queryKey: ["beneficiaries"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit beneficiary" : "Register beneficiary"}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit((v) => mutation.mutate(v))} loading={isSubmitting || mutation.isPending}>
            {isEditing ? "Save changes" : "Register"}
          </Button>
        </>
      }
    >
      <form className="grid grid-cols-1 gap-4 sm:grid-cols-2" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div className="sm:col-span-2">
          <Label htmlFor="beneficiaryName" required>
            Full name
          </Label>
          <Input id="beneficiaryName" invalid={!!errors.beneficiaryName} {...register("beneficiaryName")} />
          <FieldError>{errors.beneficiaryName?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="identificationNumber">ID number</Label>
          <Input id="identificationNumber" {...register("identificationNumber")} />
        </div>
        <div>
          <Label htmlFor="familySize" required>
            Family size
          </Label>
          <Input id="familySize" type="number" min={1} invalid={!!errors.familySize} {...register("familySize")} />
          <FieldError>{errors.familySize?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="contactNumber">Contact number</Label>
          <Input id="contactNumber" {...register("contactNumber")} />
        </div>
        <div>
          <Label htmlFor="priorityLevel" required>
            Priority level
          </Label>
          <Controller
            control={control}
            name="priorityLevel"
            render={({ field }) => (
              <Select id="priorityLevel" {...field}>
                {PRIORITY_LEVEL.map((p) => (
                  <option key={p} value={p}>
                    {PRIORITY_LABELS[p]}
                  </option>
                ))}
              </Select>
            )}
          />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="address">Address</Label>
          <Textarea id="address" rows={2} {...register("address")} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="notes">Notes</Label>
          <Textarea id="notes" rows={2} {...register("notes")} />
        </div>
      </form>
    </Dialog>
  );
}
