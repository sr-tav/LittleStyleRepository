package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MedicionCrecimientoRepository extends JpaRepository<MedicionCrecimiento, Long> {

    List<MedicionCrecimiento> findAllByPerfilIdAndPerfilClienteId(Long perfilId, Long clienteId);

    Optional<MedicionCrecimiento> findByIdAndPerfilIdAndPerfilClienteId(Long id, Long perfilId, Long clienteId);
}
