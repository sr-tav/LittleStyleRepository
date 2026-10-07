package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.CrearPerfilConMedicionRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.MedicionRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.MedicionResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilRequest;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.dto.PerfilResponse;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.AlergiaTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.PerfilInfantil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.MedicionCrecimientoRepository;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository.PerfilInfantilRepository;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import co.edu.uniquindio.littlestyle.shared.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Period;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PerfilInfantilService {

    private final PerfilInfantilRepository perfilRepository;
    private final MedicionCrecimientoRepository medicionRepository;
    private final UsuarioRepository usuarioRepository;
    private final ValidadorMedidas validadorMedidas;
    private final CalculadoraCompletitud calculadoraCompletitud;

    @Value("${app.perfiles.max:10}")
    private int maximoPerfiles;

    @Transactional(readOnly = true)
    public List<PerfilResponse> listar() {
        Long clienteId = clienteActual().id();
        return perfilRepository.findAllByClienteIdOrderByFechaActualizacionDesc(clienteId)
                .stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public PerfilResponse obtener(Long id) {
        return respuesta(obtenerPropio(id, clienteActual().id()));
    }

    @Transactional
    public PerfilResponse crear(PerfilRequest request) {
        bloquearClienteActual();
        return respuesta(crearPerfil(request));
    }

    @Transactional
    public PerfilResponse crearConMedicionInicial(CrearPerfilConMedicionRequest request) {
        bloquearClienteActual();
        PerfilInfantil perfil = crearPerfil(request.perfil());
        MedicionRequest medicion = request.medicionInicial();
        validadorMedidas.validar(perfil.getFechaNacimiento(), medicion.fechaMedicion(),
                medicion.estaturaCm(), medicion.pesoKg());
        perfil.setFechaActualizacion(LocalDateTime.now());
        perfilRepository.save(perfil);
        MedicionCrecimiento entidad = MedicionCrecimiento.builder()
                .perfil(perfil)
                .fechaMedicion(medicion.fechaMedicion())
                .estaturaCm(medicion.estaturaCm())
                .pesoKg(medicion.pesoKg())
                .build();
        medicionRepository.save(entidad);
        perfil.getMediciones().add(entidad);
        return respuesta(perfil);
    }

    private PerfilInfantil crearPerfil(PerfilRequest request) {
        Long clienteId = clienteActual().id();
        if (perfilRepository.countByClienteId(clienteId) >= maximoPerfiles) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Un cliente puede tener máximo " + maximoPerfiles + " perfiles infantiles", "perfiles");
        }
        validarFechaNacimiento(request.fechaNacimiento());
        validarPerfilNoDuplicado(request.nombre(), request.fechaNacimiento(), clienteId, null);
        validarAlergias(request);
        Preferencias preferencias = validarPreferencias(request);
        PerfilInfantil perfil = aplicar(request, new PerfilInfantil());
        aplicarPreferencias(preferencias, perfil);
        perfil.setCliente(Usuario.builder().id(clienteId).build());
        return perfilRepository.save(perfil);
    }

    @Transactional
    public PerfilResponse actualizar(Long id, PerfilRequest request) {
        Long clienteId = bloquearClienteActual();
        PerfilInfantil perfil = obtenerPropio(id, clienteId);
        validarFechaNacimiento(request.fechaNacimiento());
        validarPerfilNoDuplicado(request.nombre(), request.fechaNacimiento(), clienteId, id);
        validarAlergias(request);
        Preferencias preferencias = validarPreferencias(request);
        perfil.getMediciones().forEach(medicion -> validadorMedidas.validar(
                request.fechaNacimiento(), medicion.getFechaMedicion(),
                medicion.getEstaturaCm(), medicion.getPesoKg()));
        aplicar(request, perfil);
        aplicarPreferencias(preferencias, perfil);
        return respuesta(perfilRepository.save(perfil));
    }

    @Transactional
    public void eliminar(Long id) {
        bloquearClienteActual();
        perfilRepository.delete(obtenerPropio(id, clienteActual().id()));
    }

    @Transactional
    public void eliminarTodosDeCliente(Long clienteId) {
        perfilRepository.deleteAll(perfilRepository.findAllByClienteIdOrderByFechaActualizacionDesc(clienteId));
    }

    @Transactional
    public MedicionResponse crearMedicion(Long perfilId, MedicionRequest request) {
        PerfilInfantil perfil = obtenerPropioForUpdate(perfilId, clienteActual().id());
        validadorMedidas.validar(perfil.getFechaNacimiento(), request.fechaMedicion(),
                request.estaturaCm(), request.pesoKg());
        MedicionCrecimiento ultimaMedicion = ultimaMedicion(perfil);
        if (ultimaMedicion != null
                && request.fechaMedicion().isBefore(ultimaMedicion.getFechaMedicion())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La nueva medición no puede ser anterior a la última registrada; corrige la medición existente",
                    "fechaMedicion");
        }
        if (ultimaMedicion != null
                && request.estaturaCm().compareTo(ultimaMedicion.getEstaturaCm()) < 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La estatura no puede ser menor que la última registrada; usa corregir para ajustar una medición existente",
                    "estaturaCm");
        }
        perfil.setFechaActualizacion(LocalDateTime.now());
        perfilRepository.save(perfil);
        MedicionCrecimiento medicion = MedicionCrecimiento.builder()
                .perfil(perfil)
                .fechaMedicion(request.fechaMedicion())
                .estaturaCm(request.estaturaCm())
                .pesoKg(request.pesoKg())
                .build();
        return MedicionResponse.from(medicionRepository.save(medicion));
    }

    @Transactional
    public MedicionResponse actualizarMedicion(Long perfilId, Long medicionId, MedicionRequest request) {
        Long clienteId = clienteActual().id();
        PerfilInfantil perfil = obtenerPropioForUpdate(perfilId, clienteId);
        MedicionCrecimiento medicion = medicionRepository
                .findByIdAndPerfilIdAndPerfilClienteId(medicionId, perfilId, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Medición no encontrada"));
        validadorMedidas.validar(perfil.getFechaNacimiento(), request.fechaMedicion(),
                request.estaturaCm(), request.pesoKg());
        medicion.setFechaMedicion(request.fechaMedicion());
        medicion.setEstaturaCm(request.estaturaCm());
        medicion.setPesoKg(request.pesoKg());
        perfil.setFechaActualizacion(LocalDateTime.now());
        perfilRepository.save(perfil);
        return MedicionResponse.from(medicionRepository.save(medicion));
    }

    @Transactional(readOnly = true)
    public List<MedicionResponse> listarMediciones(Long perfilId) {
        Long clienteId = clienteActual().id();
        obtenerPropio(perfilId, clienteId);
        return medicionRepository
                .findAllByPerfilIdAndPerfilClienteId(perfilId, clienteId)
                .stream()
                .sorted(Comparator.comparing(MedicionCrecimiento::getFechaMedicion).reversed()
                        .thenComparing(MedicionCrecimiento::getId, Comparator.reverseOrder()))
                .map(MedicionResponse::from).toList();
    }

    private PerfilInfantil aplicar(PerfilRequest request, PerfilInfantil perfil) {
        perfil.setNombre(request.nombre().trim());
        perfil.setFechaNacimiento(request.fechaNacimiento());
        perfil.setContextura(request.contextura());
        perfil.setHolgura(request.holgura());
        perfil.setAlergias(Set.copyOf(request.alergias()));
        perfil.setSinAlergias(request.sinAlergias());
        perfil.setOtraAlergia(request.otraAlergia() == null ? null : request.otraAlergia().trim());
        return perfil;
    }

    private void validarFechaNacimiento(LocalDate fechaNacimiento) {
        if (fechaNacimiento.isAfter(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha de nacimiento no puede ser futura", "fechaNacimiento");
        }
        if (fechaNacimiento.isEqual(LocalDate.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La fecha de nacimiento debe ser anterior a hoy", "fechaNacimiento");
        }
        if (Period.between(fechaNacimiento, LocalDate.now()).getYears() >= 18) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El perfil debe corresponder a una persona menor de 18 años", "fechaNacimiento");
        }
    }

    private void validarAlergias(PerfilRequest request) {
        boolean contieneOtra = request.alergias().contains(AlergiaTextil.OTRA);
        if (request.sinAlergias()
                && (!request.alergias().isEmpty() || request.otraAlergia() != null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Si indica que no tiene alergias, no puede registrar alergias ni otra alergia",
                    "alergias");
        }
        if (!request.sinAlergias() && request.alergias().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Declare las alergias o indique que no tiene alergias", "alergias");
        }
        if (contieneOtra && (request.otraAlergia() == null || request.otraAlergia().trim().isEmpty())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Describa la otra alergia", "otraAlergia");
        }
        if (!contieneOtra && request.otraAlergia() != null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La descripción solo aplica cuando selecciona Otra alergia", "otraAlergia");
        }
        if (request.otraAlergia() != null && request.otraAlergia().length() > 60) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "La descripción de otra alergia no puede superar 60 caracteres", "otraAlergia");
        }
    }

    private Preferencias validarPreferencias(PerfilRequest request) {
        Set<ColorPreferido> colores = request.coloresPreferidos().isEmpty()
                ? EnumSet.noneOf(ColorPreferido.class)
                : EnumSet.copyOf(request.coloresPreferidos());
        Set<Estampado> estampados = request.estampadosPreferidos().isEmpty()
                ? EnumSet.noneOf(Estampado.class)
                : EnumSet.copyOf(request.estampadosPreferidos());
        String otroColor = normalizarPreferencia(request.otroColor(), "otroColor");
        String otroEstampado = normalizarPreferencia(request.otroEstampado(), "otroEstampado");

        ColorPreferido colorCoincidente = enumCoincidente(otroColor, ColorPreferido.class);
        if (colorCoincidente != null) {
            colores.add(colorCoincidente);
            otroColor = null;
        }
        Estampado estampadoCoincidente = enumCoincidente(otroEstampado, Estampado.class);
        if (estampadoCoincidente != null) {
            estampados.add(estampadoCoincidente);
            otroEstampado = null;
        }
        if (colores.size() + (otroColor == null ? 0 : 1) > 10) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Puede registrar máximo 10 colores preferidos", "coloresPreferidos");
        }
        if (estampados.size() + (otroEstampado == null ? 0 : 1) > 10) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "Puede registrar máximo 10 estampados preferidos", "estampadosPreferidos");
        }
        return new Preferencias(colores, estampados, otroColor, otroEstampado);
    }

    private String normalizarPreferencia(String valor, String campo) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim();
        if (normalizado.length() < 2 || normalizado.length() > 30
                || !normalizado.matches("(?U)[\\p{L} ]+")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "El valor debe tener de 2 a 30 caracteres y contener solo letras y espacios", campo);
        }
        int primerCaracter = normalizado.codePointAt(0);
        int longitudPrimerCaracter = Character.charCount(primerCaracter);
        return normalizado.substring(0, longitudPrimerCaracter).toUpperCase(Locale.ROOT)
                + normalizado.substring(longitudPrimerCaracter);
    }

    private <E extends Enum<E>> E enumCoincidente(String texto, Class<E> tipo) {
        if (texto == null) {
            return null;
        }
        String clave = simplificar(texto);
        for (E opcion : tipo.getEnumConstants()) {
            if (simplificar(opcion.name()).equals(clave)) {
                return opcion;
            }
        }
        return null;
    }

    private String simplificar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace("_", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);
    }

    private void aplicarPreferencias(Preferencias preferencias, PerfilInfantil perfil) {
        perfil.setColoresPreferidos(preferencias.colores());
        perfil.setEstampadosPreferidos(preferencias.estampados());
        perfil.setOtroColor(preferencias.otroColor());
        perfil.setOtroEstampado(preferencias.otroEstampado());
    }

    private PerfilInfantil obtenerPropio(Long id, Long clienteId) {
        return perfilRepository.findByIdAndClienteId(id, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Perfil infantil no encontrado"));
    }

    private PerfilInfantil obtenerPropioForUpdate(Long id, Long clienteId) {
        return perfilRepository.findByIdAndClienteIdForUpdate(id, clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND,
                        "Perfil infantil no encontrado"));
    }

    private void validarPerfilNoDuplicado(String nombre, LocalDate fechaNacimiento,
                                          Long clienteId, Long perfilExcluidoId) {
        String nombreNormalizado = normalizarNombre(nombre);
        boolean duplicado = perfilRepository.findAllByClienteIdOrderByFechaActualizacionDesc(clienteId)
                .stream()
                .anyMatch(perfil -> !perfil.getId().equals(perfilExcluidoId)
                        && perfil.getFechaNacimiento().equals(fechaNacimiento)
                        && normalizarNombre(perfil.getNombre()).equals(nombreNormalizado));
        if (duplicado) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "Ya existe un perfil con ese nombre y fecha de nacimiento", "nombre");
        }
    }

    private String normalizarNombre(String nombre) {
        return Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private Long bloquearClienteActual() {
        Long clienteId = clienteActual().id();
        Usuario cliente = usuarioRepository.findByIdForUpdate(clienteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
        if (cliente.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "La cuenta no está activa");
        }
        return clienteId;
    }

    private PerfilResponse respuesta(PerfilInfantil perfil) {
        MedicionCrecimiento ultima = ultimaMedicion(perfil);
        Period edad = Period.between(perfil.getFechaNacimiento(), LocalDate.now());
        int meses = edad.getMonths();
        return new PerfilResponse(
                perfil.getId(), perfil.getNombre(), perfil.getFechaNacimiento(),
                edad.getYears(), meses, perfil.getContextura(), perfil.getHolgura(),
                Set.copyOf(perfil.getAlergias()), perfil.getOtraAlergia(), perfil.isSinAlergias(),
                Set.copyOf(perfil.getColoresPreferidos()), Set.copyOf(perfil.getEstampadosPreferidos()),
                perfil.getOtroColor(), perfil.getOtroEstampado(),
                perfil.getFechaCreacion(), perfil.getFechaActualizacion(),
                ultima == null ? null : MedicionResponse.from(ultima),
                calculadoraCompletitud.calcular(perfil, ultima),
                calculadoraCompletitud.camposPendientes(perfil, ultima));
    }

    private MedicionCrecimiento ultimaMedicion(PerfilInfantil perfil) {
        return perfil.getMediciones().stream()
                .max(Comparator.comparing(MedicionCrecimiento::getFechaMedicion)
                        .thenComparing(MedicionCrecimiento::getId,
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    private AuthenticatedUser clienteActual() {
        AuthenticatedUser usuario = SecurityUtils.usuarioActual().orElseThrow(
                () -> new BusinessException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
        return usuario;
    }

    private record Preferencias(Set<ColorPreferido> colores, Set<Estampado> estampados,
                                String otroColor, String otroEstampado) {
    }
}
