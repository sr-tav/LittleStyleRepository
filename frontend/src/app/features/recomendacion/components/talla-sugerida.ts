import { Component, input } from '@angular/core';
import { RecomendacionItem} from '../models/recomendacion.models';

@Component({
  selector: 'app-talla-sugerida',
  standalone: true,
  template: `
    @if (item(); as r) {
      <span class="talla-badge">Talla Sugerida: {{ r.tallaSugerida }}</span>
      @if (r.riesgo === 'ADVERTENCIA') {
        <span class="alert-warn" role="alert">⚠ {{ (r.advertencias ?? []).join(', ') }}</span>
      }
      @if (!r.tieneStock) {
        <span class="alert-stock">Sin stock en esa talla</span>
      }
    }
  `
})
export class TallaSugerida { item = input.required<RecomendacionItem>(); }
