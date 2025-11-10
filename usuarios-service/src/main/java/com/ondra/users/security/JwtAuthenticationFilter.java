package com.ondra.users.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Filtro de autenticación JWT que valida tokens en cada petición.
 *
 * <p>Este filtro se ejecuta una vez por cada petición HTTP y valida el token JWT
 * presente en el header Authorization. Si el token es válido, establece la autenticación
 * en el SecurityContext de Spring Security.</p>
 *
 * <p><strong>Funcionamiento:</strong></p>
 * <ol>
 *   <li>Extrae el token del header Authorization (formato: "Bearer {token}")</li>
 *   <li>Valida el token JWT (firma, expiración, estructura)</li>
 *   <li>Extrae el userId del token y lo establece como nombre de usuario en Spring Security</li>
 *   <li>Crea un objeto de autenticación y lo registra en el SecurityContext</li>
 * </ol>
 *
 * <p><strong>IMPORTANTE:</strong> El microservicio valida el token localmente sin necesidad
 * de un API Gateway. El userId extraído del token se usa en los controllers mediante
 * {@code Authentication.getName()}.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String secretKey;

    /**
     * Método principal del filtro que se ejecuta en cada petición.
     *
     * @param request Petición HTTP
     * @param response Respuesta HTTP
     * @param filterChain Cadena de filtros
     * @throws ServletException si ocurre un error de servlet
     * @throws IOException si ocurre un error de I/O
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extraer token del header Authorization
            String token = extractTokenFromRequest(request);

            if (token != null && validateToken(token)) {
                // Extraer userId del token
                String userId = extractUserIdFromToken(token);

                // Crear objeto de autenticación
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, // El userId se establece como "name" del Authentication
                                null,
                                Collections.emptyList() // Sin roles por ahora
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Establecer autenticación en el contexto de seguridad
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Usuario ID: {} autenticado correctamente", userId);
            }
        } catch (ExpiredJwtException e) {
            log.warn("Token JWT expirado: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"TOKEN_EXPIRED\",\"message\":\"El token ha expirado\"}");
            return;
        } catch (SignatureException e) {
            log.warn("Firma JWT inválida: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Token inválido\"}");
            return;
        } catch (MalformedJwtException e) {
            log.warn("Token JWT malformado: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"MALFORMED_TOKEN\",\"message\":\"Token malformado\"}");
            return;
        } catch (UnsupportedJwtException e) {
            log.warn("Token JWT no soportado: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"UNSUPPORTED_TOKEN\",\"message\":\"Token no soportado\"}");
            return;
        } catch (IllegalArgumentException e) {
            log.warn("Claims JWT vacíos: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"INVALID_TOKEN\",\"message\":\"Token inválido\"}");
            return;
        } catch (Exception e) {
            log.error("Error inesperado al validar JWT: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Error al procesar el token\"}");
            return;
        }

        // Continuar con la cadena de filtros
        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header Authorization.
     *
     * @param request Petición HTTP
     * @return Token JWT sin el prefijo "Bearer ", o null si no existe
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Eliminar "Bearer "
        }

        return null;
    }

    /**
     * Valida un token JWT.
     *
     * @param token Token JWT a validar
     * @return true si el token es válido, false en caso contrario
     */
    private boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            // Las excepciones específicas se capturan en doFilterInternal
            throw e;
        }
    }

    /**
     * Extrae el userId del token JWT.
     *
     * @param token Token JWT
     * @return userId como String
     */
    private String extractUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // El userId se almacena como claim "userId" en el token
        Object userIdObj = claims.get("userId");

        if (userIdObj == null) {
            throw new IllegalArgumentException("El token no contiene el claim 'userId'");
        }

        // Convertir a String (puede venir como Integer o Long)
        return String.valueOf(userIdObj);
    }

    /**
     * Obtiene la clave de firma para validar el token JWT.
     *
     * @return SecretKey para validar el token
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}