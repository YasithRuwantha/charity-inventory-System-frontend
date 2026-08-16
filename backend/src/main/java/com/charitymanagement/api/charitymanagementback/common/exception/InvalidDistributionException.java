package com.charitymanagement.api.charitymanagementback.common.exception;

import org.springframework.http.HttpStatus;

/** Illegal distribution state transition, e.g. approving a cancelled request. */
public class InvalidDistributionException extends ApiException {

    public InvalidDistributionException(String message) {
        super(message, HttpStatus.CONFLICT, "INVALID_DISTRIBUTION_STATE");
    }

    public InvalidDistributionException(String message, String errorCode) {
        super(message, HttpStatus.CONFLICT, errorCode);
    }
}
