package com.ondra.users.models.dao;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la relación de seguimiento entre usuarios.
 *
 * <p>Reglas de negocio:
 * <ul>
 *   <li>Un usuario NORMAL puede seguir a artistas y otros usuarios normales</li>
 *   <li>Un ARTISTA NO puede seguir a nadie (solo puede ser seguido)</li>
 *   <li>No se puede seguir a uno mismo</li>
 *   <li>No se pueden crear seguimientos duplicados</li>
 * </ul>
 * </p>
 */
@Entity
@Table(
        name = "Seguimientos",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"id_seguidor", "id_seguido"},
                name = "uk_seguimiento_unico"
        ),
        indexes = {
                @Index(name = "idx_seguidor", columnList = "id_seguidor"),
                @Index(name = "idx_seguido", columnList = "id_seguido"),
                @Index(name = "idx_fecha_seguimiento", columnList = "fecha_seguimiento")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seguimiento {

    /**
     * Identificador único del seguimiento.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idSeguimiento;

    /**
     * Usuario que sigue (follower).
     *
     * <p>DEBE ser un usuario de tipo NORMAL.
     * Los artistas NO pueden seguir a nadie.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seguidor", nullable = false)
    private Usuario seguidor;

    /**
     * Usuario seguido (following).
     *
     * <p>Puede ser un usuario NORMAL o ARTISTA.
     * Representa a quien se está siguiendo.</p>
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_seguido", nullable = false)
    private Usuario seguido;

    /**
     * Fecha y hora en que se realizó el seguimiento.
     */
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime fechaSeguimiento = LocalDateTime.now();
}