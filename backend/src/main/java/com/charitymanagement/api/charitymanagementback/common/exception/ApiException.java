package com.charitymanagement.api.charitymanagementback.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Base class for every business exception surfaced through the REST layer. */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    protected ApiException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
