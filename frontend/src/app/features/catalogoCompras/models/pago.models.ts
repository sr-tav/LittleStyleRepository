export interface InicioPago {
  pedidoId: number;
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

export type MotivoReembolso = 'STOCK_INSUFICIENTE' | 'PEDIDO_VENCIDO' | 'PEDIDO_NO_PAGABLE';

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
  pedidoId: number;
  referencia: string;
  estadoPago: EstadoTransaccionPago;
  estadoPedido: 'PENDIENTE_PAGO' | 'PAGADO' | 'EN_PREPARACION' | 'ENVIADO' | 'ENTREGADO' | 'CANCELADO';
  motivoReembolso: MotivoReembolso | null;
  metodoPago: string | null;
  total: number;
  prendasAgotadas: PrendaAgotadaPago[];
  lineasDisponibles: LineaRecompra[];
}
