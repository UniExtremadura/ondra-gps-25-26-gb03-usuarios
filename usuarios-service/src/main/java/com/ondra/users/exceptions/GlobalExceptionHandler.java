package com.ondra.users.exceptions;

import com.ondra.users.dto.ErrorDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para la API de usuarios.
 *
 * <p>Intercepta excepciones lanzadas por los controladores y servicios,
 * generando respuestas homogéneas en formato {@link ErrorDTO} con código HTTP,
 * mensaje descriptivo y marca temporal según la especificación OpenAPI.</p>
 *
 * <p>Las excepciones se organizan por categorías:</p>
 * <ul>
 *   <li>Autenticación: credenciales, tokens, cuentas inactivas</li>
 *   <li>Recursos: usuarios, artistas, redes sociales, métodos de pago no encontrados</li>
 *   <li>Permisos: acceso denegado</li>
 *   <li>Validación: datos inválidos, métodos de pago incorrectos, duplicados</li>
 *   <li>Seguimientos: validaciones de seguir/dejar de seguir</li>
 *   <li>Archivos: validaciones de imágenes y subida</li>
 *   <li>Generales: errores de validación de Spring y errores inesperados</li>
 * </ul>
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==================== MÉTODO HELPER ====================

    /**
     * Método helper para crear respuestas de error estandarizadas.
     *
     * @param errorCode Código de error en SCREAMING_SNAKE_CASE
     * @param message Mensaje descriptivo del error
     * @param status Código de estado HTTP
     * @param logWarning Si debe registrar un warning en logs
     * @return ResponseEntity con ErrorDTO estandarizado
     */
    private ResponseEntity<ErrorDTO> createErrorResponse(
            String errorCode,
            String message,
            HttpStatus status,
            boolean logWarning) {

        if (logWarning) {
            log.warn("Error [{}]: {}", errorCode, message);
        }

        ErrorDTO error = ErrorDTO.builder()
                .error(errorCode)
                .message(message)
                .statusCode(status.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(status).body(error);
    }

    // ==================== EXCEPCIONES DE AUTENTICACIÓN ====================

    /**
     * Maneja excepciones cuando el email ya existe durante el registro.
     * <p>Código de error: EMAIL_ALREADY_EXISTS | HTTP 409</p>
     */
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorDTO> handleEmailAlreadyExists(EmailAlreadyExistsException ex) {
        return createErrorResponse(
                "EMAIL_ALREADY_EXISTS",
                ex.getMessage(),
                HttpStatus.CONFLICT,
                false
        );
    }

    /**
     * Maneja excepciones cuando las credenciales de login son incorrectas.
     * <p>Código de error: INVALID_CREDENTIALS | HTTP 401</p>
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorDTO> handleInvalidCredentials(InvalidCredentialsException ex) {
        return createErrorResponse(
                "INVALID_CREDENTIALS",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                true
        );
    }

    /**
     * Maneja excepciones cuando la cuenta está inactiva.
     * <p>Código de error: ACCOUNT_INACTIVE | HTTP 403</p>
     */
    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ErrorDTO> handleAccountInactive(AccountInactiveException ex) {
        return createErrorResponse(
                "ACCOUNT_INACTIVE",
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                false
        );
    }

    /**
     * Maneja excepciones cuando el email no ha sido verificado.
     * <p>Código de error: EMAIL_NOT_VERIFIED | HTTP 403</p>
     */
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorDTO> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return createErrorResponse(
                "EMAIL_NOT_VERIFIED",
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                false
        );
    }

    /**
     * Maneja excepciones cuando el token de verificación es inválido.
     * <p>Código de error: INVALID_VERIFICATION_TOKEN | HTTP 400</p>
     */
    @ExceptionHandler(InvalidVerificationTokenException.class)
    public ResponseEntity<ErrorDTO> handleInvalidVerificationToken(InvalidVerificationTokenException ex) {
        return createErrorResponse(
                "INVALID_VERIFICATION_TOKEN",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                false
        );
    }

    /**
     * Maneja excepciones cuando el token de Google es inválido.
     * <p>Código de error: INVALID_GOOGLE_TOKEN | HTTP 401</p>
     */
    @ExceptionHandler(InvalidGoogleTokenException.class)
    public ResponseEntity<ErrorDTO> handleInvalidGoogleToken(InvalidGoogleTokenException ex) {
        return createErrorResponse(
                "INVALID_GOOGLE_TOKEN",
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                true
        );
    }

    /**
     * Maneja excepciones cuando el login con Google está deshabilitado.
     * <p>Código de error: GOOGLE_LOGIN_DISABLED | HTTP 403</p>
     */
    @ExceptionHandler(GoogleLoginDisabledException.class)
    public ResponseEntity<ErrorDTO> handleGoogleLoginDisabled(GoogleLoginDisabledException ex) {
        return createErrorResponse(
                "GOOGLE_LOGIN_DISABLED",
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                false
        );
    }

    /**
     * Maneja excepciones cuando el refresh token es inválido.
     * <p>Código de error: INVALID_REFRESH_TOKEN | HTTP 400</p>
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorDTO> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
        return createErrorResponse(
                "INVALID_REFRESH_TOKEN",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                true
        );
    }

    /**
     * Maneja excepciones cuando el token de recuperación de contraseña es inválido.
     * <p>Código de error: INVALID_PASSWORD_RESET_TOKEN | HTTP 400</p>
     */
    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ErrorDTO> handleInvalidPasswordResetToken(InvalidPasswordResetTokenException ex) {
        return createErrorResponse(
                "INVALID_PASSWORD_RESET_TOKEN",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                false
        );
    }

    // ==================== EXCEPCIONES DE RECURSOS NO ENCONTRADOS ====================

    /**
     * Maneja excepciones cuando no se encuentra un usuario.
     * <p>Código de error: USER_NOT_FOUND | HTTP 404</p>
     */
    @ExceptionHandler(UsuarioNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleUsuarioNotFound(UsuarioNotFoundException ex) {
        return createErrorResponse(
                "USER_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                false
        );
    }

    // ==================== EXCEPCIONES DE PERMISOS ====================

    /**
     * Maneja excepciones cuando un usuario intenta acceder a recursos sin permisos.
     * <p>Código de error: FORBIDDEN | HTTP 403</p>
     */
    @ExceptionHandler(ForbiddenAccessException.class)
    public ResponseEntity<ErrorDTO> handleForbiddenAccess(ForbiddenAccessException ex) {
        return createErrorResponse(
                "FORBIDDEN",
                ex.getMessage(),
                HttpStatus.FORBIDDEN,
                false
        );
    }

    // ==================== EXCEPCIONES DE SEGUIMIENTOS ====================

    /**
     * Maneja excepciones cuando se intenta realizar un seguimiento inválido.
     * <p>Código de error: INVALID_FOLLOW | HTTP 400</p>
     */
    @ExceptionHandler(InvalidFollowException.class)
    public ResponseEntity<ErrorDTO> handleInvalidFollow(InvalidFollowException ex) {
        return createErrorResponse(
                "INVALID_FOLLOW",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                true
        );
    }

    /**
     * Maneja excepciones cuando se intenta crear un seguimiento duplicado.
     * <p>Código de error: DUPLICATE_FOLLOW | HTTP 409</p>
     */
    @ExceptionHandler(DuplicateFollowException.class)
    public ResponseEntity<ErrorDTO> handleDuplicateFollow(DuplicateFollowException ex) {
        return createErrorResponse(
                "DUPLICATE_FOLLOW",
                ex.getMessage(),
                HttpStatus.CONFLICT,
                true
        );
    }

    /**
     * Maneja excepciones cuando no se encuentra un seguimiento existente.
     * <p>Código de error: FOLLOW_NOT_FOUND | HTTP 404</p>
     */
    @ExceptionHandler(FollowNotFoundException.class)
    public ResponseEntity<ErrorDTO> handleFollowNotFound(FollowNotFoundException ex) {
        return createErrorResponse(
                "FOLLOW_NOT_FOUND",
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                true
        );
    }

    // ==================== EXCEPCIONES DE VALIDACIÓN DE SPRING ====================

    /**
     * Maneja errores de validación de argumentos de métodos (@Valid).
     * <p>Recopila los errores de cada campo y los retorna en formato {@link ErrorDTO}.</p>
     * <p>Código de error: VALIDATION_ERROR | HTTP 400</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Error de validación: {}", errors);

        ErrorDTO error = ErrorDTO.builder()
                .error("VALIDATION_ERROR")
                .message("Error de validación: " + errors)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ==================== EXCEPCIÓN GENÉRICA ====================

    /**
     * Maneja excepciones generales no controladas específicamente.
     * <p>Actúa como red de seguridad para cualquier excepción no manejada.</p>
     * <p>Código de error: INTERNAL_ERROR | HTTP 500</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> handleException(Exception ex) {
        log.error("Error interno no controlado: {}", ex.getMessage(), ex);

        ErrorDTO error = ErrorDTO.builder()
                .error("INTERNAL_ERROR")
                .message("Ha ocurrido un error interno en el servidor")
                .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .timestamp(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}