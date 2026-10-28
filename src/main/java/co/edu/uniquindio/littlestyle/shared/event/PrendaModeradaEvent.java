package co.edu.uniquindio.littlestyle.shared.event;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;

public record PrendaModeradaEvent(Long prendaId, Long vendedorId, EstadoModeracion resultado,
                                  String motivo) {
}
