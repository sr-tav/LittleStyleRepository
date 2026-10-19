package co.edu.uniquindio.littlestyle.modules.catalogocompras.pago.model;

/** Por qué un cobro aprobado por Wompi no se convirtió en pedido pagado y debe devolverse. */
public enum MotivoReembolso {
    STOCK_INSUFICIENTE,
    PEDIDO_NO_PAGABLE,
    /** El cliente anuló el pago y aun así Wompi aprobó el cobro; hay que devolver el dinero. */
    PAGO_ANULADO
}
