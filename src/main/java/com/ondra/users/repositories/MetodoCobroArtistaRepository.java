package com.ondra.users.repositories;

import com.ondra.users.models.dao.Artista;
import com.ondra.users.models.dao.MetodoCobroArtista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad {@link MetodoCobroArtista}.
 *
 * <p>Proporciona operaciones de acceso a datos para métodos de cobro de artistas,
 * incluyendo consultas filtradas por artista y validaciones de pertenencia.</p>
 */
@Repository
public interface MetodoCobroArtistaRepository extends JpaRepository<MetodoCobroArtista, Long> {

    /**
     * Obtiene todos los métodos de cobro asociados a un artista.
     *
     * @param artista entidad del artista
     * @return lista de métodos de cobro del artista
     */
    List<MetodoCobroArtista> findByArtista(Artista artista);

    /**
     * Busca un método de cobro específico verificando su pertenencia al artista.
     *
     * @param id identificador del método de cobro
     * @param artista entidad del artista propietario
     * @return Optional que contiene el método de cobro si existe y pertenece al artista
     */
    Optional<MetodoCobroArtista> findByIdMetodoCobroArtistaAndArtista(Long id, Artista artista);

    /**
     * Elimina un método de cobro verificando su pertenencia al artista.
     *
     * @param id identificador del método de cobro
     * @param artista entidad del artista propietario
     */
    void deleteByIdMetodoCobroArtistaAndArtista(Long id, Artista artista);

    /**
     * Obtiene todos los métodos de cobro de un artista por su identificador.
     *
     * @param idArtista identificador del artista
     * @return lista de métodos de cobro del artista
     */
    List<MetodoCobroArtista> findByArtista_IdArtista(Long idArtista);

    /**
     * Obtiene el primer método de cobro creado para un artista.
     *
     * @param idArtista identificador del artista
     * @return método de cobro más antiguo o null si no hay registros
     */
    MetodoCobroArtista findFirstByArtista_IdArtistaOrderByFechaCreacionAsc(Long idArtista);
}
