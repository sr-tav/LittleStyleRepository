package co.edu.uniquindio.littlestyle.modules.auth.repository;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;
import co.edu.uniquindio.littlestyle.modules.auth.model.Rol;
import co.edu.uniquindio.littlestyle.modules.auth.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryBuscarAdminTest {

    @Autowired private UsuarioRepository repositorio;
    @Autowired private TestEntityManager em;

    @BeforeEach
    void sembrar() {
        em.persist(usuario("activo@correo.com", EstadoUsuario.ACTIVO));
        em.persist(usuario("suspendido@correo.com", EstadoUsuario.SUSPENDIDO));
        em.persist(usuario("eliminado@correo.com", EstadoUsuario.ELIMINADO));
        em.flush();
    }

    private Usuario usuario(String email, EstadoUsuario estado) {
        return Usuario.builder().nombre("Ana").apellido("Ruiz").email(email).password("x")
                .rol(Rol.CLIENTE).estado(estado).fechaRegistro(LocalDateTime.now()).build();
    }

    @Test
    void sinFiltroDeEstadoNoListaLasCuentasEliminadas() {
        var pagina = repositorio.buscarAdmin(null, null, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(Usuario::getEstado)
                .doesNotContain(EstadoUsuario.ELIMINADO)
                .contains(EstadoUsuario.ACTIVO, EstadoUsuario.SUSPENDIDO);
    }

    @Test
    void filtrandoPorEliminadoSiLasLista() {
        var pagina = repositorio.buscarAdmin(null, EstadoUsuario.ELIMINADO, null, PageRequest.of(0, 20));

        assertThat(pagina.getContent()).extracting(Usuario::getEmail).contains("eliminado@correo.com");
    }
}