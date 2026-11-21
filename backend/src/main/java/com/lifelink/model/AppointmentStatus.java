package com.lifelink.model;

public enum AppointmentStatus {
    BOOKED("Booked", "primary"),
    COMPLETED("Completed", "success"),
    CANCELLED("Cancelled", "secondary"),
    NO_SHOW("No show", "warning");

    private final String label;
    private final String badge;

    AppointmentStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    public String getBadge() {
        return badge;
    }
}
