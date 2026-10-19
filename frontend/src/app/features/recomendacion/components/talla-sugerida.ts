import { Component, input } from '@angular/core';
import { RecomendacionItem} from '../models/recomendacion.models';

/**
 * Presenta la talla sugerida, las advertencias de seguridad y la disponibilidad de la talla.
 * @see RNF-11
 */
@Component({
  selector: 'app-talla-sugerida',
  standalone: true,
  template: `
  @if (item(); as r) {
    <div class="talla-sugerida">
      @if (r.tallaSugerida) {
        <span class="talla-badge">Talla sugerida: {{ r.tallaSugerida }}</span>
      }
      @if (r.riesgo === 'ADVERTENCIA') {
        <span class="alert-warn" role="alert">⚠ {{ r.advertencias.join(', ') }}</span>
      }
      @if (r.riesgo === 'EXCLUIDA') {
        <span class="alert-danger" role="alert">⛔ No compatible: {{ r.advertencias.join(', ') }}</span>
      }
      @if (r.tallaSugerida && !r.tieneStock) {
        <span class="alert-stock">Sin stock en esa talla</span>
      }
    </div>
  }
`,
})
export class TallaSugerida { item = input.required<RecomendacionItem>(); }
