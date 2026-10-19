package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.repository;

import co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.model.MedicionCrecimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Acceso a las mediciones de crecimiento. */
public interface MedicionCrecimientoRepository extends JpaRepository<MedicionCrecimiento, Long> {

    /** Mediciones de un perfil del cliente. */
    List<MedicionCrecimiento> findAllByPerfilIdAndPerfilClienteId(Long perfilId, Long clienteId);

    /** Medición si pertenece al perfil y este al cliente. */
    Optional<MedicionCrecimiento> findByIdAndPerfilIdAndPerfilClienteId(Long id, Long perfilId, Long clienteId);
}
