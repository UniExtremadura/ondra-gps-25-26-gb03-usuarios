package com.ondra.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO básico que representa un método de cobro.
 *
 * Se utiliza para compartir información mínima del método entre distintos
 * microservicios, incluyendo su identificador y el tipo de método
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoCobroBasicoDTO {

    /** Identificador único del método de cobro. */
    private Long idMetodoCobro;

    /** Tipo de método de cobro (PAYPAL, BIZUM o TRANSFERENCIA). */
    private String tipo;
}
