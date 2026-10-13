export interface DireccionEnvio {
  destinatario: string;
  direccion: string;
  complemento: string | null;
  codigoPostal: string | null;
  departamento: string | null;
  municipio: string | null;
  telefono: string;
}

export interface DireccionGuardada {
  guardada: boolean;
  direccion: DireccionEnvio | null;
}

export interface CrearPedidoRequest {
  perfilInfantilId: number;
  direccion: DireccionEnvio;
}

export interface ResumenCheckoutRequest {
  perfilInfantilId: number;
  direccion: DireccionEnvio;
}

export interface LineaResumenCheckout {
  prendaId: number;
  nombre: string;
  talla: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

export interface ResumenCheckout {
  perfilInfantilId: number;
  items: LineaResumenCheckout[];
  subtotal: number;
  costoEnvio: number;
  total: number;
}

export interface PedidoCreado {
  pedidoId: number;
  estado: 'PENDIENTE_PAGO';
  perfilInfantilId: number;
  direccion: DireccionEnvio;
  items: LineaResumenCheckout[];
  subtotal: number;
  costoEnvio: number;
  total: number;
}
