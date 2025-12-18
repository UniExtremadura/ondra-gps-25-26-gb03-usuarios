package com.ondra.users.services;

import com.ondra.users.clients.ContenidosClient;
import com.ondra.users.clients.RecomendacionesClient;
import com.ondra.users.dto.*;
import com.ondra.users.exceptions.*;
import com.ondra.users.models.dao.Artista;
import com.ondra.users.models.dao.MetodoCobroArtista;
import com.ondra.users.models.dao.RedSocial;
import com.ondra.users.models.dao.Usuario;
import com.ondra.users.models.enums.TipoUsuario;
import com.ondra.users.repositories.*;
import com.ondra.users.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio para la gestión de perfiles de artistas.
 *
 * <p>
 * Proporciona operaciones para crear, editar, eliminar y consultar perfiles de
 * artistas,
 * así como la conversión de usuarios normales a artistas y viceversa.
 * </p>
 *
 * <p>
 * Las operaciones incluyen gestión de datos en múltiples microservicios y
 * sincronización con servicios externos como Cloudinary.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistaService {

    private final ArtistaRepository artistaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RedSocialRepository redSocialRepository;
    private final MetodoCobroArtistaRepository metodoCobroArtistaRepository;
    private final CloudinaryService cloudinaryService;
    private final SlugGeneratorService slugGeneratorService;
    private final MetodoPagoUsuarioRepository metodoPagoUsuarioRepository;
    private final ContenidosClient contenidosClient;
    private final RecomendacionesClient recomendacionesClient;
    private final JwtService jwtService;

    /**
     * Lista los artistas marcados como tendencia.
     *
     * @param limit número máximo de artistas a listar, debe estar entre 1 y 20
     * @return lista de artistas en tendencia con usuarios activos
     * @throws InvalidDataException si el límite está fuera del rango permitido
     */
    @Transactional(readOnly = true)
    public List<ArtistaDTO> listarArtistasTendencia(int limit) {
        if (limit <= 0 || limit > 20) {
            log.warn("Intento de listar artistas con límite inválido: {}", limit);
            throw new InvalidDataException("El límite debe estar entre 1 y 20");
        }

        log.debug("Listando hasta {} artistas en tendencia", limit);

        List<Artista> artistas = artistaRepository
                .findByEsTendenciaTrueOrderByFechaInicioArtisticoDesc()
                .stream()
                .filter(artista -> artista.getUsuario().isActivo()
                        && artista.getUsuario().getTipoUsuario() == TipoUsuario.ARTISTA)
                .limit(limit)
                .toList();

        log.info("Se encontraron {} artistas en tendencia con usuarios activos", artistas.size());

        return artistas.stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el perfil completo de un artista por su identificador.
     *
     * @param id identificador del artista
     * @return datos del artista incluyendo redes sociales
     * @throws ArtistaNotFoundException si el artista no existe
     */
    @Transactional(readOnly = true)
    public ArtistaDTO obtenerArtista(Long id) {
        log.debug("Obteniendo perfil de artista ID: {}", id);

        Artista artista = artistaRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Artista no encontrado ID: {}", id);
                    return new ArtistaNotFoundException(id);
                });

        log.info("Perfil de artista obtenido exitosamente: {} (ID: {})",
                artista.getNombreArtistico(), id);

        return convertirADTO(artista);
    }

    /**
     * Edita el perfil de un artista.
     *
     * @param idArtista           identificador del artista a editar
     * @param editarDTO           datos de actualización
     * @param authenticatedUserId identificador del usuario autenticado
     * @return datos actualizados del artista
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario del perfil
     */
    @Transactional
    public ArtistaDTO editarArtista(
            Long idArtista,
            EditarArtistaDTO editarDTO,
            Long authenticatedUserId) {
        log.debug("Editando perfil de artista ID: {} por usuario ID: {}",
                idArtista, authenticatedUserId);

        Artista artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> {
                    log.warn("Intento de editar artista no existente ID: {}", idArtista);
                    return new ArtistaNotFoundException(idArtista);
                });

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó editar perfil de artista ID: {} sin permisos",
                    authenticatedUserId, idArtista);
            throw new ForbiddenAccessException(
                    "No tienes permiso para modificar este perfil de artista");
        }

        boolean cambioNombreArtistico = false;

        if (editarDTO.getNombreArtistico() != null && !editarDTO.getNombreArtistico().isBlank()) {
            String nuevoNombre = editarDTO.getNombreArtistico().trim();
            if (!nuevoNombre.equals(artista.getNombreArtistico())) {
                artista.setNombreArtistico(nuevoNombre);
                cambioNombreArtistico = true;
                log.debug("Nombre artístico actualizado a: {}", nuevoNombre);
            }
        }

        if (cambioNombreArtistico) {
            String nuevoSlug = slugGeneratorService.generarSlugArtista(
                    artista.getNombreArtistico());
            artista.setSlugArtistico(nuevoSlug);
            log.info("Slug de artista actualizado: {}", nuevoSlug);
        }

        if (editarDTO.getBiografiaArtistico() != null) {
            artista.setBiografiaArtistico(editarDTO.getBiografiaArtistico().trim());
            log.debug("Biografía artística actualizada");
        }

        if (editarDTO.getFotoPerfilArtistico() != null &&
                !editarDTO.getFotoPerfilArtistico().isBlank()) {

            String fotoAntigua = artista.getFotoPerfilArtistico();

            if (fotoAntigua != null && !fotoAntigua.isEmpty()) {
                try {
                    cloudinaryService.eliminarImagen(fotoAntigua);
                    log.info("✅ Imagen anterior de artista eliminada de Cloudinary: {}", fotoAntigua);
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo eliminar la imagen anterior: {}", fotoAntigua);
                }
            }

            artista.setFotoPerfilArtistico(editarDTO.getFotoPerfilArtistico().trim());
            log.debug("Foto de perfil artístico actualizada");
        }

        Artista artistaActualizado = artistaRepository.save(artista);

        log.info("✅ Perfil de artista actualizado: {} (ID: {}, slug: {})",
                artistaActualizado.getNombreArtistico(), idArtista, artistaActualizado.getSlugArtistico());

        return convertirADTO(artistaActualizado);
    }

    /**
     * Elimina el perfil de artista y desactiva la cuenta de usuario.
     *
     * @param idArtista           identificador del artista a eliminar
     * @param authenticatedUserId identificador del usuario autenticado
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario del perfil
     */
    @Transactional
    public void eliminarArtista(Long idArtista, Long authenticatedUserId) {
        log.debug("Iniciando eliminación de artista ID: {} por usuario ID: {}",
                idArtista, authenticatedUserId);

        Artista artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> {
                    log.warn("Intento de eliminar artista no existente ID: {}", idArtista);
                    return new ArtistaNotFoundException(idArtista);
                });

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó eliminar perfil de artista ID: {} sin permisos",
                    authenticatedUserId, idArtista);
            throw new ForbiddenAccessException(
                    "No tienes permiso para eliminar este perfil de artista");
        }

        Usuario usuario = artista.getUsuario();

        if (artista.getFotoPerfilArtistico() != null &&
                !artista.getFotoPerfilArtistico().isEmpty()) {
            try {
                cloudinaryService.eliminarImagen(artista.getFotoPerfilArtistico());
                log.info("✅ Foto de perfil artístico eliminada de Cloudinary para artista ID: {}",
                        idArtista);
            } catch (Exception e) {
                log.warn("⚠️ No se pudo eliminar la foto de Cloudinary: {}",
                        artista.getFotoPerfilArtistico());
            }
        }

        List<RedSocial> redesSociales = redSocialRepository.findByArtista_IdArtista(idArtista);
        if (!redesSociales.isEmpty()) {
            redSocialRepository.deleteAll(redesSociales);
            redSocialRepository.flush();
            log.info("✅ Redes sociales eliminadas para artista ID: {} (total: {})",
                    idArtista, redesSociales.size());
        }

        List<MetodoCobroArtista> metodosCobro = metodoCobroArtistaRepository.findByArtista(artista);
        if (!metodosCobro.isEmpty()) {
            metodoCobroArtistaRepository.deleteAll(metodosCobro);
            metodoCobroArtistaRepository.flush();
            log.info("✅ Métodos de cobro eliminados para artista ID: {} (total: {})",
                    idArtista, metodosCobro.size());
        }

        try {
            contenidosClient.eliminarComentariosUsuario(artista.getUsuario().getIdUsuario());
            log.info("✅ Comentarios del artista ID: {} eliminados del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar comentarios del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarValoracionesUsuario(artista.getUsuario().getIdUsuario());
            log.info("✅ Valoraciones del artista ID: {} eliminadas del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar valoraciones del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarAlbumesArtista(idArtista);
            log.info("✅ Álbumes del artista ID: {} eliminados del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar álbumes del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarCancionesArtista(idArtista);
            log.info("✅ Canciones del artista ID: {} eliminadas del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar canciones del artista ID: {}", idArtista, e);
        }

        try {
            log.info("Eliminando preferencias en microservicio Recomendaciones...");
            recomendacionesClient.eliminarPreferenciasUsuario(usuario.getIdUsuario());
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar preferencias en Recomendaciones", e);
        }

        usuario.setArtista(null);
        usuarioRepository.save(usuario);
        usuarioRepository.flush();
        log.info("✅ Artista desasociado del usuario");

        usuario.setActivo(false);
        usuarioRepository.save(usuario);
        usuarioRepository.flush();
        log.info("✅ Usuario ID: {} marcado como inactivo", usuario.getIdUsuario());

        artistaRepository.delete(artista);
        artistaRepository.flush();
        log.info("✅ Perfil de artista eliminado de la base de datos: {} (ID: {})",
                artista.getNombreArtistico(), idArtista);

        log.warn("⚠️ Eliminación completa de artista ID: {} realizada por usuario ID: {}",
                idArtista, authenticatedUserId);
    }

    /**
     * Permite a un artista renunciar a su perfil y volver a ser usuario normal.
     *
     * <p>
     * Elimina el perfil de artista y todos sus datos asociados, pero mantiene
     * la cuenta de usuario activa con tipo de usuario normal.
     * </p>
     *
     * @param idArtista           identificador del artista que renuncia
     * @param authenticatedUserId identificador del usuario autenticado
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario del perfil
     */
    @Transactional
    public void renunciarPerfilArtista(Long idArtista, Long authenticatedUserId) {
        log.debug("Iniciando renuncia de perfil artístico ID: {} por usuario ID: {}",
                idArtista, authenticatedUserId);

        Artista artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> {
                    log.warn("Intento de renunciar a perfil artista no existente ID: {}", idArtista);
                    return new ArtistaNotFoundException(idArtista);
                });

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó renunciar a perfil de artista ID: {} sin permisos",
                    authenticatedUserId, idArtista);
            throw new ForbiddenAccessException(
                    "No tienes permiso para renunciar a este perfil de artista");
        }

        Usuario usuario = artista.getUsuario();

        if (artista.getFotoPerfilArtistico() != null &&
                !artista.getFotoPerfilArtistico().isEmpty()) {
            try {
                cloudinaryService.eliminarImagen(artista.getFotoPerfilArtistico());
                log.info("✅ Foto de perfil artístico eliminada de Cloudinary para artista ID: {}",
                        idArtista);
            } catch (Exception e) {
                log.warn("⚠️ No se pudo eliminar la foto de Cloudinary: {}",
                        artista.getFotoPerfilArtistico());
            }
        }

        List<RedSocial> redesSociales = redSocialRepository.findByArtista_IdArtista(idArtista);
        if (!redesSociales.isEmpty()) {
            redSocialRepository.deleteAll(redesSociales);
            redSocialRepository.flush();
            log.info("✅ Redes sociales eliminadas para artista ID: {} (total: {})",
                    idArtista, redesSociales.size());
        }

        List<MetodoCobroArtista> metodosCobro = metodoCobroArtistaRepository.findByArtista(artista);
        if (!metodosCobro.isEmpty()) {
            metodoCobroArtistaRepository.deleteAll(metodosCobro);
            metodoCobroArtistaRepository.flush();
            log.info("✅ Métodos de cobro eliminados para artista ID: {} (total: {})",
                    idArtista, metodosCobro.size());
        }

        try {
            contenidosClient.eliminarComentariosUsuario(artista.getUsuario().getIdUsuario());
            log.info("✅ Comentarios del artista ID: {} eliminados del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar comentarios del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarValoracionesUsuario(artista.getUsuario().getIdUsuario());
            log.info("✅ Valoraciones del artista ID: {} eliminadas del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar valoraciones del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarAlbumesArtista(idArtista);
            log.info("✅ Álbumes del artista ID: {} eliminados del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar álbumes del artista ID: {}", idArtista, e);
        }

        try {
            contenidosClient.eliminarCancionesArtista(idArtista);
            log.info("✅ Canciones del artista ID: {} eliminadas del microservicio Contenidos", idArtista);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar canciones del artista ID: {}", idArtista, e);
        }

        usuario.setArtista(null);
        usuarioRepository.save(usuario);
        usuarioRepository.flush();
        log.info("✅ Artista desasociado del usuario");

        usuario.setTipoUsuario(TipoUsuario.NORMAL);
        usuarioRepository.save(usuario);
        usuarioRepository.flush();
        log.info("✅ Usuario ID: {} convertido a NORMAL", usuario.getIdUsuario());

        // Revocar todos los tokens activos para forzar re-autenticación con el nuevo
        // tipo de usuario
        jwtService.revocarTodosLosTokensDelUsuario(usuario.getIdUsuario());
        log.info("✅ Tokens revocados para usuario ID: {}. Debe autenticarse nuevamente.", usuario.getIdUsuario());

        artistaRepository.delete(artista);
        artistaRepository.flush();
        log.info("✅ Perfil de artista eliminado: {} (ID: {})",
                artista.getNombreArtistico(), idArtista);

        log.info("✅ Renuncia de perfil artístico completada. Usuario ID: {} ahora es usuario normal",
                authenticatedUserId);
    }

    /**
     * Busca artistas con filtros opcionales y paginación.
     *
     * @param search      término de búsqueda para el nombre artístico
     * @param esTendencia filtrar por artistas en tendencia
     * @param orderBy     tipo de ordenamiento ('most_recent' o 'oldest')
     * @param page        número de página
     * @param limit       tamaño de página
     * @return página de artistas que cumplen los criterios
     */
    @Transactional(readOnly = true)
    public Page<ArtistaDTO> buscarArtistas(
            String search,
            Boolean esTendencia,
            String orderBy,
            int page,
            int limit) {

        log.debug("Buscando artistas - search: {}, esTendencia: {}, orderBy: {}, page: {}, limit: {}",
                search, esTendencia, orderBy, page, limit);

        // Determinar dirección de ordenamiento
        Sort.Direction direction = "oldest".equals(orderBy)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Sort sort = Sort.by(direction, "fechaInicioArtistico");
        Pageable pageable = PageRequest.of(page, limit, sort);

        // Buscar artistas
        Page<Artista> artistasPage = artistaRepository.buscarArtistas(
                search,
                esTendencia,
                pageable);

        log.info("Encontrados {} artistas (página {} de {})",
                artistasPage.getNumberOfElements(),
                artistasPage.getNumber() + 1,
                artistasPage.getTotalPages());

        // Convertir a DTO
        return artistasPage.map(this::convertirADTO);
    }

    /**
     * Convierte un usuario normal en artista.
     *
     * <p>
     * Crea un perfil de artista, sube la foto de perfil a Cloudinary y
     * actualiza el tipo de usuario.
     * </p>
     *
     * @param crearArtistaDTO     datos del perfil artístico
     * @param foto                archivo de imagen para el perfil, puede ser null
     * @param authenticatedUserId identificador del usuario autenticado
     * @return datos del artista creado
     * @throws UsuarioNotFoundException   si el usuario no existe
     * @throws InvalidDataException       si el usuario ya es artista o el nombre
     *                                    artístico existe
     * @throws ImageUploadFailedException si ocurre un error al subir la foto
     */
    @Transactional
    public ArtistaDTO convertirseEnArtista(
            CrearArtistaDTO crearArtistaDTO,
            MultipartFile foto,
            Long authenticatedUserId) {

        log.debug("Usuario ID: {} intentando convertirse en artista", authenticatedUserId);

        Usuario usuario = usuarioRepository.findById(authenticatedUserId)
                .orElseThrow(() -> {
                    log.warn("Usuario no encontrado ID: {}", authenticatedUserId);
                    return new UsuarioNotFoundException(authenticatedUserId);
                });

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA) {
            log.warn("Usuario ID: {} ya es artista", authenticatedUserId);
            throw new InvalidDataException("Este usuario ya es un artista");
        }

        Optional<Artista> artistaExistente = artistaRepository
                .findByNombreArtistico(crearArtistaDTO.getNombreArtistico());

        if (artistaExistente.isPresent()) {
            log.warn("Ya existe un artista con el nombre: {}", crearArtistaDTO.getNombreArtistico());
            throw new InvalidDataException("Ya existe un artista con ese nombre artístico");
        }

        String slugArtistico = slugGeneratorService.generarSlugArtista(
                crearArtistaDTO.getNombreArtistico());

        String fotoUrl = null;
        if (foto != null && !foto.isEmpty()) {
            try {
                fotoUrl = cloudinaryService.subirImagen(foto, "artistas");
                log.info("✅ Foto de perfil artístico subida a Cloudinary: {}", fotoUrl);
            } catch (Exception e) {
                log.error("❌ Error al subir foto de perfil artístico", e);
                throw new ImageUploadFailedException("Error al subir la foto de perfil artístico", e);
            }
        }

        Artista nuevoArtista = Artista.builder()
                .usuario(usuario)
                .nombreArtistico(crearArtistaDTO.getNombreArtistico())
                .biografiaArtistico(crearArtistaDTO.getBiografiaArtistico())
                .fotoPerfilArtistico(fotoUrl)
                .slugArtistico(slugArtistico)
                .esTendencia(false)
                .fechaInicioArtistico(LocalDateTime.now())
                .build();

        Artista artistaGuardado = artistaRepository.save(nuevoArtista);

        usuario.setTipoUsuario(TipoUsuario.ARTISTA);
        usuarioRepository.save(usuario);

        // Revocar todos los tokens activos para forzar re-autenticación con el nuevo
        // tipo de usuario
        jwtService.revocarTodosLosTokensDelUsuario(usuario.getIdUsuario());
        log.info("✅ Tokens revocados para usuario ID: {}. Debe autenticarse nuevamente.", usuario.getIdUsuario());

        log.info("Eliminando datos de usuario en microservicio Contenidos al convertirse en artista...");
        try {
            contenidosClient.eliminarComprasUsuario(authenticatedUserId);
            log.info("✅ Compras eliminadas para usuario convertido a artista ID: {}", authenticatedUserId);
        } catch (Exception e) {
            log.warn("⚠️ Error al eliminar compras del usuario ID: {}", authenticatedUserId, e);
        }

        try {
            contenidosClient.eliminarFavoritosUsuario(authenticatedUserId);
            log.info("✅ Favoritos eliminados para usuario convertido a artista ID: {}", authenticatedUserId);
        } catch (Exception e) {
            log.warn("⚠️ Error al eliminar favoritos del usuario ID: {}", authenticatedUserId, e);
        }

        try {
            contenidosClient.eliminarCarritoUsuario(authenticatedUserId);
            log.info("✅ Carrito eliminado para usuario convertido a artista ID: {}", authenticatedUserId);
        } catch (Exception e) {
            log.warn("⚠️ Error al eliminar carrito del usuario ID: {}", authenticatedUserId, e);
        }

        try {
            contenidosClient.eliminarComentariosUsuario(authenticatedUserId);
            log.info("✅ Comentarios eliminados para usuario convertido a artista ID: {}", authenticatedUserId);
        } catch (Exception e) {
            log.warn("⚠️ Error al eliminar comentarios del usuario ID: {}", authenticatedUserId, e);
        }

        try {
            contenidosClient.eliminarValoracionesUsuario(authenticatedUserId);
            log.info("✅ Valoraciones eliminadas para usuario convertido a artista ID: {}", authenticatedUserId);
        } catch (Exception e) {
            log.warn("⚠️ Error al eliminar valoraciones del usuario ID: {}", authenticatedUserId, e);
        }

        log.info("✅ Usuario ID: {} convertido en artista exitosamente (ID artista: {}, slug: {})",
                authenticatedUserId, artistaGuardado.getIdArtista(), slugArtistico);

        return convertirADTO(artistaGuardado);
    }

    /**
     * Obtiene el perfil público de un artista por su slug.
     *
     * @param slugArtistico slug del artista
     * @return información pública del artista
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws AccountInactiveException si el usuario está inactivo
     */
    @Transactional(readOnly = true)
    public UsuarioPublicoDTO obtenerPerfilArtistaPorSlug(String slugArtistico) {
        log.debug("Obteniendo perfil de artista por slug: {}", slugArtistico);

        Artista artista = artistaRepository.findBySlugArtistico(slugArtistico)
                .orElseThrow(() -> {
                    log.warn("Artista no encontrado con slug: {}", slugArtistico);
                    return new ArtistaNotFoundException("No se encontró el artista: " + slugArtistico);
                });

        Usuario usuario = artista.getUsuario();

        if (!usuario.isActivo()) {
            throw new AccountInactiveException("Este perfil no está disponible");
        }

        log.info("Perfil de artista obtenido exitosamente: {} (slug: {})",
                artista.getNombreArtistico(), slugArtistico);

        return UsuarioPublicoDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .slug(usuario.getSlug())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .nombreArtistico(artista.getNombreArtistico())
                .slugArtistico(artista.getSlugArtistico())
                .idArtista(artista.getIdArtista())
                .biografiaArtistico(artista.getBiografiaArtistico())
                .fotoPerfil(artista.getFotoPerfilArtistico() != null ? artista.getFotoPerfilArtistico()
                        : usuario.getFotoPerfil())
                .tipoUsuario(usuario.getTipoUsuario())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }

    /**
     * Convierte una entidad de artista a su representación DTO.
     *
     * @param artista entidad a convertir
     * @return datos del artista incluyendo redes sociales
     */
    private ArtistaDTO convertirADTO(Artista artista) {
        List<RedSocial> redesSociales = redSocialRepository
                .findByArtista_IdArtista(artista.getIdArtista());

        List<RedSocialDTO> redesSocialesDTO = redesSociales.stream()
                .map(red -> RedSocialDTO.builder()
                        .idRedSocial(red.getIdRedSocial())
                        .idArtista(artista.getIdArtista())
                        .tipoRedSocial(red.getTipoRedSocial().name().toLowerCase())
                        .urlRedSocial(red.getUrlRedSocial())
                        .build())
                .collect(Collectors.toList());

        return ArtistaDTO.builder()
                .idArtista(artista.getIdArtista())
                .idUsuario(artista.getUsuario().getIdUsuario())
                .nombreArtistico(artista.getNombreArtistico())
                .biografiaArtistico(artista.getBiografiaArtistico())
                .fotoPerfilArtistico(artista.getFotoPerfilArtistico())
                .esTendencia(artista.isEsTendencia())
                .slugArtistico(artista.getSlugArtistico())
                .redesSociales(redesSocialesDTO)
                .build();
    }
}