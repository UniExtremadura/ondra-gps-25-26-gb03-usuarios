package com.ondra.users.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando un usuario intenta realizar una operación
 * sin los permisos necesarios.
 *
 * <p>Se utiliza principalmente para validar que un usuario solo pueda
 * modificar o eliminar sus propios recursos (artistas, pagos, etc.).</p>
 *
 * <p>Genera una respuesta HTTP 403 (Forbidden) con el código de error
 * <code>FORBIDDEN</code> según la especificación de la API.</p>
 *
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenAccessException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje personalizado
     * describiendo la violación de permisos.
     *
     * @param message Descripción del error de permisos
     */
    public ForbiddenAccessException(String message) {
        super(message);
    }
}