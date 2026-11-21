package com.lifelink.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Blood request workflow:
 * PENDING -> UNDER_REVIEW -> APPROVED -> FULFILLED, with REJECTED possible before fulfilment,
 * and CANCELLED when the recipient withdraws a still-pending request.
 */
public enum RequestStatus {
    PENDING("Pending", "warning"),
    UNDER_REVIEW("Under Review", "info"),
    APPROVED("Approved", "primary"),
    FULFILLED("Fulfilled", "success"),
    REJECTED("Rejected", "danger"),
    CANCELLED("Cancelled", "secondary");

    private final String label;
    private final String badge;

    RequestStatus(String label, String badge) {
        this.label = label;
        this.badge = badge;
    }

    public String getLabel() {
        return label;
    }

    /** Bootstrap colour name used for the status badge. */
    public String getBadge() {
        return badge;
    }

    /** Statuses staff may move this request to. */
    public Set<RequestStatus> getAllowedNext() {
        switch (this) {
            case PENDING:
                return EnumSet.of(UNDER_REVIEW, APPROVED, REJECTED);
            case UNDER_REVIEW:
                return EnumSet.of(APPROVED, REJECTED);
            case APPROVED:
                return EnumSet.of(FULFILLED, REJECTED);
            default:
                return EnumSet.noneOf(RequestStatus.class);
        }
    }

    public boolean canMoveTo(RequestStatus next) {
        return getAllowedNext().contains(next);
    }

    /** True while the request is still being worked on. */
    public boolean isOpen() {
        return this == PENDING || this == UNDER_REVIEW || this == APPROVED;
    }
}
