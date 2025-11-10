package com.ondra.users.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecuperarPasswordDTO {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Email inválido")
    private String emailUsuario;
}