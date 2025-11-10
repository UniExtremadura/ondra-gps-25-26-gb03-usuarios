package com.ondra.users.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

/**
 * Configuración de Firebase para autenticación con Google OAuth.
 *
 * <p>Se encarga de inicializar el SDK de Firebase Admin utilizando las credenciales
 * configuradas en la aplicación y expone el bean de {@link FirebaseAuth}
 * para la validación de tokens de Google.</p>
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    /**
     * Ruta al archivo de credenciales de Firebase.
     * Se obtiene de las propiedades de la aplicación.
     */
    @Value("${firebase.credentials.path:classpath:firebase-credentials.json}")
    private Resource firebaseCredentials;

    /**
     * URL de la base de datos de Firebase.
     * Se obtiene de las propiedades de la aplicación.
     */
    @Value("${firebase.database-url:https://your-project.firebaseio.com}")
    private String firebaseDatabaseUrl;

    /**
     * Inicializa Firebase Admin SDK al arrancar la aplicación.
     * Carga las credenciales de servicio y configura la URL de la base de datos.
     *
     * @throws RuntimeException si no se pueden cargar las credenciales o inicializar Firebase
     */
    @PostConstruct
    public void initializeFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                log.info("🔄 Cargando credenciales de Firebase desde: {}", firebaseCredentials.getFilename());
                InputStream serviceAccount = firebaseCredentials.getInputStream();

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .setDatabaseUrl(firebaseDatabaseUrl)
                        .build();

                FirebaseApp.initializeApp(options);
                log.info("✅ Firebase Admin SDK inicializado correctamente con base de datos: {}", firebaseDatabaseUrl);
            } else {
                log.info("ℹ️ Firebase Admin SDK ya estaba inicializado, se reutiliza la configuración existente");
            }
        } catch (IOException e) {
            log.error("❌ Error al inicializar Firebase Admin SDK: {}", e.getMessage(), e);
            throw new RuntimeException("No se pudo inicializar Firebase", e);
        }
    }

    /**
     * Proporciona el bean de FirebaseAuth para validar tokens de Google.
     *
     * @return instancia de {@link FirebaseAuth}
     */
    @Bean
    public FirebaseAuth firebaseAuth() {
        log.info("✅ Bean FirebaseAuth creado y disponible para inyección");
        return FirebaseAuth.getInstance();
    }
}