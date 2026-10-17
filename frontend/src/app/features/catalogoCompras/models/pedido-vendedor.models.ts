import { EstadoPedido } from '../../pedidos/models/pedido.models';

/** Paquete del vendedor dentro de un pedido (US-12 multi-vendedor). */
export interface DespachoVendedor {
  despachoId: number;
  pedidoId: number;
  vendedorId: number;
  tienda: string;
  estado: EstadoPedido;
  numeroGuia: string | null;
  fechaActualizacion: string;
  items: PedidoVendedorItem[];
}

/** Alias de compatibilidad con el panel anterior (pedido por vendedor). */
export type PedidoVendedorResumen = DespachoVendedor;
export type PedidoVendedorDetalle = DespachoVendedor & {
  historial?: { anterior: EstadoPedido | null; nuevo: EstadoPedido; fecha: string }[];
};

export interface PedidoVendedorItem {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  imagenUrl?: string | null;
}
