package com.r2s.core.exception;

/**
 * Throw khi register/update voi email da ton tai.
 *
 * <p>Mapped to HTTP 409 Conflict trong {@link GlobalExceptionHandler}.
 */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String email) {
        super("Email already exists: " + email);
    }
}
