package com.ondra.users.services;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ondra.users.dto.*;
import com.ondra.users.security.JwtService;
import com.ondra.users.exceptions.*;
import com.ondra.users.models.dao.*;
import com.ondra.users.models.enums.TipoUsuario;
import com.ondra.users.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final FirebaseAuth firebaseAuth;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;

    /**
     * Registra un nuevo usuario en el sistema con email y contraseña.
     * Envía un correo de verificación automáticamente.
     * La cuenta estará activa pero NO podrá hacer login hasta verificar.
     *
     * @param registroDTO Datos del nuevo usuario
     * @return {@link UsuarioDTO} con los datos del usuario creado
     * @throws EmailAlreadyExistsException Si el email ya está registrado
     */
    @Transactional
    public UsuarioDTO registrarUsuario(RegistroUsuarioDTO registroDTO) {
        if (usuarioRepository.findByEmailUsuario(registroDTO.getEmailUsuario()).isPresent()) {
            log.warn("Intento de registro con email duplicado: {}", registroDTO.getEmailUsuario());
            throw new EmailAlreadyExistsException("El email " + registroDTO.getEmailUsuario() + " ya está registrado");
        }

        // Generar token de verificación único
        String tokenVerificacion = UUID.randomUUID().toString();
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusHours(24); // Token válido 24 horas

        Usuario usuario = Usuario.builder()
                .emailUsuario(registroDTO.getEmailUsuario())
                .passwordUsuario(passwordEncoder.encode(registroDTO.getPasswordUsuario()))
                .nombreUsuario(registroDTO.getNombreUsuario())
                .apellidosUsuario(registroDTO.getApellidosUsuario())
                .tipoUsuario(TipoUsuario.valueOf(registroDTO.getTipoUsuario().toString().toUpperCase()))
                .fechaRegistro(LocalDateTime.now())
                .activo(true)
                .emailVerificado(false) // Cuenta NO verificada inicialmente
                .tokenVerificacion(tokenVerificacion)
                .fechaExpiracionToken(fechaExpiracion)
                .permiteGoogle(false)
                .build();

        usuario = usuarioRepository.save(usuario);
        log.info("Usuario registrado exitosamente con ID: {} y email: {}", usuario.getIdUsuario(), usuario.getEmailUsuario());

        // Enviar correo de verificación
        try {
            emailService.enviarEmailVerificacion(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario(),
                    tokenVerificacion
            );
            log.info("Correo de verificación enviado a: {}", usuario.getEmailUsuario());
        } catch (Exception e) {
            log.error("Error al enviar correo de verificación: {}", e.getMessage());
            // No falla el registro si el email no se envía
        }

        return convertirAUsuarioDTO(usuario);
    }

    /**
     * Verifica el email de un usuario mediante el token recibido por correo.
     *
     * @param token Token de verificación
     * @throws InvalidVerificationTokenException Si el token es inválido o ha expirado
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
            return; // Ya está verificado, no hacer nada
        }

        usuario.setEmailVerificado(true);
        usuario.setTokenVerificacion(null); // Limpiar token usado
        usuario.setFechaExpiracionToken(null);
        usuarioRepository.save(usuario);

        log.info("Email verificado exitosamente para usuario ID: {}", usuario.getIdUsuario());
    }

    /**
     * Reenvía el correo de verificación a un usuario que no ha verificado su email.
     *
     * @param reenviarDTO Email del usuario
     * @throws UsuarioNotFoundException Si el usuario no existe
     * @throws EmailNotVerifiedException Si el email ya está verificado (código 403)
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

        // Generar nuevo token (el anterior podría haber expirado)
        String nuevoToken = UUID.randomUUID().toString();
        LocalDateTime nuevaFechaExpiracion = LocalDateTime.now().plusHours(24);

        usuario.setTokenVerificacion(nuevoToken);
        usuario.setFechaExpiracionToken(nuevaFechaExpiracion);
        usuarioRepository.save(usuario);

        // Enviar correo
        emailService.enviarEmailVerificacion(
                usuario.getEmailUsuario(),
                usuario.getNombreUsuario(),
                nuevoToken
        );

        log.info("Correo de verificación reenviado a: {}", usuario.getEmailUsuario());
    }

    /**
     * Convierte una entidad Usuario a UsuarioDTO.
     *
     * @param usuario Entidad Usuario
     * @return UsuarioDTO
     */
    private UsuarioDTO convertirAUsuarioDTO(Usuario usuario) {
        return UsuarioDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .emailUsuario(usuario.getEmailUsuario())
                .nombreUsuario(usuario.getNombreUsuario())
                .apellidosUsuario(usuario.getApellidosUsuario())
                .tipoUsuario(usuario.getTipoUsuario())
                .fotoPerfil(usuario.getFotoPerfil())
                .activo(usuario.isActivo())
                .permiteGoogle(usuario.isPermiteGoogle())
                .emailVerificado(usuario.isEmailVerificado())
                .build();
    }

    /**
     * Autentica un usuario con email y contraseña, retornando JWT y refresh token.
     * SOLO permite login si el email ha sido verificado.
     *
     * @param loginDTO Credenciales del usuario
     * @return {@link AuthResponseDTO} con tokens y datos del usuario
     * @throws InvalidCredentialsException Si las credenciales son incorrectas
     * @throws AccountInactiveException Si la cuenta está inactiva
     * @throws EmailNotVerifiedException Si el email no ha sido verificado
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

        // VALIDAR QUE EL EMAIL ESTÉ VERIFICADO
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
                usuario.getTipoUsuario().name()
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
     * Autentica o registra un usuario mediante token de Google/Firebase.
     * Los usuarios de Google NO necesitan verificación de email (Google ya lo verifica).
     *
     * @param loginGoogleDTO Token de autenticación de Google
     * @return {@link AuthResponseDTO} con token JWT y datos del usuario
     */
    @Transactional
    public AuthResponseDTO loginGoogle(LoginGoogleDTO loginGoogleDTO) {
        FirebaseToken decodedToken;

        try {
            decodedToken = firebaseAuth.verifyIdToken(loginGoogleDTO.getIdToken());
        } catch (FirebaseAuthException e) {
            log.error("Token de Google inválido: {}", e.getMessage());
            throw new InvalidGoogleTokenException("El token de Google es inválido o ha expirado");
        }

        String googleUid = decodedToken.getUid();
        String email = decodedToken.getEmail();
        String nombre = decodedToken.getName();

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
                log.info("Google UID vinculado al usuario ID: {}", usuario.getIdUsuario());
            }

            log.info("Login con Google exitoso para usuario ID: {}", usuario.getIdUsuario());
        } else {
            String[] nombreCompleto = separarNombreCompleto(nombre);

            usuario = Usuario.builder()
                    .emailUsuario(email)
                    .googleUid(googleUid)
                    .nombreUsuario(nombreCompleto[0])
                    .apellidosUsuario(nombreCompleto[1])
                    .tipoUsuario(TipoUsuario.NORMAL)
                    .fechaRegistro(LocalDateTime.now())
                    .activo(true)
                    .emailVerificado(true) // Google ya verifica el email
                    .permiteGoogle(true)
                    .fotoPerfil(decodedToken.getPicture())
                    .build();

            usuario = usuarioRepository.save(usuario);
            log.info("Nuevo usuario registrado vía Google con ID: {} y email: {}", usuario.getIdUsuario(), usuario.getEmailUsuario());
        }

        String token = jwtService.generarToken(
                usuario.getEmailUsuario(),
                usuario.getIdUsuario(),
                usuario.getTipoUsuario().name()
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
     *
     * @param refreshTokenString String del refresh token
     * @return Nuevos access token y refresh token
     * @throws InvalidRefreshTokenException Si el refresh token es inválido
     */
    @Transactional
    public RefreshTokenResponseDTO renovarAccessToken(String refreshTokenString) {
        RefreshToken refreshToken = jwtService.validarRefreshToken(refreshTokenString);
        Usuario usuario = refreshToken.getUsuario();

        String nuevoAccessToken = jwtService.generarToken(
                usuario.getEmailUsuario(),
                usuario.getIdUsuario(),
                usuario.getTipoUsuario().name()
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
     * Separa un nombre completo en nombre y apellidos.
     *
     * @param nombreCompleto Nombre completo
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
     * Genera un código aleatorio de 6 dígitos.
     *
     * @return String con 6 dígitos numéricos
     */
    private String generarCodigoAleatorio() {
        Random random = new Random();
        int codigo = 100000 + random.nextInt(900000); // Rango: 100000-999999
        return String.valueOf(codigo);
    }

    /**
     * Cierra la sesión de un usuario revocando su refresh token.
     *
     * @param refreshToken Refresh token a revocar
     */
    @Transactional
    public void logout(String refreshToken) {
        jwtService.revocarRefreshToken(refreshToken);
        log.info("Sesión cerrada (refresh token revocado)");
    }

    /**
     * Cierra todas las sesiones activas de un usuario.
     *
     * @param idUsuario ID del usuario
     */
    @Transactional
    public void logoutGlobal(Long idUsuario) {
        jwtService.revocarTodosLosTokensDelUsuario(idUsuario);
        log.info("Todas las sesiones cerradas para usuario ID: {}", idUsuario);
    }

    /**
     * Solicita recuperación de contraseña enviando email con código de 6 dígitos y token.
     * NO revela si el email existe para prevenir enumeración de usuarios.
     *
     * @param dto Contiene el email del usuario
     */
    @Transactional
    public void solicitarRecuperacionPassword(RecuperarPasswordDTO dto) {
        Optional<Usuario> usuarioOpt = usuarioRepository
                .findByEmailUsuario(dto.getEmailUsuario());

        // IMPORTANTE: Siempre responde igual (no revela si existe el email)
        if (usuarioOpt.isEmpty()) {
            log.info("Solicitud de recuperación para email no registrado: {}",
                    dto.getEmailUsuario());
            return; // No hacer nada, pero el controller responderá OK
        }

        Usuario usuario = usuarioOpt.get();

        // Si la cuenta está inactiva, no enviar email
        if (!usuario.isActivo()) {
            log.warn("Solicitud de recuperación para cuenta inactiva. Usuario ID: {}",
                    usuario.getIdUsuario());
            return; // No revelar que la cuenta existe pero está inactiva
        }

        // Generar código de 6 dígitos aleatorio
        String codigoVerificacion = generarCodigoAleatorio();

        // Generar token de recuperación (UUID para el enlace)
        String tokenRecuperacion = UUID.randomUUID().toString();
        LocalDateTime fechaExpiracion = LocalDateTime.now().plusHours(1); // 1 hora

        // Guardar el código Y el token en la base de datos
        usuario.setCodigoRecuperacion(codigoVerificacion);
        usuario.setTokenRecuperacion(tokenRecuperacion);
        usuario.setFechaExpiracionTokenRecuperacion(fechaExpiracion);
        usuarioRepository.save(usuario);

        // Enviar email con el código Y el token
        try {
            emailService.enviarEmailRecuperacionConCodigo(
                    usuario.getEmailUsuario(),
                    codigoVerificacion,      // Código de 6 dígitos
                    tokenRecuperacion        // Token para el enlace
            );
            log.info("Email de recuperación enviado a: {}", usuario.getEmailUsuario());
        } catch (Exception e) {
            log.error("Error al enviar email de recuperación: {}", e.getMessage());
            // No lanzar excepción para no revelar información
        }
    }

    /**
     * Restablece la contraseña usando el token y código recibidos por email.
     * Este endpoint es público (no requiere autenticación).
     *
     * @param dto Contiene el token, código de 6 dígitos y la nueva contraseña
     * @throws InvalidPasswordResetTokenException Si el token o código son inválidos o expiraron
     */
    @Transactional
    public void restablecerPassword(RestablecerPasswordDTO dto) {
        Usuario usuario = usuarioRepository.findByTokenRecuperacion(dto.getToken())
                .orElseThrow(() -> new InvalidPasswordResetTokenException(
                        "Token de recuperación inválido"
                ));

        // Validar que no haya expirado
        if (usuario.getFechaExpiracionTokenRecuperacion() == null ||
                usuario.getFechaExpiracionTokenRecuperacion().isBefore(LocalDateTime.now())) {
            log.warn("Token de recuperación expirado para usuario ID: {}",
                    usuario.getIdUsuario());
            throw new InvalidPasswordResetTokenException("El token ha expirado");
        }

        // Validar que el código de 6 dígitos coincida
        if (!dto.getCodigoVerificacion().equals(usuario.getCodigoRecuperacion())) {
            log.warn("Código de verificación incorrecto para usuario ID: {}",
                    usuario.getIdUsuario());
            throw new InvalidPasswordResetTokenException("El código de verificación es incorrecto");
        }

        // Validar que la cuenta esté activa
        if (!usuario.isActivo()) {
            log.warn("Intento de restablecer contraseña en cuenta inactiva. Usuario ID: {}",
                    usuario.getIdUsuario());
            throw new AccountInactiveException("La cuenta está inactiva. Contacta con soporte");
        }

        // Actualizar contraseña (hasheada)
        usuario.setPasswordUsuario(passwordEncoder.encode(dto.getNuevaPassword()));

        // Limpiar token Y código usados (evita reutilización)
        usuario.setTokenRecuperacion(null);
        usuario.setFechaExpiracionTokenRecuperacion(null);
        usuario.setCodigoRecuperacion(null);

        usuarioRepository.save(usuario);

        // SEGURIDAD: Revocar todos los refresh tokens activos (logout global)
        jwtService.revocarTodosLosTokensDelUsuario(usuario.getIdUsuario());

        // Enviar email de confirmación del cambio (notificación de seguridad)
        try {
            emailService.enviarEmailConfirmacionCambioPassword(
                    usuario.getEmailUsuario(),
                    usuario.getNombreUsuario()
            );
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación: {}", e.getMessage());
            // No fallar el proceso si el email no se envía
        }

        log.info("Contraseña restablecida exitosamente para usuario ID: {}",
                usuario.getIdUsuario());
    }

}