package com.ondra.users.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticasSeguimientoDTO {
    private Long idUsuario;
    private long seguidos;
    private long seguidores;
}