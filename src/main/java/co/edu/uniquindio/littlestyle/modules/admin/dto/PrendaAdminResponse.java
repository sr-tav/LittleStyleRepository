package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PrendaAdminResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        EstadoPrenda estado,
        EstadoModeracion estadoModeracion,
        String motivoModeracion,
        Long vendedorId,
        String vendedorEmail,
        String vendedorTienda,
        LocalDateTime fechaActualizacion) {

    public static PrendaAdminResponse from(Prenda prenda) {
        return new PrendaAdminResponse(
                prenda.getId(), prenda.getNombre(), prenda.getPrecio(), prenda.getEstado(),
                prenda.getEstadoModeracion(), prenda.getMotivoModeracion(),
                prenda.getVendedor().getId(), prenda.getVendedor().getEmail(),
                prenda.getVendedor().getNombreTienda(), prenda.getFechaActualizacion());
    }
}
