package com.ondra.users.controllers;

import com.ondra.users.dto.ImagenResponseDTO;
import com.ondra.users.dto.SuccessfulResponseDTO;
import com.ondra.users.services.CloudinaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

/**
 * Controlador para gestionar operaciones relacionadas con imágenes en Cloudinary.
 *
 * <p><strong>Funcionalidades:</strong></p>
 * <ul>
 *   <li>Subir imágenes de perfil de usuarios</li>
 *   <li>Subir imágenes de perfil de artistas</li>
 *   <li>Eliminar imágenes de Cloudinary</li>
 * </ul>
 *
 * <p><strong>Todos los endpoints requieren autenticación JWT.</strong></p>
 *
 * <p><strong>Validaciones implementadas:</strong></p>
 * <ul>
 *   <li>Formatos permitidos: JPG, PNG, WEBP</li>
 *   <li>Tamaño máximo: 5MB</li>
 *   <li>Transformación automática: 500x500px, crop fill, calidad auto</li>
 * </ul>
 *
 * <p>Las excepciones de negocio se propagan al
 * {@link com.ondra.users.exceptions.GlobalExceptionHandler}
 * que genera las respuestas HTTP apropiadas.</p>
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class ImagenController {

    private final CloudinaryService cloudinaryService;

    /**
     * Sube una imagen de perfil para un usuario.
     * Requiere autenticación JWT.
     *
     * <p>La imagen se guarda en la carpeta: <code>images/usuarios</code></p>
     * <p>Se aplica transformación automática a 500x500px con crop fill.</p>
     *
     * @param file Archivo de imagen a subir (máximo 5MB, formatos: JPG, PNG, WEBP)
     * @param authentication Objeto de autenticación de Spring Security
     * @return {@link ImagenResponseDTO} con la URL de la imagen subida
     * @throws com.ondra.users.exceptions.NoFileProvidedException Si no se proporciona archivo
     * @throws com.ondra.users.exceptions.InvalidImageFormatException Si el formato es inválido
     * @throws com.ondra.users.exceptions.ImageSizeExceededException Si excede el tamaño permitido
     * @throws com.ondra.users.exceptions.ImageUploadFailedException Si falla la subida
     */
    @PostMapping(value = "/imagenes/usuario",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImagenResponseDTO> subirImagenUsuario(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        // El servicio realiza todas las validaciones y lanza excepciones si es necesario
        String imageUrl = cloudinaryService.subirImagen(file, "usuarios");

        ImagenResponseDTO response = ImagenResponseDTO.builder()
                .url(imageUrl)
                .mensaje("Imagen de usuario subida correctamente")
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Sube una imagen de perfil para un artista.
     * Requiere autenticación JWT.
     *
     * <p>La imagen se guarda en la carpeta: <code>images/artistas</code></p>
     * <p>Se aplica transformación automática a 500x500px con crop fill.</p>
     *
     * @param file Archivo de imagen a subir (máximo 5MB, formatos: JPG, PNG, WEBP)
     * @param authentication Objeto de autenticación de Spring Security
     * @return {@link ImagenResponseDTO} con la URL de la imagen subida
     * @throws com.ondra.users.exceptions.NoFileProvidedException Si no se proporciona archivo
     * @throws com.ondra.users.exceptions.InvalidImageFormatException Si el formato es inválido
     * @throws com.ondra.users.exceptions.ImageSizeExceededException Si excede el tamaño permitido
     * @throws com.ondra.users.exceptions.ImageUploadFailedException Si falla la subida
     */
    @PostMapping(value = "/imagenes/artista",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ImagenResponseDTO> subirImagenArtista(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        // El servicio realiza todas las validaciones y lanza excepciones si es necesario
        String imageUrl = cloudinaryService.subirImagen(file, "artistas");

        ImagenResponseDTO response = ImagenResponseDTO.builder()
                .url(imageUrl)
                .mensaje("Imagen de artista subida correctamente")
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Elimina una imagen de Cloudinary.
     * Requiere autenticación JWT.
     *
     * <p><strong>Importante:</strong> Este endpoint solo elimina la imagen de Cloudinary.
     * La actualización de la base de datos debe hacerse en los endpoints específicos
     * de usuario o artista.</p>
     *
     * @param imageUrl URL completa de la imagen a eliminar
     * @param authentication Objeto de autenticación de Spring Security
     * @return {@link SuccessfulResponseDTO} con el resultado de la operación
     * @throws com.ondra.users.exceptions.ImageDeletionFailedException Si falla la eliminación
     */
    @DeleteMapping(value = "/imagenes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SuccessfulResponseDTO> eliminarImagen(
            @RequestParam("url") String imageUrl,
            Authentication authentication) {

        Long authenticatedUserId = Long.parseLong(authentication.getName());

        // El servicio lanza ImageDeletionFailedException si hay un error
        cloudinaryService.eliminarImagen(imageUrl);

        SuccessfulResponseDTO response = SuccessfulResponseDTO.builder()
                .successful("Eliminación de imagen exitosa")
                .message("La imagen ha sido eliminada correctamente de Cloudinary")
                .statusCode(HttpStatus.OK.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.ok(response);
    }
}