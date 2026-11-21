package com.lifelink.model;

/** How a donor is willing to be contacted. NONE means staff must not contact the donor directly. */
public enum ContactPreference {
    PHONE_AND_EMAIL("Phone or email"),
    EMAIL_ONLY("Email only"),
    NONE("No direct contact (through the blood bank only)");

    private final String label;

    ContactPreference(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
