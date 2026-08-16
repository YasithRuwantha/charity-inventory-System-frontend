package com.charitymanagement.api.charitymanagementback.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Business tuning knobs, all overridable from application.properties. */
@Component
@ConfigurationProperties(prefix = "charity")
@Getter
@Setter
public class CharityProperties {

    private final Distribution distribution = new Distribution();
    private final Inventory inventory = new Inventory();
    private final Organization organization = new Organization();

    @Getter
    @Setter
    public static class Distribution {
        /** Window used to flag a repeat of the same item to the same beneficiary. */
        private int duplicateDays = 30;
    }

    @Getter
    @Setter
    public static class Inventory {
        /** Horizon used by the "expiring soon" queries and dashboard alerts. */
        private int expiringSoonDays = 30;
    }

    @Getter
    @Setter
    public static class Organization {
        /** Printed on donation receipts. */
        private String name = "Charity Inventory Management System";
        private String address = "";
        private String contact = "";
    }
}
