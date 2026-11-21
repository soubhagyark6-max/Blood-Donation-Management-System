package com.lifelink.model;

/** Kind of inventory movement. */
public enum TxType {
    RECEIVED("Received", "success"),
    ISSUED("Issued", "primary"),
    DISCARDED("Discarded (expired)", "secondary");

    private final String label;
    private final String badge;

    TxType(String label, String badge) {
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
