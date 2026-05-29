package com.r2s.core.exception;

/**
 * Base exception cho cac loi business logic (vi pham rule, invalid state...).
 *
 * <p>Khac voi {@link CustomException}: BusinessException la base class
 * de cac domain exception ke thua (DuplicateUsernameException, ...).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
