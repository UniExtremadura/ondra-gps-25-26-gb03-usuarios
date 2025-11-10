package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando se intenta hacer login con Google en una cuenta que no tiene habilitada esta opción.
 *
 * <p>Esta excepción se utiliza cuando un usuario existente intenta autenticarse con Google
 * pero su cuenta tiene el flag {@code permiteGoogle} establecido en {@code false}.
 * Esto ocurre típicamente con cuentas creadas mediante registro tradicional que no han
 * vinculado su cuenta de Google.</p>
 *
 * <p>Código de error API: GOOGLE_LOGIN_DISABLED</p>
 * <p>Código HTTP: 400 Bad Request</p>
 *
 */
public class GoogleLoginDisabledException extends RuntimeException {

    /**
     * Construye una nueva excepción de login con Google deshabilitado con el mensaje especificado.
     *
     * @param message el mensaje de detalle que describe el error
     */
    public GoogleLoginDisabledException(String message) {
        super(message);
    }
}