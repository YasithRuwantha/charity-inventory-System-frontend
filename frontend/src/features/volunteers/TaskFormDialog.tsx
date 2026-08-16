import { useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError, Textarea } from "@/components/ui/Input";
import { createVolunteerTask, updateVolunteerTask, type VolunteerTask } from "@/api/volunteerTasks";
import { notifyError, notifySuccess } from "@/lib/toast";
import { todayIso } from "@/lib/utils";

const schema = z.object({
  title: z.string().min(1, "Title is required").max(200),
  description: z.string().max(1000).optional(),
  taskDate: z.string().min(1, "Task date is required"),
  dueDate: z.string().optional(),
  relatedDistributionId: z.preprocess(
    (val) => (val === "" || val === null || val === undefined ? undefined : val),
    z.coerce.number().int().positive().optional()
  ),
});
type FormInput = z.input<typeof schema>;
type FormValues = z.output<typeof schema>;

export function TaskFormDialog({
  open,
  onClose,
  volunteerId,
  task,
}: {
  open: boolean;
  onClose: () => void;
  volunteerId: number;
  task?: VolunteerTask | null;
}) {
  const queryClient = useQueryClient();
  const isEditing = !!task;

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<FormInput, unknown, FormValues>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!open) return;
    reset(
      task
        ? {
            title: task.title,
            description: task.description ?? "",
            taskDate: task.taskDate,
            dueDate: task.dueDate ?? "",
            relatedDistributionId: task.relatedDistributionId ?? undefined,
          }
        : { title: "", description: "", taskDate: todayIso(), dueDate: "", relatedDistributionId: undefined }
    );
  }, [open, task, reset]);

  const mutation = useMutation({
    mutationFn: (values: FormValues) => {
      const payload = {
        title: values.title,
        description: values.description || undefined,
        taskDate: values.taskDate,
        dueDate: values.dueDate || undefined,
        relatedDistributionId: values.relatedDistributionId || undefined,
      };
      return isEditing
        ? updateVolunteerTask(task!.id, payload)
        : createVolunteerTask({ ...payload, volunteerId });
    },
    onSuccess: () => {
      notifySuccess(isEditing ? "Task updated" : "Task assigned");
      queryClient.invalidateQueries({ queryKey: ["volunteers", volunteerId, "tasks"] });
      queryClient.invalidateQueries({ queryKey: ["volunteer-tasks"] });
      onClose();
    },
    onError: (err) => notifyError(err),
  });

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={isEditing ? "Edit task" : "Assign task"}
      size="lg"
      footer={
        <>
          <Button variant="outline" onClick={onClose}>
            Cancel
          </Button>
          <Button onClick={handleSubmit((v) => mutation.mutate(v))} loading={isSubmitting || mutation.isPending}>
            {isEditing ? "Save changes" : "Assign task"}
          </Button>
        </>
      }
    >
      <form className="grid grid-cols-1 gap-4 sm:grid-cols-2" onSubmit={handleSubmit((v) => mutation.mutate(v))}>
        <div className="sm:col-span-2">
          <Label htmlFor="title" required>
            Title
          </Label>
          <Input id="title" invalid={!!errors.title} {...register("title")} />
          <FieldError>{errors.title?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="taskDate" required>
            Task date
          </Label>
          <Input id="taskDate" type="date" invalid={!!errors.taskDate} {...register("taskDate")} />
          <FieldError>{errors.taskDate?.message}</FieldError>
        </div>
        <div>
          <Label htmlFor="dueDate">Due date</Label>
          <Input id="dueDate" type="date" {...register("dueDate")} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="relatedDistributionId">Related distribution ID (optional)</Label>
          <Input id="relatedDistributionId" type="number" min={1} {...register("relatedDistributionId")} />
        </div>
        <div className="sm:col-span-2">
          <Label htmlFor="description">Description</Label>
          <Textarea id="description" rows={3} {...register("description")} />
        </div>
      </form>
    </Dialog>
  );
}
