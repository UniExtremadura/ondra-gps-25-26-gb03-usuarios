package com.ondra.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO utilizado para la autenticación de un usuario mediante email y contraseña.
 *
 * <p>Contiene las credenciales necesarias para generar un token JWT en el sistema.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginUsuarioDTO {

    /**
     * Correo electrónico del usuario.
     *
     * <p>Debe ser un email válido y es obligatorio para el login.</p>
     */
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email es inválido")
    private String emailUsuario;

    /**
     * Contraseña del usuario.
     *
     * <p>Es obligatoria para la autenticación.</p>
     */
    @NotBlank(message = "La contraseña es obligatoria")
    private String passwordUsuario;
}