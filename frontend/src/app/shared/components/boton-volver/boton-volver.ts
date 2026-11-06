import { Location } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-boton-volver',
  template: `
    <button class="boton-volver" type="button" (click)="volver()">
      <span aria-hidden="true">←</span>
      <span>Volver</span>
    </button>
  `,
})
export class BotonVolver {
  readonly fallbackUrl = input.required<string>();

  private readonly location = inject(Location);
  private readonly router = inject(Router);

  protected volver(): void {
    if (window.history.length > 1) {
      this.location.back();
    } else {
      void this.router.navigateByUrl(this.fallbackUrl());
    }
  }
}
