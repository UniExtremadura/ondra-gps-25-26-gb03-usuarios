package com.ondra.users.services;

import com.ondra.users.dto.EstadisticasSeguimientoDTO;
import com.ondra.users.dto.SeguimientoDTO;
import com.ondra.users.dto.UsuarioBasicoDTO;
import com.ondra.users.exceptions.*;
import com.ondra.users.models.dao.Seguimiento;
import com.ondra.users.models.dao.Usuario;
import com.ondra.users.models.enums.TipoUsuario;
import com.ondra.users.repositories.SeguimientoRepository;
import com.ondra.users.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestionar seguimientos entre usuarios.
 *
 * <p>Implementa la lógica de negocio para seguir/dejar de seguir usuarios,
 * cumpliendo las siguientes reglas:</p>
 * <ul>
 *   <li>Solo usuarios NORMAL pueden seguir a otros</li>
 *   <li>Los ARTISTAS NO pueden seguir, solo ser seguidos</li>
 *   <li>No se puede seguir a uno mismo</li>
 *   <li>No se permiten seguimientos duplicados</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeguimientoService {

    private final SeguimientoRepository seguimientoRepository;
    private final UsuarioRepository usuarioRepository;

    /**
     * Crea un nuevo seguimiento entre dos usuarios.
     *
     * @param idSeguidor ID del usuario que sigue
     * @param idSeguido ID del usuario a seguir
     * @param authenticatedUserId ID del usuario autenticado
     * @return DTO con información del seguimiento creado
     * @throws ForbiddenAccessException Si intenta seguir desde otra cuenta
     * @throws InvalidFollowException Si intenta seguirse a sí mismo o es artista
     * @throws UsuarioNotFoundException Si alguno de los usuarios no existe
     * @throws AccountInactiveException Si el usuario seguido está inactivo
     * @throws DuplicateFollowException Si ya existe el seguimiento
     */
    @Transactional
    public SeguimientoDTO seguirUsuario(
            Long idSeguidor,
            Long idSeguido,
            Long authenticatedUserId
    ) {
        // Validar que el usuario autenticado es quien intenta seguir
        if (!idSeguidor.equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó crear seguimiento para usuario ID: {}",
                    authenticatedUserId, idSeguidor);
            throw new ForbiddenAccessException(
                    "Solo puedes seguir usuarios desde tu propia cuenta"
            );
        }

        // Validar que no intente seguirse a sí mismo
        if (idSeguidor.equals(idSeguido)) {
            log.warn("Usuario ID: {} intentó seguirse a sí mismo", idSeguidor);
            throw new InvalidFollowException("No puedes seguirte a ti mismo");
        }

        // Obtener usuarios
        Usuario seguidor = usuarioRepository.findById(idSeguidor)
                .orElseThrow(() -> new UsuarioNotFoundException(idSeguidor));

        Usuario seguido = usuarioRepository.findById(idSeguido)
                .orElseThrow(() -> new UsuarioNotFoundException(idSeguido));

        // Validar que el seguidor sea un usuario NORMAL
        if (seguidor.getTipoUsuario() != TipoUsuario.NORMAL) {
            log.warn("Artista ID: {} intentó seguir a alguien", idSeguidor);
            throw new InvalidFollowException(
                    "Los artistas no pueden seguir a otros usuarios"
            );
        }

        // Validar que el usuario seguido esté activo
        if (!seguido.isActivo()) {
            log.warn("Intento de seguir a usuario inactivo ID: {}", idSeguido);
            throw new AccountInactiveException("No puedes seguir a este usuario");
        }

        // Validar que no exista ya el seguimiento
        if (seguimientoRepository.existsBySeguidorIdUsuarioAndSeguidoIdUsuario(
                idSeguidor, idSeguido)) {
            log.warn("Usuario ID: {} ya sigue a usuario ID: {}", idSeguidor, idSeguido);
            throw new DuplicateFollowException("Ya sigues a este usuario");
        }

        // Crear seguimiento
        Seguimiento seguimiento = Seguimiento.builder()
                .seguidor(seguidor)
                .seguido(seguido)
                .build();

        seguimiento = seguimientoRepository.save(seguimiento);
        log.info("Seguimiento creado: Usuario ID: {} ahora sigue a usuario ID: {}",
                idSeguidor, idSeguido);

        return convertirASeguimientoDTO(seguimiento);
    }

    /**
     * Elimina un seguimiento existente (dejar de seguir).
     *
     * @param idSeguidor ID del usuario que sigue
     * @param idSeguido ID del usuario seguido
     * @param authenticatedUserId ID del usuario autenticado
     * @throws ForbiddenAccessException Si intenta dejar de seguir desde otra cuenta
     * @throws FollowNotFoundException Si no existe el seguimiento
     */
    @Transactional
    public void dejarDeSeguir(
            Long idSeguidor,
            Long idSeguido,
            Long authenticatedUserId
    ) {
        // Validar que el usuario autenticado es quien intenta dejar de seguir
        if (!idSeguidor.equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó eliminar seguimiento para usuario ID: {}",
                    authenticatedUserId, idSeguidor);
            throw new ForbiddenAccessException(
                    "Solo puedes dejar de seguir desde tu propia cuenta"
            );
        }

        // Verificar que existe el seguimiento
        if (!seguimientoRepository.existsBySeguidorIdUsuarioAndSeguidoIdUsuario(
                idSeguidor, idSeguido)) {
            log.warn("Usuario ID: {} intentó dejar de seguir a usuario ID: {} pero no lo seguía",
                    idSeguidor, idSeguido);
            throw new FollowNotFoundException("No sigues a este usuario");
        }

        // Eliminar seguimiento
        seguimientoRepository.deleteBySeguidorIdUsuarioAndSeguidoIdUsuario(
                idSeguidor, idSeguido
        );

        log.info("Seguimiento eliminado: Usuario ID: {} dejó de seguir a usuario ID: {}",
                idSeguidor, idSeguido);
    }

    /**
     * Obtiene la lista de usuarios que sigue un usuario específico.
     *
     * @param idUsuario ID del usuario
     * @return Lista de usuarios seguidos
     * @throws UsuarioNotFoundException Si el usuario no existe
     */
    @Transactional(readOnly = true)
    public List<UsuarioBasicoDTO> obtenerSeguidos(Long idUsuario) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNotFoundException(idUsuario));

        List<Seguimiento> seguimientos = seguimientoRepository
                .findBySeguidorIdUsuario(idUsuario);

        return seguimientos.stream()
                .map(s -> convertirAUsuarioBasicoDTO(s.getSeguido()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la lista de seguidores de un usuario específico.
     *
     * @param idUsuario ID del usuario
     * @return Lista de seguidores
     * @throws UsuarioNotFoundException Si el usuario no existe
     */
    @Transactional(readOnly = true)
    public List<UsuarioBasicoDTO> obtenerSeguidores(Long idUsuario) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNotFoundException(idUsuario));

        List<Seguimiento> seguimientos = seguimientoRepository
                .findBySeguidoIdUsuario(idUsuario);

        return seguimientos.stream()
                .map(s -> convertirAUsuarioBasicoDTO(s.getSeguidor()))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene las estadísticas de seguimientos de un usuario.
     *
     * @param idUsuario ID del usuario
     * @return DTO con contador de seguidos y seguidores
     * @throws UsuarioNotFoundException Si el usuario no existe
     */
    @Transactional(readOnly = true)
    public EstadisticasSeguimientoDTO obtenerEstadisticas(Long idUsuario) {
        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNotFoundException(idUsuario));

        long cantidadSeguidos = seguimientoRepository
                .countBySeguidorIdUsuario(idUsuario);
        long cantidadSeguidores = seguimientoRepository
                .countBySeguidoIdUsuario(idUsuario);

        return EstadisticasSeguimientoDTO.builder()
                .idUsuario(idUsuario)
                .seguidos(cantidadSeguidos)
                .seguidores(cantidadSeguidores)
                .build();
    }

    /**
     * Verifica si un usuario sigue a otro.
     *
     * @param idSeguidor ID del usuario seguidor
     * @param idSeguido ID del usuario seguido
     * @return true si el seguimiento existe, false en caso contrario
     */
    @Transactional(readOnly = true)
    public boolean verificarSeguimiento(Long idSeguidor, Long idSeguido) {
        return seguimientoRepository.existsBySeguidorIdUsuarioAndSeguidoIdUsuario(
                idSeguidor, idSeguido
        );
    }

    // ==================== MÉTODOS AUXILIARES ====================

    /**
     * Convierte una entidad Seguimiento a SeguimientoDTO.
     *
     * @param seguimiento Entidad Seguimiento
     * @return SeguimientoDTO
     */
    private SeguimientoDTO convertirASeguimientoDTO(Seguimiento seguimiento) {
        return SeguimientoDTO.builder()
                .idSeguimiento(seguimiento.getIdSeguimiento())
                .seguidor(convertirAUsuarioBasicoDTO(seguimiento.getSeguidor()))
                .seguido(convertirAUsuarioBasicoDTO(seguimiento.getSeguido()))
                .fechaSeguimiento(seguimiento.getFechaSeguimiento())
                .build();
    }

    /**
     * Convierte una entidad Usuario a UsuarioBasicoDTO.
     *
     * @param usuario Entidad Usuario
     * @return UsuarioBasicoDTO
     */
    private UsuarioBasicoDTO convertirAUsuarioBasicoDTO(Usuario usuario) {
        return UsuarioBasicoDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .tipoUsuario(usuario.getTipoUsuario())
                .build();
    }
}