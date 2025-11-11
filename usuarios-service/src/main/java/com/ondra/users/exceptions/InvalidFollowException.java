package com.ondra.users.exceptions;

public class InvalidFollowException extends RuntimeException {
    public InvalidFollowException(String message) {
        super(message);
    }
}