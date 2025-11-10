package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando un refresh token es inválido, expirado o revocado.
 *
 * <p>Esta excepción debe resultar en un código de estado HTTP 400 (Bad Request)
 * ya que indica un problema con los datos enviados por el cliente.</p>
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }

    public InvalidRefreshTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}