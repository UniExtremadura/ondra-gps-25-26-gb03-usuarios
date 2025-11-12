package com.ondra.users.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción lanzada cuando los datos proporcionados son inválidos
 * o no cumplen con las reglas de negocio.
 *
 * <p>Se utiliza para validaciones personalizadas que van más allá
 * de las validaciones estándar de Bean Validation (@Valid).</p>
 *
 * <p>Genera una respuesta HTTP 400 (Bad Request) con el código de error
 * <code>INVALID_DATA</code> según la especificación de la API.</p>
 *
 * <p>Ejemplos de uso:</p>
 * <ul>
 *   <li>Validación de límites en parámetros de consulta</li>
 *   <li>Validación de estados o combinaciones de campos</li>
 *   <li>Validación de formatos específicos del dominio</li>
 * </ul>
 *
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDataException extends RuntimeException {

    /**
     * Construye una nueva excepción con un mensaje personalizado
     * describiendo el error de validación.
     *
     * @param message Descripción del error de validación
     */
    public InvalidDataException(String message) {
        super(message);
    }
}