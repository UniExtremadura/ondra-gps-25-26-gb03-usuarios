package com.ondra.users.services;

import com.cloudinary.Cloudinary;
import com.cloudinary.api.ApiResponse;
import com.cloudinary.utils.ObjectUtils;
import com.ondra.users.exceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para gestionar la subida y eliminación de imágenes en Cloudinary.
 *
 * <p><strong>Funcionalidades:</strong></p>
 * <ul>
 *   <li>Subir imágenes con validación de formato y tamaño</li>
 *   <li>Transformación automática a 500x500px</li>
 *   <li>Eliminación de imágenes</li>
 *   <li>Limpieza de carpetas (útil para seeding)</li>
 * </ul>
 *
 * <p><strong>Configuración:</strong></p>
 * <ul>
 *   <li>Formatos permitidos: JPG, PNG, WEBP</li>
 *   <li>Tamaño máximo: 5MB</li>
 *   <li>Carpeta base: configurable en application.properties</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder}")
    private String folder;

    /**
     * Sube una imagen a Cloudinary en la carpeta configurada.
     *
     * <p>Aplica las siguientes transformaciones:</p>
     * <ul>
     *   <li>Redimensionamiento: 500x500px</li>
     *   <li>Crop: fill (mantiene aspecto y rellena)</li>
     *   <li>Calidad: automática (optimización de Cloudinary)</li>
     * </ul>
     *
     * @param file Archivo de imagen a subir
     * @param carpeta Subcarpeta específica dentro del folder principal (ej: "usuarios", "artistas")
     * @return URL pública de la imagen subida
     * @throws NoFileProvidedException Si el archivo es nulo o está vacío
     * @throws InvalidImageFormatException Si el formato de imagen no es válido
     * @throws ImageSizeExceededException Si el tamaño excede el límite permitido (5MB)
     * @throws ImageUploadFailedException Si ocurre un error durante la subida
     */
    public String subirImagen(MultipartFile file, String carpeta) {
        log.debug("Iniciando subida de imagen a carpeta: {}", carpeta);

        // Validar que existe archivo
        if (file == null || file.isEmpty()) {
            log.warn("Intento de subir imagen sin proporcionar archivo");
            throw new NoFileProvidedException("No se ha proporcionado ningún archivo");
        }

        // Validar formato
        if (!esImagenValida(file)) {
            log.warn("Intento de subir archivo con formato inválido: {}", file.getContentType());
            throw new InvalidImageFormatException(
                    "El archivo debe ser una imagen válida (JPG, PNG, WEBP)"
            );
        }

        // Validar tamaño
        if (!esTamanoValido(file)) {
            log.warn("Intento de subir imagen que excede el tamaño máximo: {} bytes", file.getSize());
            throw new ImageSizeExceededException("La imagen no puede superar los 5MB");
        }

        try {
            String publicId = generarPublicId();
            String folderPath = folder + "/" + carpeta;

            log.debug("Subiendo imagen con public_id: {} a carpeta: {}", publicId, folderPath);

            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", folderPath,
                            "resource_type", "image",
                            "overwrite", true,
                            "transformation", new com.cloudinary.Transformation()
                                    .width(500).height(500)
                                    .crop("fill")
                                    .quality("auto")
                    ));

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Imagen subida exitosamente a Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("❌ Error al subir imagen a Cloudinary: {}", e.getMessage(), e);
            throw new ImageUploadFailedException("Error al subir la imagen a Cloudinary", e);
        }
    }

    /**
     * Elimina una imagen de Cloudinary dado su URL.
     *
     * <p>Extrae automáticamente el public_id de la URL y elimina el recurso.</p>
     *
     * @param imageUrl URL completa de la imagen a eliminar
     * @throws ImageDeletionFailedException Si ocurre un error durante la eliminación
     */
    public void eliminarImagen(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            log.warn("Se intentó eliminar una imagen con URL nula o vacía");
            return;
        }

        String publicId = extraerPublicId(imageUrl);
        if (publicId == null) {
            log.warn("No se pudo extraer el public_id de la URL: {}", imageUrl);
            return;
        }

        try {
            log.debug("Eliminando imagen de Cloudinary con public_id: {}", publicId);

            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String resultStatus = (String) result.get("result");

            if ("ok".equals(resultStatus)) {
                log.info("✅ Imagen eliminada de Cloudinary: {}", publicId);
            } else {
                log.warn("⚠️ Resultado inesperado al eliminar imagen: {} - Status: {}",
                        publicId, resultStatus);
            }
        } catch (IOException e) {
            log.error("❌ Error al eliminar imagen de Cloudinary: {}", e.getMessage(), e);
            throw new ImageDeletionFailedException("Error al eliminar la imagen de Cloudinary", e);
        }
    }

    /**
     * Extrae el public_id de una URL de Cloudinary.
     *
     * <p>Ejemplo de URL:</p>
     * <pre>
     * https://res.cloudinary.com/demo/image/upload/v1234567890/images/usuarios/abc123.jpg
     *                                                           ↑ public_id: images/usuarios/abc123
     * </pre>
     *
     * @param imageUrl URL completa de la imagen
     * @return public_id extraído o null si no se puede extraer
     */
    private String extraerPublicId(String imageUrl) {
        try {
            // Buscar el segmento "/upload/"
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                log.warn("URL no contiene '/upload/': {}", imageUrl);
                return null;
            }

            // Obtener todo después de "/upload/"
            String afterUpload = imageUrl.substring(uploadIndex + 8);

            // Saltar la versión (v1234567890/)
            int versionEnd = afterUpload.indexOf("/");
            if (versionEnd == -1) {
                log.warn("URL no tiene formato de versión correcto: {}", imageUrl);
                return null;
            }

            // Obtener el path con extensión
            String pathWithExtension = afterUpload.substring(versionEnd + 1);

            // Quitar la extensión (.jpg, .png, etc.)
            int lastDot = pathWithExtension.lastIndexOf(".");
            String publicId = lastDot != -1
                    ? pathWithExtension.substring(0, lastDot)
                    : pathWithExtension;

            log.debug("Public ID extraído: {}", publicId);
            return publicId;

        } catch (Exception e) {
            log.error("Error al extraer public_id de la URL: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * Genera un identificador único para la imagen usando UUID.
     *
     * @return public_id único en formato UUID
     */
    private String generarPublicId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Valida que el archivo sea una imagen válida.
     *
     * <p>Formatos permitidos:</p>
     * <ul>
     *   <li>image/jpeg</li>
     *   <li>image/jpg</li>
     *   <li>image/png</li>
     *   <li>image/webp</li>
     * </ul>
     *
     * @param file Archivo a validar
     * @return true si es una imagen válida, false en caso contrario
     */
    public boolean esImagenValida(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        return contentType.equals("image/jpeg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/webp");
    }

    /**
     * Valida el tamaño del archivo (máximo 5MB).
     *
     * @param file Archivo a validar
     * @return true si el tamaño es válido, false si excede el límite
     */
    public boolean esTamanoValido(MultipartFile file) {
        long maxSize = 5 * 1024 * 1024; // 5MB
        return file != null && file.getSize() <= maxSize;
    }

    /**
     * Elimina todas las imágenes de una carpeta específica en Cloudinary.
     *
     * <p><strong>Uso principal:</strong> Limpieza de datos de seeding antes de volver a poblar la base de datos.</p>
     *
     * <p><strong>Advertencia:</strong> Esta operación es irreversible.</p>
     *
     * @param carpeta Subcarpeta dentro del folder principal a limpiar (ej: "usuarios", "artistas")
     * @return Número de imágenes eliminadas
     */
    public int limpiarCarpeta(String carpeta) {
        String folderPath = folder + "/" + carpeta;
        int imagenesEliminadas = 0;

        try {
            log.info("🧹 Iniciando limpieza de la carpeta: {}", folderPath);

            // Obtener todas las imágenes de la carpeta
            ApiResponse result = cloudinary.api().resources(
                    ObjectUtils.asMap(
                            "type", "upload",
                            "prefix", folderPath,
                            "max_results", 500
                    ));

            List<Map> resources = (List<Map>) result.get("resources");

            if (resources == null || resources.isEmpty()) {
                log.info("ℹ️ No se encontraron imágenes en la carpeta: {}", folderPath);
                return 0;
            }

            log.info("📦 Se encontraron {} imágenes para eliminar", resources.size());

            // Eliminar cada imagen
            for (Map resource : resources) {
                String publicId = (String) resource.get("public_id");
                try {
                    cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                    imagenesEliminadas++;
                    log.debug("🗑️ Imagen eliminada: {}", publicId);
                } catch (Exception e) {
                    log.warn("⚠️ No se pudo eliminar la imagen: {} - Error: {}",
                            publicId, e.getMessage());
                }
            }

            log.info("✅ Limpieza completada: {} imágenes eliminadas de {}",
                    imagenesEliminadas, folderPath);

        } catch (Exception e) {
            log.error("❌ Error durante la limpieza de la carpeta {}: {}",
                    folderPath, e.getMessage(), e);
        }

        return imagenesEliminadas;
    }
}