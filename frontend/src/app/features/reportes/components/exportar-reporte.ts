import { DOCUMENT } from '@angular/common';
import { Component, OnDestroy, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import {
  AccionReporte,
  EstadoExportacion,
  OPCIONES_REPORTE,
  TipoReporte,
} from '../models/reporte.models';
import { ReportesService } from '../services/reportes.service';

/**
 * Exporta o imprime un reporte PDF según el período y la categoría que recibe del dashboard.
 * El reporte de inventario es una foto actual: ignora las fechas.
 */
@Component({
  selector: 'app-exportar-reporte',
  standalone: true,
  imports: [FormsModule],
  template: `
    <div class="export-box" aria-label="Exportar reporte">
      <div class="export-field">
        <label for="tipo-reporte">Reporte</label>
        <select
          id="tipo-reporte"
          [ngModel]="tipo()"
          (ngModelChange)="tipo.set($event)"
          [disabled]="ocupado()"
        >
          @for (op of opciones; track op.valor) {
            <option [value]="op.valor">{{ op.etiqueta }}</option>
          }
        </select>
      </div>

      <div class="export-actions">
        <button type="button" class="btn-primary" (click)="exportar('descargar')" [disabled]="ocupado()">
          Descargar PDF
        </button>
        <button type="button" class="btn-secondary" (click)="exportar('imprimir')" [disabled]="ocupado()">
          Imprimir
        </button>
      </div>

      @if (ocupado()) {
        <div class="progress-wrap" role="status" aria-live="polite">
          <div
            class="progress"
            role="progressbar"
            aria-valuemin="0"
            aria-valuemax="100"
            [attr.aria-valuenow]="progreso()"
            [class.indeterminada]="progreso() === null"
          >
            <div class="progress-fill" [style.width.%]="progreso() ?? 40"></div>
          </div>
          <span class="progress-label">{{ mensajeFase() }}</span>
          <button type="button" class="link" (click)="cancelar()">Cancelar</button>
        </div>
      }
      @if (error()) {
        <p class="msg error" role="alert">{{ error() }}</p>
      }
      @if (exito()) {
        <p class="msg ok" role="status">{{ exito() }}</p>
      }
    </div>
  `,
  styles: [
    `
      .export-box {
        display: grid;
        grid-template-columns: minmax(190px, 1fr) minmax(0, 2fr);
        align-items: end;
        gap: 1.25rem;
      }

      .export-field {
        display: flex;
        min-width: 0;
        flex-direction: column;
        gap: 0.35rem;
      }

      .export-field label {
        color: var(--ls-muted, #6b7280);
        font-size: 0.7rem;
        font-weight: 800;
        letter-spacing: 0.06em;
        text-transform: uppercase;
      }

      select {
        box-sizing: border-box;
        width: 100%;
        height: 42px;
        border: 1px solid #d6d3d1;
        border-radius: 10px;
        background: #fff;
        color: #14532d;
        padding: 0 0.75rem;
        font: inherit;
        font-size: 0.86rem;
        font-weight: 600;
      }

      select:focus {
        border-color: #15803d;
        box-shadow: 0 0 0 3px rgba(21, 128, 61, 0.14);
        outline: none;
      }

      .export-actions {
        display: grid;
        grid-template-columns: repeat(2, minmax(150px, 1fr));
        gap: 0.6rem;
      }

      button {
        box-sizing: border-box;
        width: 100%;
        min-height: 42px;
        border-radius: 10px;
        padding: 0 1.1rem;
        font: inherit;
        font-size: 0.85rem;
        font-weight: 700;
        cursor: pointer;
      }

      .btn-primary {
        border: 1px solid #15803d;
        background: #15803d;
        color: #fff;
      }

      .btn-secondary {
        border: 1px solid #15803d;
        background: #fff;
        color: #15803d;
      }

      button:disabled {
        cursor: not-allowed;
        opacity: 0.55;
      }

      .progress-wrap {
        display: flex;
        flex-basis: 100%;
        grid-column: 1 / -1;
        flex-wrap: wrap;
        align-items: center;
        gap: 0.75rem;
        color: #374151;
        font-size: 0.82rem;
      }

      .progress {
        flex: 1;
        min-width: 140px;
        max-width: 320px;
        height: 8px;
        border-radius: 999px;
        background: #e7e5e4;
        overflow: hidden;
      }

      .progress-fill {
        height: 100%;
        background: #15803d;
        transition: width 0.2s ease;
      }

      .progress.indeterminada .progress-fill {
        animation: deslizar 1.2s ease-in-out infinite alternate;
      }

      @keyframes deslizar {
        from {
          margin-left: 0;
        }
        to {
          margin-left: 60%;
        }
      }

      .link {
        min-height: auto;
        padding: 0;
        border: 0;
        background: none;
        color: #15803d;
        font-weight: 700;
        text-decoration: underline;
      }

      .msg {
        grid-column: 1 / -1;
        margin: 0;
        font-size: 0.82rem;
      }

      .msg.error {
        color: #b91c1c;
      }

      .msg.ok {
        color: #166534;
      }

      @media (max-width: 640px) {
        .export-box {
          grid-template-columns: 1fr;
          gap: 0.9rem;
        }

        .export-actions {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }
      }

      @media (max-width: 380px) {
        .export-actions {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class ExportarReporte implements OnDestroy {
  private readonly reportes = inject(ReportesService);
  private readonly ventanaGlobal = inject(DOCUMENT).defaultView;

  readonly startDate = input<string>('');
  readonly endDate = input<string>('');
  readonly categoria = input<string | null>(null);

  readonly opciones = OPCIONES_REPORTE;
  readonly tipo = signal<TipoReporte>('ventas');
  readonly fase = signal<'inactiva' | 'generando' | 'descargando'>('inactiva');
  readonly progreso = signal<number | null>(null);
  readonly error = signal('');
  readonly exito = signal('');

  readonly ocupado = computed(() => this.fase() !== 'inactiva');
  readonly mensajeFase = computed(() => {
    if (this.fase() === 'generando') return 'Generando el reporte…';
    const p = this.progreso();
    return p === null ? 'Descargando…' : `Descargando… ${p}%`;
  });

  private suscripcion?: Subscription;
  private ventanaImpresion: Window | null = null;

  exportar(accion: AccionReporte): void {
    if (this.ocupado()) return;
    this.error.set('');
    this.exito.set('');
    this.progreso.set(null);
    this.fase.set('generando');

    // La ventana se abre dentro del clic: si se abriera al terminar, el navegador bloquearía el popup.
    this.ventanaImpresion = accion === 'imprimir' ? (this.ventanaGlobal?.open('', '_blank') ?? null) : null;

    this.suscripcion = this.reportes
      .exportar(this.tipo(), {
        startDate: this.startDate() || undefined,
        endDate: this.endDate() || undefined,
        categoria: this.categoria() ?? undefined,
      })
      .subscribe({
        next: (estado) => this.aplicar(estado, accion),
        error: (err: Error) => {
          this.ventanaImpresion?.close();
          this.terminar();
          this.error.set(err.message);
        },
      });
  }

  cancelar(): void {
    this.suscripcion?.unsubscribe();
    this.ventanaImpresion?.close();
    this.terminar();
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
  }

  private aplicar(estado: EstadoExportacion, accion: AccionReporte): void {
    switch (estado.fase) {
      case 'generando':
        this.fase.set('generando');
        break;
      case 'descargando':
        this.fase.set('descargando');
        this.progreso.set(estado.progreso);
        break;
      case 'listo':
        this.reportes.entregar(estado.archivo, accion, this.ventanaImpresion);
        this.exito.set(
          accion === 'imprimir'
            ? 'Reporte listo: imprímelo desde la pestaña que se abrió.'
            : `Reporte descargado: ${estado.archivo.nombre}`,
        );
        this.terminar();
        break;
    }
  }

  private terminar(): void {
    this.fase.set('inactiva');
    this.progreso.set(null);
    this.ventanaImpresion = null;
  }
}
