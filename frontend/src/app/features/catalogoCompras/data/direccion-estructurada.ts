/** Tipos de vía de la nomenclatura urbana colombiana; deben coincidir con el backend. */
export const TIPOS_VIA = [
  'Calle', 'Carrera', 'Avenida', 'Avenida Calle', 'Avenida Carrera',
  'Diagonal', 'Transversal', 'Circular', 'Autopista', 'Variante', 'Kilómetro',
] as const;

/** Número de vía o de cruce: 45, 45A, 45 bis, 45 Sur. */
export const PATRON_NUMERO_VIA = /^\d{1,3}[A-Za-z]?( ?[Bb][Ii][Ss])?( ?(Norte|Sur|Este|Oeste))?$/;

/** Placa: de 1 a 3 dígitos. */
export const PATRON_PLACA = /^\d{1,3}$/;

export interface PartesDireccion {
  tipoVia: string;
  numeroVia: string;
  numeroCruce: string;
  placa: string;
}

/**
 * Compone el texto legible de la dirección, igual que el backend: «Calle 45 # 12 - 34».
 * @returns Texto compuesto, o cadena vacía si falta alguna parte.
 */
export function componerDireccion(
  tipoVia: string | null | undefined,
  numeroVia: string | null | undefined,
  numeroCruce: string | null | undefined,
  placa: string | null | undefined,
): string {
  const partes = [tipoVia, numeroVia, numeroCruce, placa].map((p) => (p ?? '').trim());
  if (partes.some((p) => p === '')) return '';
  return `${partes[0]} ${partes[1]} # ${partes[2]} - ${partes[3]}`;
}

/**
 * Separa una dirección escrita como «Calle 45A # 12 - 34» en sus campos estructurados.
 * @returns Las partes de la dirección, o `null` si el texto no cumple el formato admitido.
 */
export function parsearDireccion(direccion: string | null | undefined): PartesDireccion | null {
  const texto = (direccion ?? '').trim();
  const tipoVia = [...TIPOS_VIA]
    .sort((a, b) => b.length - a.length)
    .find((tipo) => texto.toLocaleLowerCase('es').startsWith(`${tipo.toLocaleLowerCase('es')} `));
  if (!tipoVia) return null;

  const partes = texto.slice(tipoVia.length).trim().match(/^(.+?)\s*#\s*(.+?)\s*-\s*(\d{1,3})$/);
  if (!partes) return null;

  const [, numeroVia, numeroCruce, placa] = partes;
  if (!PATRON_NUMERO_VIA.test(numeroVia) || !PATRON_NUMERO_VIA.test(numeroCruce) || !PATRON_PLACA.test(placa)) {
    return null;
  }

  return { tipoVia, numeroVia, numeroCruce, placa };
}
