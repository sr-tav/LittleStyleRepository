package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionClienteResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionGuardadaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.DireccionCliente;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Gestiona las direcciones de envío guardadas del cliente autenticado. */
@Service
@RequiredArgsConstructor
public class DireccionClienteService {

    private final DireccionClienteRepository direccionRepository;

    /** Máximo de direcciones por cliente; configurable para abrir la regla a volúmenes mayores. */
    @Value("${app.direcciones.max:5}")
    private int maximoDirecciones;

    /** Direcciones guardadas del cliente autenticado. */
    @Transactional(readOnly = true)
    public List<DireccionClienteResponse> listar() {
        return direccionRepository.findAllByClienteIdOrderByIdAsc(clienteActualId()).stream()
                .map(this::respuestaConId)
                .toList();
    }

    /** Primera dirección guardada, para mantener el endpoint anterior compatible. */
    @Transactional(readOnly = true)
    public DireccionGuardadaResponse obtener() {
        return direccionRepository.findAllByClienteIdOrderByIdAsc(clienteActualId()).stream().findFirst()
                .map(this::respuesta)
                .orElseGet(() -> new DireccionGuardadaResponse(false, null, false));
    }

    /**
     * Crea una dirección nueva para el cliente autenticado respetando el límite configurable.
     * La primera dirección queda predeterminada; las demás no, sin importar cuántas haya.
     * @param request Datos estructurados de la dirección.
     * @return La dirección guardada con su identificador.
     * @throws BusinessException 409 cuando el cliente ya alcanzó el máximo de direcciones.
     */
    @Transactional
    public DireccionClienteResponse crear(DireccionEnvioRequest request) {
        Long clienteId = clienteActualId();
        long existentes = direccionRepository.countByClienteId(clienteId);
        if (existentes >= maximoDirecciones) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Solo puedes guardar hasta " + maximoDirecciones + " direcciones de envío", "direcciones");
        }
        DireccionCliente direccion = DireccionCliente.builder()
                .cliente(Usuario.builder().id(clienteId).build())
                .predeterminada(existentes == 0)
                .build();
        aplicar(direccion, request);
        return respuestaConId(direccionRepository.save(direccion));
    }

    /** Actualiza una dirección únicamente si pertenece al cliente autenticado. */
    @Transactional
    public DireccionClienteResponse actualizar(Long id, DireccionEnvioRequest request) {
        DireccionCliente direccion = direccionRepository.findByIdAndClienteId(id, clienteActualId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));
        aplicar(direccion, request);
        return respuestaConId(direccionRepository.save(direccion));
    }

    /**
     * Elimina una dirección solo si pertenece al cliente autenticado. Si era la predeterminada,
     * la marca pasa a la más antigua de las restantes para conservar una por cliente.
     * @param id Identificador de la dirección propia.
     * @throws BusinessException 404 cuando la dirección no existe o es de otro cliente.
     */
    @Transactional
    public void eliminar(Long id) {
        Long clienteId = clienteActualId();
        DireccionCliente direccion = direccionRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));

        List<DireccionCliente> restantes = direccionRepository.findAllByClienteIdOrderByIdAsc(clienteId).stream()
                .filter(restante -> !restante.getId().equals(direccion.getId()))
                .toList();

        DireccionCliente predeterminada = restantes.stream()
                .filter(restante -> Boolean.TRUE.equals(restante.getPredeterminada()))
                .findFirst()
                .orElse(restantes.isEmpty() ? null : restantes.get(0));

        for (DireccionCliente restante : restantes) {
            restante.setPredeterminada(
                    predeterminada != null && restante.getId().equals(predeterminada.getId()));
        }

        direccionRepository.saveAll(restantes);
        direccionRepository.delete(direccion);
    }

    /** Marca una dirección propia como predeterminada y desmarca las demás. */
    @Transactional
    public DireccionClienteResponse marcarPredeterminada(Long id) {
        Long clienteId = clienteActualId();
        DireccionCliente seleccionada = direccionRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Dirección no encontrada"));

        List<DireccionCliente> direcciones =
                direccionRepository.findAllByClienteIdOrderByIdAsc(clienteId);
        for (DireccionCliente direccion : direcciones) {
            direccion.setPredeterminada(direccion.getId().equals(seleccionada.getId()));
        }
        direccionRepository.saveAll(direcciones);

        return respuestaConId(seleccionada);
    }

    /** Crea o reemplaza la primera dirección para mantener el endpoint anterior compatible. */
    @Transactional
    public DireccionGuardadaResponse guardar(DireccionEnvioRequest request) {
        Long clienteId = clienteActualId();
        List<DireccionCliente> direcciones =
                direccionRepository.findAllByClienteIdOrderByIdAsc(clienteId);

        DireccionCliente direccion = direcciones.stream()
                .findFirst()
                .orElseGet(() -> DireccionCliente.builder()
                        .cliente(Usuario.builder().id(clienteId).build())
                        .predeterminada(true)
                        .build());

        if (direcciones.stream().noneMatch(d -> Boolean.TRUE.equals(d.getPredeterminada()))) {
            direccion.setPredeterminada(true);
        }

        aplicar(direccion, request);
        return respuesta(direccionRepository.save(direccion));
    }

    private void aplicar(DireccionCliente direccion, DireccionEnvioRequest request) {
        direccion.setDestinatario(request.destinatario().trim());
        direccion.setDireccion(request.direccion().trim());
        direccion.setTipoVia(request.tipoVia().trim());
        direccion.setNumeroVia(request.numeroVia().trim());
        direccion.setNumeroCruce(request.numeroCruce().trim());
        direccion.setPlaca(request.placa().trim());
        direccion.setComplemento(normalizarOpcional(request.complemento()));
        direccion.setCodigoPostal(request.codigoPostal().trim());
        direccion.setDepartamento(request.departamento().trim());
        direccion.setCiudad(request.municipio().trim());
        direccion.setTelefono(request.telefono());
    }

    private DireccionClienteResponse respuestaConId(DireccionCliente direccion) {
        return new DireccionClienteResponse(
                direccion.getId(),
                respuesta(direccion).direccion(),
                Boolean.TRUE.equals(direccion.getPredeterminada()));
    }

    /** Convierte la entidad en la respuesta de dirección guardada. */
    private DireccionGuardadaResponse respuesta(DireccionCliente direccion) {
        return new DireccionGuardadaResponse(
                true,
                new DireccionEnvioRequest(
                        direccion.getDestinatario(),
                        direccion.getDireccion(),
                        direccion.getTipoVia(),
                        direccion.getNumeroVia(),
                        direccion.getNumeroCruce(),
                        direccion.getPlaca(),
                        direccion.getComplemento(),
                        direccion.getCodigoPostal(),
                        direccion.getDepartamento(),
                        direccion.getCiudad(),
                        direccion.getTelefono()),
                Boolean.TRUE.equals(direccion.getPredeterminada()));
    }

    /** Id del cliente autenticado; 401 si no hay sesión. */
    private Long clienteActualId() {
        return SecurityUtils.usuarioActual()
                .map(usuario -> usuario.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    /** Texto sin espacios sobrantes, o {@code null} si viene vacío. */
    private static String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
