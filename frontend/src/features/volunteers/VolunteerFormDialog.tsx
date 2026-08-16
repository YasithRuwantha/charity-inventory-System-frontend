import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Textarea } from "@/components/ui/Input";
import { createVolunteer, updateVolunteer, type Volunteer } from "@/api/volunteers";
import { notifyError, notifySuccess } from "@/lib/toast";
import { todayIso } from "@/lib/utils";

const schema = z.object({
  volunteerName: z.string().min(1, "Name is required").max(200),
  phone: z.string().max(40).optional(),
  email: z.union([z.string().email("Enter a valid email"), z.literal("")]).optional(),
  address: z.string().max(500).optional(),
  joinedDate: z.string().optional(),
  notes: z.string().max(1000).optional(),
});
type FormValues = z.infer<typeof schema>;

export function VolunteerFormDialog({ open, onClose, volunteer }: { open: boolean; onClose: () => void; volunteer: Volunteer | null }) {
  const queryClient = useQueryClient();
  const isEditing = !!volunteer;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset(
      volunteer
        ? {
            volunteerName: volunteer.volunteerName,
            phone: volunteer.phone ?? "",
            email: volunteer.email ?? "",
            address: volunteer.address ?? "",
            joinedDate: volunteer.joinedDate ?? "",
            notes: volunteer.notes ?? "",
          }
        : { volunteerName: "", phone: "", email: "", address: "", joinedDate: todayIso(), notes: "" }
    );
  }, [open, volunteer, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        volunteerName: values.volunteerName,
        phone: values.phone || undefined,
        email: values.email || undefined,
        address: values.address || undefined,
        joinedDate: values.joinedDate || undefined,
        notes: values.notes || undefined,
      };
      return isEditing ? updateVolunteer(volunteer!.id, payload) : createVolunteer(payload);
    },
    onSuccess: () => {
      notifySuccess(isEditing ? "Volunteer updated" : "Volunteer registered");
      queryClient.invalidateQueries({ queryKey: ["volunteers"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit volunteer" : "Register volunteer"}
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
          <Label htmlFor="volunteerName" required>
            Full name
          </Label>
          <Input id="volunteerName" invalid={!!errors.volunteerName} {...register("volunteerName")} />
          <FieldError>{errors.volunteerName?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="phone">Phone</Label>
          <Input id="phone" {...register("phone")} />
        </div>
        <div>
          <Label htmlFor="email">Email</Label>
          <Input id="email" type="email" invalid={!!errors.email} {...register("email")} />
          <FieldError>{errors.email?.message}</FieldError>
          <p className="mt-1.5 text-xs text-ink-400">Use the same email as their login account so they can view their own tasks.</p>
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="joinedDate">Joined date</Label>
          <Input id="joinedDate" type="date" max={todayIso()} {...register("joinedDate")} />
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
