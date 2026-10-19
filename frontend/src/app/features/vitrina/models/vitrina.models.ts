/** Resumen de producto presentado en los resultados del catálogo cliente. */
export interface VitrinaItem {
  id: number;
  nombre: string;
  categoria: string;
  genero: string | null;
  marca: string | null;
  precio: number;
  imagenUrl: string | null;
  disponible: boolean;
  tallasDisponibles: string[];
}

/** Talla con medidas declaradas y existencia disponible en la vitrina. */
export interface VitrinaTalla {
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  edadMinMeses?: number | null;
  edadMaxMeses?: number | null;
  stockDisponible: number;
  disponible: boolean;
}

/** Ficha pública de un producto con composición, imágenes y tallas. */
export interface VitrinaDetalle {
  id: number;
  nombre: string;
  categoria: string;
  genero: string | null;
  descripcion: string | null;
  instruccionesCuidado: string | null;
  marca: string | null;
  precio: number;
  imagenes: string[];
  disponible: boolean;
  colores: string[];
  estampado: string | null;
  composicion: { material: string; porcentaje: number }[];
  tallas: VitrinaTalla[];
  fechaActualizacion: string;
}

/** Página de resultados con contenido y metadatos de paginación. */
export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

/** Filtros opcionales que se envían al explorar el catálogo cliente.
 * @see US-09
 * @see US-07
 * @see UI-8
 */
export interface FiltrosVitrina {
  q?: string;
  categoria?: string;
  genero?: string;
  precioMin?: number;
  precioMax?: number;
  disponible?: boolean;
  material?: string;
  talla?: string;
  /** Edad en meses: coincide con tallas que declaran el rango (o no declaran). */
  edadMeses?: number;
  /**
   * Filtro "Solo mi talla sugerida": el frontend manda solo perfilId y el
   * servidor calcula los ids recomendados dentro (los ids ya no viajan en la
   * URL para no superar el límite de cabecera de Tomcat).
   */
  perfilId?: number;
  soloMiTalla?: boolean;
  pagina?: number;
  tamano?: number;
  orden?: string;
}
