/**
 * Calcula una talla orientativa por edad, provisional hasta US-07.
 * @param edadAnios Edad expresada en años.
 * @returns Talla par estimada entre 0 y 16; devuelve 0 para edades no válidas.
 * @see US-07
 */
export function tallaProvisional(edadAnios: number): number {
  if (!Number.isFinite(edadAnios) || edadAnios < 0) return 0;
  return Math.min(16, Math.floor(edadAnios / 2) * 2);
}
