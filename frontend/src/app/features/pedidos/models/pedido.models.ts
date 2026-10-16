export type EstadoPedido =
  | 'PENDIENTE_PAGO'
  | 'PAGADO'
  | 'REGISTRADO'
  | 'EN_PREPARACION'
  | 'ENVIADO'
  | 'CANCELADO';

export const ETIQUETAS_ESTADO: Record<EstadoPedido, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  PAGADO: 'Pagado',
  REGISTRADO: 'Registrado',
  EN_PREPARACION: 'En preparación',
  ENVIADO: 'Enviado',
  CANCELADO: 'Cancelado',
};

/** Orden canónico del ciclo para la línea de tiempo (ENVIADO es terminal). */
export const ORDEN_ESTADOS: EstadoPedido[] = [
  'PENDIENTE_PAGO',
  'PAGADO',
  'REGISTRADO',
  'EN_PREPARACION',
  'ENVIADO',
];

export interface PedidoResumen {
  id: number;
  estado: EstadoPedido;
  total: number;
  fechaCreacion: string;
  cantidadItems: number;
  numeroGuia: string | null;
}

export interface PedidoItem {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface MovimientoPedido {
  anterior: EstadoPedido | null;
  nuevo: EstadoPedido;
  fecha: string;
}

export interface PedidoDetalle {
  id: number;
  estado: EstadoPedido;
  subtotal: number;
  costoEnvio: number;
  total: number;
  fechaCreacion: string;
  numeroGuia: string | null;
  ciudad: string;
  departamento: string;
  items: PedidoItem[];
  historial: MovimientoPedido[];
}
