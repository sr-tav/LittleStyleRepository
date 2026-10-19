package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Reglas de disponibilidad de stock para el carrito. */
@Service
public class DisponibilidadStockService {

    /** Lanza 409 si la cantidad pedida supera el stock disponible. */
    public void validarDisponibilidad(int stock, long cantidad) {
        if (cantidad > Math.max(0, stock)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No hay unidades suficientes disponibles para la prenda y talla seleccionadas",
                    "cantidad");
        }
    }

    /** Indica si el stock alcanza para la cantidad pedida. */
    public boolean hayDisponibilidad(int stock, int cantidad) {
        return cantidad <= Math.max(0, stock);
    }
}
