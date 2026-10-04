/**
 * Calcula una talla orientativa por edad, provisional hasta US-07.
 */
export function tallaProvisional(edadAnios: number): number {
  if (!Number.isFinite(edadAnios) || edadAnios < 0) return 0;
  return Math.min(16, Math.floor(edadAnios / 2) * 2);
}
