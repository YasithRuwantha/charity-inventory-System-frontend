import { useEffect, useState } from "react";
import { Dialog } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";
import { Textarea, Label, FieldError } from "@/components/ui/Input";

interface ReasonDialogProps {
  open: boolean;
  onClose: () => void;
  onConfirm: (reason: string) => void;
  title: string;
  description?: string;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
  placeholder?: string;
}

export function ReasonDialog({
  open,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = "Confirm",
  danger,
  loading,
  placeholder = "Explain why…",
}: ReasonDialogProps) {
  const [reason, setReason] = useState("");
  const [touched, setTouched] = useState(false);

  useEffect(() => {
    if (open) {
      setReason("");
      setTouched(false);
    }
  }, [open]);

  const invalid = touched && reason.trim().length === 0;

  const handleConfirm = () => {
    if (reason.trim().length === 0) {
      setTouched(true);
      return;
    }
    onConfirm(reason.trim());
  };

  return (
    <Dialog
      open={open}
      onClose={onClose}
      title={title}
      description={description}
      size="sm"
      footer={
        <>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button variant={danger ? "danger" : "primary"} onClick={handleConfirm} loading={loading}>
            {confirmLabel}
          </Button>
        </>
      }
    >
      <Label htmlFor="reason" required>
        Reason
      </Label>
      <Textarea
        id="reason"
        rows={3}
        autoFocus
        placeholder={placeholder}
        value={reason}
        invalid={invalid}
        onChange={(e) => setReason(e.target.value)}
        onBlur={() => setTouched(true)}
      />
      <FieldError>{invalid ? "A reason is required." : undefined}</FieldError>
    </Dialog>
  );
}
