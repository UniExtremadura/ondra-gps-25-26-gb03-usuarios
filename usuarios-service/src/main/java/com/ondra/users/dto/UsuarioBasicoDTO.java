package com.ondra.users.dto;

import com.ondra.users.models.enums.TipoUsuario;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsuarioBasicoDTO {
    private Long idUsuario;
    private String nombreUsuario;
    private String apellidosUsuario;
    private String fotoPerfil;
    private TipoUsuario tipoUsuario;
}