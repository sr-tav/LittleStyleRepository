/**
 * Tabla de referencia de medidas corporales para tallas habituales de ropa infantil.
 *
 * Fuente y criterio: rangos aproximados construidos a partir de los estándares de crecimiento
 * de la OMS (medianas y dispersiones de estatura/peso/edad para niñez) y de las tablas de
 * confección habituales en el comercio de ropa para niños en Colombia (tallas por meses hasta
 * los 2 años, tallas numéricas entre los 2 y los 16 años, y tallas por letras equivalentes).
 * Son valores ORIENTATIVOS para ayudar al vendedor a completar cada variante; nunca sustituyen
 * los límites duraderos del backend (TallaRequest: estatura 40–200 cm, peso 2–120 kg,
 * edad 0–216 meses) ni su validación, que sigue mandando.
 */

/** Rangos corporales sugeridos para una talla de la tabla de referencia. */
export interface RangosTallaReferencia {
  estaturaMinCm: number;
  estaturaMaxCm: number;
  pesoMinKg: number;
  pesoMaxKg: number;
  edadMinMeses: number;
  edadMaxMeses: number;
}

/** Entrada de la tabla: nombre canónico, alias reconocibles y sus rangos sugeridos. */
export interface TallaReferencia extends RangosTallaReferencia {
  /** Nombre canónico que se muestra al vendedor (normalizado en mayúsculas). */
  nombre: string;
  /** Otras escrituras que apuntan a esta talla (por ejemplo «4T» o «0-3»). */
  aliases: readonly string[];
}

/** Límites corporales vigentes del backend (TallaRequest y ValidadorPrenda). */
export const LIMITES_TALLA_CORPORAL = {
  estaturaMinCm: 40,
  estaturaMaxCm: 200,
  pesoMinKg: 2,
  pesoMaxKg: 120,
  edadMinMeses: 0,
  edadMaxMeses: 216,
} as const;

/** Tabla de referencia ordenada por edad: bebé (meses), niñez (numéricas) y equivalentes por letra. */
export const TALLAS_REFERENCIA: readonly TallaReferencia[] = [
  { nombre: '0-3M', aliases: ['0-3'], estaturaMinCm: 45, estaturaMaxCm: 60, pesoMinKg: 3, pesoMaxKg: 6.5, edadMinMeses: 0, edadMaxMeses: 3 },
  { nombre: '3-6M', aliases: ['3-6'], estaturaMinCm: 60, estaturaMaxCm: 68, pesoMinKg: 6, pesoMaxKg: 8.5, edadMinMeses: 3, edadMaxMeses: 6 },
  { nombre: '6-9M', aliases: ['6-9'], estaturaMinCm: 68, estaturaMaxCm: 74, pesoMinKg: 8, pesoMaxKg: 10, edadMinMeses: 6, edadMaxMeses: 9 },
  { nombre: '9-12M', aliases: ['9-12'], estaturaMinCm: 74, estaturaMaxCm: 80, pesoMinKg: 9.5, pesoMaxKg: 12, edadMinMeses: 9, edadMaxMeses: 12 },
  { nombre: '12-18M', aliases: ['12-18'], estaturaMinCm: 80, estaturaMaxCm: 86, pesoMinKg: 11, pesoMaxKg: 14, edadMinMeses: 12, edadMaxMeses: 18 },
  { nombre: '18-24M', aliases: ['18-24'], estaturaMinCm: 86, estaturaMaxCm: 93, pesoMinKg: 12, pesoMaxKg: 15, edadMinMeses: 18, edadMaxMeses: 24 },
  { nombre: '2', aliases: ['2T', 'T2'], estaturaMinCm: 88, estaturaMaxCm: 98, pesoMinKg: 12, pesoMaxKg: 16, edadMinMeses: 24, edadMaxMeses: 36 },
  { nombre: '4', aliases: ['4T', 'T4'], estaturaMinCm: 98, estaturaMaxCm: 110, pesoMinKg: 15, pesoMaxKg: 19, edadMinMeses: 36, edadMaxMeses: 48 },
  { nombre: '6', aliases: ['6T', 'T6'], estaturaMinCm: 108, estaturaMaxCm: 124, pesoMinKg: 19, pesoMaxKg: 25, edadMinMeses: 48, edadMaxMeses: 72 },
  { nombre: '8', aliases: ['8T', 'T8'], estaturaMinCm: 122, estaturaMaxCm: 136, pesoMinKg: 25, pesoMaxKg: 31, edadMinMeses: 72, edadMaxMeses: 96 },
  { nombre: '10', aliases: ['T10'], estaturaMinCm: 134, estaturaMaxCm: 146, pesoMinKg: 31, pesoMaxKg: 38, edadMinMeses: 96, edadMaxMeses: 120 },
  { nombre: '12', aliases: ['T12'], estaturaMinCm: 144, estaturaMaxCm: 156, pesoMinKg: 37, pesoMaxKg: 45, edadMinMeses: 120, edadMaxMeses: 156 },
  { nombre: '14', aliases: ['T14'], estaturaMinCm: 154, estaturaMaxCm: 166, pesoMinKg: 45, pesoMaxKg: 53, edadMinMeses: 156, edadMaxMeses: 192 },
  { nombre: '16', aliases: ['T16'], estaturaMinCm: 162, estaturaMaxCm: 178, pesoMinKg: 52, pesoMaxKg: 64, edadMinMeses: 168, edadMaxMeses: 216 },
  { nombre: 'XS', aliases: [], estaturaMinCm: 100, estaturaMaxCm: 112, pesoMinKg: 16, pesoMaxKg: 21, edadMinMeses: 42, edadMaxMeses: 66 },
  { nombre: 'S', aliases: [], estaturaMinCm: 110, estaturaMaxCm: 124, pesoMinKg: 20, pesoMaxKg: 28, edadMinMeses: 60, edadMaxMeses: 96 },
  { nombre: 'M', aliases: [], estaturaMinCm: 122, estaturaMaxCm: 138, pesoMinKg: 27, pesoMaxKg: 35, edadMinMeses: 84, edadMaxMeses: 120 },
  { nombre: 'L', aliases: [], estaturaMinCm: 134, estaturaMaxCm: 150, pesoMinKg: 33, pesoMaxKg: 44, edadMinMeses: 108, edadMaxMeses: 156 },
  { nombre: 'XL', aliases: [], estaturaMinCm: 146, estaturaMaxCm: 166, pesoMinKg: 42, pesoMaxKg: 56, edadMinMeses: 132, edadMaxMeses: 216 },
];

/** Nombres canónicos de la tabla para el autocompletado del formulario. */
export const NOMBRES_TALLAS_REFERENCIA: readonly string[] = TALLAS_REFERENCIA.map((t) => t.nombre);

/** Índice de búsqueda: la talla y todos sus alias apuntan a la misma entrada. */
const INDICE_REFERENCIA = new Map<string, TallaReferencia>();
for (const entrada of TALLAS_REFERENCIA) {
  INDICE_REFERENCIA.set(entrada.nombre, entrada);
  for (const alias of entrada.aliases) {
    INDICE_REFERENCIA.set(alias, entrada);
  }
}

/**
 * Normaliza el nombre de una talla: recorta, compacta espacios y pasa a mayúsculas,
 * igual que hace el backend en ValidadorPrenda.normalizarTalla (« xs » → «XS»).
 * @param valor Texto escrito por el vendedor.
 * @returns Nombre normalizado, vacío si no se escribió nada.
 */
export function normalizarNombreTalla(valor: string | null | undefined): string {
  return (valor ?? '').trim().replace(/\s+/g, ' ').toUpperCase();
}

/**
 * Busca la entrada de la tabla que coincide con la talla escrita, tolerando mayúsculas,
 * espacios sobrantes y alias (« xs », «4T », «0 - 3M»).
 * @param valor Texto original de la talla.
 * @returns Entrada de la tabla o `null` si la talla no es reconocida.
 */
export function buscarTallaReferencia(valor: string | null | undefined): TallaReferencia | null {
  const nombre = normalizarNombreTalla(valor);
  if (!nombre) return null;
  const sinEspacios = nombre.replace(/\s+/g, '');
  return INDICE_REFERENCIA.get(sinEspacios) ?? INDICE_REFERENCIA.get(nombre) ?? null;
}

/**
 * Arma el texto de sugerencia para mostrar al vendedor («100–110 cm · 15–19 kg · 36–48 meses»).
 * @param referencia Entrada de la tabla de referencia.
 * @returns Resumen legible de los tres rangos.
 */
export function describirSugerencia(referencia: TallaReferencia): string {
  return `${referencia.estaturaMinCm}–${referencia.estaturaMaxCm} cm · `
    + `${referencia.pesoMinKg}–${referencia.pesoMaxKg} kg · `
    + `${referencia.edadMinMeses}–${referencia.edadMaxMeses} meses`;
}
