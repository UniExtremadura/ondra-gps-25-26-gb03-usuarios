package com.ondra.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para representar una respuesta exitosa de la API.
 *
 * <p>Incluye información sobre el resultado, mensaje descriptivo,
 * código de estado HTTP y marca temporal.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuccessfulResponseDTO {

    /**
     * Identificador o código de éxito de la operación.
     */
    private String successful;

    /**
     * Mensaje descriptivo de la operación realizada.
     */
    private String message;

    /**
     * Código de estado HTTP asociado a la respuesta.
     */
    private int statusCode;

    /**
     * Marca temporal de cuándo se generó la respuesta.
     */
    private String timestamp;
}