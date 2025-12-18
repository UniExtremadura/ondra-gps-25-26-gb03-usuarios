package com.ondra.users.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Arrays;
import java.util.List;

/**
 * Proporciona datos predefinidos de artistas y su información asociada.
 */
public class ArtistasData {

    /**
     * Contiene la información principal de un artista.
     */
    @Data
    @AllArgsConstructor
    public static class ArtistaInfo {

        /**
         * Nombre artístico del artista.
         */
        public String nombreArtistico;

        /**
         * Nombre real del artista.
         */
        public String nombreReal;

        /**
         * Identificador único utilizado en la plataforma.
         */
        public String username;

        /**
         * Breve descripción del artista.
         */
        public String biografia;

        /**
         * Lista de redes sociales en las que el artista tiene presencia.
         */
        public List<String> redesSociales;

        /**
         * Indica si el artista es tendencia.
         */
        public boolean esTendencia;

        /**
         * URL de la imagen compartida correspondiente al artista.
         */
        public String urlImagenCompartida;
    }

    /**
     * Lista predefinida de artistas con información básica.
     */
    public static final List<ArtistaInfo> ARTISTAS_PREDEFINIDOS = Arrays.asList(
            new ArtistaInfo(
                    "Duki",
                    "Mauro Ezequiel Lombardo Quiroga",
                    "duki",
                    "Cantante, rapero y compositor argentino, considerado uno de los mayores exponentes del trap latino.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK", "FACEBOOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1765996054/duki_m8x2jb.jpg"
            ),
            new ArtistaInfo(
                    "Aitana",
                    "Aitana Ocaña Morales",
                    "aitana",
                    "Cantante española de pop. Participó en OT 2017 y cuenta con temas reconocidos.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK", "FACEBOOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1765996118/aitana_phwv23.jpg"
            ),
            new ArtistaInfo(
                    "Avicii",
                    "Tim Bergling",
                    "avicii",
                    "DJ y productor sueco de referencia en el género electrónico.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201873/avicii_q2afmo.jpg"
            ),
            new ArtistaInfo(
                    "Daddy Yankee",
                    "Ramón Luis Ayala Rodríguez",
                    "daddyyankee",
                    "Artista puertorriqueño considerado pionero del género urbano.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK", "TIKTOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201880/daddyyankee_vigfof.jpg"
            ),
            new ArtistaInfo(
                    "Rosalía",
                    "Rosalía Vila Tobella",
                    "rosalia",
                    "Artista española que fusiona flamenco y pop urbano.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201917/rosalia_x1mhzj.jpg"
            ),
            new ArtistaInfo(
                    "Sanguijuelas del Guadiana",
                    "Sanguijuelas del Guadiana",
                    "sanguijuelasdelguadiana",
                    "Banda española que mezcla rock alternativo, blues y poesía urbana.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764201921/sanguijuelasdelguadiana_io5l2x.jpg"
            ),
            new ArtistaInfo(
                    "Quevedo",
                    "Pedro Luis Domínguez Quevedo",
                    "quevedo",
                    "Cantante español de trap y reguetón con impacto internacional.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/quevedo_b822uv.jpg"
            ),
            new ArtistaInfo(
                    "Extremoduro",
                    "Extremoduro",
                    "extremoduro",
                    "Banda española de rock liderada por Robe Iniesta.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/extremoduro_n3ir0m.jpg"
            ),
            new ArtistaInfo(
                    "Arde Bogotá",
                    "Arde Bogotá",
                    "ardebogota",
                    "Banda española de indie rock reconocida por su puesta en escena.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259677/ardebogota_ik3zwo.jpg"
            ),
            new ArtistaInfo(
                    "Myke Towers",
                    "Michael Anthony Torres Monge",
                    "myketowers",
                    "Artista puertorriqueño destacado en trap y reguetón.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259673/myketowers_gg8ttu.jpg"
            ),
            new ArtistaInfo(
                    "Melendi",
                    "Ramón Melendi Espina",
                    "melendi",
                    "Cantautor español que combina pop, rumba y flamenco.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259673/melendi_xau7np.jpg"
            ),
            new ArtistaInfo(
                    "Bad Bunny",
                    "Benito Antonio Martínez Ocasio",
                    "badbunny",
                    "Artista puertorriqueño de relevancia internacional en música urbana.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK", "FACEBOOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259677/badbunny_hgsxp4.jpg"
            ),
            new ArtistaInfo(
                    "Fito y Fitipaldis",
                    "Fito y Fitipaldis",
                    "fitoyfitipaldis",
                    "Banda española de rock liderada por Fito Cabrales.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/fitofitipaldis_obs5zc.jpg"
            ),
            new ArtistaInfo(
                    "Juan Magán",
                    "Juan Manuel Magán González",
                    "juanmagan",
                    "DJ y productor español asociado al electro latino.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/juanmagan_wwm3cq.jpg"
            ),
            new ArtistaInfo(
                    "Estopa",
                    "Estopa",
                    "estopa",
                    "Dúo español de rumba y rock formado por los hermanos Muñoz.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/estopa_sq01bj.jpg"
            ),
            new ArtistaInfo(
                    "Eladio Carrión",
                    "Eladio Carrión Morales",
                    "eladiocarrion",
                    "Cantante puertorriqueño de trap con colaboraciones destacadas.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259674/eladiocarrion_xwiuvd.jpg"
            ),
            new ArtistaInfo(
                    "Coldplay",
                    "Coldplay",
                    "coldplay",
                    "Banda británica de rock alternativo de reconocimiento mundial.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "FACEBOOK", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259675/coldplay_iyjftl.jpg"
            ),
            new ArtistaInfo(
                    "Taylor Swift",
                    "Taylor Alison Swift",
                    "taylor-swift",
                    "Cantautora estadounidense influyente en pop y country contemporáneo.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK", "FACEBOOK"),
                    true,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259673/taylorswift_nwpyru.jpg"
            ),
            new ArtistaInfo(
                    "Recycled J",
                    "Jorge Escorial Puy",
                    "recycled-j",
                    "Artista español que fusiona pop urbano, rap y electrónica.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764259935/recycledj_fjcmgy.jpg"
            ),
            new ArtistaInfo(
                    "Los Psicomotronic",
                    "Amador Rivas",
                    "los-psicootronic",
                    "Grupo ficticio asociado al tema Mandanga Style.",
                    Arrays.asList("INSTAGRAM", "X", "SPOTIFY", "YOUTUBE", "TIKTOK"),
                    false,
                    "https://res.cloudinary.com/dh6w4hrx7/image/upload/v1764260119/psicomotronic_mwh4te.jpg"
            )
    );
}
