import { CrearPedidoRequest } from './checkout.models';

/** Datos requeridos para iniciar el widget de pago. */
export interface InicioPago {
  pedidoId: number | null;
  referencia: string;
  montoCentavos: number;
  moneda: string;
  llavePublica: string;
  firmaIntegridad: string;
  urlRedireccion: string | null;
  emailCliente: string | null;
  nombreCliente: string | null;
  telefonoCliente: string | null;
}

/** Estados de transacción reconocidos por la interfaz. */
export type EstadoTransaccionPago =
  | 'CREADA'
  | 'PENDIENTE'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'ANULADA'
  | 'ERROR'
  | 'REEMBOLSO_PENDIENTE';

/** Códigos de motivo de reembolso manejados por la interfaz. */
export type MotivoReembolso =
  | 'STOCK_INSUFICIENTE'
  | 'PEDIDO_NO_PAGABLE'
  | 'PAGO_ANULADO';

/** Prenda cuya disponibilidad de stock afecta el resultado del pago. */
export interface PrendaAgotadaPago {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidadSolicitada: number;
  stockDisponible: number;
}

/** Línea disponible para iniciar una recompra. */
export interface LineaRecompra {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
}

/** Resultado del pago y estado asociado del pedido. */
export interface ResultadoPago {
  pedidoId: number | null;
  referencia: string;
  estadoPago: EstadoTransaccionPago;
  estadoPedido: 'PENDIENTE_PAGO' | 'PAGADO' | 'REGISTRADO' | 'EN_PREPARACION' | 'ENVIADO' | 'CANCELADO' | null;
  motivoReembolso: MotivoReembolso | null;
  metodoPago: string | null;
  total: number;
  prendasAgotadas: PrendaAgotadaPago[];
  lineasDisponibles: LineaRecompra[];
}

/** Solicitud de pago basada en los datos de creación del pedido. */
export type SolicitudPago = CrearPedidoRequest;
