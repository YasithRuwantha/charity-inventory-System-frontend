package com.charitymanagement.api.charitymanagementback.volunteer.enums;

import java.util.Set;

public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    /** Allowed next states. COMPLETED and CANCELLED are final, so a finished task cannot be reopened. */
    public Set<TaskStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> Set.of(IN_PROGRESS, COMPLETED, CANCELLED);
            case IN_PROGRESS -> Set.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> Set.of();
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
