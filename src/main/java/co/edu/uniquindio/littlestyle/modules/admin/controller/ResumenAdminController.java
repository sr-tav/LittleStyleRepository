package co.edu.uniquindio.littlestyle.modules.admin.controller;

import co.edu.uniquindio.littlestyle.modules.admin.dto.ResumenAdminResponse;
import co.edu.uniquindio.littlestyle.modules.admin.service.ResumenAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Indicadores operativos del panel administrativo (US-14, RNF-17). */
@RestController
@RequestMapping("/api/admin/resumen")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ResumenAdminController {

    private final ResumenAdminService resumenAdminService;

    /** Devuelve conteos de cuentas, moderacion e incidencias junto con la tasa de resolucion en 24 horas. */
    @GetMapping
    public ResumenAdminResponse obtener() {
        return resumenAdminService.resumen();
    }
}
