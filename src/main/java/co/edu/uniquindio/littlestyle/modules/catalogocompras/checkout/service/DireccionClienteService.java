package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.service;

import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionEnvioRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.dto.DireccionGuardadaResponse;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.DireccionCliente;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.DireccionClienteRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DireccionClienteService {

    private final DireccionClienteRepository direccionRepository;

    @Transactional(readOnly = true)
    public DireccionGuardadaResponse obtener() {
        return direccionRepository.findByClienteId(clienteActualId())
                .map(this::respuesta)
                .orElseGet(() -> new DireccionGuardadaResponse(false, null));
    }

    @Transactional
    public DireccionGuardadaResponse guardar(DireccionEnvioRequest request) {
        Long clienteId = clienteActualId();
        DireccionCliente direccion = direccionRepository.findByClienteId(clienteId)
                .orElseGet(() -> DireccionCliente.builder()
                        .cliente(Usuario.builder().id(clienteId).build())
                        .build());
        direccion.setDestinatario(request.destinatario().trim());
        direccion.setDireccion(request.direccion().trim());
        direccion.setComplemento(normalizarOpcional(request.complemento()));
        direccion.setCodigoPostal(request.codigoPostal().trim());
        direccion.setDepartamento(request.departamento().trim());
        direccion.setCiudad(request.municipio().trim());
        direccion.setTelefono(request.telefono());
        return respuesta(direccionRepository.save(direccion));
    }

    private DireccionGuardadaResponse respuesta(DireccionCliente direccion) {
        return new DireccionGuardadaResponse(true, new DireccionEnvioRequest(
                direccion.getDestinatario(),
                direccion.getDireccion(),
                direccion.getComplemento(),
                direccion.getCodigoPostal(),
                direccion.getDepartamento(),
                direccion.getCiudad(),
                direccion.getTelefono()));
    }

    private Long clienteActualId() {
        return SecurityUtils.usuarioActual()
                .map(usuario -> usuario.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    private static String normalizarOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
