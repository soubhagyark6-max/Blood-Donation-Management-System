package com.lifelink.model;

public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    OTHER("Other / Prefer not to say");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
