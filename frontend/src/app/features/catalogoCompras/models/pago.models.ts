import { CrearPedidoRequest } from './checkout.models';

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

export type EstadoTransaccionPago =
  | 'CREADA'
  | 'PENDIENTE'
  | 'APROBADA'
  | 'RECHAZADA'
  | 'ANULADA'
  | 'ERROR'
  | 'REEMBOLSO_PENDIENTE';

export type MotivoReembolso =
  | 'STOCK_INSUFICIENTE'
  | 'PEDIDO_NO_PAGABLE'
  | 'PAGO_ANULADO';

export interface PrendaAgotadaPago {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidadSolicitada: number;
  stockDisponible: number;
}

export interface LineaRecompra {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
}

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

export type SolicitudPago = CrearPedidoRequest;
