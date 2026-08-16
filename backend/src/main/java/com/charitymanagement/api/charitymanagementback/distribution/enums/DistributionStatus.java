package com.charitymanagement.api.charitymanagementback.distribution.enums;

/**
 * Lifecycle of an aid request. Stock is only ever deducted on the move into
 * {@link #COMPLETED} — every earlier state leaves inventory untouched.
 */
public enum DistributionStatus {
    PENDING,
    APPROVED,
    REJECTED,
    COMPLETED,
    CANCELLED;

    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED || this == CANCELLED;
    }
}
