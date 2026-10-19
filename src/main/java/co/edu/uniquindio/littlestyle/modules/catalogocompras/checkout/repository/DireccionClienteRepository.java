package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.DireccionCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Acceso a las direcciones guardadas de los clientes. */
public interface DireccionClienteRepository extends JpaRepository<DireccionCliente, Long> {
    /** Dirección guardada del cliente. */
    Optional<DireccionCliente> findByClienteId(Long clienteId);

    /** Borra la dirección del cliente (al eliminar su cuenta). */
    void deleteByClienteId(Long clienteId);
}
