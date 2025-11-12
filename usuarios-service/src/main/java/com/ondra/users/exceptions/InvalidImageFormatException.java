package com.ondra.users.exceptions;

/**
 * Excepción lanzada cuando el archivo de imagen es inválido.
 * Se usa cuando el tipo de archivo no es soportado (no es JPG, PNG, WEBP).
 */
public class InvalidImageFormatException extends RuntimeException {
    public InvalidImageFormatException(String message) {
        super(message);
    }
}