package org.vgu.labservice.exception;

public class LabNotFoundException extends RuntimeException {
    public LabNotFoundException(String message) {
        super(message);
    }
}
