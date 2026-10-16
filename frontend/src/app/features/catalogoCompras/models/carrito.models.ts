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
  tallaSugerida?: string | null;
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
  /** Talla sugerida por el motor al agregar; base de la métrica RNF-11. */
  tallaSugerida?: string | null;
}

export interface CambiarCantidadCarritoRequest {
  cantidad: number;
}
