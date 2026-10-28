package co.edu.uniquindio.littlestyle.config;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import co.edu.uniquindio.littlestyle.modules.auth.repository.UsuarioRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.CategoriaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoModeracion;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.EstadoPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.GeneroPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.Prenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.model.TallaPrenda;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.repository.PrendaRepository;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.EstadoPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.LineaPedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.Pedido;
import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Profile("local")
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalDashboardSeed implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PrendaRepository prendaRepository;
    private final PedidoRepository pedidoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (pedidoRepository.count() > 0 || prendaRepository.count() > 0) {
            log.info("Seed local de dashboard omitido porque ya hay datos en la base local");
            return;
        }

        Usuario vendedor = usuarioRepository.save(Usuario.builder()
                .nombre("Laura")
                .apellido("Vargas")
                .email("vendedor.local@littlestyle.com")
                .password(passwordEncoder.encode("Vendedor123"))
                .telefono("3001112233")
                .nombreTienda("Luna Kids")
                .rol(Rol.VENDEDOR)
                .estado(EstadoUsuario.ACTIVO)
                .build());

        Usuario cliente = usuarioRepository.save(Usuario.builder()
                .nombre("Mateo")
                .apellido("Perez")
                .email("cliente.local@littlestyle.com")
                .password(passwordEncoder.encode("Cliente123"))
                .telefono("3102223344")
                .rol(Rol.CLIENTE)
                .estado(EstadoUsuario.ACTIVO)
                .build());

        Prenda prenda1 = crearPrenda(vendedor, "Vestido Rosa Primavera", CategoriaPrenda.VESTIDOS, GeneroPrenda.NINA,
                new BigDecimal("69000"), "Vestido de algodón para niñas", "Luna Kids", 4);
        Prenda prenda2 = crearPrenda(vendedor, "Conjunto Verano", CategoriaPrenda.CONJUNTOS, GeneroPrenda.NINO,
                new BigDecimal("82000"), "Conjunto casual de verano", "Luna Kids", 3);
        Prenda prenda3 = crearPrenda(vendedor, "Pantalón Urbano", CategoriaPrenda.PANTALONES, GeneroPrenda.UNISEX,
                new BigDecimal("76000"), "Pantalón cómodo diario", "Luna Kids", 5);
        Prenda prenda4 = crearPrenda(vendedor, "Camiseta Básica", CategoriaPrenda.CAMISETAS, GeneroPrenda.NINO,
                new BigDecimal("34000"), "Camiseta de uso diario", "Luna Kids", 7);

        List<Prenda> prendas = prendaRepository.saveAll(List.of(prenda1, prenda2, prenda3, prenda4));

        crearPedido(cliente, LocalDateTime.now().minusDays(12), EstadoPedido.ENVIADO,
                new PedidoItem(prendas.get(0).getId(), "Vestido Rosa Primavera", "6", 2, new BigDecimal("69000"), new BigDecimal("138000")),
                new PedidoItem(prendas.get(1).getId(), "Conjunto Verano", "8", 1, new BigDecimal("82000"), new BigDecimal("82000")));

        crearPedido(cliente, LocalDateTime.now().minusDays(40), EstadoPedido.PAGADO,
                new PedidoItem(prendas.get(2).getId(), "Pantalón Urbano", "10", 3, new BigDecimal("76000"), new BigDecimal("228000")),
                new PedidoItem(prendas.get(3).getId(), "Camiseta Básica", "8", 4, new BigDecimal("34000"), new BigDecimal("136000")));

        crearPedido(cliente, LocalDateTime.now().minusDays(65), EstadoPedido.ENVIADO,
                new PedidoItem(prendas.get(0).getId(), "Vestido Rosa Primavera", "6", 1, new BigDecimal("69000"), new BigDecimal("69000")),
                new PedidoItem(prendas.get(2).getId(), "Pantalón Urbano", "10", 2, new BigDecimal("76000"), new BigDecimal("152000")));

        log.info("Seed local de dashboard creado con 3 pedidos y 4 prendas demo para admin");
    }

    private Prenda crearPrenda(Usuario vendedor, String nombre, CategoriaPrenda categoria, GeneroPrenda genero,
                               BigDecimal precio, String descripcion, String marca, int stock) {
        Prenda prenda = Prenda.builder()
                .vendedor(vendedor)
                .nombre(nombre)
                .categoria(categoria)
                .genero(genero)
                .descripcion(descripcion)
                .instruccionesCuidado("Lavar a máquina a 30°C; no usar blanqueador.")
                .marca(marca)
                .precio(precio)
                .estado(EstadoPrenda.ACTIVA)
                .estadoModeracion(EstadoModeracion.APROBADA)
                .estampado(null)
                .tintesSinteticos(false)
                .brochesMetalicos(false)
                .contieneNiquel(false)
                .tratamientoFormaldehido(false)
                .fechaCreacion(LocalDateTime.now().minusDays(20))
                .fechaActualizacion(LocalDateTime.now().minusDays(5))
                .build();

        TallaPrenda talla = TallaPrenda.builder()
                .prenda(prenda)
                .talla("6")
                .estaturaMinCm(new BigDecimal("110.0"))
                .estaturaMaxCm(new BigDecimal("115.0"))
                .pesoMinKg(new BigDecimal("18.0"))
                .pesoMaxKg(new BigDecimal("22.0"))
                .edadMinMeses(72)
                .edadMaxMeses(96)
                .stockMinimo(2)
                .stock(stock)
                .build();

        prenda.setTallas(List.of(talla));
        prenda.setColores(java.util.Collections.emptySet());
        prenda.setComposicion(java.util.Collections.emptyList());
        prenda.setImagenes(java.util.Collections.emptyList());
        return prenda;
    }

    private void crearPedido(Usuario cliente, LocalDateTime fecha, EstadoPedido estado, PedidoItem... items) {
        Pedido pedido = Pedido.builder()
                .cliente(cliente)
                .perfilInfantilId(1L)
                .estado(estado)
                .destinatario("Ana Gómez")
                .direccion("Carrera 12 # 30-40")
                .complemento("Apto 302")
                .codigoPostal("760001")
                .departamento("Caldas")
                .ciudad("Manizales")
                .telefono("3009876543")
                .subtotal(itemsToTotal(items))
                .costoEnvio(new BigDecimal("7000"))
                .total(itemsToTotal(items).add(new BigDecimal("7000")))
                .fechaCreacion(fecha)
                .fechaActualizacion(fecha)
                .build();

        for (PedidoItem item : items) {
            pedido.agregar(LineaPedido.builder()
                    .prendaId(item.prendaId())
                    .nombre(item.nombre())
                    .talla(item.talla())
                    .cantidad(item.cantidad())
                    .precioUnitario(item.precioUnitario())
                    .subtotal(item.subtotal())
                    .build());
        }

        pedidoRepository.save(pedido);
    }

    private BigDecimal itemsToTotal(PedidoItem... items) {
        return java.util.Arrays.stream(items)
                .map(PedidoItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record PedidoItem(Long prendaId, String nombre, String talla, int cantidad,
                             BigDecimal precioUnitario, BigDecimal subtotal) {
    }
}
