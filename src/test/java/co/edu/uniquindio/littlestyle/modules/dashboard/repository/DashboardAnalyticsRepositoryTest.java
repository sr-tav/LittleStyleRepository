package co.edu.uniquindio.littlestyle.modules.dashboard.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.dashboard.dto.DashboardSerieResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DashboardAnalyticsRepositoryTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 1, 0, 0);
    private static final LocalDateTime FIN = LocalDateTime.of(2026, 11, 1, 0, 0);

    @Autowired
    private DashboardAnalyticsRepository repositorio;

    @Autowired
    private TestEntityManager em;

    @BeforeEach
    void sembrar() {
        Usuario vendedor = em.persist(usuario("vendedor-dashboard@correo.com", Rol.VENDEDOR));
        Usuario cliente = em.persist(usuario("cliente-dashboard@correo.com", Rol.CLIENTE));

        Prenda prenda = em.persist(prenda(vendedor));

        em.persist(pedido(cliente, 1, EstadoPedido.REGISTRADO,
                linea(prenda, "4T", 2),
                linea(prenda, "6T", 4)));

        em.persist(pedido(cliente, 2, EstadoPedido.PAGADO,
                linea(prenda, "4T", 3)));

        // Debe excluirse al consultar con los estados usados para ventas.
        em.persist(pedido(cliente, 3, EstadoPedido.CANCELADO,
                linea(prenda, "4T", 99)));

        em.flush();
        em.clear();
    }

    @Test
    void agrupaUnidadesPorTallaYRespetaLosEstadosDeVenta() {
        List<EstadoPedido> estadosVenta = List.of(
                EstadoPedido.PAGADO,
                EstadoPedido.REGISTRADO,
                EstadoPedido.EN_PREPARACION,
                EstadoPedido.PARCIALMENTE_ENVIADO,
                EstadoPedido.ENVIADO
        );

        List<DashboardSerieResponse> resultado = repositorio.demandaPorTalla(
                INICIO, FIN, estadosVenta, null, null);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).label()).isEqualTo("4T");
        assertThat(resultado.get(0).cantidad()).isEqualTo(5L);
        assertThat(resultado.get(1).label()).isEqualTo("6T");
        assertThat(resultado.get(1).cantidad()).isEqualTo(4L);
    }

    private static Usuario usuario(String email, Rol rol) {
        return Usuario.builder()
                .nombre("Usuario")
                .apellido("Prueba")
                .email(email)
                .password("x")
                .rol(rol)
                .estado(EstadoUsuario.ACTIVO)
                .fechaRegistro(INICIO)
                .build();
    }

    private static Prenda prenda(Usuario vendedor) {
        return Prenda.builder()
                .vendedor(vendedor)
                .nombre("Prenda de prueba")
                .categoria(CategoriaPrenda.CAMISAS)
                .precio(new BigDecimal("20000"))
                .estado(EstadoPrenda.ACTIVA)
                .fechaCreacion(INICIO)
                .fechaActualizacion(INICIO)
                .build();
    }

    private static LineaPedido linea(Prenda prenda, String talla, int cantidad) {
        return LineaPedido.builder()
                .prendaId(prenda.getId())
                .nombre(prenda.getNombre())
                .talla(talla)
                .cantidad(cantidad)
                .precioUnitario(new BigDecimal("20000"))
                .subtotal(new BigDecimal("20000").multiply(BigDecimal.valueOf(cantidad)))
                .build();
    }

    private static Pedido pedido(
            Usuario cliente,
            int numero,
            EstadoPedido estado,
            LineaPedido... lineas
    ) {
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .numeroCliente(numero)
                .perfilInfantilId(1L)
                .estado(estado)
                .destinatario("Cliente")
                .direccion("Calle 1")
                .ciudad("Armenia")
                .telefono("3000000000")
                .fechaActualizacion(INICIO)
                .fechaCreacion(INICIO.plusDays(1))
                .subtotal(new BigDecimal("20000"))
                .costoEnvio(BigDecimal.ZERO)
                .total(new BigDecimal("20000"))
                .build();

        for (LineaPedido linea : lineas) {
            pedido.agregar(linea);
        }
        return pedido;
    }
}