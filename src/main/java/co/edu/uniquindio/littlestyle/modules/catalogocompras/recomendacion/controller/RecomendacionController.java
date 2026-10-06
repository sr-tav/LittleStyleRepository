package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.controller;


import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.strategy.*;
import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cliente/recomendaciones")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class RecomendacionController {
    private final PerfilBiometricoPort perfiles;
    private final MotorRecomendacionService motor;
    private final CatalogoRecomendacionPort catalogo;
    private final EstrategiaTalla estrategia;
    private final FiltroAlergia filtro;

    @GetMapping
    public ResponseEntity<List<RecomendacionItem>> listar(@RequestParam Long perfilId) {
        long ini = System.nanoTime();
        PerfilBiometrico pb = perfiles.obtenerActivos(perfilId);
        List<RecomendacionItem> res = motor.recomendar(pb.bio(), pb.alergias());
        long ms = (System.nanoTime() - ini) / 1_000_000;
        return ResponseEntity.ok().header("X-Recomendacion-Ms", String.valueOf(ms)).body(res);
    }

    @GetMapping("/prenda/{prendaId}")
    public ResponseEntity<RecomendacionItem> porPrenda(@PathVariable Long prendaId, @RequestParam Long perfilId) {
        PerfilBiometrico pb = perfiles.obtenerActivos(perfilId);
        var prenda = catalogo.buscarPrendaPublicada(prendaId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Prenda no encontrada"));
        var talla = estrategia.sugerir(pb.bio(), prenda.tablaTallas())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Sin talla compatible"));
        var eval = filtro.evaluar(pb.alergias(), prenda);
        if (eval.excluye()) throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Prenda excluida por alergia: " + String.join(", ", eval.motivos()));
        int puntaje = Math.max(0, 100 - distancia(pb.bio(), talla)
                - (eval.nivel() == NivelRiesgo.ADVERTENCIA ? 30 : 0)
                + (prenda.tieneStock(talla.talla()) ? 5 : 0));
        return ResponseEntity.ok(new RecomendacionItem(prenda.id(), prenda.nombre(),
                talla.talla(), puntaje, eval.nivel(), eval.motivos(), prenda.tieneStock(talla.talla())));
    }

    private int distancia(co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.DatosBiometricos bio,
                          co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.RangoTalla r) {
        java.math.BigDecimal ce = r.estaturaMinCm().add(r.estaturaMaxCm())
                .divide(java.math.BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal cp = r.pesoMinKg().add(r.pesoMaxKg())
                .divide(java.math.BigDecimal.valueOf(2), 4, java.math.RoundingMode.HALF_UP);
        double de = ce.subtract(bio.estaturaCm()).doubleValue();
        double dp = cp.subtract(bio.pesoKg()).doubleValue();
        return (int) Math.sqrt(de * de + dp * dp);
    }
}
