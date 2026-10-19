package co.edu.uniquindio.littlestyle.shared.event;

/** Se publica cuando un usuario elimina su cuenta, para que cada módulo borre los datos que guarda de él. */
public record CuentaEliminadaEvent(Long usuarioId) {
}
