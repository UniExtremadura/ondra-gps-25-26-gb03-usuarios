package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando no se proporciona ningún archivo en la petición.
 */
public class NoFileProvidedException extends RuntimeException {
    public NoFileProvidedException(String message) {
        super(message);
    }
}