import { Pipe, PipeTransform } from '@angular/core';

import { ETIQUETAS_COLORES } from '../../features/perfilesInfantiles/models/perfil-opciones';

/** Muestra el nombre legible de un color ("ROJO" → "Rojo"); vacío si no hay color. */
@Pipe({ name: 'colorEtiqueta' })
export class ColorEtiquetaPipe implements PipeTransform {
  transform(color: string | null | undefined): string {
    return color ? (ETIQUETAS_COLORES[color as keyof typeof ETIQUETAS_COLORES] ?? color) : '';
  }
}
