package co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.checkout.model.DireccionCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DireccionClienteRepository extends JpaRepository<DireccionCliente, Long> {
    Optional<DireccionCliente> findByClienteId(Long clienteId);

    void deleteByClienteId(Long clienteId);
}
