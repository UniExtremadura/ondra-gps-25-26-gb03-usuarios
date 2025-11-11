package com.ondra.users.controllers;

import com.ondra.users.dto.*;
import com.ondra.users.services.SeguimientoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador para gestionar seguimientos entre usuarios.
 *
 * <p><strong>REGLAS DE NEGOCIO:</strong></p>
 * <ul>
 *   <li>Solo usuarios NORMAL pueden seguir a otros</li>
 *   <li>Los ARTISTAS NO pueden seguir, solo ser seguidos</li>
 *   <li>No se puede seguir a uno mismo</li>
 *   <li>No se permiten seguimientos duplicados</li>
 * </ul>
 *
 * <p><strong>Endpoints públicos (no requieren autenticación):</strong></p>
 * <ul>
 *   <li>GET /api/seguimientos/{idUsuario}/seguidos</li>
 *   <li>GET /api/seguimientos/{idUsuario}/seguidores</li>
 *   <li>GET /api/seguimientos/{idUsuario}/estadisticas</li>
 * </ul>
 *
 * <p><strong>Endpoints protegidos (requieren JWT):</strong></p>
 * <ul>
 *   <li>POST /api/seguimientos</li>
 *   <li>DELETE /api/seguimientos/{idUsuario}</li>
 *   <li>GET /api/seguimientos/{idUsuario}/verificar</li>
 * </ul>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/seguimientos")
public class SeguimientoController {

    private final SeguimientoService seguimientoService;

    /**
     * Crea un nuevo seguimiento (seguir a un usuario).
     * El usuario seguidor se obtiene del token JWT.
     *
     * @param dto Contiene el ID del usuario a seguir
     * @param authentication Objeto de autenticación de Spring Security
     * @return DTO con información del seguimiento creado
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SeguimientoDTO> seguirUsuario(
            @Valid @RequestBody SeguirUsuarioDTO dto,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        SeguimientoDTO seguimiento = seguimientoService.seguirUsuario(
                authenticatedUserId,
                dto.getIdUsuarioASeguir(),
                authenticatedUserId
        );

        return new ResponseEntity<>(seguimiento, HttpStatus.CREATED);
    }

    /**
     * Elimina un seguimiento (dejar de seguir a un usuario).
     *
     * @param idUsuario ID del usuario al que se dejará de seguir
     * @param authentication Objeto de autenticación de Spring Security
     * @return Respuesta vacía con código 200
     */
    @DeleteMapping("/{idUsuario}")
    public ResponseEntity<Void> dejarDeSeguir(
            @PathVariable Long idUsuario,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        seguimientoService.dejarDeSeguir(
                authenticatedUserId,
                idUsuario,
                authenticatedUserId
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Obtiene la lista de usuarios que sigue un usuario específico.
     * Endpoint público - no requiere autenticación.
     *
     * @param idUsuario ID del usuario
     * @return Lista de usuarios seguidos
     */
    @GetMapping("/{idUsuario}/seguidos")
    public ResponseEntity<List<UsuarioBasicoDTO>> obtenerSeguidos(
            @PathVariable Long idUsuario) {

        List<UsuarioBasicoDTO> seguidos = seguimientoService.obtenerSeguidos(idUsuario);
        return ResponseEntity.ok(seguidos);
    }

    /**
     * Obtiene la lista de seguidores de un usuario específico.
     * Endpoint público - no requiere autenticación.
     *
     * @param idUsuario ID del usuario
     * @return Lista de seguidores
     */
    @GetMapping("/{idUsuario}/seguidores")
    public ResponseEntity<List<UsuarioBasicoDTO>> obtenerSeguidores(
            @PathVariable Long idUsuario) {

        List<UsuarioBasicoDTO> seguidores = seguimientoService.obtenerSeguidores(idUsuario);
        return ResponseEntity.ok(seguidores);
    }

    /**
     * Obtiene las estadísticas de seguimientos de un usuario.
     * Incluye cantidad de seguidos y seguidores.
     * Endpoint público - no requiere autenticación.
     *
     * @param idUsuario ID del usuario
     * @return DTO con estadísticas
     */
    @GetMapping("/{idUsuario}/estadisticas")
    public ResponseEntity<EstadisticasSeguimientoDTO> obtenerEstadisticas(
            @PathVariable Long idUsuario) {

        EstadisticasSeguimientoDTO estadisticas =
                seguimientoService.obtenerEstadisticas(idUsuario);
        return ResponseEntity.ok(estadisticas);
    }

    /**
     * Verifica si el usuario autenticado sigue a un usuario específico.
     * Requiere autenticación JWT.
     *
     * @param idUsuario ID del usuario a verificar
     * @param authentication Objeto de autenticación de Spring Security
     * @return true si lo sigue, false en caso contrario
     */
    @GetMapping("/{idUsuario}/verificar")
    public ResponseEntity<Boolean> verificarSeguimiento(
            @PathVariable Long idUsuario,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        boolean sigue = seguimientoService.verificarSeguimiento(
                authenticatedUserId,
                idUsuario
        );

        return ResponseEntity.ok(sigue);
    }
}