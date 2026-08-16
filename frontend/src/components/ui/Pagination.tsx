import { ChevronLeft, ChevronRight } from "lucide-react";
import type { PaginationMeta } from "@/api/types";
import { Button } from "@/components/ui/Button";

interface PaginationProps {
  meta: PaginationMeta;
  onPageChange: (page: number) => void;
}

export function Pagination({ meta, onPageChange }: PaginationProps) {
  const { page, totalPages, totalElements, size } = meta;
  if (totalElements === 0) return null;

  const start = page * size + 1;
  const end = Math.min((page + 1) * size, totalElements);

  return (
    <div className="flex items-center justify-between border-t border-ink-100 px-5 py-3">
      <p className="text-sm text-ink-500">
        Showing <span className="font-medium text-ink-700">{start}</span>–
        <span className="font-medium text-ink-700">{end}</span> of{" "}
        <span className="font-medium text-ink-700">{totalElements}</span>
      </p>
      <div className="flex items-center gap-1.5">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
        >
          <ChevronLeft className="h-4 w-4" />
          Previous
        </Button>
        <span className="px-2 text-sm text-ink-500">
          Page {page + 1} of {Math.max(totalPages, 1)}
        </span>
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Next
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
