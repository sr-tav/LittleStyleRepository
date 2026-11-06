/** Dirección de entrega asociada a una compra. */
export interface DireccionEnvio {
  destinatario: string;
  /** Texto compuesto por el servidor, p. ej. «Calle 45 # 12 - 34». */
  direccion: string;
  tipoVia: string | null;
  numeroVia: string | null;
  numeroCruce: string | null;
  placa: string | null;
  complemento: string | null;
  codigoPostal: string | null;
  departamento: string | null;
  municipio: string | null;
  telefono: string;
}

/** Estado de existencia y datos de una dirección guardada. */
export interface DireccionGuardada {
  guardada: boolean;
  direccion: DireccionEnvio | null;
}

/** Dirección guardada con su identificador para seleccionarla o editarla. */
export interface DireccionClienteGuardada {
  id: number;
  direccion: DireccionEnvio;
}

/** Datos necesarios para crear un pedido. */
export interface CrearPedidoRequest {
  perfilInfantilId: number;
  direccion: DireccionEnvio;
}

/** Perfil y dirección usados para calcular el resumen del checkout. */
export interface ResumenCheckoutRequest {
  perfilInfantilId: number;
  direccion: DireccionEnvio;
}

/** Línea de producto incluida en el resumen previo al pedido. */
export interface LineaResumenCheckout {
  prendaId: number;
  nombre: string;
  talla: string;
  color?: string | null;
  estampado?: string | null;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  tallaSugerida?: string | null;
}

/** Importes y líneas calculados para el checkout. */
export interface ResumenCheckout {
  perfilInfantilId: number;
  items: LineaResumenCheckout[];
  subtotal: number;
  costoEnvio: number;
  total: number;
}

export interface DireccionGuardada {
  guardada: boolean;
  direccion: DireccionEnvio | null;
  predeterminada: boolean;
}
export interface DireccionClienteGuardada {
  id: number;
  direccion: DireccionEnvio;
  predeterminada: boolean;
}
