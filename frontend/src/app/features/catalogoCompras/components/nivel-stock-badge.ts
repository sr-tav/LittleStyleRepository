import { Component, computed, input } from '@angular/core';

import { ETIQUETAS_NIVEL } from '../models/prenda-opciones';
import { NivelStock } from '../models/prenda.models';

/** Etiqueta Disponible / Stock bajo / Agotado de UI-19, UI-21 y UI-22. */
@Component({
  selector: 'app-nivel-stock-badge',
  template: `<span class="stock-badge" [class]="'stock-badge stock-' + nivel()">{{ etiqueta() }}</span>`,
})
export class NivelStockBadge {
  readonly nivel = input.required<NivelStock>();
  protected readonly etiqueta = computed(() => ETIQUETAS_NIVEL[this.nivel()]);
}
