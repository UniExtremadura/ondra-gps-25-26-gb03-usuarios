package com.ondra.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para la autenticación de un usuario mediante Google OAuth.
 *
 * <p>Contiene el token proporcionado por Google que permite verificar la identidad
 * del usuario y generar un JWT en el sistema.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginGoogleDTO {

    /**
     * Token de Google obtenido tras la autenticación en el cliente.
     *
     * <p>Este campo es obligatorio para validar al usuario mediante Firebase/Google.</p>
     */
    @NotBlank(message = "El token de Google es obligatorio")
    private String idToken;
}