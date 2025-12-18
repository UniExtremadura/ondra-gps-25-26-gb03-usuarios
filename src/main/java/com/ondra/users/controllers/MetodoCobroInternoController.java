package com.ondra.users.controllers;

import com.ondra.users.dto.MetodoCobroBasicoDTO;
import com.ondra.users.services.MetodoCobroArtistaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST interno para proporcionar información básica
 * de métodos de cobro a otros microservicios.
 *
 * <p>Los endpoints están protegidos mediante service token y no
 * requieren autenticación de usuario final.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/internal/metodos-cobro")
public class MetodoCobroInternoController {

    private final MetodoCobroArtistaService metodoCobroService;

    /**
     * Obtiene el primer método de cobro registrado de un artista.
     *
     * <p>Devuelve únicamente información básica: ID de método y tipo.
     * Si el artista no tiene métodos registrados, se responde con 404.</p>
     *
     * @param idArtista ID del artista
     * @return Método de cobro básico o 404 si no existe ninguno
     */
    @GetMapping(value = "/artistas/{idArtista}/primer", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MetodoCobroBasicoDTO> obtenerPrimerMetodoCobro(
            @PathVariable Long idArtista
    ) {
        log.info("🔍 GET /internal/metodos-cobro/artistas/{}/primer", idArtista);

        MetodoCobroBasicoDTO metodo = metodoCobroService.obtenerPrimerMetodoCobroBasico(idArtista);

        if (metodo == null) {
            log.warn("⚠️ No se encontró método de cobro para artista {}", idArtista);
            return ResponseEntity.notFound().build();
        }

        log.info("✅ Método de cobro encontrado: {} - Tipo: {}", metodo.getIdMetodoCobro(), metodo.getTipo());
        return ResponseEntity.ok(metodo);
    }

    /**
     * Obtiene un método de cobro básico por su ID.
     *
     * <p>Devuelve solo información mínima destinada al intercambio
     * entre microservicios. Si el método no existe, se responde con 404.</p>
     *
     * @param idMetodoCobro ID del método de cobro
     * @return Método de cobro básico o 404 si no existe
     */
    @GetMapping(value = "/{idMetodoCobro}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MetodoCobroBasicoDTO> obtenerMetodoCobroPorId(
            @PathVariable Long idMetodoCobro
    ) {
        log.info("🔍 GET /internal/metodos-cobro/{}", idMetodoCobro);

        MetodoCobroBasicoDTO metodo = metodoCobroService.obtenerMetodoCobroBasicoPorId(idMetodoCobro);

        if (metodo == null) {
            log.warn("⚠️ No se encontró método de cobro {}", idMetodoCobro);
            return ResponseEntity.notFound().build();
        }

        log.info("✅ Método de cobro encontrado: Tipo {}", metodo.getTipo());
        return ResponseEntity.ok(metodo);
    }
}