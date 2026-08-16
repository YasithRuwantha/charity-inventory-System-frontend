package com.charitymanagement.api.charitymanagementback.common.exception;

import org.springframework.http.HttpStatus;

public class ExpiredInventoryException extends ApiException {

    public ExpiredInventoryException(String message) {
        super(message, HttpStatus.CONFLICT, "EXPIRED_INVENTORY");
    }
}
