package com.lifelink.model;

public enum NotificationType {
    REQUEST_STATUS("Request update", "bi-clipboard2-pulse"),
    EMERGENCY("Emergency request", "bi-exclamation-triangle-fill"),
    LOW_STOCK("Low stock", "bi-droplet-half"),
    EXPIRY("Expiring stock", "bi-hourglass-split"),
    CAMP("Donation camp", "bi-calendar-event"),
    APPOINTMENT("Appointment", "bi-calendar-check"),
    SYSTEM("System", "bi-info-circle");

    private final String label;
    private final String icon;

    NotificationType(String label, String icon) {
        this.label = label;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap Icons class. */
    public String getIcon() {
        return icon;
    }
}
