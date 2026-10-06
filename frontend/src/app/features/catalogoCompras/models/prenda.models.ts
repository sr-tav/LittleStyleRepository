import { ColorPreferido, Estampado } from '../../perfilesInfantiles/models/perfil-infantil.models';

export type CategoriaPrenda =
  | 'VESTIDOS'
  | 'PANTALONES'
  | 'SHORTS'
  | 'FALDAS'
  | 'CAMISAS'
  | 'CAMISETAS'
  | 'BUZOS'
  | 'CHAQUETAS'
  | 'CONJUNTOS'
  | 'PIJAMAS'
  | 'ACCESORIOS';

export type MaterialTextil =
  | 'ALGODON'
  | 'POLIESTER'
  | 'NYLON'
  | 'ACRILICO'
  | 'ELASTANO'
  | 'LANA'
  | 'VISCOSA'
  | 'CUERO'
  | 'LATEX'
  | 'OTRO';

export type EstadoPrenda = 'ACTIVA' | 'INACTIVA';
export type NivelStock = 'DISPONIBLE' | 'STOCK_BAJO' | 'AGOTADO';

export interface Componente {
  material: MaterialTextil;
  porcentaje: number;
}

export interface Talla {
  id: number;
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  stock: number;
}

export interface ImagenPrenda {
  id: number;
  url: string;
  orden: number;
}

export interface Prenda {
  id: number;
  nombre: string;
  categoria: CategoriaPrenda;
  descripcion: string | null;
  marca: string | null;
  precio: number;
  estado: EstadoPrenda;
  stockMinimo: number;
  stockTotal: number;
  nivelStock: NivelStock;
  colores: ColorPreferido[];
  estampado: Estampado | null;
  tintesSinteticos: boolean;
  brochesMetalicos: boolean;
  contieneNiquel: boolean;
  tratamientoFormaldehido: boolean;
  composicion: Componente[];
  tallas: Talla[];
  imagenes: ImagenPrenda[];
  fechaCreacion: string;
  fechaActualizacion: string;
}

export interface TallaRequest {
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  /** Solo se usa para tallas nuevas; el stock de las existentes se cambia en el inventario. */
  stock: number | null;
}

export interface PrendaRequest {
  nombre: string;
  categoria: CategoriaPrenda;
  descripcion: string | null;
  marca: string | null;
  precio: number;
  stockMinimo: number;
  colores: ColorPreferido[];
  estampado: Estampado | null;
  tintesSinteticos: boolean;
  brochesMetalicos: boolean;
  contieneNiquel: boolean;
  tratamientoFormaldehido: boolean;
  composicion: Componente[];
  tallas: TallaRequest[];
}

export interface InventarioItem {
  prendaId: number;
  nombre: string;
  categoria: CategoriaPrenda;
  imagenUrl: string | null;
  estado: EstadoPrenda;
  stockTotal: number;
  stockMinimo: number;
  nivel: NivelStock;
  tallas: { talla: string; stock: number }[];
}

export interface ActualizarStockRequest {
  tallas: { talla: string; stock: number }[];
  stockMinimo: number | null;
}

export interface AlertaInventario {
  id: number;
  prendaId: number;
  nombre: string;
  categoria: CategoriaPrenda;
  imagenUrl: string | null;
  nivel: Exclude<NivelStock, 'DISPONIBLE'>;
  stockActual: number;
  stockMinimo: number;
  diferencia: number;
  fechaEmision: string;
}
