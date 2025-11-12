package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando falla la eliminación de imagen de Cloudinary.
 * Puede ocurrir si la imagen ya no existe o por problemas de conexión.
 */
public class ImageDeletionFailedException extends RuntimeException {
    public ImageDeletionFailedException(String message) {
        super(message);
    }

    public ImageDeletionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
