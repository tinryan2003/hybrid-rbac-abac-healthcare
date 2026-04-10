package org.vgu.userservice.exception;

/**
 * Exception thrown when duplicate user is detected
 */
public class DuplicateUserException extends RuntimeException {
    public DuplicateUserException(String message) {
        super(message);
    }
}

