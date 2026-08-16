import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, Pencil, Plus, HardHat, MoreVertical, Play, CheckCheck, Ban } from "lucide-react";
import { Card, CardHeader } from "@/components/ui/Card";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Table, THead, TBody, TR, TH, TD } from "@/components/ui/Table";
import { PageSpinner, TableSkeleton } from "@/components/ui/Skeleton";
import { EmptyState } from "@/components/ui/EmptyState";
import { Tabs } from "@/components/ui/Tabs";
import { DropdownMenu, DropdownItem } from "@/components/ui/DropdownMenu";
import { ReasonDialog } from "@/components/ui/ReasonDialog";
import { getVolunteer, getVolunteerActivity, getVolunteerTasks } from "@/api/volunteers";
import { updateVolunteerTaskStatus, type VolunteerTask } from "@/api/volunteerTasks";
import { useAuthStore } from "@/store/auth";
import type { TaskStatus } from "@/types/enums";
import { formatDate, formatDateTime, toTitleCase } from "@/lib/utils";
import { notifyError, notifySuccess } from "@/lib/toast";
import { VolunteerFormDialog } from "@/features/volunteers/VolunteerFormDialog";
import { TaskFormDialog } from "@/features/volunteers/TaskFormDialog";

const TASK_TONE: Record<TaskStatus, BadgeTone> = {
  PENDING: "neutral",
  IN_PROGRESS: "info",
  COMPLETED: "success",
  CANCELLED: "danger",
};

export default function VolunteerDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const role = useAuthStore((s) => s.user?.role);
  const isAdmin = role === "ADMIN";
  const volunteerId = Number(id);

  const [tab, setTab] = useState<"tasks" | "activity">("tasks");
  const [formOpen, setFormOpen] = useState(false);
  const [taskFormOpen, setTaskFormOpen] = useState(false);
  const [editingTask, setEditingTask] = useState<VolunteerTask | null>(null);
  const [cancelTarget, setCancelTarget] = useState<VolunteerTask | null>(null);

  const { data: volunteer, isLoading } = useQuery({
    queryKey: ["volunteers", volunteerId],
    queryFn: () => getVolunteer(volunteerId),
    enabled: Number.isFinite(volunteerId),
  });

  const { data: tasks, isLoading: tasksLoading } = useQuery({
    queryKey: ["volunteers", volunteerId, "tasks"],
    queryFn: () => getVolunteerTasks(volunteerId, { size: 20, sort: "taskDate,desc" }),
    enabled: Number.isFinite(volunteerId) && tab === "tasks",
  });

  const { data: activity, isLoading: activityLoading } = useQuery({
    queryKey: ["volunteers", volunteerId, "activity"],
    queryFn: () => getVolunteerActivity(volunteerId),
    enabled: Number.isFinite(volunteerId) && tab === "activity",
  });

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

  if (isLoading) return <PageSpinner />;
  if (!volunteer) return <EmptyState icon={HardHat} title="Volunteer not found" />;

  return (
    <div>
      <button onClick={() => navigate(-1)} className="mb-4 inline-flex items-center gap-1.5 text-sm font-medium text-ink-500 hover:text-ink-800">
        <ArrowLeft className="h-4 w-4" /> Back
      </button>

      <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <h1 className="font-display text-xl font-bold text-ink-900">{volunteer.volunteerName}</h1>
            <Badge tone={volunteer.status === "ACTIVE" ? "success" : "neutral"} dot>
              {toTitleCase(volunteer.status)}
            </Badge>
          </div>
          <p className="mt-1 text-sm text-ink-500">
            {volunteer.volunteerCode}
            {volunteer.joinedDate && ` · Joined ${formatDate(volunteer.joinedDate)}`}
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            onClick={() => {
              setEditingTask(null);
              setTaskFormOpen(true);
            }}
          >
            <Plus className="h-4 w-4" />
            Assign task
          </Button>
          {isAdmin && (
            <Button onClick={() => setFormOpen(true)}>
              <Pencil className="h-4 w-4" />
              Edit
            </Button>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-1">
          <CardHeader title="Contact" />
          <div className="px-5 py-4">
            <dl className="space-y-3">
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Phone</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{volunteer.phone || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Email</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{volunteer.email || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Address</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{volunteer.address || "—"}</dd>
              </div>
              <div>
                <dt className="text-xs font-medium uppercase tracking-wide text-ink-400">Notes</dt>
                <dd className="mt-0.5 text-sm text-ink-800">{volunteer.notes || "—"}</dd>
              </div>
            </dl>
          </div>
        </Card>

        <Card className="lg:col-span-2">
          <div className="px-5 pt-4">
            <Tabs
              items={[
                { key: "tasks", label: "Tasks" },
                { key: "activity", label: "Activity" },
              ]}
              active={tab}
              onChange={(k) => setTab(k as "tasks" | "activity")}
            />
          </div>

          {tab === "tasks" ? (
            tasksLoading ? (
              <TableSkeleton cols={4} />
            ) : !tasks || tasks.items.length === 0 ? (
              <EmptyState title="No tasks assigned yet" />
            ) : (
              <Table>
                <THead>
                  <TR>
                    <TH>Task</TH>
                    <TH>Date</TH>
                    <TH>Status</TH>
                    <TH className="w-10" />
                  </TR>
                </THead>
                <TBody>
                  {tasks.items.map((task) => (
                    <TR key={task.id}>
                      <TD>
                        <p className="font-medium text-ink-900">{task.title}</p>
                        {task.overdue && task.status !== "COMPLETED" && task.status !== "CANCELLED" && (
                          <p className="text-xs text-rose-600">Overdue</p>
                        )}
                      </TD>
                      <TD className="text-ink-500">{formatDate(task.taskDate)}</TD>
                      <TD>
                        <Badge tone={TASK_TONE[task.status]} dot>
                          {toTitleCase(task.status)}
                        </Badge>
                      </TD>
                      <TD>
                        {task.status !== "COMPLETED" && task.status !== "CANCELLED" && (
                          <DropdownMenu
                            trigger={
                              <button className="rounded-md p-1.5 text-ink-400 hover:bg-ink-100 hover:text-ink-700">
                                <MoreVertical className="h-4 w-4" />
                              </button>
                            }
                          >
                            {task.status === "PENDING" && (
                              <DropdownItem
                                icon={<Play className="h-4 w-4" />}
                                onClick={() => statusMutation.mutate({ taskId: task.id, status: "IN_PROGRESS" })}
                              >
                                Start
                              </DropdownItem>
                            )}
                            <DropdownItem
                              icon={<CheckCheck className="h-4 w-4" />}
                              onClick={() => statusMutation.mutate({ taskId: task.id, status: "COMPLETED" })}
                            >
                              Mark complete
                            </DropdownItem>
                            <DropdownItem
                              icon={<Pencil className="h-4 w-4" />}
                              onClick={() => {
                                setEditingTask(task);
                                setTaskFormOpen(true);
                              }}
                            >
                              Edit
                            </DropdownItem>
                            <DropdownItem icon={<Ban className="h-4 w-4" />} danger onClick={() => setCancelTarget(task)}>
                              Cancel
                            </DropdownItem>
                          </DropdownMenu>
                        )}
                      </TD>
                    </TR>
                  ))}
                </TBody>
              </Table>
            )
          ) : activityLoading ? (
            <TableSkeleton cols={2} />
          ) : !activity || activity.length === 0 ? (
            <EmptyState title="No activity recorded yet" />
          ) : (
            <ul className="divide-y divide-ink-100">
              {activity.map((a, i) => (
                <li key={i} className="px-5 py-3">
                  <p className="text-sm text-ink-800">{a.description}</p>
                  <p className="mt-0.5 text-xs text-ink-400">{formatDateTime(a.occurredAt)}</p>
                </li>
              ))}
            </ul>
          )}
        </Card>
      </div>

      <VolunteerFormDialog open={formOpen} onClose={() => setFormOpen(false)} volunteer={volunteer} />
      <TaskFormDialog open={taskFormOpen} onClose={() => setTaskFormOpen(false)} volunteerId={volunteerId} task={editingTask} />
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
