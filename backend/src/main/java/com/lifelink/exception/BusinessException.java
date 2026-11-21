package com.lifelink.exception;

/** A rule of the system was broken (e.g. not enough stock). The message is safe to show to the user. */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
