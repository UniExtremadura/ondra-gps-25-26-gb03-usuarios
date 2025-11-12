package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando el tamaño de la imagen excede el límite permitido.
 * El límite actual es de 5MB.
 */
public class ImageSizeExceededException extends RuntimeException {
    public ImageSizeExceededException(String message) {
        super(message);
    }
}