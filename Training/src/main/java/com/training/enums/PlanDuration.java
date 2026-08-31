package com.training.enums;

public enum PlanDuration {
    ONE_MONTH(1),
    TWO_MONTHS(2),
    THREE_MONTHS(3),
    SIX_MONTHS(6),
    TWELVE_MONTHS(12);

    private final int months;

    PlanDuration(int months) {
        this.months = months;
    }

    public int getMonths() {
        return months;
    }
}
