import { Pipe, PipeTransform } from '@angular/core';

import { ETIQUETAS_ESTAMPADOS } from '../../features/perfilesInfantiles/models/perfil-opciones';

/** Muestra el nombre legible de un estampado ("DIBUJOS_ANIMADOS" → "Dibujos animados"); vacío si no hay. */
@Pipe({ name: 'estampadoEtiqueta' })
export class EstampadoEtiquetaPipe implements PipeTransform {
  transform(estampado: string | null | undefined): string {
    return estampado
      ? (ETIQUETAS_ESTAMPADOS[estampado as keyof typeof ETIQUETAS_ESTAMPADOS] ?? estampado)
      : '';
  }
}
