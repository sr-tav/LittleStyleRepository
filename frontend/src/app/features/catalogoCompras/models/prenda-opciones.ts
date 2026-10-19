import { CategoriaPrenda, Componente, GeneroPrenda, MaterialTextil, NivelStock } from './prenda.models';

/** Etiquetas legibles para cada categoría de prenda. */
export const ETIQUETAS_CATEGORIAS: Record<CategoriaPrenda, string> = {
  VESTIDOS: 'Vestidos',
  PANTALONES: 'Pantalones',
  SHORTS: 'Shorts',
  FALDAS: 'Faldas',
  CAMISAS: 'Camisas',
  CAMISETAS: 'Camisetas',
  BUZOS: 'Buzos',
  CHAQUETAS: 'Chaquetas',
  CONJUNTOS: 'Conjuntos',
  PIJAMAS: 'Pijamas',
  ACCESORIOS: 'Accesorios',
};

/** Opciones de categoría para controles de selección. */
export const OPCIONES_CATEGORIAS = Object.entries(ETIQUETAS_CATEGORIAS).map(([valor, etiqueta]) => ({
  valor: valor as CategoriaPrenda,
  etiqueta,
}));

/** Etiquetas legibles para los materiales textiles. */
export const ETIQUETAS_MATERIALES: Record<MaterialTextil, string> = {
  ALGODON: 'Algodón',
  POLIESTER: 'Poliéster',
  NYLON: 'Nylon',
  ACRILICO: 'Acrílico',
  ELASTANO: 'Elastano',
  LANA: 'Lana',
  VISCOSA: 'Viscosa',
  CUERO: 'Cuero',
  LATEX: 'Látex',
  OTRO: 'Otro',
};

/** Opciones de material para controles de selección. */
export const OPCIONES_MATERIALES = Object.entries(ETIQUETAS_MATERIALES).map(([valor, etiqueta]) => ({
  valor: valor as MaterialTextil,
  etiqueta,
}));

/** Etiquetas para los niveles de stock. */
export const ETIQUETAS_NIVEL: Record<NivelStock, string> = {
  DISPONIBLE: 'Disponible',
  STOCK_BAJO: 'Stock bajo',
  AGOTADO: 'Agotado',
};

/** Etiquetas legibles para los géneros de prenda. */
export const ETIQUETAS_GENERO: Record<GeneroPrenda, string> = {
  NINO: 'Niños',
  NINA: 'Niñas',
  UNISEX: 'Unisex',
};

/** Opciones de género para controles de selección. */
export const OPCIONES_GENERO = Object.entries(ETIQUETAS_GENERO).map(([valor, etiqueta]) => ({
  valor: valor as GeneroPrenda,
  etiqueta,
}));

/** Mismos límites que el backend (PrendaRequest y AlmacenamientoProperties). */
export const LIMITES_PRENDA = {
  maxTallas: 15,
  maxMateriales: 6,
  maxImagenesPorPrenda: 8,
  maxImagenesPorCarga: 10,
  maxTamanoImagenMb: 5,
  tiposImagen: ['image/jpeg', 'image/png', 'image/webp'],
};

/**
 * "80% Algodón, 20% Poliéster", como en UI-19.
 * @param composicion Componentes textiles y sus porcentajes.
 * @returns Composición convertida en una lista legible.
 * @see UI-19
 */
export function describirComposicion(composicion: Componente[]): string {
  return composicion.map((c) => `${c.porcentaje}% ${ETIQUETAS_MATERIALES[c.material]}`).join(', ');
}

const FORMATO_COP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

/**
 * Formatea un importe con la configuración monetaria de Colombia.
 * @param valor Precio expresado en pesos colombianos.
 * @returns Precio con formato monetario de Colombia.
 */
export function formatearPrecio(valor: number): string {
  return FORMATO_COP.format(valor);
}
