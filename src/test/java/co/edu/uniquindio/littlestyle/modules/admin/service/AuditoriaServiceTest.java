package co.edu.uniquindio.littlestyle.modules.admin.service;

import co.edu.uniquindio.littlestyle.modules.admin.model.AccionAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.RegistroAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.ResultadoAuditoria;
import co.edu.uniquindio.littlestyle.modules.admin.model.TipoEntidad;
import co.edu.uniquindio.littlestyle.modules.admin.repository.RegistroAuditoriaRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditoriaServiceTest {

    private final RegistroAuditoriaRepository registroRepository =
            mock(RegistroAuditoriaRepository.class);
    private final AuditoriaService auditoriaService = new AuditoriaService(registroRepository);

    @Test
    void calculaHashSha256Determinista() {
        RegistroAuditoria registro = registro(1L, "ACTIVO", null, AuditoriaService.GENESIS);

        String hash = auditoriaService.calcularHash(registro, AuditoriaService.GENESIS);

        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hash).isEqualTo(auditoriaService.calcularHash(registro, AuditoriaService.GENESIS));
    }

    @Test
    void camposConSeparadoresNoProducenColisiones() {
        RegistroAuditoria primero = registro(1L, "ACTIVO|x", "BLOQUEADO",
                AuditoriaService.GENESIS, AuditoriaService.GENESIS);
        RegistroAuditoria segundo = registro(1L, "ACTIVO", "x|BLOQUEADO",
                AuditoriaService.GENESIS, AuditoriaService.GENESIS);

        assertThat(auditoriaService.calcularHash(primero, AuditoriaService.GENESIS))
                .isNotEqualTo(auditoriaService.calcularHash(segundo, AuditoriaService.GENESIS));
    }

    @Test
    void verificaCadenaValida() {
        RegistroAuditoria primero = registro(1L, "ACTIVO", null, AuditoriaService.GENESIS);
        String hashPrimero = auditoriaService.calcularHash(primero, AuditoriaService.GENESIS);
        primero = primero.conHash(hashPrimero);
        RegistroAuditoria segundo = registro(2L, "SUSPENDIDO", hashPrimero, hashPrimero);
        segundo = segundo.conHash(auditoriaService.calcularHash(segundo, hashPrimero));
        when(registroRepository.findAllByOrderByIdAsc()).thenReturn(List.of(primero, segundo));

        AuditoriaService.ResultadoIntegridad resultado = auditoriaService.verificarIntegridad();

        assertThat(resultado.integra()).isTrue();
        assertThat(resultado.totalRegistros()).isEqualTo(2);
        assertThat(resultado.primerInconsistenteId()).isNull();
    }

    @Test
    void detectaManipulacionDeUnRegistro() {
        RegistroAuditoria original = registro(1L, "ACTIVO", null, AuditoriaService.GENESIS);
        String hashOriginal = auditoriaService.calcularHash(original, AuditoriaService.GENESIS);
        RegistroAuditoria manipulado = RegistroAuditoria.builder()
                .id(original.getId())
                .fechaHora(original.getFechaHora())
                .adminId(original.getAdminId())
                .adminEmail(original.getAdminEmail())
                .rol(original.getRol())
                .accion(original.getAccion())
                .tipoEntidad(original.getTipoEntidad())
                .entidadId(original.getEntidadId())
                .valorAnterior(original.getValorAnterior())
                .valorNuevo("BLOQUEADO")
                .motivo(original.getMotivo())
                .resultado(original.getResultado())
                .hash(hashOriginal)
                .hashAnterior(AuditoriaService.GENESIS)
                .build();
        when(registroRepository.findAllByOrderByIdAsc()).thenReturn(List.of(manipulado));

        AuditoriaService.ResultadoIntegridad resultado = auditoriaService.verificarIntegridad();

        assertThat(resultado.integra()).isFalse();
        assertThat(resultado.primerInconsistenteId()).isEqualTo(1L);
    }

    private static RegistroAuditoria registro(Long id, String valorNuevo, String hashAnterior,
                                              String hashParaCalcular) {
        return registro(id, "ACTIVO", valorNuevo, hashAnterior, hashParaCalcular);
    }

    private static RegistroAuditoria registro(Long id, String valorAnterior, String valorNuevo,
                                              String hashAnterior, String hashParaCalcular) {
        return RegistroAuditoria.builder()
                .id(id)
                .fechaHora(Instant.parse("2026-10-26T12:00:00Z"))
                .adminId(9L)
                .adminEmail("admin@littlestyle.co")
                .rol("ADMINISTRADOR")
                .accion(AccionAuditoria.SUSPENDER_USUARIO)
                .tipoEntidad(TipoEntidad.USUARIO)
                .entidadId("42")
                .valorAnterior(valorAnterior)
                .valorNuevo(valorNuevo)
                .motivo("Incumplimiento de las políticas de la plataforma")
                .resultado(ResultadoAuditoria.EXITO)
                .hashAnterior(hashAnterior == null ? AuditoriaService.GENESIS : hashAnterior)
                .hash(hashParaCalcular)
                .build();
    }
}
