package co.edu.uniquindio.littlestyle.modules.catalogocompras.carrito.service;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DisponibilidadStockService {

    public void validarDisponibilidad(int stock, long cantidad) {
        if (cantidad > Math.max(0, stock)) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "No hay unidades suficientes disponibles para la prenda y talla seleccionadas",
                    "cantidad");
        }
    }

    public boolean hayDisponibilidad(int stock, int cantidad) {
        return cantidad <= Math.max(0, stock);
    }
}
