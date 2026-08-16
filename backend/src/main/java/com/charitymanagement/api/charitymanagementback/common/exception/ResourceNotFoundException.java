package com.charitymanagement.api.charitymanagementback.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " not found: " + identifier, HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
}
