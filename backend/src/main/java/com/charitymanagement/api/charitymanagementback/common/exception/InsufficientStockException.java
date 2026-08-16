package com.charitymanagement.api.charitymanagementback.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {

    public InsufficientStockException(String message) {
        super(message, HttpStatus.CONFLICT, "INSUFFICIENT_STOCK");
    }

    public static InsufficientStockException forItem(String itemCode, String itemName, int available, int requested) {
        return new InsufficientStockException(
                "Insufficient inventory for " + itemName + " (" + itemCode + "): available " + available
                        + ", requested " + requested);
    }
}
