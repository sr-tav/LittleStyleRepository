export type EstadoPedido =
  | 'PENDIENTE_PAGO'
  | 'PAGADO'
  | 'REGISTRADO'
  | 'EN_PREPARACION'
  | 'PARCIALMENTE_ENVIADO'
  | 'ENVIADO'
  | 'CANCELADO';

export const ETIQUETAS_ESTADO: Record<EstadoPedido, string> = {
  PENDIENTE_PAGO: 'Pendiente de pago',
  PAGADO: 'Pagado',
  REGISTRADO: 'Registrado',
  EN_PREPARACION: 'En preparación',
  PARCIALMENTE_ENVIADO: 'Parcialmente enviado',
  ENVIADO: 'Enviado',
  CANCELADO: 'Cancelado',
};

/** Orden canónico del ciclo para la línea de tiempo (ENVIADO es terminal). */
export const ORDEN_ESTADOS: EstadoPedido[] = [
  'PENDIENTE_PAGO',
  'PAGADO',
  'REGISTRADO',
  'EN_PREPARACION',
  'PARCIALMENTE_ENVIADO',
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
  imagenUrl?: string | null;
}

export interface MovimientoPedido {
  anterior: EstadoPedido | null;
  nuevo: EstadoPedido;
  fecha: string;
  numeroGuia?: string | null;
}

export interface DespachoBloque {
  despachoId: number;
  pedidoId: number;
  vendedorId: number;
  tienda: string;
  estado: EstadoPedido;
  numeroGuia: string | null;
  fechaActualizacion: string;
  items: PedidoItem[];
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
  despachos: DespachoBloque[];
  historial: MovimientoPedido[];
}
