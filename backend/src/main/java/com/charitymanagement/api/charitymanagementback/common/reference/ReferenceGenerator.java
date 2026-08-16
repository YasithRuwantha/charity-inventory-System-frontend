package com.charitymanagement.api.charitymanagementback.common.reference;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Concurrency-safe generator for human-readable references such as {@code INV-000001} and
 * {@code DON-2026-000001}.
 *
 * <p>Numbering may contain gaps when a caller's transaction rolls back. That is a deliberate
 * trade: reserving the number in its own locked transaction is what guarantees two concurrent
 * requests never produce the same reference, which {@code count() + 1} cannot do.
 */
@Service
@RequiredArgsConstructor
public class ReferenceGenerator {

    public static final String INVENTORY_ITEM = "INV";
    public static final String DONOR = "DONOR";
    public static final String DONATION = "DON";
    public static final String BENEFICIARY = "BEN";
    public static final String DISTRIBUTION = "DIST";
    public static final String VOLUNTEER = "VOL";

    private static final int PAD_WIDTH = 6;

    private final ReferenceSequenceService sequenceService;

    /** {@code PREFIX-000001} */
    public String next(String prefix) {
        return prefix + "-" + pad(sequenceService.nextValue(prefix));
    }

    /** {@code PREFIX-YYYY-000001}, restarting the counter each calendar year. */
    public String nextYearly(String prefix) {
        return nextYearly(prefix, LocalDate.now().getYear());
    }

    public String nextYearly(String prefix, int year) {
        String sequenceName = prefix + "-" + year;
        return sequenceName + "-" + pad(sequenceService.nextValue(sequenceName));
    }

    private static String pad(long value) {
        return String.format("%0" + PAD_WIDTH + "d", value);
    }
}
