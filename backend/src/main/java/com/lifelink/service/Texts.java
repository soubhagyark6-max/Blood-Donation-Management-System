package com.lifelink.service;

/** Small text helpers shared by services. */
public final class Texts {

    private Texts() {
    }

    /** Builds a safe SQL LIKE pattern: escapes % and _ typed by the user, lower-cases, wraps with %. */
    public static String like(String input) {
        String s = input.trim().toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + s + "%";
    }

    public static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    public static String blankToNull(String s) {
        return hasText(s) ? s.trim() : null;
    }
}
