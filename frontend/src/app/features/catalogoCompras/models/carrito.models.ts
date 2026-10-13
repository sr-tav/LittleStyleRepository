export interface ItemCarrito {
  id: number;
  prendaId: number;
  nombre: string;
  imagenUrl: string | null;
  talla: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  disponible: boolean;
}

export interface Carrito {
  items: ItemCarrito[];
  cantidad: number;
  subtotal: number;
  costoEnvio: number | null;
  total: number | null;
}

export interface AgregarItemCarritoRequest {
  prendaId: number;
  talla: string;
  cantidad: number;
}

export interface CambiarCantidadCarritoRequest {
  cantidad: number;
}
