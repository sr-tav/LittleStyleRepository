package co.edu.uniquindio.littlestyle.shared.event;

import co.edu.uniquindio.littlestyle.modules.auth.model.EstadoUsuario;

public record CuentaEstadoCambiadaEvent(Long usuarioId, EstadoUsuario estadoAnterior,
                                        EstadoUsuario estadoNuevo, String motivo) {
}

