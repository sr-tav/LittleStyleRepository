import { CategoriaPrenda, Componente, MaterialTextil, NivelStock } from './prenda.models';

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

export const OPCIONES_CATEGORIAS = Object.entries(ETIQUETAS_CATEGORIAS).map(([valor, etiqueta]) => ({
  valor: valor as CategoriaPrenda,
  etiqueta,
}));

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

export const OPCIONES_MATERIALES = Object.entries(ETIQUETAS_MATERIALES).map(([valor, etiqueta]) => ({
  valor: valor as MaterialTextil,
  etiqueta,
}));

export const ETIQUETAS_NIVEL: Record<NivelStock, string> = {
  DISPONIBLE: 'Disponible',
  STOCK_BAJO: 'Stock bajo',
  AGOTADO: 'Agotado',
};

/** Mismos límites que el backend (PrendaRequest y AlmacenamientoProperties). */
export const LIMITES_PRENDA = {
  maxTallas: 15,
  maxMateriales: 6,
  maxImagenesPorPrenda: 8,
  maxImagenesPorCarga: 10,
  maxTamanoImagenMb: 5,
  tiposImagen: ['image/jpeg', 'image/png', 'image/webp'],
};

/** "80% Algodón, 20% Poliéster", como en UI-19. */
export function describirComposicion(composicion: Componente[]): string {
  return composicion.map((c) => `${c.porcentaje}% ${ETIQUETAS_MATERIALES[c.material]}`).join(', ');
}

const FORMATO_COP = new Intl.NumberFormat('es-CO', {
  style: 'currency',
  currency: 'COP',
  maximumFractionDigits: 0,
});

export function formatearPrecio(valor: number): string {
  return FORMATO_COP.format(valor);
}
