package com.ondra.users.services;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ondra.users.clients.ContenidosClient;
import com.ondra.users.clients.RecomendacionesClient;
import com.ondra.users.dto.*;
import com.ondra.users.exceptions.*;
import com.ondra.users.models.dao.*;
import com.ondra.users.models.enums.TipoUsuario;
import com.ondra.users.repositories.*;
import com.ondra.users.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Servicio principal para la gestión de usuarios.
 * Maneja autenticación, registro, recuperación de contraseñas y operaciones de perfil.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final ArtistaRepository artistaRepository;
    private final RedSocialRepository redSocialRepository;
    private final MetodoPagoUsuarioRepository metodoPagoUsuarioRepository;
    private final MetodoCobroArtistaRepository metodoCobroArtistaRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CloudinaryService cloudinaryService;
    private final EmailService emailService;
    private final SlugGeneratorService slugGeneratorService;
    private final ContenidosClient contenidosClient;
    private final RecomendacionesClient recomendacionesClient;

    @Value("${google.oauth.client-id}")
    private String googleClientId;

    /**
     * Registra un nuevo usuario en el sistema.
     * Genera token de verificación y envía email de confirmación.
     * Crea automáticamente perfil de artista si el tipo es ARTISTA.
     *
     * @param registroDTO Datos de registro del usuario
     * @return DTO del usuario registrado
     * @throws EmailAlreadyExistsException Si el email ya está registrado
     */
    @Transactional
    public UsuarioDTO registrarUsuario(RegistroUsuarioDTO registroDTO) {
        if (usuarioRepository.findByEmailUsuario(registroDTO.getEmailUsuario()).isPresent()) {
            log.warn("Intento de registro con email duplicado: {}", registroDTO.getEmailUsuario());
            throw new EmailAlreadyExistsException("El email " + registroDTO.getEmailUsuario() + " ya está registrado");
        }

        String tokenVerificacion = UUID.randomUUID().toString();
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusHours(24);

        TipoUsuario tipo = registroDTO.getTipoUsuario();
        boolean esArtista = (tipo == TipoUsuario.ARTISTA);

        Usuario usuario = Usuario.builder()
                .emailUsuario(registroDTO.getEmailUsuario())
                .passwordUsuario(passwordEncoder.encode(registroDTO.getPasswordUsuario()))
                .nombreUsuario(registroDTO.getNombreUsuario())
                .apellidosUsuario(registroDTO.getApellidosUsuario())
                .tipoUsuario(TipoUsuario.valueOf(registroDTO.getTipoUsuario().toString().toUpperCase()))
                .fechaRegistro(LocalDateTime.now())
                .onboardingCompletado(false)
                .activo(true)
                .emailVerificado(false)
                .tokenVerificacion(tokenVerificacion)
                .slug(esArtista ? null : slugGeneratorService.generarSlugUsuario(
                        registroDTO.getNombreUsuario(),
                        registroDTO.getApellidosUsuario()
                ))
                .fechaExpiracionToken(fechaExpiracion)
                .permiteGoogle(false)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente con ID: {} y email: {}", usuario.getIdUsuario(), usuario.getEmailUsuario());

        if (esArtista) {
            String nombreArtistico = registroDTO.getNombreUsuario() + " " + registroDTO.getApellidosUsuario();
            String slugArtistico = slugGeneratorService.generarSlugArtista(nombreArtistico);

            Artista artista = Artista.builder()
                    .usuario(usuario)
                    .nombreArtistico(nombreArtistico.trim())
                    .biografiaArtistico("")
                    .slugArtistico(slugArtistico)
                    .fotoPerfilArtistico(null)
                    .fechaInicioArtistico(LocalDateTime.now())
                    .esTendencia(false)
                    .build();

            artista = artistaRepository.save(artista);
            usuario.setArtista(artista);

            log.info("✅ Perfil de artista creado con ID: {} para usuario ID: {}",
                    artista.getIdArtista(), usuario.getIdUsuario());
        }

        try {
            emailService.enviarEmailVerificacion(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario(),
                    tokenVerificacion
            );
            log.info("Correo de verificación enviado a: {}", usuario.getEmailUsuario());
        } catch (Exception e) {
            log.error("Error al enviar correo de verificación: {}", e.getMessage());
        }

        return convertirAUsuarioDTO(usuario);
    }

    /**
     * Obtiene el perfil público de un usuario por su slug.
     * Solo devuelve información no sensible.
     *
     * @param slug Slug único del usuario
     * @return DTO con información pública del perfil
     * @throws UsuarioNotFoundException Si no existe el usuario
     * @throws AccountInactiveException Si la cuenta está desactivada
     */
    @Transactional(readOnly = true)
    public UsuarioPublicoDTO obtenerPerfilPublicoBySlug(String slug) {
        Usuario usuario = usuarioRepository.findBySlug(slug)
                .orElseThrow(() -> new UsuarioNotFoundException("No se encontró el perfil: " + slug));

        if (!usuario.isActivo()) {
            throw new AccountInactiveException("Este perfil no está disponible");
        }

        UsuarioPublicoDTO.UsuarioPublicoDTOBuilder builder = UsuarioPublicoDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .slug(usuario.getSlug())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .tipoUsuario(usuario.getTipoUsuario())
                .fechaRegistro(usuario.getFechaRegistro());

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA && usuario.getArtista() != null) {
            builder.nombreArtistico(usuario.getArtista().getNombreArtistico())
                    .slugArtistico(usuario.getArtista().getSlugArtistico())
                    .biografiaArtistico(usuario.getArtista().getBiografiaArtistico())
                    .fotoPerfilArtistico(usuario.getArtista().getFotoPerfilArtistico());
        }

        return builder.build();
    }

    /**
     * Verifica el email de un usuario mediante token.
     * Valida la expiración del token antes de confirmar.
     *
     * @param token Token de verificación enviado por email
     * @throws InvalidVerificationTokenException Si el token es inválido o expiró
     */
    @Transactional
    public void verificarEmail(String token) {
        Usuario usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(() -> new InvalidVerificationTokenException("Token de verificación inválido"));

        if (usuario.getFechaExpiracionToken().isBefore(LocalDateTime.now())) {
            log.warn("Token de verificación expirado para usuario ID: {}", usuario.getIdUsuario());
            throw new InvalidVerificationTokenException("El token de verificación ha expirado");
        }

        if (usuario.isEmailVerificado()) {
            log.info("Intento de verificar email ya verificado para usuario ID: {}", usuario.getIdUsuario());
            return;
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null);
        usuario.setFechaExpiracionToken(null);
        usuarioRepository.save(usuario);

        log.info("Email verificado exitosamente para usuario ID: {}", usuario.getIdUsuario());
    }

    /**
     * Reenvía el correo de verificación a un usuario.
     * Genera un nuevo token con validez de 24 horas.
     *
     * @param reenviarDTO Contiene el email del usuario
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws EmailNotVerifiedException Si el email ya está verificado
     */
    @Transactional
    public void reenviarEmailVerificacion(ReenviarVerificacionDTO reenviarDTO) {
        Usuario usuario = usuarioRepository.findByEmailUsuario(reenviarDTO.getEmailUsuario())
                .orElseThrow(() -> new UsuarioNotFoundException(
                        "No se encontró un usuario con el email " + reenviarDTO.getEmailUsuario()
                ));

        if (usuario.isEmailVerificado()) {
            throw new EmailNotVerifiedException("Este email ya está verificado");
        }

        String nuevoToken = UUID.randomUUID().toString();
        LocalDateTime nuevaFechaExpiracion = LocalDateTime.now().plusHours(24);

        usuario.setTokenVerificacion(nuevoToken);
        usuario.setFechaExpiracionToken(nuevaFechaExpiracion);
        usuarioRepository.save(usuario);

        emailService.enviarEmailVerificacion(
                usuario.getEmailUsuario(),
                usuario.getNombreUsuario(),
                nuevoToken
        );

        log.info("Correo de verificación reenviado a: {}", usuario.getEmailUsuario());
    }

    /**
     * Autentica un usuario con email y contraseña.
     * Requiere que el email esté verificado para permitir el acceso.
     *
     * @param loginDTO Credenciales del usuario
     * @return DTO con tokens JWT y datos del usuario
     * @throws InvalidCredentialsException Si las credenciales son incorrectas
     * @throws AccountInactiveException Si la cuenta está desactivada
     * @throws EmailNotVerifiedException Si el email no está verificado
     */
    @Transactional
    public AuthResponseDTO loginUsuario(LoginUsuarioDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByEmailUsuario(loginDTO.getEmailUsuario())
                .orElseThrow(() -> {
                    log.warn("Intento de login con email no registrado: {}", loginDTO.getEmailUsuario());
                    return new InvalidCredentialsException("Email o contraseña incorrectos");
                });

        if (!usuario.isActivo()) {
            log.warn("Intento de login en cuenta inactiva. Usuario ID: {}", usuario.getIdUsuario());
            throw new AccountInactiveException("La cuenta está inactiva. Contacta con soporte");
        }

        if (!usuario.isEmailVerificado()) {
            log.warn("Intento de login sin verificar email. Usuario ID: {}", usuario.getIdUsuario());
            throw new EmailNotVerifiedException("Debes verificar tu email antes de iniciar sesión. Revisa tu bandeja de entrada");
        }

        if (usuario.getPasswordUsuario() == null || usuario.getPasswordUsuario().isEmpty()) {
            log.warn("Intento de login con contraseña en cuenta de Google. Usuario ID: {}", usuario.getIdUsuario());
            throw new InvalidCredentialsException("Esta cuenta solo permite login con Google");
        }

        if (!passwordEncoder.matches(loginDTO.getPasswordUsuario(), usuario.getPasswordUsuario())) {
            log.warn("Contraseña incorrecta para usuario: {}", loginDTO.getEmailUsuario());
            throw new InvalidCredentialsException("Email o contraseña incorrectos");
        }

        String token = jwtService.generarToken(
                usuario.getEmailUsuario(),
                usuario.getIdUsuario(),
                usuario.getTipoUsuario().name(),
                usuario.getArtista() != null ? usuario.getArtista().getIdArtista() : null
        );

        RefreshToken refreshToken = jwtService.generarRefreshToken(usuario);

        log.info("Login exitoso para usuario ID: {}", usuario.getIdUsuario());

        return AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .usuario(convertirAUsuarioDTO(usuario))
                .build();
    }

    /**
     * Autentica o registra un usuario mediante Google OAuth.
     * Valida el token de Google y crea automáticamente usuarios nuevos.
     * No requiere verificación de email adicional.
     *
     * @param loginGoogleDTO Token de autenticación de Google
     * @return DTO con tokens JWT y datos del usuario
     * @throws InvalidGoogleTokenException Si el token es inválido
     * @throws GoogleLoginDisabledException Si la cuenta no permite login con Google
     * @throws AccountInactiveException Si la cuenta está desactivada
     */
    @Transactional
    public AuthResponseDTO loginGoogle(LoginGoogleDTO loginGoogleDTO) {
        String email;
        String nombre;
        String googleUid;
        String fotoPerfil = null;

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(loginGoogleDTO.getIdToken());

            if (idToken == null) {
                log.error("Token de Google inválido o expirado");
                throw new InvalidGoogleTokenException("El token de Google es inválido o ha expirado");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            googleUid = payload.getSubject();
            email = payload.getEmail();
            nombre = (String) payload.get("name");
            fotoPerfil = (String) payload.get("picture");

            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.warn("Intento de login con email no verificado por Google: {}", email);
                throw new InvalidGoogleTokenException("El email de Google no está verificado");
            }

            log.info("✅ Token de Google validado exitosamente para: {}", email);

        } catch (InvalidGoogleTokenException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error al validar token de Google: {}", e.getMessage(), e);
            throw new InvalidGoogleTokenException("El token de Google es inválido o ha expirado");
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByGoogleUid(googleUid);
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = usuarioRepository.findByEmailUsuario(email);
        }

        Usuario usuario;

        if (usuarioOpt.isPresent()) {
            usuario = usuarioOpt.get();

            if (!usuario.isPermiteGoogle()) {
                log.warn("Intento de login con Google en cuenta sin permiso. Usuario ID: {}", usuario.getIdUsuario());
                throw new GoogleLoginDisabledException("Esta cuenta no tiene habilitado el login con Google");
            }

            if (!usuario.isActivo()) {
                log.warn("Intento de login con Google en cuenta inactiva. Usuario ID: {}", usuario.getIdUsuario());
                throw new AccountInactiveException("La cuenta está inactiva. Contacta con soporte");
            }

            if (usuario.getGoogleUid() == null || usuario.getGoogleUid().isEmpty()) {
                usuario.setGoogleUid(googleUid);
                usuario = usuarioRepository.save(usuario);
                log.info("🔗 Google UID vinculado al usuario ID: {}", usuario.getIdUsuario());
            }

            if (fotoPerfil != null && !fotoPerfil.equals(usuario.getFotoPerfil())) {
                usuario.setFotoPerfil(fotoPerfil);
                usuario = usuarioRepository.save(usuario);
                log.info("📸 Foto de perfil actualizada desde Google para usuario ID: {}", usuario.getIdUsuario());
            }

            log.info("✅ Login con Google exitoso para usuario ID: {}", usuario.getIdUsuario());

        } else {
            String[] nombreCompleto = separarNombreCompleto(nombre);

            usuario = Usuario.builder()
                    .emailUsuario(email)
                    .googleUid(googleUid)
                    .nombreUsuario(nombreCompleto[0])
                    .apellidosUsuario(nombreCompleto[1])
                    .tipoUsuario(TipoUsuario.NORMAL)
                    .fechaRegistro(LocalDateTime.now())
                    .onboardingCompletado(false)
                    .activo(true)
                    .emailVerificado(true)
                    .permiteGoogle(true)
                    .slug(slugGeneratorService.generarSlugUsuario(
                            nombreCompleto[0],
                            nombreCompleto[1]
                    ))
                    .fotoPerfil(fotoPerfil)
                    .build();

            usuario = usuarioRepository.save(usuario);
            log.info("🆕 Nuevo usuario registrado vía Google con ID: {} y email: {}",
                    usuario.getIdUsuario(), usuario.getEmailUsuario());
        }

        String token = jwtService.generarToken(
                usuario.getEmailUsuario(),
                usuario.getIdUsuario(),
                usuario.getTipoUsuario().name(),
                usuario.getArtista() != null ? usuario.getArtista().getIdArtista() : null
        );

        RefreshToken refreshToken = jwtService.generarRefreshToken(usuario);

        return AuthResponseDTO.builder()
                .token(token)
                .refreshToken(refreshToken.getToken())
                .usuario(convertirAUsuarioDTO(usuario))
                .build();
    }

    /**
     * Renueva un access token usando un refresh token válido.
     * Revoca el refresh token anterior y genera uno nuevo.
     *
     * @param refreshTokenString Token de actualización
     * @return DTO con nuevos tokens
     * @throws InvalidRefreshTokenException Si el refresh token es inválido
     */
    @Transactional
    public RefreshTokenResponseDTO renovarAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = jwtService.validarRefreshToken(refreshTokenString);
        Usuario usuario = refreshToken.getUsuario();

        Long idArtista = null;
        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA && usuario.getArtista() != null) {
            idArtista = usuario.getArtista().getIdArtista();
        }

        String nuevoAccessToken = jwtService.generarToken(
                usuario.getEmailUsuario(),
                usuario.getIdUsuario(),
                usuario.getTipoUsuario().name(),
                idArtista
        );

        jwtService.revocarRefreshToken(refreshTokenString);
        RefreshToken nuevoRefreshToken = jwtService.generarRefreshToken(usuario);

        log.info("Access token renovado para usuario ID: {}", usuario.getIdUsuario());

        return RefreshTokenResponseDTO.builder()
                .accessToken(nuevoAccessToken)
                .refreshToken(nuevoRefreshToken.getToken())
                .build();
    }

    /**
     * Obtiene estadísticas básicas de usuarios activos.
     *
     * @return DTO con contadores de usuarios normales y artistas
     */
    @Transactional(readOnly = true)
    public EstadisticasGlobalesDTO obtenerStats() {
        log.debug("Obteniendo estadísticas de usuarios activos");

        long totalUsuarios = usuarioRepository.countByTipoUsuarioAndActivoTrue(TipoUsuario.NORMAL);
        long totalArtistas = usuarioRepository.countByTipoUsuarioAndActivoTrue(TipoUsuario.ARTISTA);

        return EstadisticasGlobalesDTO.builder()
                .totalUsuarios(totalUsuarios)
                .totalArtistas(totalArtistas)
                .build();
    }

    /**
     * Obtiene el perfil completo de un usuario.
     * Solo el propietario puede acceder a su perfil completo.
     *
     * @param id ID del usuario
     * @param authenticatedUserId ID del usuario autenticado
     * @return DTO con datos completos del perfil
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws ForbiddenAccessException Si intenta acceder a otro perfil
     */
    @Transactional(readOnly = true)
    public UsuarioDTO obtenerUsuario(Long id, Long authenticatedUserId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó acceder al perfil del usuario ID: {}", authenticatedUserId, id);
            throw new ForbiddenAccessException("No tienes permiso para modificar este perfil");
        }

        return convertirAUsuarioDTO(usuario);
    }

    /**
     * Cierra la sesión de un usuario revocando su refresh token.
     *
     * @param refreshToken Token a revocar
     */
    @Transactional
    public void logout(String refreshToken) {
        jwtService.revocarRefreshToken(refreshToken);
        log.info("Sesión cerrada (refresh token revocado)");
    }

    /**
     * Cierra todas las sesiones activas de un usuario.
     * Revoca todos los refresh tokens asociados.
     *
     * @param idUsuario ID del usuario
     */
    @Transactional
    public void logoutGlobal(Long idUsuario) {
        jwtService.revocarTodosLosTokensDelUsuario(idUsuario);
        log.info("Todas las sesiones cerradas para usuario ID: {}", idUsuario);
    }

    /**
     * Edita los datos básicos de un usuario.
     * Regenera el slug si cambia el nombre o apellidos.
     *
     * @param id ID del usuario
     * @param editarDTO Datos a actualizar
     * @param authenticatedUserId ID del usuario autenticado
     * @return DTO del usuario actualizado
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws ForbiddenAccessException Si intenta editar otro perfil
     */
    @Transactional
    public UsuarioDTO editarUsuario(Long id, EditarUsuarioDTO editarDTO, Long authenticatedUserId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.getIdUsuario().equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permiso para modificar este perfil");
        }

        boolean cambioNombre = false;

        if (editarDTO.getNombreUsuario() != null && !editarDTO.getNombreUsuario().isEmpty()) {
            usuario.setNombreUsuario(editarDTO.getNombreUsuario());
            cambioNombre = true;
        }

        if (editarDTO.getApellidosUsuario() != null && !editarDTO.getApellidosUsuario().isEmpty()) {
            usuario.setApellidosUsuario(editarDTO.getApellidosUsuario());
            cambioNombre = true;
        }

        if (cambioNombre) {
            usuario.setSlug(slugGeneratorService.generarSlugUsuario(
                    usuario.getNombreUsuario(),
                    usuario.getApellidosUsuario()
            ));
        }

        if (editarDTO.getFotoPerfil() != null && !editarDTO.getFotoPerfil().isEmpty()) {
            String fotoAntigua = usuario.getFotoPerfil();
            if (fotoAntigua != null && !fotoAntigua.isEmpty() &&
                    !fotoAntigua.contains("googleusercontent.com")) {
                cloudinaryService.eliminarImagen(fotoAntigua);
            }
            usuario.setFotoPerfil(editarDTO.getFotoPerfil());
        }

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario ID: {} actualizado exitosamente", id);

        return convertirAUsuarioDTO(usuario);
    }

    /**
     * Elimina un usuario del sistema (soft delete).
     * Elimina datos asociados en microservicios relacionados.
     * Solo el propietario puede eliminar su cuenta.
     *
     * @param id ID del usuario
     * @param authenticatedUserId ID del usuario autenticado
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws ForbiddenAccessException Si intenta eliminar otra cuenta
     */
    @Transactional
    public void eliminarUsuario(Long id, Long authenticatedUserId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó eliminar la cuenta del usuario ID: {}", authenticatedUserId, id);
            throw new ForbiddenAccessException("No tienes permiso para modificar este perfil");
        }

        // Eliminar foto de perfil del usuario
        if (usuario.getFotoPerfil() != null && !usuario.getFotoPerfil().isEmpty() &&
                !usuario.getFotoPerfil().contains("googleusercontent.com")) {
            cloudinaryService.eliminarImagen(usuario.getFotoPerfil());
            log.info("Foto de perfil eliminada de Cloudinary para usuario ID: {}", id);
        }

        // Eliminar métodos de pago del usuario
        List<MetodoPagoUsuario> metodosDeUsuario = metodoPagoUsuarioRepository.findByUsuario_IdUsuario(id);
        if (!metodosDeUsuario.isEmpty()) {
            metodoPagoUsuarioRepository.deleteAll(metodosDeUsuario);
            log.info("Métodos de pago eliminados para usuario ID: {}", id);
        }

        // Eliminar seguimientos (como seguidor o seguido)
        seguimientoRepository.deleteBySeguidorIdUsuarioOrSeguidoIdUsuario(id, id);
        log.info("Seguimientos eliminados para usuario ID: {}", id);

        // Si es artista, eliminar primero el contenido del artista
        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA) {
            Optional<Artista> artistaOpt = artistaRepository.findByUsuario_IdUsuario(id);
            if (artistaOpt.isPresent()) {
                Artista artista = artistaOpt.get();
                log.info("Procesando eliminación de artista ID: {} asociado al usuario ID: {}",
                        artista.getIdArtista(), id);

                // Eliminar foto de perfil artístico
                if (artista.getFotoPerfilArtistico() != null && !artista.getFotoPerfilArtistico().isEmpty()) {
                    cloudinaryService.eliminarImagen(artista.getFotoPerfilArtistico());
                    log.info("Foto de artista eliminada de Cloudinary para artista ID: {}", artista.getIdArtista());
                }

                // Eliminar redes sociales del artista
                List<RedSocial> redesSociales = redSocialRepository.findByArtista_IdArtista(artista.getIdArtista());
                if (!redesSociales.isEmpty()) {
                    redSocialRepository.deleteAll(redesSociales);
                    log.info("Redes sociales eliminadas para artista ID: {}", artista.getIdArtista());
                }

                // Eliminar métodos de cobro del artista
                List<MetodoCobroArtista> metodosCobroArtista = metodoCobroArtistaRepository
                        .findByArtista_IdArtista(artista.getIdArtista());
                if (!metodosCobroArtista.isEmpty()) {
                    metodoCobroArtistaRepository.deleteAll(metodosCobroArtista);
                    log.info("Métodos de cobro eliminados para artista ID: {}", artista.getIdArtista());
                }
                
                log.info("Eliminando contenido del artista en microservicio Contenidos...");

                try {
                    contenidosClient.eliminarComentariosUsuario(id);
                    log.info("✅ Comentarios del artista ID: {} eliminados", artista.getIdArtista());
                } catch (Exception e) {
                    log.error("⚠️ Error al eliminar comentarios del artista ID: {}", artista.getIdArtista(), e);
                }

                try {
                    contenidosClient.eliminarValoracionesUsuario(id);
                    log.info("✅ Valoraciones del artista ID: {} eliminadas", artista.getIdArtista());
                } catch (Exception e) {
                    log.error("⚠️ Error al eliminar valoraciones del artista ID: {}", artista.getIdArtista(), e);
                }

                try {
                    contenidosClient.eliminarAlbumesArtista(artista.getIdArtista());
                    log.info("✅ Álbumes del artista ID: {} eliminados", artista.getIdArtista());
                } catch (Exception e) {
                    log.error("⚠️ Error al eliminar álbumes del artista ID: {}", artista.getIdArtista(), e);
                }

                try {
                    contenidosClient.eliminarCancionesArtista(artista.getIdArtista());
                    log.info("✅ Canciones del artista ID: {} eliminadas", artista.getIdArtista());
                } catch (Exception e) {
                    log.error("⚠️ Error al eliminar canciones del artista ID: {}", artista.getIdArtista(), e);
                }

                // Eliminar el registro de artista
                artistaRepository.delete(artista);
                log.info("Registro de artista ID: {} eliminado", artista.getIdArtista());
            }
        }

        // Eliminar datos comunes del usuario en microservicio Contenidos
        log.info("Eliminando datos del usuario en microservicio Contenidos...");

        try {
            contenidosClient.eliminarComprasUsuario(id);
            log.info("✅ Compras del usuario ID: {} eliminadas", id);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar compras del usuario ID: {}", id, e);
        }

        try {
            contenidosClient.eliminarFavoritosUsuario(id);
            log.info("✅ Favoritos del usuario ID: {} eliminados", id);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar favoritos del usuario ID: {}", id, e);
        }

        try {
            contenidosClient.eliminarCarritoUsuario(id);
            log.info("✅ Carrito del usuario ID: {} eliminado", id);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar carrito del usuario ID: {}", id, e);
        }

        // Eliminar preferencias en microservicio Recomendaciones
        log.info("Eliminando preferencias en microservicio Recomendaciones...");
        try {
            recomendacionesClient.eliminarPreferenciasUsuario(id);
            log.info("✅ Preferencias del usuario ID: {} eliminadas", id);
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar preferencias del usuario ID: {}", id, e);
        }

        // Revocar todos los tokens de sesión activos
        jwtService.revocarTodosLosTokensDelUsuario(id);
        log.info("Todos los tokens de sesión revocados para usuario ID: {}", id);

        // Marcar cuenta como inactiva (soft delete)
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        log.warn("✅ Cuenta de usuario eliminada (soft delete). Usuario ID: {} eliminado por usuario ID: {}",
                id, authenticatedUserId);
    }

    /**
     * Marca el onboarding como completado para un usuario.
     *
     * @param idUsuario ID del usuario
     * @throws UsuarioNotFoundException Si el usuario no existe
     */
    @Transactional
    public void marcarOnboardingCompletado(Long idUsuario) {
        log.info("📝 Marcando onboarding completado para usuario {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new UsuarioNotFoundException(
                        "Usuario con ID " + idUsuario + " no encontrado"
                ));

        usuario.setOnboardingCompletado(true);
        usuarioRepository.save(usuario);

        log.info("✅ Onboarding marcado como completado para usuario {}", idUsuario);
    }

    /**
     * Solicita recuperación de contraseña enviando código de 6 dígitos por email.
     * No revela si el email existe para prevenir enumeración de usuarios.
     *
     * @param dto Contiene el email del usuario
     */
    @Transactional
    public void solicitarRecuperacionPassword(RecuperarPasswordDTO dto) {
        Optional<Usuario> usuarioOpt = usuarioRepository
                .findByEmailUsuario(dto.getEmailUsuario());

        if (usuarioOpt.isEmpty()) {
            log.info("Solicitud de recuperación para email no registrado: {}",
                    dto.getEmailUsuario());
            return;
        }

        Usuario usuario = usuarioOpt.get();

        if (!usuario.isActivo()) {
            log.warn("Solicitud de recuperación para cuenta inactiva. Usuario ID: {}",
                    usuario.getIdUsuario());
            return;
        }

        String codigoVerificacion = generarCodigoAleatorio();
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusHours(1);

        usuario.setCodigoRecuperacion(codigoVerificacion);
        usuario.setFechaExpiracionTokenRecuperacion(fechaExpiracion);

        usuarioRepository.save(usuario);

        try {
            emailService.enviarEmailRecuperacion(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario(),
                    codigoVerificacion
            );
            log.info("Email de recuperación enviado a: {}", usuario.getEmailUsuario());
        } catch (Exception e) {
            log.error("Error al enviar email de recuperación: {}", e.getMessage());
        }
    }

    /**
     * Obtiene el perfil público de un usuario por ID.
     * Solo devuelve información no sensible.
     *
     * @param id ID del usuario
     * @return DTO con información pública del perfil
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws AccountInactiveException Si la cuenta está desactivada
     */
    @Transactional(readOnly = true)
    public UsuarioPublicoDTO obtenerPerfilPublico(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.isActivo()) {
            throw new AccountInactiveException("Este perfil no está disponible");
        }

        UsuarioPublicoDTO.UsuarioPublicoDTOBuilder builder = UsuarioPublicoDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .tipoUsuario(usuario.getTipoUsuario())
                .fechaRegistro(usuario.getFechaRegistro());

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA && usuario.getArtista() != null) {
            builder.nombreArtistico(usuario.getArtista().getNombreArtistico())
                    .idArtista(usuario.getArtista().getIdArtista())
                    .biografiaArtistico(usuario.getArtista().getBiografiaArtistico());
        }

        return builder.build();
    }

    /**
     * Restablece la contraseña usando email y código de 6 dígitos.
     * Valida el código y actualiza la contraseña hasheada.
     * Revoca todas las sesiones activas por seguridad.
     *
     * @param dto Contiene email, código y nueva contraseña
     * @throws InvalidPasswordResetTokenException Si el código es inválido o expiró
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws AccountInactiveException Si la cuenta está desactivada
     */
    @Transactional
    public void restablecerPassword(RestablecerPasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByEmailUsuario(dto.getEmailUsuario())
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "No se encontró una solicitud de recuperación activa para este email"
                ));

        if (usuario.getCodigoRecuperacion() == null || usuario.getCodigoRecuperacion().isEmpty()) {
            log.warn("No hay código de recuperación activo para usuario ID: {}", usuario.getIdUsuario());
            throw new InvalidPasswordResetTokenException(
                    "No hay ninguna solicitud de recuperación activa. Solicita un nuevo código"
            );
        }

        if (!dto.getCodigoVerificacion().equals(usuario.getCodigoRecuperacion())) {
            log.warn("Código de verificación incorrecto para usuario ID: {}", usuario.getIdUsuario());
            throw new InvalidPasswordResetTokenException(
                    "El código de verificación es incorrecto"
            );
        }

        if (!usuario.isActivo()) {
            log.warn("Intento de restablecer contraseña en cuenta inactiva. Usuario ID: {}",
                    usuario.getIdUsuario());
            throw new AccountInactiveException("La cuenta está inactiva. Contacta con soporte");
        }

        usuario.setPasswordUsuario(passwordEncoder.encode(dto.getNuevaPassword()));

        usuario.setCodigoRecuperacion(null);
        usuario.setFechaExpiracionTokenRecuperacion(null);

        usuarioRepository.save(usuario);

        jwtService.revocarTodosLosTokensDelUsuario(usuario.getIdUsuario());

        try {
            emailService.enviarEmailConfirmacionCambioPassword(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario()
            );
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación: {}", e.getMessage());
        }

        log.info("Contraseña restablecida exitosamente para usuario ID: {} usando código de 6 dígitos",
                usuario.getIdUsuario());
    }

    /**
     * Cambia la contraseña de un usuario autenticado.
     * Requiere validación de la contraseña actual.
     * Revoca todas las sesiones activas por seguridad.
     *
     * @param id ID del usuario
     * @param dto Contiene contraseña actual y nueva
     * @param authenticatedUserId ID del usuario autenticado
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws ForbiddenAccessException Si intenta cambiar contraseña de otro usuario
     * @throws InvalidCredentialsException Si la contraseña actual es incorrecta
     * @throws AccountInactiveException Si la cuenta está desactivada
     * @throws InvalidDataException Si la nueva contraseña es igual a la actual
     */
    @Transactional
    public void cambiarPassword(Long id, CambiarPasswordDTO dto, Long authenticatedUserId) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsuarioNotFoundException(id));

        if (!usuario.getIdUsuario().equals(authenticatedUserId)) {
            log.warn("Usuario ID: {} intentó cambiar contraseña del usuario ID: {}",
                    authenticatedUserId, id);
            throw new ForbiddenAccessException(
                    "No tienes permiso para modificar este perfil"
            );
        }

        if (!usuario.isActivo()) {
            log.warn("Intento de cambiar contraseña en cuenta inactiva. Usuario ID: {}", id);
            throw new AccountInactiveException("La cuenta está inactiva. Contacta con soporte");
        }

        if (usuario.getPasswordUsuario() == null || usuario.getPasswordUsuario().isEmpty()) {
            log.warn("Intento de cambiar contraseña en cuenta de Google. Usuario ID: {}", id);
            throw new InvalidCredentialsException(
                    "Las cuentas de Google no pueden establecer contraseña. " +
                            "Usa la opción 'Olvidé mi contraseña' para crear una"
            );
        }

        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPasswordUsuario())) {
            log.warn("Contraseña actual incorrecta para usuario ID: {}", id);
            throw new InvalidCredentialsException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(dto.getNuevaPassword(), usuario.getPasswordUsuario())) {
            log.warn("Usuario ID: {} intentó usar la misma contraseña", id);
            throw new InvalidDataException(
                    "La nueva contraseña debe ser diferente a la actual"
            );
        }

        usuario.setPasswordUsuario(passwordEncoder.encode(dto.getNuevaPassword()));
        usuarioRepository.save(usuario);

        jwtService.revocarTodosLosTokensDelUsuario(id);

        try {
            emailService.enviarEmailConfirmacionCambioPassword(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario()
            );
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación: {}", e.getMessage());
        }

        log.info("Contraseña cambiada exitosamente para usuario ID: {}", id);
    }

    /**
     * Genera un código aleatorio de 6 dígitos numéricos.
     *
     * @return String con código de 6 dígitos
     */
    private String generarCodigoAleatorio() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000);
        return String.valueOf(codigo);
    }

    /**
     * Convierte una entidad Usuario a UsuarioDTO.
     * Incluye información del perfil artístico si aplica.
     *
     * @param usuario Entidad Usuario
     * @return DTO con datos del usuario
     */
    private UsuarioDTO convertirAUsuarioDTO(Usuario usuario) {
        UsuarioDTO.UsuarioDTOBuilder builder = UsuarioDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .emailUsuario(usuario.getEmailUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .tipoUsuario(usuario.getTipoUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .activo(usuario.isActivo())
                .onboardingCompletado(usuario.getOnboardingCompletado())
                .permiteGoogle(usuario.isPermiteGoogle())
                .emailVerificado(usuario.isEmailVerificado())
                .fechaRegistro(usuario.getFechaRegistro())
                .slug(usuario.getSlug());

        if (usuario.getTipoUsuario() == TipoUsuario.ARTISTA && usuario.getArtista() != null) {
            Artista artista = usuario.getArtista();
            builder.idArtista(artista.getIdArtista())
                    .nombreArtistico(artista.getNombreArtistico())
                    .biografiaArtistico(artista.getBiografiaArtistico())
                    .slugArtistico(artista.getSlugArtistico())
                    .fotoPerfilArtistico(artista.getFotoPerfilArtistico());
        }

        return builder.build();
    }

    /**
     * Separa un nombre completo en nombre y apellidos.
     * Divide por el primer espacio encontrado.
     *
     * @param nombreCompleto Nombre completo a separar
     * @return Array con [nombre, apellidos]
     */
    private String[] separarNombreCompleto(String nombreCompleto) {
        if (nombreCompleto == null || nombreCompleto.isEmpty()) {
            return new String[]{"Usuario", ""};
        }

        String[] partes = nombreCompleto.trim().split("\\s+", 2);
        String nombre = partes[0];
        String apellidos = partes.length > 1 ? partes[1] : "";

        return new String[]{nombre, apellidos};
    }

    /**
     * Obtiene el nombre completo de un artista o usuario según el tipo indicado.
     *
     * @param id   ID del artista o del usuario según el tipo
     * @param tipo Tipo de usuario (ARTISTA o NORMAL)
     * @return DTO con nombre completo, slug y foto
     */
    public DatosUsuarioDTO obtenerDatosUsuario(Long id, TipoUsuario tipo) {

        String nombreCompleto;
        String slug;
        String urlFoto;

        if (tipo == TipoUsuario.ARTISTA) {
            // Buscar directamente por ID de artista
            Artista artista = artistaRepository.findById(id)
                    .orElseThrow(() -> new UsuarioNotFoundException(
                            "Artista no encontrado con ID: " + id
                    ));

            nombreCompleto = artista.getNombreArtistico();
            slug = artista.getSlugArtistico();
            urlFoto = artista.getFotoPerfilArtistico();

        } else if (tipo == TipoUsuario.NORMAL) {
            // Buscar por ID de usuario
            Usuario usuario = usuarioRepository.findById(id)
                    .orElseThrow(() -> new UsuarioNotFoundException(
                            "Usuario no encontrado con ID: " + id
                    ));

            nombreCompleto = usuario.getNombreUsuario() + " " + usuario.getApellidosUsuario();
            slug = usuario.getSlug();
            urlFoto = usuario.getFotoPerfil();

        } else {
            throw new IllegalArgumentException("Tipo de usuario no soportado: " + tipo);
        }

        return DatosUsuarioDTO.builder()
                .nombreCompleto(nombreCompleto)
                .slug(slug)
                .urlFoto(urlFoto)
                .tipoUsuario(tipo.name())
                .build();
    }
}