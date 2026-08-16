package com.charitymanagement.api.charitymanagementback.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Raised when a beneficiary is about to receive an item they already received inside the
 * configured duplicate window and no authorised ADMIN override was supplied.
 */
public class DuplicateDistributionException extends ApiException {

    private final transient Object details;

    public DuplicateDistributionException(String message, Object details) {
        super(message, HttpStatus.CONFLICT, "DUPLICATE_DISTRIBUTION");
        this.details = details;
    }

    public Object getDetails() {
        return details;
    }
}
