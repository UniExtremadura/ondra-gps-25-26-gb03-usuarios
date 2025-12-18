package com.ondra.users.services;

import com.ondra.users.dto.MetodoCobroArtistaCrearDTO;
import com.ondra.users.dto.MetodoCobroArtistaDTO;
import com.ondra.users.dto.MetodoCobroArtistaEditarDTO;
import com.ondra.users.dto.MetodoCobroBasicoDTO;
import com.ondra.users.exceptions.ArtistaNotFoundException;
import com.ondra.users.exceptions.ForbiddenAccessException;
import com.ondra.users.exceptions.InvalidPaymentMethodException;
import com.ondra.users.exceptions.MetodoPagoUsuarioNotFoundException;
import com.ondra.users.models.dao.MetodoCobroArtista;
import com.ondra.users.models.enums.TipoMetodoPago;
import com.ondra.users.repositories.ArtistaRepository;
import com.ondra.users.repositories.MetodoCobroArtistaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de métodos de cobro de artistas.
 *
 * <p>Implementa operaciones CRUD sobre métodos de cobro con validaciones específicas
 * por tipo de método y control de acceso basado en propiedad del perfil de artista.</p>
 *
 * <p>Restricciones:</p>
 * <ul>
 *   <li>Artistas no pueden usar TARJETA como método de cobro</li>
 *   <li>Solo el propietario del perfil puede gestionar sus métodos de cobro</li>
 *   <li>Cada tipo de método requiere campos específicos obligatorios</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MetodoCobroArtistaService {

    private final ArtistaRepository artistaRepository;
    private final MetodoCobroArtistaRepository metodoCobroRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Obtiene todos los métodos de cobro de un artista.
     *
     * @param idArtista identificador del artista
     * @param authenticatedUserId identificador del usuario autenticado
     * @return lista de métodos de cobro
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario
     */
    @Transactional(readOnly = true)
    public List<MetodoCobroArtistaDTO> listarMetodosCobro(Long idArtista, Long authenticatedUserId) {
        var artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> new ArtistaNotFoundException(idArtista));

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permiso para ver los métodos de cobro de este artista");
        }

        return metodoCobroRepository.findByArtista(artista)
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }

    /**
     * Crea un método de cobro para un artista.
     *
     * <p>Valida que el tipo sea permitido para artistas y que contenga
     * los campos específicos requeridos según el método seleccionado.</p>
     *
     * @param idArtista identificador del artista
     * @param crearDTO datos del método de cobro
     * @param authenticatedUserId identificador del usuario autenticado
     * @return método de cobro creado
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario
     * @throws InvalidPaymentMethodException si el método no es válido o falta información
     */
    @Transactional
    public MetodoCobroArtistaDTO crearMetodoCobro(Long idArtista, MetodoCobroArtistaCrearDTO crearDTO, Long authenticatedUserId) {
        var artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> new ArtistaNotFoundException(idArtista));

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permiso para añadir métodos de cobro a este artista");
        }

        TipoMetodoPago tipoMetodoPago;
        try {
            tipoMetodoPago = TipoMetodoPago.valueOf(crearDTO.getMetodoPago().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidPaymentMethodException("Método de cobro no válido: " + crearDTO.getMetodoPago());
        }

        if (tipoMetodoPago == TipoMetodoPago.TARJETA) {
            throw new InvalidPaymentMethodException("Los artistas no pueden usar tarjeta como método de cobro");
        }

        validarCamposEspecificos(tipoMetodoPago, crearDTO);

        MetodoCobroArtista metodoCobro = MetodoCobroArtista.builder()
                .artista(artista)
                .tipoCobro(tipoMetodoPago)
                .propietario(crearDTO.getPropietario())
                .direccion(crearDTO.getDireccion())
                .pais(crearDTO.getPais())
                .provincia(crearDTO.getProvincia())
                .codigoPostal(crearDTO.getCodigoPostal())
                .build();

        asignarCamposEspecificos(metodoCobro, tipoMetodoPago, crearDTO);

        var metodoCobroGuardado = metodoCobroRepository.save(metodoCobro);
        log.info("✅ Método de cobro creado con ID: {}", metodoCobroGuardado.getIdMetodoCobroArtista());

        return convertirADTO(metodoCobroGuardado);
    }

    /**
     * Actualiza un método de cobro existente.
     *
     * <p>Modifica únicamente los campos no nulos del DTO de edición.</p>
     *
     * @param idArtista identificador del artista
     * @param idMetodoCobro identificador del método de cobro
     * @param editarDTO datos a actualizar
     * @param authenticatedUserId identificador del usuario autenticado
     * @return método de cobro actualizado
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario
     * @throws MetodoPagoUsuarioNotFoundException si el método de cobro no existe
     */
    @Transactional
    public MetodoCobroArtistaDTO editarMetodoCobro(Long idArtista, Long idMetodoCobro, MetodoCobroArtistaEditarDTO editarDTO, Long authenticatedUserId) {
        var artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> new ArtistaNotFoundException(idArtista));

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permiso para modificar métodos de cobro de este artista");
        }

        var metodoCobro = metodoCobroRepository.findByIdMetodoCobroArtistaAndArtista(idMetodoCobro, artista)
                .orElseThrow(() -> new MetodoPagoUsuarioNotFoundException(idMetodoCobro));

        if (editarDTO.getPropietario() != null) {
            metodoCobro.setPropietario(editarDTO.getPropietario());
        }

        actualizarCamposEspecificos(metodoCobro, editarDTO);

        var metodoCobroActualizado = metodoCobroRepository.save(metodoCobro);
        log.info("✅ Método de cobro actualizado con ID: {}", metodoCobroActualizado.getIdMetodoCobroArtista());

        return convertirADTO(metodoCobroActualizado);
    }

    /**
     * Elimina un método de cobro de un artista.
     *
     * @param idArtista identificador del artista
     * @param idMetodoCobro identificador del método de cobro
     * @param authenticatedUserId identificador del usuario autenticado
     * @throws ArtistaNotFoundException si el artista no existe
     * @throws ForbiddenAccessException si el usuario no es propietario
     * @throws MetodoPagoUsuarioNotFoundException si el método de cobro no existe
     */
    @Transactional
    public void eliminarMetodoCobro(Long idArtista, Long idMetodoCobro, Long authenticatedUserId) {
        var artista = artistaRepository.findById(idArtista)
                .orElseThrow(() -> new ArtistaNotFoundException(idArtista));

        if (!artista.getUsuario().getIdUsuario().equals(authenticatedUserId)) {
            throw new ForbiddenAccessException("No tienes permiso para eliminar este método de cobro");
        }

        var metodoCobro = metodoCobroRepository.findByIdMetodoCobroArtistaAndArtista(idMetodoCobro, artista)
                .orElseThrow(() -> new MetodoPagoUsuarioNotFoundException(idMetodoCobro));

        metodoCobroRepository.delete(metodoCobro);
        log.info("✅ Método de cobro eliminado con ID: {}", idMetodoCobro);
    }

    /**
     * Obtiene el primer método de cobro registrado por un artista.
     *
     * <p>Devuelve únicamente información básica del método, incluyendo su
     * identificador y su tipo. Si el artista no tiene métodos de cobro,
     * se devuelve {@code null}.</p>
     *
     * @param idArtista identificador del artista
     * @return DTO con la información básica del primer método de cobro,
     *         o {@code null} si no existen métodos asociados
     */
    @Transactional(readOnly = true)
    public MetodoCobroBasicoDTO obtenerPrimerMetodoCobroBasico(Long idArtista) {
        MetodoCobroArtista metodo = metodoCobroRepository
                .findFirstByArtista_IdArtistaOrderByFechaCreacionAsc(idArtista);

        if (metodo == null) {
            return null;
        }

        return MetodoCobroBasicoDTO.builder()
                .idMetodoCobro(metodo.getIdMetodoCobroArtista())
                .tipo(metodo.getTipoCobro().name())
                .build();
    }

    /**
     * Obtiene un método de cobro en formato básico a partir de su identificador.
     *
     * <p>Devuelve únicamente los datos esenciales del método: su ID y su tipo.
     * Si no existe ningún método con el identificador proporcionado,
     * se devuelve {@code null}.</p>
     *
     * @param idMetodoCobro identificador del método de cobro
     * @return DTO básico del método de cobro, o {@code null} si no existe
     */
    @Transactional(readOnly = true)
    public MetodoCobroBasicoDTO obtenerMetodoCobroBasicoPorId(Long idMetodoCobro) {
        return metodoCobroRepository.findById(idMetodoCobro)
                .map(metodo -> MetodoCobroBasicoDTO.builder()
                        .idMetodoCobro(metodo.getIdMetodoCobroArtista())
                        .tipo(metodo.getTipoCobro().name())
                        .build())
                .orElse(null);
    }

    /**
     * Valida campos específicos requeridos según el tipo de método.
     *
     * @param tipoMetodoPago tipo de método de cobro
     * @param dto datos a validar
     * @throws InvalidPaymentMethodException si faltan campos requeridos
     */
    private void validarCamposEspecificos(TipoMetodoPago tipoMetodoPago, MetodoCobroArtistaCrearDTO dto) {
        switch (tipoMetodoPago) {
            case PAYPAL:
                if (dto.getEmailPaypal() == null) {
                    throw new InvalidPaymentMethodException("PayPal requiere: emailPaypal");
                }
                break;
            case BIZUM:
                if (dto.getTelefonoBizum() == null) {
                    throw new InvalidPaymentMethodException("Bizum requiere: telefonoBizum");
                }
                break;
            case TRANSFERENCIA:
                if (dto.getIban() == null) {
                    throw new InvalidPaymentMethodException("Transferencia requiere: iban");
                }
                break;
            default:
                throw new InvalidPaymentMethodException("Método de cobro no soportado: " + tipoMetodoPago);
        }
    }

    /**
     * Asigna campos específicos según el tipo de método.
     *
     * @param metodoCobro entidad destino
     * @param tipo tipo de método de cobro
     * @param dto datos de origen
     */
    private void asignarCamposEspecificos(MetodoCobroArtista metodoCobro, TipoMetodoPago tipo, MetodoCobroArtistaCrearDTO dto) {
        switch (tipo) {
            case PAYPAL:
                metodoCobro.setEmailPaypal(dto.getEmailPaypal());
                break;
            case BIZUM:
                metodoCobro.setTelefonoBizum(dto.getTelefonoBizum());
                break;
            case TRANSFERENCIA:
                metodoCobro.setIban(dto.getIban());
                break;
        }
    }

    /**
     * Actualiza campos específicos según el tipo de método.
     *
     * <p>Solo actualiza campos no nulos del DTO.</p>
     *
     * @param metodoCobro entidad a actualizar
     * @param dto datos de actualización
     */
    private void actualizarCamposEspecificos(MetodoCobroArtista metodoCobro, MetodoCobroArtistaEditarDTO dto) {
        switch (metodoCobro.getTipoCobro()) {
            case PAYPAL:
                if (dto.getEmailPaypal() != null) metodoCobro.setEmailPaypal(dto.getEmailPaypal());
                break;
            case BIZUM:
                if (dto.getTelefonoBizum() != null) metodoCobro.setTelefonoBizum(dto.getTelefonoBizum());
                break;
            case TRANSFERENCIA:
                if (dto.getIban() != null) metodoCobro.setIban(dto.getIban());
                break;
        }
    }

    /**
     * Convierte una entidad a su representación DTO.
     *
     * @param metodoCobro entidad a convertir
     * @return DTO con la información del método de cobro
     */
    private MetodoCobroArtistaDTO convertirADTO(MetodoCobroArtista metodoCobro) {
        return MetodoCobroArtistaDTO.builder()
                .idMetodoCobro(metodoCobro.getIdMetodoCobroArtista())
                .tipo(metodoCobro.getTipoCobro().name().toLowerCase())
                .propietario(metodoCobro.getPropietario())
                .direccion(metodoCobro.getDireccion())
                .pais(metodoCobro.getPais())
                .provincia(metodoCobro.getProvincia())
                .codigoPostal(metodoCobro.getCodigoPostal())
                .emailPaypal(metodoCobro.getEmailPaypal())
                .telefonoBizum(metodoCobro.getTelefonoBizum())
                .iban(metodoCobro.getIban())
                .fechaCreacion(metodoCobro.getFechaCreacion().format(FORMATTER))
                .fechaActualizacion(metodoCobro.getFechaActualizacion().format(FORMATTER))
                .build();
    }
}