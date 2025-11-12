package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando falla la subida de imagen a Cloudinary.
 * Puede ocurrir por problemas de red, credenciales inválidas, o límites de cuota.
 */
public class ImageUploadFailedException extends RuntimeException {
    public ImageUploadFailedException(String message) {
        super(message);
    }

    public ImageUploadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}