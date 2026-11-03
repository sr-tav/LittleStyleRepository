package co.edu.uniquindio.littlestyle.modules.catalogocompras;

import co.edu.uniquindio.littlestyle.config.security.AuthenticatedUser;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.dto.*;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.*;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.ColorPreferido;
import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.Estampado;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/** Datos de prueba compartidos por las pruebas del catálogo y el inventario. */
public final class CatalogoFixtures {

    public static final Long VENDEDOR_ID = 21L;

    /** Cabecera PNG válida: basta para que el detector de tipo la acepte. */
    public static final byte[] PNG_MINIMO = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

    /** Alta de prenda como la espera el API: parte JSON {@code prenda} y una foto en {@code archivos}. */
    public static MockMultipartHttpServletRequestBuilder prendaMultipart(String json) {
        return multipart("/api/vendedor/prendas")
                .file(new MockMultipartFile("prenda", "", MediaType.APPLICATION_JSON_VALUE,
                        json.getBytes(StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("archivos", "foto.png", "image/png", PNG_MINIMO));
    }

    private CatalogoFixtures() {
    }

    public static void autenticarVendedor() {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                new AuthenticatedUser(VENDEDOR_ID, "vendedor@test.com", "Vendedora", Rol.VENDEDOR), null, List.of()));
    }

    public static TallaRequest tallaRequest(String talla, int estMin, int estMax, int pesoMin, int pesoMax, Integer stock) {
        return new TallaRequest(talla, BigDecimal.valueOf(estMin), BigDecimal.valueOf(estMax),
                BigDecimal.valueOf(pesoMin), BigDecimal.valueOf(pesoMax), stock, null, null, null);
    }

    public static PrendaRequest prendaRequest(List<ComponenteRequest> composicion, List<TallaRequest> tallas) {
        return new PrendaRequest("  Vestido Floral Rosa ", CategoriaPrenda.VESTIDOS, "Vestido de algodón", " Lavar a mano en agua fría ", null,
                new BigDecimal("45900"), Set.of(ColorPreferido.ROSA), Estampado.FLORES, GeneroPrenda.NINA,
                false, false, false, false, composicion, tallas);
    }

    public static PrendaRequest prendaRequestValida() {
        return prendaRequest(List.of(new ComponenteRequest(MaterialTextil.ALGODON, 100)),
                List.of(tallaRequest(" 4", 100, 110, 15, 20, 7), tallaRequest("6", 111, 122, 21, 26, 5)));
    }

    public static TallaPrenda talla(Prenda prenda, String nombre, int estMin, int estMax, int stock) {
        return talla(prenda, nombre, estMin, estMax, stock, null);
    }

    public static TallaPrenda talla(Prenda prenda, String nombre, int estMin, int estMax, int stock,
                                    Integer stockMinimo) {
        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda).talla(nombre)
                .estaturaMinCm(BigDecimal.valueOf(estMin)).estaturaMaxCm(BigDecimal.valueOf(estMax))
                .pesoMinKg(BigDecimal.valueOf(15)).pesoMaxKg(BigDecimal.valueOf(26))
                .stock(stock)
                .stockMinimo(stockMinimo)
                .build();
        prenda.getTallas().add(talla);
        return talla;
    }

    /** Prenda activa del vendedor de prueba con tallas 4 y 6 (mínimo por talla). */
    public static Prenda prenda(Long id, int stockMinimo, int stockTalla4, int stockTalla6) {
        Prenda prenda = Prenda.builder()
                .id(id)
                .vendedor(Usuario.builder().id(VENDEDOR_ID).nombreTienda("Tienda Sol").build())
                .nombre("Pantalón Jogger Azul")
                .categoria(CategoriaPrenda.PANTALONES)
                .precio(new BigDecimal("32900"))
                .estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(LocalDateTime.now())
                .fechaActualizacion(LocalDateTime.now())
                .build();
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.ALGODON, 80));
        prenda.getComposicion().add(new ComponenteComposicion(MaterialTextil.POLIESTER, 20));
        talla(prenda, "4", 100, 110, stockTalla4, stockMinimo);
        talla(prenda, "6", 111, 122, stockTalla6, stockMinimo);
        return prenda;
    }
}
