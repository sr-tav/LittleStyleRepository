package co.edu.uniquindio.littlestyle.modules.catalogocompras;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.ComponenteRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.PrendaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.TallaRequest;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.ComponenteComposicion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.recomendacion.model.MaterialTextil;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Datos de prueba compartidos por las pruebas del catálogo y el inventario. */
public final class CatalogoFixtures {

    public static final Long VENDEDOR_ID = 21L;

    private CatalogoFixtures() {
    }

    public static void autenticarVendedor() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(VENDEDOR_ID, "vendedor@test.com", "Vendedora", Rol.VENDEDOR), null, List.of()));
    }

    public static TallaRequest tallaRequest(String talla, int estMin, int estMax, int pesoMin, int pesoMax, Integer stock) {
        return new TallaRequest(talla, BigDecimal.valueOf(estMin), BigDecimal.valueOf(estMax),
                BigDecimal.valueOf(pesoMin), BigDecimal.valueOf(pesoMax), stock);
    }

    public static PrendaRequest prendaRequest(List<ComponenteRequest> composicion, List<TallaRequest> tallas) {
        return new PrendaRequest("  Vestido Floral Rosa ", CategoriaPrenda.VESTIDOS, "Vestido de algodón", null,
                new BigDecimal("45900"), 5, Set.of(ColorPreferido.ROSA), Estampado.FLORES, GeneroPrenda.NINA,
                false, false, false, false, composicion, tallas);
    }

    public static PrendaRequest prendaRequestValida() {
        return prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 100)),
                List.of(tallaRequest(" 4", 100, 110, 15, 20, 7), tallaRequest("6", 111, 122, 21, 26, 5)));
    }

    public static TallaPrenda talla(Prenda prenda, String nombre, int estMin, int estMax, int stock) {
        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda).talla(nombre)
                .estaturaMinCm(BigDecimal.valueOf(estMin)).estaturaMaxCm(BigDecimal.valueOf(estMax))
                .pesoMinKg(BigDecimal.valueOf(15)).pesoMaxKg(BigDecimal.valueOf(26))
                .stock(stock)
                .build();
        prenda.getTallas().add(talla);
        return talla;
    }

    /** Prenda activa del vendedor de prueba con tallas 4 y 6. */
    public static Prenda prenda(Long id, int stockMinimo, int stockTalla4, int stockTalla6) {
        Prenda prenda = Prenda.builder()
                .id(id)
                .vendedor(Usuario.builder().id(VENDEDOR_ID).nombreTienda("Tienda Sol").build())
                .nombre("Pantalón Jogger Azul")
                .categoria(CategoriaPrenda.PANTALONES)
                .precio(new BigDecimal("32900"))
                .estado(EstadoPrenda.ACTIVA)
                .stockMinimo(stockMinimo)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.ALGODON, 80));
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.POLIESTER, 20));
        talla(prenda, "4", 100, 110, stockTalla4);
        talla(prenda, "6", 111, 122, stockTalla6);
        return prenda;
    }
}
