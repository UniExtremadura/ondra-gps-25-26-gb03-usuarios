package com.ondra.users.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeguimientoDTO {
    private Long idSeguimiento;
    private UsuarioBasicoDTO seguidor;
    private UsuarioBasicoDTO seguido;
    private LocalDateTime fechaSeguimiento;
}