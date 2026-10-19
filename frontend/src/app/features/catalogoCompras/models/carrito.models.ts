/** Línea individual con cantidad, precio y disponibilidad en el carrito. */
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

/** Contenido y totales calculados del carrito de compras. */
export interface Carrito {
  items: ItemCarrito[];
  cantidad: number;
  subtotal: number;
  costoEnvio: number | null;
  total: number | null;
}

/**
 * Datos para agregar una prenda y talla al carrito.
 * @see RNF-11
 * @see US-10
 * @see US-07
 * @see RNF-11
 */
export interface AgregarItemCarritoRequest {
  prendaId: number;
  talla: string;
  cantidad: number;
  /** Talla sugerida por el motor al agregar; base de la métrica RNF-11. */
  tallaSugerida?: string | null;
  /**
   * Perfil infantil activo con el que se agrega. El servicio lo agrega
   * automáticamente; el backend bloquea con 422 y código
   * PRENDA_EXCLUIDA_ALERGIA si la prenda choca con una alergia grave.
   */
  perfilInfantilId?: number | null;
}

/** Cambio de cantidad solicitado para una línea del carrito. */
export interface CambiarCantidadCarritoRequest {
  cantidad: number;
}
