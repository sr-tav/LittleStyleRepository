package co.edu.uniquindio.littlestyle.modules.perfilesinfantiles.service;

import co.edu.uniquindio.littlestyle.shared.event.CuentaEliminadaEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EliminacionDatosCuentaListener {

    private final PerfilInfantilService perfilInfantilService;

    @EventListener
    public void alEliminarCuenta(CuentaEliminadaEvent evento) {
        perfilInfantilService.eliminarTodosDeCliente(evento.usuarioId());
    }
}
