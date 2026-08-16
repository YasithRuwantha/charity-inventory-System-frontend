import { useEffect, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Settings, HardHat, Play, CheckCheck, Ban, ClipboardList } from "lucide-react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Card, CardBody } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input, Label, FieldError } from "@/components/ui/Input";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { EmptyState } from "@/components/ui/EmptyState";
import { PageSpinner } from "@/components/ui/Skeleton";
import { Tabs } from "@/components/ui/Tabs";
import { ReasonDialog } from "@/components/ui/ReasonDialog";
import { getVolunteerActivity, getVolunteerTasks } from "@/api/volunteers";
import { updateVolunteerTaskStatus, type VolunteerTask } from "@/api/volunteerTasks";
import type { TaskStatus } from "@/types/enums";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { ApiRequestError } from "@/api/types";

const STORAGE_KEY = "charity-my-volunteer-id";
const TASK_TONE: Record<TaskStatus, BadgeTone> = {
  PENDING: "neutral",
  IN_PROGRESS: "info",
  COMPLETED: "success",
  CANCELLED: "danger",
};

export default function MyTasksPage() {
  const queryClient = useQueryClient();
  const [volunteerId, setVolunteerId] = useState<number | null>(() => {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? Number(stored) : null;
  });
  const [setupOpen, setSetupOpen] = useState(!volunteerId);
  const [inputValue, setInputValue] = useState("");
  const [tab, setTab] = useState<"active" | "history" | "activity">("active");
  const [cancelTarget, setCancelTarget] = useState<VolunteerTask | null>(null);

  const { data: tasks, isLoading, error } = useQuery({
    queryKey: ["volunteers", volunteerId, "tasks", "mine"],
    queryFn: () => getVolunteerTasks(volunteerId!, { size: 50, sort: "taskDate,desc" }),
    enabled: !!volunteerId,
    retry: false,
  });

  const { data: activity, isLoading: activityLoading } = useQuery({
    queryKey: ["volunteers", volunteerId, "activity", "mine"],
    queryFn: () => getVolunteerActivity(volunteerId!),
    enabled: !!volunteerId && tab === "activity",
    retry: false,
  });

  const accessError = error instanceof ApiRequestError && (error.status === 403 || error.status === 404);

  useEffect(() => {
    if (accessError) setSetupOpen(true);
  }, [accessError]);

  const statusMutation = useMutation({
    mutationFn: ({ taskId, status, reason }: { taskId: number; status: TaskStatus; reason?: string }) =>
      updateVolunteerTaskStatus(taskId, status, reason),
    onSuccess: () => {
      notifySuccess("Task updated");
      queryClient.invalidateQueries({ queryKey: ["volunteers", volunteerId, "tasks"] });
      setCancelTarget(null);
    },
    onError: (err) => notifyError(err),
  });

  const saveVolunteerId = () => {
    const id = Number(inputValue);
    if (!Number.isFinite(id) || id <= 0) return;
    localStorage.setItem(STORAGE_KEY, String(id));
    setVolunteerId(id);
    setSetupOpen(false);
  };

  if (setupOpen) {
    return (
      <div className="mx-auto max-w-md pt-12">
        <div className="mb-6 flex flex-col items-center text-center">
          <div className="mb-3 flex h-12 w-12 items-center justify-center rounded-full bg-brand-50">
            <HardHat className="h-5.5 w-5.5 text-brand-600" />
          </div>
          <h1 className="font-display text-xl font-bold text-ink-900">Connect your volunteer profile</h1>
          <p className="mt-1.5 text-sm text-ink-500">
            {accessError
              ? "That ID doesn't match your account. Double-check it with your coordinator."
              : "Enter your Volunteer ID, given to you when your coordinator registered you, to see your assigned tasks."}
          </p>
        </div>
        <Card>
          <CardBody>
            <Label htmlFor="volunteerIdInput" required>
              Volunteer ID
            </Label>
            <Input
              id="volunteerIdInput"
              type="number"
              min={1}
              placeholder="e.g. 4"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              invalid={accessError}
            />
            <FieldError>{accessError ? "Access denied for this ID." : undefined}</FieldError>
            <Button className="mt-4 w-full" onClick={saveVolunteerId}>
              Continue
            </Button>
            {volunteerId && (
              <Button variant="ghost" className="mt-2 w-full" onClick={() => setSetupOpen(false)}>
                Cancel
              </Button>
            )}
          </CardBody>
        </Card>
      </div>
    );
  }

  const activeTasks = (tasks?.items ?? []).filter((t) => t.status === "PENDING" || t.status === "IN_PROGRESS");
  const historyTasks = (tasks?.items ?? []).filter((t) => t.status === "COMPLETED" || t.status === "CANCELLED");
  const visibleTasks = tab === "active" ? activeTasks : tab === "history" ? historyTasks : [];

  return (
    <div>
      <PageHeader
        title="My tasks"
        description="Tasks assigned to you by the coordination team."
        actions={
          <Button variant="outline" size="sm" onClick={() => { setInputValue(String(volunteerId)); setSetupOpen(true); }}>
            <Settings className="h-4 w-4" />
            Change ID
          </Button>
        }
      />

      <Card>
        <div className="px-5 pt-4">
          <Tabs
            items={[
              { key: "active", label: "Active", count: activeTasks.length },
              { key: "history", label: "History", count: historyTasks.length },
              { key: "activity", label: "Activity" },
            ]}
            active={tab}
            onChange={(k) => setTab(k as typeof tab)}
          />
        </div>

        {tab === "activity" ? (
          activityLoading ? (
            <PageSpinner />
          ) : !activity || activity.length === 0 ? (
            <EmptyState icon={ClipboardList} title="No activity yet" />
          ) : (
            <ul className="divide-y divide-ink-100">
              {activity.map((a, i) => (
                <li key={i} className="px-5 py-3">
                  <p className="text-sm text-ink-800">{a.description}</p>
                  <p className="mt-0.5 text-xs text-ink-400">{formatDateTime(a.occurredAt)}</p>
                </li>
              ))}
            </ul>
          )
        ) : isLoading ? (
          <PageSpinner />
        ) : visibleTasks.length === 0 ? (
          <EmptyState icon={ClipboardList} title={tab === "active" ? "No active tasks" : "No completed tasks yet"} />
        ) : (
          <ul className="divide-y divide-ink-100">
            {visibleTasks.map((task) => (
              <li key={task.id} className="flex items-start justify-between gap-4 px-5 py-4">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-ink-900">{task.title}</p>
                    <Badge tone={TASK_TONE[task.status]} dot>
                      {toTitleCase(task.status)}
                    </Badge>
                    {task.overdue && task.status !== "COMPLETED" && task.status !== "CANCELLED" && <Badge tone="danger">Overdue</Badge>}
                  </div>
                  {task.description && <p className="mt-1 text-sm text-ink-500">{task.description}</p>}
                  <p className="mt-1 text-xs text-ink-400">
                    Task date {formatDate(task.taskDate)}
                    {task.dueDate && ` · Due ${formatDate(task.dueDate)}`}
                  </p>
                </div>
                {(task.status === "PENDING" || task.status === "IN_PROGRESS") && (
                  <div className="flex shrink-0 items-center gap-1.5">
                    {task.status === "PENDING" && (
                      <Button size="sm" variant="outline" onClick={() => statusMutation.mutate({ taskId: task.id, status: "IN_PROGRESS" })}>
                        <Play className="h-3.5 w-3.5" />
                        Start
                      </Button>
                    )}
                    <Button size="sm" onClick={() => statusMutation.mutate({ taskId: task.id, status: "COMPLETED" })}>
                      <CheckCheck className="h-3.5 w-3.5" />
                      Complete
                    </Button>
                    <Button size="sm" variant="danger-outline" onClick={() => setCancelTarget(task)}>
                      <Ban className="h-3.5 w-3.5" />
                    </Button>
                  </div>
                )}
              </li>
            ))}
          </ul>
        )}
      </Card>

      <ReasonDialog
        open={!!cancelTarget}
        onClose={() => setCancelTarget(null)}
        onConfirm={(reason) => cancelTarget && statusMutation.mutate({ taskId: cancelTarget.id, status: "CANCELLED", reason })}
        title="Cancel this task?"
        confirmLabel="Cancel task"
        danger
        loading={statusMutation.isPending}
      />
    </div>
  );
}
