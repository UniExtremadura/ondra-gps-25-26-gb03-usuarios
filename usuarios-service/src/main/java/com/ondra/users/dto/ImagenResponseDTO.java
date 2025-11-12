package com.ondra.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para respuestas de operaciones de subida de imágenes.
 *
 * <p>Contiene la URL de la imagen subida y un mensaje descriptivo.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImagenResponseDTO {

    /**
     * URL pública de la imagen en Cloudinary.
     *
     * <p>Ejemplo: https://res.cloudinary.com/demo/image/upload/v1234567890/images/usuarios/abc123.jpg</p>
     */
    private String url;

    /**
     * Mensaje descriptivo de la operación.
     */
    private String mensaje;
}