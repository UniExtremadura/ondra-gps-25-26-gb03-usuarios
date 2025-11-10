package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando se intenta autenticar con una cuenta que está marcada como inactiva.
 *
 * <p>Esta excepción se utiliza tanto en login tradicional como en login con Google
 * para prevenir el acceso a cuentas que han sido desactivadas por el usuario o
 * por el sistema.</p>
 *
 * <p>Código de error API: ACCOUNT_INACTIVE</p>
 * <p>Código HTTP: 401 Unauthorized</p>
 *
 */
public class AccountInactiveException extends RuntimeException {

    /**
     * Construye una nueva excepción de cuenta inactiva con el mensaje especificado.
     *
     * @param message el mensaje de detalle que describe el error
     */
    public AccountInactiveException(String message) {
        super(message);
    }
}