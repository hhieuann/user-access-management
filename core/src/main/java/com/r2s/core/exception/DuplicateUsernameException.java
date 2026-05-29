package com.r2s.core.exception;

/**
 * Throw khi register username da ton tai.
 *
 * <p>Mapped to HTTP 409 Conflict trong {@link GlobalExceptionHandler}.
 */
public class DuplicateUsernameException extends BusinessException {

    public DuplicateUsernameException(String username) {
        super("Username already exists: " + username);
    }
}
