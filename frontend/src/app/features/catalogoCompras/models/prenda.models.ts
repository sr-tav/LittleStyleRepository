import { ColorPreferido, Estampado } from '../../perfilesInfantiles/models/perfil-infantil.models';

/** Categorías aceptadas para clasificar prendas. */
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

/** Materiales textiles reconocidos en la composición de una prenda. */
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

/** Estados de publicación de una prenda. */
export type EstadoPrenda = 'ACTIVA' | 'INACTIVA';
/** Estados de moderación posterior asignados por el administrador. */
export type EstadoModeracion = 'SIN_REVISAR' | 'APROBADA' | 'DESACTIVADA';
/** Clasificación derivada del nivel de stock. */
export type NivelStock = 'DISPONIBLE' | 'STOCK_BAJO' | 'AGOTADO';
/** Opciones de género asociables a una prenda. */
export type GeneroPrenda = 'NINO' | 'NINA' | 'UNISEX';

/** Material textil y porcentaje que integran la composición de una prenda. */
export interface Componente {
  material: MaterialTextil;
  porcentaje: number;
}

/** Talla con rangos de medidas y stock asociado. */
export interface Talla {
  id: number;
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  /** Edad en meses que cubre la talla; nula si no se declara. */
  edadMinMeses?: number | null;
  edadMaxMeses?: number | null;
  stock: number;
  stockMinimo?: number | null;
}

/** Imagen asociada a una prenda y su orden de presentación. */
export interface ImagenPrenda {
  id: number;
  url: string;
  orden: number;
}

/** Ficha de producto con atributos textiles, tallas, stock e imágenes. */
export interface Prenda {
  id: number;
  nombre: string;
  categoria: CategoriaPrenda;
  genero: GeneroPrenda | null;
  descripcion: string | null;
  instruccionesCuidado: string | null;
  marca: string | null;
  precio: number;
  estado: EstadoPrenda;
  estadoModeracion?: EstadoModeracion | null;
  motivoModeracion?: string | null;
  fechaModeracion?: string | null;
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

/** Datos de tallas enviados al crear o actualizar una prenda. */
export interface TallaRequest {
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  /** Edad en meses que cubre la talla; nula si no se declara. */
  edadMinMeses?: number | null;
  edadMaxMeses?: number | null;
  /** Solo se usa para tallas nuevas; el stock de las existentes se cambia en el inventario. */
  stock: number | null;
  /** Mínimo de alerta de esta talla; nulo si no alerta. */
  stockMinimo?: number | null;
}

/** Datos de producto enviados para crear o actualizar una prenda. */
export interface PrendaRequest {
  nombre: string;
  categoria: CategoriaPrenda;
  genero: GeneroPrenda | null;
  descripcion: string | null;
  instruccionesCuidado: string | null;
  marca: string | null;
  precio: number;
  colores: ColorPreferido[];
  estampado: Estampado | null;
  tintesSinteticos: boolean;
  brochesMetalicos: boolean;
  contieneNiquel: boolean;
  tratamientoFormaldehido: boolean;
  composicion: Componente[];
  tallas: TallaRequest[];
}

/** Resumen de stock de una prenda para el inventario. */
export interface InventarioItem {
  prendaId: number;
  nombre: string;
  categoria: CategoriaPrenda;
  imagenUrl: string | null;
  estado: EstadoPrenda;
  stockTotal: number;
  nivel: NivelStock;
  tallas: { talla: string; stock: number; stockMinimo: number | null }[];
}

/** Existencias y mínimos solicitados para actualizar el inventario. */
export interface ActualizarStockRequest {
  tallas: { talla: string; stock: number; stockMinimo: number | null }[];
}

/** Alerta de inventario emitida para una prenda o talla. */
export interface AlertaInventario {
  id: number;
  prendaId: number;
  nombre: string;
  categoria: CategoriaPrenda;
  imagenUrl: string | null;
  talla: string | null;
  nivel: Exclude<NivelStock, 'DISPONIBLE'>;
  stockActual: number;
  stockMinimo: number;
  diferencia: number;
  fechaEmision: string;
}
