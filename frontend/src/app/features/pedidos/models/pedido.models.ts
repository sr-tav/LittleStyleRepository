import { ColorPreferido } from '../../perfilesInfantiles/models/perfil-infantil.models';

/** Estados utilizados para representar el ciclo de vida de un pedido. */
export type EstadoPedido =
  | 'PENDIENTE_PAGO'
  | 'PAGADO'
  | 'REGISTRADO'
  | 'EN_PREPARACION'
  | 'PARCIALMENTE_ENVIADO'
  | 'ENVIADO'
  | 'CANCELADO';

/** Etiquetas en español para los estados del pedido. */
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

/** Resumen de un pedido mostrado en el historial del cliente. */
export interface PedidoResumen {
  /** Identificador interno; se usa en las rutas, no se muestra al cliente. */
  id: number;
  /** Consecutivo del pedido para el cliente (1, 2, 3…); es el número que se muestra. */
  numeroPedido: number;
  estado: EstadoPedido;
  total: number;
  fechaCreacion: string;
  cantidadItems: number;
  numeroGuia: string | null;
}

/** Producto y cantidades incluidos en un pedido. */
export interface PedidoItem {
  prendaId: number;
  nombre: string;
  talla: string;
  color?: ColorPreferido | null;
  estampado?: string | null;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  imagenUrl?: string | null;
}

/** Cambio de estado registrado en el historial de un pedido. */
export interface MovimientoPedido {
  anterior: EstadoPedido | null;
  nuevo: EstadoPedido;
  fecha: string;
  numeroGuia?: string | null;
}

/**
 * Bloque de productos y seguimiento perteneciente a un vendedor dentro del pedido.
 * @see US-12
 */
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

/** Datos completos de un pedido, incluidos sus despachos e historial de estados. */
export interface PedidoDetalle {
  /** Identificador interno; se usa en las rutas, no se muestra al cliente. */
  id: number;
  /** Consecutivo del pedido para el cliente (1, 2, 3…); es el número que se muestra. */
  numeroPedido: number;
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
