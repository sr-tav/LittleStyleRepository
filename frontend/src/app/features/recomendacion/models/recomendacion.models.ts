export type NivelRiesgo = 'APTA' | 'ADVERTENCIA' | 'EXCLUIDA';
export interface RecomendacionItem {
  prendaId: number; nombre: string; precio: number | null; tallaSugerida: string;
  /** Compatibilidad 0-100: solo talla y seguridad (lo que mide RNF-11). */
  puntaje: number;
  /** Afinidad de estilo 0-10 aparte: solo reordena, nunca compensa avisos. */
  afinidad: number;
  riesgo: NivelRiesgo; advertencias: string[]; tieneStock: boolean;
  imagenUrl: string | null;
}
