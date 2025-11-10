package com.ondra.users.controllers;

import com.ondra.users.services.UsuarioService;
import com.ondra.users.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador encargado de manejar las solicitudes relacionadas con los usuarios.
 *
 * IMPORTANTE: Los endpoints con rutas específicas DEBEN ir ANTES que los que usan {id}
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Registra un nuevo usuario en el sistema mediante email y contraseña.
     * Envía automáticamente un correo de verificación.
     * La cuenta estará activa pero NO podrá hacer login hasta que se verifique el email.
     *
     * @param registroDTO datos necesarios para el registro de usuario
     * @return objeto {@link UsuarioDTO} con la información del usuario creado
     */
    @PostMapping(value = "/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioDTO> registrarUsuario(@Valid @RequestBody RegistroUsuarioDTO registroDTO) {
        UsuarioDTO usuario = usuarioService.registrarUsuario(registroDTO);
        return new ResponseEntity<>(usuario, HttpStatus.CREATED);
    }

    /**
     * Verifica el email de un usuario mediante el token recibido por correo.
     * Este endpoint es público.
     *
     * @param token Token de verificación (recibido por query parameter)
     * @return Mensaje de éxito
     */
    @GetMapping("/usuarios/verificar-email")
    public ResponseEntity<String> verificarEmail(@RequestParam String token) {
        usuarioService.verificarEmail(token);
        return ResponseEntity.ok("Email verificado correctamente. Ya puedes iniciar sesión");
    }

    /**
     * Reenvía el correo de verificación a un usuario que no ha verificado su email.
     * Este endpoint es público.
     *
     * @param reenviarDTO Email del usuario
     * @return Mensaje de éxito
     */
    @PostMapping("/usuarios/reenviar-verificacion")
    public ResponseEntity<String> reenviarVerificacion(@Valid @RequestBody ReenviarVerificacionDTO reenviarDTO) {
        usuarioService.reenviarEmailVerificacion(reenviarDTO);
        return ResponseEntity.ok("Correo de verificación reenviado. Revisa tu bandeja de entrada");
    }

    /**
     * Autentica a un usuario mediante email y contraseña, generando un JWT.
     * Solo permite login si el email ha sido verificado.
     *
     * @param loginDTO credenciales de acceso (email y contraseña)
     * @return objeto {@link AuthResponseDTO} con el token JWT y datos del usuario
     */
    @PostMapping(value = "/usuarios/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDTO> loginUsuario(@Valid @RequestBody LoginUsuarioDTO loginDTO) {
        AuthResponseDTO authResponse = usuarioService.loginUsuario(loginDTO);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Autentica o registra a un usuario mediante el token de Google/Firebase.
     * Los usuarios de Google NO requieren verificación de email.
     *
     * @param loginGoogleDTO token de autenticación proporcionado por Google
     * @return objeto {@link AuthResponseDTO} con el token JWT y datos del usuario
     */
    @PostMapping(value = "/usuarios/login/google", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthResponseDTO> loginGoogle(@Valid @RequestBody LoginGoogleDTO loginGoogleDTO) {
        AuthResponseDTO authResponse = usuarioService.loginGoogle(loginGoogleDTO);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Renueva un access token usando un refresh token válido.
     *
     * @param request objeto con el refresh token
     * @return nuevo access token y refresh token
     */
    @PostMapping(value = "/usuarios/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RefreshTokenResponseDTO> refreshToken(
            @Valid @RequestBody RefreshTokenRequestDTO request) {
        RefreshTokenResponseDTO response = usuarioService.renovarAccessToken(
                request.getRefreshToken()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Cierra la sesión de un usuario revocando su refresh token.
     *
     * @param request objeto con el refresh token a revocar
     * @return respuesta vacía con código 200
     */
    @PostMapping("/usuarios/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        usuarioService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    /**
     * Cierra todas las sesiones activas de un usuario.
     * Requiere autenticación JWT.
     *
     * @param authentication Objeto de autenticación de Spring Security
     * @return respuesta vacía con código 200
     */
    @PostMapping("/usuarios/logout-all")
    public ResponseEntity<Void> logoutAll(Authentication authentication) {
        Long authenticatedUserId = Long.parseLong(authentication.getName());
        usuarioService.logoutGlobal(authenticatedUserId);
        return ResponseEntity.ok().build();
    }

    /**
     * Solicita un enlace de recuperación de contraseña por email.
     * Este endpoint es público.
     */
    @PostMapping("/usuarios/recuperar-password")
    public ResponseEntity<String> recuperarPassword(
            @Valid @RequestBody RecuperarPasswordDTO dto) {
        usuarioService.solicitarRecuperacionPassword(dto);
        return ResponseEntity.ok(
                "Si el email existe, recibirás un enlace de recuperación"
        );
    }

    /**
     * Restablece la contraseña usando el token recibido por email.
     * Este endpoint es público.
     */
    @PostMapping("/usuarios/restablecer-password")
    public ResponseEntity<String> restablecerPassword(
            @Valid @RequestBody RestablecerPasswordDTO dto) {
        usuarioService.restablecerPassword(dto);
        return ResponseEntity.ok("Contraseña restablecida correctamente");
    }
}