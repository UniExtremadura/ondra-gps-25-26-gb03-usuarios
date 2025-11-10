package com.ondra.users.security;

import com.ondra.users.models.dao.RefreshToken;
import com.ondra.users.models.dao.Usuario;
import com.ondra.users.exceptions.*;
import com.ondra.users.repositories.RefreshTokenRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio encargado de la generación y gestión de tokens JWT y refresh tokens.
 *
 * <p>Este servicio genera tokens en los endpoints de login y gestiona el ciclo
 * de vida de los refresh tokens. La validación de los access tokens se realiza
 * localmente mediante el filtro JWT.</p>
 *
 * <p><strong>Versión:</strong> Compatible con jjwt 0.12.5</p>
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration; // 15 minutos recomendado (900000 ms)

    @Value("${jwt.refresh.expiration}")
    private Long refreshExpiration; // 7 días recomendado (604800000 ms)

    /**
     * Genera un token JWT de acceso para un usuario autenticado.
     *
     * @param email  Email del usuario
     * @param userId ID del usuario
     * @return Token JWT firmado
     */
    public String generarToken(String email, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);

        return createToken(claims, email);
    }

    /**
     * Genera un token JWT de acceso incluyendo el tipo de usuario.
     *
     * @param email       Email del usuario
     * @param userId      ID del usuario
     * @param tipoUsuario Tipo de usuario (NORMAL, ARTISTA, etc.)
     * @return Token JWT firmado
     */
    public String generarToken(String email, Long userId, String tipoUsuario) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("tipoUsuario", tipoUsuario);

        return createToken(claims, email);
    }

    /**
     * Genera un refresh token único y lo almacena en la base de datos.
     *
     * @param usuario Usuario para el que se genera el token
     * @return Refresh token generado
     */
    @Transactional
    public RefreshToken generarRefreshToken(Usuario usuario) {
        // Generar token único
        String token = UUID.randomUUID().toString();

        // Calcular fecha de expiración
        LocalDateTime fechaExpiracion = LocalDateTime.now()
                .plusSeconds(refreshExpiration / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .usuario(usuario)
                .fechaExpiracion(fechaExpiracion)
                .revocado(false)
                .fechaCreacion(LocalDateTime.now())
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Valida un refresh token y verifica que no haya expirado ni sido revocado.
     *
     * @param token Token a validar
     * @return RefreshToken si es válido
     * @throws InvalidRefreshTokenException si el token no existe o no es válido
     */
    @Transactional(readOnly = true)
    public RefreshToken validarRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token inválido o expirado"));

        if (!refreshToken.esValido()) {
            throw new InvalidRefreshTokenException("Refresh token inválido o expirado");
        }

        return refreshToken;
    }

    /**
     * Revoca un refresh token específico.
     *
     * @param token Token a revocar
     */
    @Transactional
    public void revocarRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevocado(true);
            refreshTokenRepository.save(rt);
        });
    }

    /**
     * Revoca todos los refresh tokens de un usuario (útil para logout global).
     *
     * @param idUsuario ID del usuario
     */
    @Transactional
    public void revocarTodosLosTokensDelUsuario(Long idUsuario) {
        refreshTokenRepository.findByUsuario_IdUsuario(idUsuario)
                .forEach(rt -> {
                    rt.setRevocado(true);
                    refreshTokenRepository.save(rt);
                });
    }

    /**
     * Elimina tokens expirados de la base de datos (para limpieza).
     * Debería ejecutarse periódicamente mediante un scheduled task.
     */
    @Transactional
    public void limpiarTokensExpirados() {
        refreshTokenRepository.eliminarTokensExpirados(LocalDateTime.now());
    }

    /**
     * Crea el token JWT con los claims proporcionados.
     * Actualizado para jjwt 0.12.5 - usa la nueva API.
     *
     * @param claims  Claims a incluir en el token
     * @param subject Subject del token (normalmente el email del usuario)
     * @return Token JWT firmado
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Obtiene la clave de firma utilizada para generar el token JWT.
     *
     * @return {@link SecretKey} para firmar el token
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}