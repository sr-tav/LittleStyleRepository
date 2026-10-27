package co.edu.uniquindio.littlestyle.modules.admin.dto;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ComponenteComposicion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PrendaAdminDetalleResponse(
        Long id,
        String nombre,
        CategoriaPrenda categoria,
        GeneroPrenda genero,
        String descripcion,
        String instruccionesCuidado,
        String marca,
        BigDecimal precio,
        EstadoPrenda estado,
        EstadoModeracion estadoModeracion,
        String motivoModeracion,
        Estampado estampado,
        boolean tintesSinteticos,
        boolean brochesMetalicos,
        boolean contieneNiquel,
        boolean tratamientoFormaldehido,
        Long vendedorId,
        String vendedorEmail,
        String vendedorTienda,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        List<ComponenteResponse> composicion,
        List<TallaResponse> tallas,
        List<String> imagenes) {

    public record ComponenteResponse(MaterialTextil material, int porcentaje) {
    }

    public record TallaResponse(String talla, int stock, BigDecimal estaturaMinCm, BigDecimal estaturaMaxCm,
                                BigDecimal pesoMinKg, BigDecimal pesoMaxKg, Integer edadMinMeses,
                                Integer edadMaxMeses, Integer stockMinimo) {
    }

    public static PrendaAdminDetalleResponse from(Prenda prenda) {
        List<ComponenteResponse> composicion = prenda.getComposicion().stream()
                .map(c -> new ComponenteResponse(c.getMaterial(), c.getPorcentaje()))
                .toList();
        composicion = prenda.getComposicion().stream()
                .map(c -> new ComponenteResponse(c.getMaterial(), c.getPorcentaje()))
                .toList();
        List<TallaResponse> tallas = prenda.getTallas().stream()
                .map(t -> new TallaResponse(t.getTalla(), t.getStock(), t.getEstaturaMinCm(),
                        t.getEstaturaMaxCm(), t.getPesoMinKg(), t.getPesoMaxKg(),
                        t.getEdadMinMeses(), t.getEdadMaxMeses(), t.getStockMinimo()))
                .toList();
        List<String> imagenes = prenda.getImagenes().stream()
                .map(i -> i.getUrl())
                .toList();
        return new PrendaAdminDetalleResponse(
                prenda.getId(), prenda.getNombre(), prenda.getCategoria(), prenda.getGenero(),
                prenda.getDescripcion(), prenda.getInstruccionesCuidado(), prenda.getMarca(),
                prenda.getPrecio(), prenda.getEstado(), prenda.getEstadoModeracion(),
                prenda.getMotivoModeracion(), prenda.getEstampado(), prenda.isTintesSinteticos(),
                prenda.isBrochesMetalicos(), prenda.isContieneNiquel(),
                prenda.isTratamientoFormaldehido(), prenda.getVendedor().getId(),
                prenda.getVendedor().getEmail(), prenda.getVendedor().getNombreTienda(),
                prenda.getFechaCreacion(), prenda.getFechaActualizacion(),
                composicion, tallas, imagenes);
    }
}