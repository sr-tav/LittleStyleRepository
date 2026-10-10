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

export interface VitrinaTalla {
  talla: string;
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  disponible: boolean;
}

export interface VitrinaDetalle {
  id: number;
  nombre: string;
  categoria: string;
  genero: string | null;
  descripcion: string | null;
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

export interface Pagina<T> {
  contenido: T[];
  pagina: number;
  tamano: number;
  totalElementos: number;
  totalPaginas: number;
}

export interface FiltrosVitrina {
  q?: string;
  categoria?: string;
  genero?: string;
  precioMin?: number;
  precioMax?: number;
  disponible?: boolean;
  material?: string;
  talla?: string;
  pagina?: number;
  tamano?: number;
  orden?: string;
}
