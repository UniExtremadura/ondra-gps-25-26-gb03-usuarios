package com.ondra.users.repositories;

import com.ondra.users.models.dao.Seguimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeguimientoRepository extends JpaRepository<Seguimiento, Long> {

    /**
     * Busca un seguimiento específico entre dos usuarios.
     *
     * @param idSeguidor ID del usuario seguidor
     * @param idSeguido ID del usuario seguido
     * @return Optional con el seguimiento si existe
     */
    Optional<Seguimiento> findBySeguidorIdUsuarioAndSeguidoIdUsuario(
            Long idSeguidor,
            Long idSeguido
    );

    /**
     * Verifica si existe un seguimiento entre dos usuarios.
     *
     * @param idSeguidor ID del usuario seguidor
     * @param idSeguido ID del usuario seguido
     * @return true si existe el seguimiento
     */
    boolean existsBySeguidorIdUsuarioAndSeguidoIdUsuario(
            Long idSeguidor,
            Long idSeguido
    );

    /**
     * Obtiene todos los seguimientos donde el usuario es el seguidor.
     * (Lista de usuarios que este usuario sigue)
     *
     * @param idSeguidor ID del usuario seguidor
     * @return Lista de seguimientos
     */
    List<Seguimiento> findBySeguidorIdUsuario(Long idSeguidor);

    /**
     * Obtiene todos los seguimientos donde el usuario es el seguido.
     * (Lista de usuarios que siguen a este usuario)
     *
     * @param idSeguido ID del usuario seguido
     * @return Lista de seguimientos
     */
    List<Seguimiento> findBySeguidoIdUsuario(Long idSeguido);

    /**
     * Cuenta cuántos usuarios sigue un usuario específico.
     *
     * @param idSeguidor ID del usuario seguidor
     * @return Número de usuarios que sigue
     */
    long countBySeguidorIdUsuario(Long idSeguidor);

    /**
     * Cuenta cuántos seguidores tiene un usuario.
     *
     * @param idSeguido ID del usuario seguido
     * @return Número de seguidores
     */
    long countBySeguidoIdUsuario(Long idSeguido);

    /**
     * Elimina todos los seguimientos relacionados con un usuario.
     * Se usa cuando se elimina una cuenta.
     *
     * @param idUsuario1 ID del usuario (como seguidor)
     * @param idUsuario2 ID del usuario (como seguido)
     */
    @Modifying
    @Query("DELETE FROM Seguimiento s WHERE s.seguidor.idUsuario = :idUsuario1 OR s.seguido.idUsuario = :idUsuario2")
    void deleteBySeguidorIdUsuarioOrSeguidoIdUsuario(
            @Param("idUsuario1") Long idUsuario1,
            @Param("idUsuario2") Long idUsuario2
    );

    /**
     * Elimina un seguimiento específico.
     *
     * @param idSeguidor ID del usuario seguidor
     * @param idSeguido ID del usuario seguido
     */
    @Modifying
    @Query("DELETE FROM Seguimiento s WHERE s.seguidor.idUsuario = :idSeguidor AND s.seguido.idUsuario = :idSeguido")
    void deleteBySeguidorIdUsuarioAndSeguidoIdUsuario(
            @Param("idSeguidor") Long idSeguidor,
            @Param("idSeguido") Long idSeguido
    );

    /**
     * Verifica si existe un seguimiento entre dos usuarios.
     *
     * @param idSeguidor ID del usuario seguidor
     * @param idSeguido ID del usuario seguido
     * @return true si el seguimiento existe, false en caso contrario
     */
    boolean existsBySeguidor_IdUsuarioAndSeguido_IdUsuario(Long idSeguidor, Long idSeguido);
}