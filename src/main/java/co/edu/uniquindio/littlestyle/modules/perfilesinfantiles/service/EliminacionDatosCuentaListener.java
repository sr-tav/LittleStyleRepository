package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Borra los perfiles infantiles cuando el cliente elimina su cuenta. */
@Component
@RequiredArgsConstructor
public class EliminacionDatosCuentaListener {

    private final PerfilInfantilService perfilInfantilService;

    /**
     * Elimina todos los perfiles y mediciones del usuario dentro de la transacción de eliminación de la
     * cuenta.
     */
    @EventListener
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        perfilInfantilService.eliminarTodosDeCliente(evento.usuarioId());
    }
}
