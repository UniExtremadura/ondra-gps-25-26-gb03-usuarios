package com.ondra.users.exceptions;

public class DuplicateFollowException extends RuntimeException {
    public DuplicateFollowException(String message) {
        super(message);
    }
}