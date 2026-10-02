package co.edu.uniquindio.littlestyle.config.security;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "fU4vAmqXuCYrZrEa0nxgNLpBz4s7E2/RIQ8Nr9UfuaVrh1FKIYse12XhkKu4MylNoNFQnI0ZsrhyTuNfBEcexQ==";
    private static final String OTRO_SECRET =
            "q1w2e3r4t5y6u7i8o9p0a1s2d3f4g5h6j7k8l9z0x1c2v3b4n5m6q7w8e9r0t1y2u3i4o5p6a7s8d9f0g1h2j3k4==";

    private JwtService jwtService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, 60_000));
        usuario = Usuario.builder()
                .id(10L).nombre("Tomás").apellido("Aristizabal").email("tomas@correo.com")
                .rol(Rol.VENDEDOR).estado(EstadoUsuario.ACTIVO).password("hash")
                .build();
    }

    @Test
    void generaTokenConClaimsDelUsuario() {
        long antes = System.currentTimeMillis();
        JwtService.TokenEmitido emitido = jwtService.generarToken(usuario);

        assertThat(emitido.token()).isNotBlank();
        assertThat(emitido.expiraEn()).isBetween(antes + 59_000, antes + 61_000);

        Claims claims = jwtService.validarToken(emitido.token());
        assertThat(claims.getSubject()).isEqualTo("tomas@correo.com");
        assertThat(claims.get("rol", String.class)).isEqualTo("VENDEDOR");
        assertThat(claims.get("nombre", String.class)).isEqualTo("Tomás");
    }

    @Test
    void convierteClaimsEnAuthenticatedUser() {
        Claims claims = jwtService.validarToken(jwtService.generarToken(usuario).token());

        AuthenticatedUser user = jwtService.toAuthenticatedUser(claims);

        assertThat(user).isEqualTo(new AuthenticatedUser(10L, "tomas@correo.com", "Tomás", Rol.VENDEDOR));
    }

    @Test
    void rechazaTokenExpirado() {
        JwtService expiraYa = new JwtService(new JwtProperties(SECRET, -1_000));
        String token = expiraYa.generarToken(usuario).token();

        assertThatThrownBy(() -> jwtService.validarToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        String token = new JwtService(new JwtProperties(OTRO_SECRET, 60_000)).generarToken(usuario).token();

        assertThatThrownBy(() -> jwtService.validarToken(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenAlterado() {
        String token = jwtService.generarToken(usuario).token();
        String[] partes = token.split("\\.");
        // Cambiar el payload invalida la firma
        String alterado = partes[0] + "." + partes[1].substring(0, partes[1].length() - 2) + "xx." + partes[2];

        assertThatThrownBy(() -> jwtService.validarToken(alterado)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenMalformado() {
        assertThatThrownBy(() -> jwtService.validarToken("esto-no-es-un-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void fallaSiNoHaySecretConfigurado() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(" ", 60_000)))
                .isInstanceOf(IllegalStateException.class);
    }
}
