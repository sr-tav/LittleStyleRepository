import { EstadoPedido } from '../../pedidos/models/pedido.models';

export interface PedidoVendedorResumen {
  id: number;
  estado: EstadoPedido;
  fechaCreacion: string;
  cantidadItemsPropios: number;
  subtotalPropio: number;
  numeroGuia: string | null;
}

export interface PedidoVendedorItem {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface PedidoVendedorDetalle {
  id: number;
  estado: EstadoPedido;
  fechaCreacion: string;
  numeroGuia: string | null;
  destinatario: string;
  direccion: string;
  complemento: string | null;
  codigoPostal: string | null;
  ciudad: string;
  departamento: string;
  telefono: string;
  items: PedidoVendedorItem[];
  historial: { anterior: EstadoPedido | null; nuevo: EstadoPedido; fecha: string }[];
}
