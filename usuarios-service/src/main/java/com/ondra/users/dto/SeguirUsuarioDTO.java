package com.ondra.users.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguirUsuarioDTO {
    @NotNull(message = "El ID del usuario a seguir es obligatorio")
    private Long idUsuarioASeguir;
}