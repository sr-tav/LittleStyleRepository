package co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.controller;


import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.port.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.service.*;
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
        return ResponseEntity.ok(motor.recomendarParaPrenda(pb.bio(), pb.alergias(), prendaId));
    }

}
