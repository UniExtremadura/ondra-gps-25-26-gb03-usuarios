package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando las credenciales de autenticación proporcionadas son incorrectas.
 *
 * <p>Esta excepción se utiliza en el proceso de login tradicional cuando el email
 * o la contraseña no coinciden con ningún usuario registrado, o cuando se intenta
 * hacer login con contraseña en una cuenta que solo permite autenticación con Google.</p>
 *
 * <p>Código de error API: INVALID_CREDENTIALS</p>
 * <p>Código HTTP: 401 Unauthorized</p>
 *
 */
public class InvalidCredentialsException extends RuntimeException {

    /**
     * Construye una nueva excepción de credenciales inválidas con el mensaje especificado.
     *
     * @param message el mensaje de detalle que describe el error
     */
    public InvalidCredentialsException(String message) {
        super(message);
    }
}