package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando el token de autenticación de Google/Firebase es inválido o ha expirado.
 *
 * <p>Esta excepción se utiliza durante el proceso de login con Google cuando
 * Firebase no puede verificar el token proporcionado, ya sea porque es inválido,
 * ha expirado, o ha sido manipulado.</p>
 *
 * <p>Código de error API: INVALID_GOOGLE_TOKEN</p>
 * <p>Código HTTP: 400 Bad Request</p>
 *
 */
public class InvalidGoogleTokenException extends RuntimeException {

    /**
     * Construye una nueva excepción de token de Google inválido con el mensaje especificado.
     *
     * @param message el mensaje de detalle que describe el error
     */
    public InvalidGoogleTokenException(String message) {
        super(message);
    }
}