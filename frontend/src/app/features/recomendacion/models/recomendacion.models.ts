export type NivelRiesgo = 'APTA' | 'ADVERTENCIA' | 'EXCLUIDA';
export interface RecomendacionItem {
  prendaId: number; nombre: string; precio: number | null; tallaSugerida: string;
  puntaje: number; riesgo: NivelRiesgo; advertencias: string[]; tieneStock: boolean;
  imagenUrl: string | null;
}
