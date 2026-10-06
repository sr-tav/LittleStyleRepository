package co.edu.uniquindio.littlestyle.modules.catalogocompras.catalogo.storage;

import co.edu.uniquindio.littlestyle.shared.exception.BusinessException;
import org.springframework.http.HttpStatus;

/** Fallo del servicio externo de almacenamiento; se responde 502 sin exponer el detalle técnico. */
public class AlmacenamientoException extends BusinessException {

    public AlmacenamientoException(String mensaje, Throwable causa) {
        super(HttpStatus.BAD_GATEWAY, mensaje);
        initCause(causa);
    }
}
