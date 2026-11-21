package com.lifelink.model;

/** The four user roles. Spring Security maps them to ROLE_ADMIN, ROLE_STAFF, ... */
public enum Role {
    ADMIN("Admin"),
    STAFF("Blood Bank Staff"),
    DONOR("Donor"),
    RECIPIENT("Recipient");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
