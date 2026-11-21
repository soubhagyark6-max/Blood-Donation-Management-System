package com.lifelink.model;

/** Blood components. They are tracked separately and are never interchangeable in stock. */
public enum ComponentType {
    WHOLE_BLOOD("Whole Blood"),
    RED_CELLS("Red Blood Cells"),
    PLATELETS("Platelets"),
    PLASMA("Plasma");

    private final String label;

    ComponentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
