package com.ondra.users.services;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Servicio para envío de correos electrónicos.
 * Utiliza Spring Mail para el envío de emails transaccionales.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @PostConstruct
    public void init() {
        log.info("EmailService inicializado. Email remitente configurado: {}", fromEmail);
        log.info("URL del frontend configurada: {}", frontendUrl);
    }

    /**
     * Envía un correo de verificación de email con un token único.
     *
     * @param destinatario Email del usuario
     * @param nombreUsuario Nombre del usuario
     * @param tokenVerificacion Token único de verificación
     */
    public void enviarEmailVerificacion(String destinatario, String nombreUsuario, String tokenVerificacion) {
        try {
            log.debug("Preparando email de verificación para: {}", destinatario);

            String enlaceVerificacion = frontendUrl + "/api/usuarios/verificar-email?token=" + tokenVerificacion;
            String htmlContent = construirMensajeVerificacionHTML(nombreUsuario, enlaceVerificacion);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail, "OndraSounds");
            helper.setTo(destinatario);
            helper.setSubject("Verifica tu cuenta - OndraSounds");
            helper.setText(htmlContent, true);

            log.debug("Enviando email desde: {} hacia: {}", fromEmail, destinatario);
            mailSender.send(mensaje);
            log.info("Email de verificación enviado exitosamente a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar email de verificación a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar el correo de verificación: " + e.getMessage());
        }
    }

    /**
     * Envía un correo de recuperación de contraseña con código de 6 dígitos.
     *
     * @param destinatario Email del usuario
     * @param codigoVerificacion Código de 6 dígitos
     * @param tokenRecuperacion Token único de recuperación para el enlace
     */
    public void enviarEmailRecuperacionConCodigo(String destinatario, String codigoVerificacion, String tokenRecuperacion) {
        try {
            log.debug("Preparando email de recuperación con código para: {}", destinatario);

            String enlaceRecuperacion = frontendUrl + "/api/usuarios/restablecer-password?token=" + tokenRecuperacion;
            String htmlContent = construirMensajeRecuperacionConCodigo(codigoVerificacion, enlaceRecuperacion);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail, "OndraSounds");
            helper.setTo(destinatario);
            helper.setSubject("Recuperación de contraseña - OndraSounds");
            helper.setText(htmlContent, true);

            mailSender.send(mensaje);
            log.info("Email de recuperación con código enviado a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar email de recuperación a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar el correo de recuperación: " + e.getMessage());
        }
    }

    /**
     * Envía un correo de confirmación después de cambiar la contraseña.
     *
     * @param destinatario Email del usuario
     * @param nombreUsuario Nombre del usuario
     */
    public void enviarEmailConfirmacionCambioPassword(String destinatario, String nombreUsuario) {
        try {
            log.debug("Preparando email de confirmación de cambio de contraseña para: {}", destinatario);

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(fromEmail, "OndraSounds");
            helper.setTo(destinatario);
            helper.setSubject("Tu contraseña ha sido cambiada - OndraSounds");
            helper.setText(construirMensajeConfirmacionCambio(nombreUsuario), true);

            mailSender.send(mensaje);
            log.info("Email de confirmación de cambio de contraseña enviado a: {}", destinatario);
        } catch (Exception e) {
            log.error("Error al enviar email de confirmación a {}: {}", destinatario, e.getMessage(), e);
        }
    }

    /**
     * Construye el mensaje HTML de verificación de email.
     */
    private String construirMensajeVerificacionHTML(String nombreUsuario, String enlaceVerificacion) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Verificación de cuenta</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f7f7f7;">
            <table role="presentation" cellpadding="0" cellspacing="0" width="100%%">
                <tr>
                    <td align="center" style="padding: 20px;">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" 
                               style="background-color: #ffffff; padding: 30px; border-radius: 8px; 
                                      box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); font-family: Arial, sans-serif;">
                            
                            <!-- Logo -->
                            <tr>
                                <td align="center" style="padding-bottom: 30px;">
                                    <img src="https://res.cloudinary.com/dh6w4hrx7/image/upload/v1759866494/logo_ondra_lqo3lv.jpg" 
                                         alt="OndraSounds Logo" width="600" 
                                         style="display: block; width: 100%%; max-width: 600px; height: auto;">
                                </td>
                            </tr>

                            <!-- Título -->
                            <tr>
                                <td style="color: black; font-size: 36px; margin-bottom: 20px;">
                                    ¡Hola, %s!
                                </td>
                            </tr>

                            <!-- Contenido -->
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Miles de usuarios ya están disfrutando de la experiencia OndraSounds.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Desde el equipo de desarrollo GC03, te agradecemos que quieras formar parte de la comunidad, 
                                    estás a un solo paso de hacerlo.
                                </td>
                            </tr>
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Haz clic en el botón a continuación para confirmar tu correo electrónico:
                                </td>
                            </tr>

                            <!-- Botón -->
                            <tr>
                                <td align="center" style="padding: 30px 0;">
                                    <table role="presentation" border="0" cellpadding="0" cellspacing="0">
                                        <tr>
                                            <td align="center">
                                                <a href="%s"
                                                   style="display: inline-block;
                                                          padding: 12px 20px;
                                                          background-color: black;
                                                          color: white;
                                                          text-decoration: none;
                                                          border-radius: 5px;
                                                          font-size: 18px;
                                                          text-align: center;">
                                                    Confirmar correo
                                                </a>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Nota -->
                            <tr>
                                <td align="center" style="margin-top: 30px; font-size: 12px; color: #777777;">
                                    Si no hiciste esta solicitud, puedes ignorar este mensaje.
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(nombreUsuario, enlaceVerificacion);
    }

    /**
     * Construye el mensaje HTML de recuperación con código de 6 dígitos.
     */
    private String construirMensajeRecuperacionConCodigo(String codigoVerificacion, String enlaceRecuperacion) {
        // Convertir el código en dígitos individuales
        StringBuilder digitosHTML = new StringBuilder();
        for (char digito : codigoVerificacion.toCharArray()) {
            digitosHTML.append(String.format("""
                <td style="width: 40px; height: 40px; background-color: #eeeeee;
                           text-align: center; vertical-align: middle;
                           font-size: 24px; font-family: monospace;
                           border-radius: 4px;">%c</td>
                """, digito));
        }

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Recuperación de contraseña</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f5f5f5;">
            <table role="presentation" cellpadding="0" cellspacing="0" width="100%%">
                <tr>
                    <td align="center" style="padding: 40px 0;">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" 
                               style="background-color: #ffffff; padding: 40px; border-radius: 8px; font-family: sans-serif;">

                            <!-- Logo -->
                            <tr>
                                <td align="center" style="padding-bottom: 30px;">
                                    <img src="https://res.cloudinary.com/dh6w4hrx7/image/upload/v1759866494/logo_ondra_lqo3lv.jpg" 
                                         alt="OndraSounds Logo" width="600"
                                         style="display: block; width: 100%%; max-width: 600px; height: auto;">
                                </td>
                            </tr>

                            <!-- Título -->
                            <tr>
                                <td align="center" style="font-size: 24px; font-weight: bold; color: #333333;">
                                    Recupera tu contraseña
                                </td>
                            </tr>

                            <!-- Subtítulo -->
                            <tr>
                                <td align="center" style="padding: 20px 0; font-size: 16px; color: #555555;">
                                    Ingresa el siguiente código para continuar con el proceso de recuperación:
                                </td>
                            </tr>

                            <!-- Código de 6 dígitos -->
                            <tr>
                                <td align="center">
                                    <table role="presentation" border="0" cellpadding="0" cellspacing="10" align="center">
                                        <tr>
                                            %s
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Botón -->
                            <tr>
                                <td align="center" style="padding: 30px 0;">
                                    <table role="presentation" border="0" cellpadding="0" cellspacing="0" align="center">
                                        <tr>
                                            <td align="center">
                                                <a href="%s"
                                                   style="display: inline-block;
                                                          padding: 12px 24px;
                                                          background-color: #000000;
                                                          color: #ffffff;
                                                          text-decoration: none;
                                                          border-radius: 5px;
                                                          font-size: 18px;
                                                          font-family: sans-serif;">
                                                    Cambiar contraseña
                                                </a>
                                            </td>
                                        </tr>
                                    </table>
                                </td>
                            </tr>

                            <!-- Nota -->
                            <tr>
                                <td align="center" style="font-size: 14px; color: #999999;">
                                    Si no solicitaste este cambio, puedes ignorar este correo.
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(digitosHTML.toString(), enlaceRecuperacion);
    }

    /**
     * Construye el mensaje de confirmación de cambio de contraseña.
     */
    private String construirMensajeConfirmacionCambio(String nombreUsuario) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>Contraseña cambiada</title>
        </head>
        <body style="margin: 0; padding: 0; background-color: #f5f5f5;">
            <table role="presentation" cellpadding="0" cellspacing="0" width="100%%">
                <tr>
                    <td align="center" style="padding: 40px 0;">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" 
                               style="background-color: #ffffff; padding: 40px; border-radius: 8px; font-family: sans-serif;">
                            
                            <!-- Logo -->
                            <tr>
                                <td align="center" style="padding-bottom: 30px;">
                                    <img src="https://res.cloudinary.com/dh6w4hrx7/image/upload/v1759866494/logo_ondra_lqo3lv.jpg" 
                                         alt="OndraSounds Logo" width="600"
                                         style="display: block; width: 100%%; max-width: 600px; height: auto;">
                                </td>
                            </tr>
                            
                            <tr>
                                <td style="font-size: 24px; font-weight: bold; color: #333333; padding-bottom: 20px;">
                                    ¡Hola, %s!
                                </td>
                            </tr>
                            
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Te confirmamos que tu contraseña de OndraSounds ha sido cambiada exitosamente.
                                </td>
                            </tr>
                            
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Si NO realizaste este cambio, por favor contacta con nuestro equipo de soporte inmediatamente:
                                    <strong>soporte@ondrasounds.com</strong>
                                </td>
                            </tr>
                            
                            <tr>
                                <td style="font-size: 16px; line-height: 1.5; color: #555555; padding: 10px 0;">
                                    Por tu seguridad, todas las sesiones activas en otros dispositivos han sido cerradas.
                                </td>
                            </tr>
                            
                            <tr>
                                <td style="font-size: 14px; color: #777777; padding-top: 30px;">
                                    Saludos,<br>
                                    El equipo de OndraSounds
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """.formatted(nombreUsuario);
    }
}