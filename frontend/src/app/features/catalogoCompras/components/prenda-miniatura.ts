import { Component, input, signal } from '@angular/core';

/** Foto principal de la prenda; si no tiene o no carga, muestra un icono de prenda. */
@Component({
  selector: 'app-prenda-miniatura',
  template: `
    @if (url() && !fallo()) {
      <img class="prenda-thumb" [src]="url()" [alt]="'Foto de ' + nombre()" loading="lazy" (error)="fallo.set(true)" />
    } @else {
      <span class="prenda-thumb prenda-thumb-vacia" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M16 3 20 8l-2 2v11H6V10L4 8l4-5 4 3 4-3Z" />
        </svg>
      </span>
    }
  `,
})
export class PrendaMiniatura {
  readonly url = input<string | null>(null);
  readonly nombre = input('');
  protected readonly fallo = signal(false);
}
