package com.ondra.users.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Arrays;
import java.util.List;

/**
 * Proporciona datos predefinidos de usuarios y su información básica.
 */
public class UsuariosData {

    /**
     * Contiene la información principal de un usuario.
     */
    @Data
    @AllArgsConstructor
    public static class UsuarioInfo {

        /**
         * Nombre del usuario.
         */
        public String nombre;

        /**
         * Apellidos del usuario.
         */
        public String apellidos;

        /**
         * Identificador único utilizado en la plataforma.
         */
        public String username;

        /**
         * URL de la imagen asociada al usuario.
         */
        public String urlImagenCompartida;
    }

    /**
     * Lista predefinida de usuarios con datos básicos.
     */
    public static final List<UsuarioInfo> USUARIOS_PREDEFINIDOS = Arrays.asList(
            new UsuarioInfo(
                    "Ana",
                    "García López",
                    "anagarcia",
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1766074554/anagarcia_lpc4ef.webp"
            ),
            new UsuarioInfo(
                    "Carlos",
                    "Martínez Ruiz",
                    "carlosmartinez",
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201949/carlosmartinez_vmpvwd.webp"
            ),
            new UsuarioInfo(
                    "Laura",
                    "Rodríguez Sánchez",
                    "laurarodriguez",
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201951/laurarodriguez_i6fh7k.jpg"
            ),
            new UsuarioInfo(
                    "Miguel",
                    "Fernández Torres",
                    "miguelfernandez",
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201952/miguelfernandez_ggngar.jpg"
            ),
            new UsuarioInfo(
                    "Sara",
                    "González Pérez",
                    "saragonzalez",
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201953/saragonzalez_tv5bgz.jpg"
            )
    );
}
