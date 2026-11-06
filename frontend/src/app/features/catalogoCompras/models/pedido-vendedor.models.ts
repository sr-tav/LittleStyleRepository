import { ColorPreferido } from '../../perfilesInfantiles/models/perfil-infantil.models';
import { EstadoPedido } from '../../pedidos/models/pedido.models';

/**
 * Paquete del vendedor dentro de un pedido (US-12 multi-vendedor).
 * @see US-12
 */
export interface DespachoVendedor {
  despachoId: number;
  pedidoId: number;
  vendedorId: number;
  tienda: string;
  estado: EstadoPedido;
  numeroGuia: string | null;
  fechaActualizacion: string;
  items: PedidoVendedorItem[];
  numeroPedido: number | null;
  destinatario: string;
}

/** Alias de compatibilidad con el panel anterior (pedido por vendedor). */
export type PedidoVendedorResumen = DespachoVendedor;
/** Detalle del despacho con historial opcional de cambios de estado. */
export type PedidoVendedorDetalle = DespachoVendedor & {
  historial?: { anterior: EstadoPedido | null; nuevo: EstadoPedido; fecha: string }[];
};

/** Producto incluido en el paquete de un vendedor. */
export interface PedidoVendedorItem {
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
