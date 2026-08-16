import { useEffect } from "react";
import { useForm, Controller } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Select, Textarea } from "@/components/ui/Input";
import { createDonor, updateDonor, type Donor } from "@/api/donors";
import { DONOR_TYPE } from "@/types/enums";
import { notifyError, notifySuccess } from "@/lib/toast";

const schema = z.object({
  donorName: z.string().min(1, "Donor name is required").max(200),
  donorType: z.enum(DONOR_TYPE),
  phone: z.string().max(40).optional(),
  email: z.union([z.string().email("Enter a valid email"), z.literal("")]).optional(),
  address: z.string().max(500).optional(),
  notes: z.string().max(1000).optional(),
});
type FormValues = z.infer<typeof schema>;

const TYPE_LABELS: Record<(typeof DONOR_TYPE)[number], string> = {
  INDIVIDUAL: "Individual",
  ORGANIZATION: "Organization",
  ANONYMOUS: "Anonymous",
};

export function DonorFormDialog({ open, onClose, donor }: { open: boolean; onClose: () => void; donor: Donor | null }) {
  const queryClient = useQueryClient();
  const isEditing = !!donor;

  const {
    register,
    handleSubmit,
    control,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset(
      donor
        ? {
            donorName: donor.donorName,
            donorType: donor.donorType,
            phone: donor.phone ?? "",
            email: donor.email ?? "",
            address: donor.address ?? "",
            notes: donor.notes ?? "",
          }
        : { donorName: "", donorType: "INDIVIDUAL", phone: "", email: "", address: "", notes: "" }
    );
  }, [open, donor, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        donorName: values.donorName,
        donorType: values.donorType,
        phone: values.phone || undefined,
        email: values.email || undefined,
        address: values.address || undefined,
        notes: values.notes || undefined,
      };
      return isEditing ? updateDonor(donor!.id, payload) : createDonor(payload);
    },
    onSuccess: () => {
      notifySuccess(isEditing ? "Donor updated" : "Donor added");
      queryClient.invalidateQueries({ queryKey: ["donors"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit donor" : "New donor"}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit((v) => mutation.mutate(v))} loading={isSubmitting || mutation.isPending}>
            {isEditing ? "Save changes" : "Add donor"}
          </Button>
        </>
      }
    >
      <form className="grid grid-cols-1 gap-4 sm:grid-cols-2" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div className="sm:col-span-2">
          <Label htmlFor="donorName" required>
            Donor name
          </Label>
          <Input id="donorName" invalid={!!errors.donorName} {...register("donorName")} />
          <FieldError>{errors.donorName?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="donorType" required>
            Donor type
          </Label>
          <Controller
            control={control}
            name="donorType"
            render={({ field }) => (
              <Select id="donorType" {...field}>
                {DONOR_TYPE.map((t) => (
                  <option key={t} value={t}>
                    {TYPE_LABELS[t]}
                  </option>
                ))}
              </Select>
            )}
          />
        </div>
        <div>
          <Label htmlFor="phone">Phone</Label>
          <Input id="phone" {...register("phone")} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" invalid={!!errors.email} {...register("email")} />
          <FieldError>{errors.email?.message}</FieldError>
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
