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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Configuración de seguridad de Spring Security.
 *
 * <p>Define la cadena de filtros de seguridad, políticas de autenticación
 * y autorización de endpoints, configuración CORS y codificación de contraseñas.</p>
 *
 * <p>Orden de filtros aplicados:</p>
 * <ol>
 *   <li>ServiceTokenFilter - Autenticación entre microservicios</li>
 *   <li>JwtAuthenticationFilter - Autenticación de usuarios</li>
 *   <li>UsernamePasswordAuthenticationFilter - Filtro estándar de Spring</li>
 * </ol>
 *
 * <p>Los endpoints públicos deben estar sincronizados con
 * {@link JwtAuthenticationFilter#shouldNotFilter(jakarta.servlet.http.HttpServletRequest)}.</p>
 */
@Slf4j
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ServiceTokenFilter serviceTokenFilter;

    /**
     * Configura el codificador de contraseñas BCrypt.
     *
     * @return codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura las políticas CORS para peticiones entre orígenes.
     *
     * @return fuente de configuración CORS
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    /**
     * Configura la cadena de filtros de seguridad y las reglas de autorización.
     *
     * <p>Reglas de acceso por recurso:</p>
     * <ul>
     *   <li>Registro y autenticación: Públicos</li>
     *   <li>Endpoints /datos-usuario y /existe: Solo microservicios (ROLE_SERVICE)</li>
     *   <li>Consultas públicas de artistas: GET públicos</li>
     *   <li>Gestión de usuarios y artistas: Requiere autenticación JWT</li>
     *   <li>Seguimientos: GET públicos, POST/DELETE autenticados</li>
     * </ul>
     *
     * @param http configurador de seguridad HTTP
     * @return cadena de filtros configurada
     * @throws Exception si ocurre un error en la configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Configuración pública
                        .requestMatchers(HttpMethod.GET, "/api/config/public").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/stats").permitAll()

                        // Registro y autenticación - Públicos
                        .requestMatchers(HttpMethod.POST, "/api/usuarios").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/login/google").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/refresh").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/logout").permitAll()

                        // Endpoints internos - Solo para microservicios
                        .requestMatchers("/api/internal/**").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*/datos-usuario").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/*/existe").hasRole("SERVICE")

                        // Recuperación de contraseña - Públicos
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/recuperar-password").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/restablecer-password").permitAll()

                        // Verificación de email - Públicos
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/verificar-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/reenviar-verificacion").permitAll()

                        // Endpoints públicos generales
                        .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                        // Artistas - Consultas públicas
                        .requestMatchers(HttpMethod.GET, "/api/artistas").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artistas/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/artistas/{id}/redes").permitAll()

                        // Artistas - Métodos de cobro (requieren autenticación)
                        .requestMatchers("/api/artistas/{id}/metodos-cobro/**").authenticated()

                        // Artistas - Redes sociales (requieren autenticación)
                        .requestMatchers(HttpMethod.POST, "/api/artistas/{id}/redes").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/artistas/{id}/redes/{id_red}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/artistas/{id}/redes/{id_red}").authenticated()

                        // Artistas - Gestión (requieren autenticación)
                        .requestMatchers(HttpMethod.POST, "/api/artistas").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/artistas/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/artistas/{id}").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/artistas/{id}/renunciar").authenticated()

                        // Usuarios - Métodos de pago (requieren autenticación)
                        .requestMatchers("/api/usuarios/{id}/metodos-pago/**").authenticated()

                        // Usuarios - Gestión de perfil (requieren autenticación)
                        .requestMatchers(HttpMethod.GET, "/api/usuarios/{id}").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/{id}").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/usuarios/{id}").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/usuarios/{id}/onboarding-completado").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/usuarios/{id}/cambiar-password").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios/logout-all").authenticated()

                        // Seguimientos - Consultas públicas
                        .requestMatchers(HttpMethod.GET, "/api/seguimientos/{idUsuario}/seguidos").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/seguimientos/{idUsuario}/seguidores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/seguimientos/{idUsuario}/estadisticas").permitAll()

                        // Seguimientos - Acciones (requieren autenticación)
                        .requestMatchers(HttpMethod.POST, "/api/seguimientos").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/seguimientos/{idUsuario}").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/seguimientos/{idUsuario}/verificar").authenticated()

                        // Actuator
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()

                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )

                // Orden de filtros: ServiceToken -> JWT -> UsernamePassword
                .addFilterBefore(serviceTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        log.info("🔒 SecurityFilterChain configurado correctamente");
        log.info("📋 Filtros registrados: ServiceTokenFilter -> JwtAuthenticationFilter");
        return http.build();
    }
}
