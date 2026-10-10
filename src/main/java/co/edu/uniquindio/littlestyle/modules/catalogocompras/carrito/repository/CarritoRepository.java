package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.repository;

import co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.model.Carrito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    Optional<Carrito> findByClienteId(Long clienteId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Carrito c where c.cliente.id = :clienteId")
    Optional<Carrito> bloquearPorClienteId(@Param("clienteId") Long clienteId);
}
