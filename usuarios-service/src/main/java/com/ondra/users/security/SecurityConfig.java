package com.ondra.users.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración de seguridad de Spring Security para el microservicio de usuarios.
 *
 * <p>A diferencia de la versión anterior que dependía de un API Gateway, esta configuración
 * valida los tokens JWT directamente en el microservicio. Define qué endpoints son públicos
 * y cuáles requieren autenticación.</p>
 *
 * <p><strong>Endpoints públicos (no requieren autenticación):</strong></p>
 * <ul>
 *   <li>Registro y autenticación: /api/usuarios (POST), /api/usuarios/login (POST), etc.</li>
 *   <li>Verificación de email: /api/usuarios/verificar-email (GET)</li>
 *   <li>Recuperación de contraseña: /api/usuarios/recuperar-password (POST), etc.</li>
 *   <li>Perfiles de artistas: /api/artistas (GET), /api/artistas/{id} (GET)</li>
 *   <li>Seguimientos públicos: /api/seguimientos/{idUsuario}/seguidos (GET), etc.</li>
 * </ul>
 *
 * <p><strong>Endpoints protegidos (requieren JWT válido):</strong></p>
 * <ul>
 *   <li>Gestión de perfil de usuario: /api/usuarios/{id} (GET, PUT, DELETE)</li>
 *   <li>Cambio de contraseña: /api/usuarios/{id}/cambiar-password (PUT)</li>
 *   <li>Logout global: /api/usuarios/logout-all (POST)</li>
 *   <li>Gestión de artistas: /api/artistas/{id} (PUT, DELETE)</li>
 *   <li>Seguimientos: /api/seguimientos (POST), /api/seguimientos/{idUsuario} (DELETE)</li>
 *   <li>Imágenes: /api/imagenes/usuario (POST), /api/imagenes/artista (POST)</li>
 *   <li>Métodos de pago: /api/usuarios/{id}/pagos, /api/artistas/{id}/pagos</li>
 *   <li>Redes sociales: /api/artistas/{id}/redes (POST, PUT, DELETE)</li>
 * </ul>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    // HU01
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/usuarios/verificar-email").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios/reenviar-verificacion").permitAll();

                    // HU02
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios/login/google").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/usuarios/refresh").permitAll();

                    auth.requestMatchers(HttpMethod.GET, "/actuator/health").permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}