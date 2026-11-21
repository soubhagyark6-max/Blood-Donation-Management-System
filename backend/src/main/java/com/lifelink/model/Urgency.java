package com.lifelink.model;

public enum Urgency {
    NORMAL("Normal", "secondary"),
    URGENT("Urgent", "warning"),
    EMERGENCY("Emergency", "danger");

    private final String label;
    private final String badge;

    Urgency(String label, String badge) {
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
