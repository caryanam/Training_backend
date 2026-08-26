package com.training.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum LeadStatus {
    NEW("new", "New"),
    ASSIGNED("assigned", "Assigned"),
    CONTACTED("contacted", "Contacted"),
    DEMO_SCHEDULED("demo_scheduled", "Demo Scheduled"),
    DEMO_COMPLETED("demo_completed", "Demo Completed"),
    INTERESTED("interested", "Interested"),
    PAYMENT_PENDING("payment_pending", "Payment Pending"),
    ENROLLED("enrolled", "Enrolled"),
    NOT_INTERESTED("not_interested", "Not Interested"),
    REJECTED("not_interested", "Not Interested"),
    FOLLOW_UP_REQUIRED("follow_up_required", "Follow Up Required"),
    CLOSED("closed", "Closed");

    private final String value;
    private final String label;

    LeadStatus(String value, String label) {
        this.value = value;
        this.label = label;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static LeadStatus fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return NEW;
        }
        String clean = text.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        
        if ("DEMO_SCHEDULE".equals(clean)) {
            return DEMO_SCHEDULED;
        }
        if ("DEMO_COMPLETE".equals(clean)) {
            return DEMO_COMPLETED;
        }
        if ("NOT_INTERESTED".equals(clean) || "REJECTED".equals(clean)) {
            return NOT_INTERESTED;
        }

        for (LeadStatus status : LeadStatus.values()) {
            if (status.name().equalsIgnoreCase(clean) || status.value.equalsIgnoreCase(text.trim())) {
                return status;
            }
        }

        try {
            return LeadStatus.valueOf(clean);
        } catch (IllegalArgumentException e) {
            return NEW;
        }
    }
}
