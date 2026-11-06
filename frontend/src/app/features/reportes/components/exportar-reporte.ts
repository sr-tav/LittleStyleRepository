import { DOCUMENT } from '@angular/common';
import { Component, OnDestroy, computed, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Subscription } from 'rxjs';

import {
  AccionReporte,
  EstadoExportacion,
  FormatoReporte,
  OPCIONES_FORMATO,
  OPCIONES_REPORTE,
  TipoReporte,
} from '../models/reporte.models';
import { ErrorReporte, ReportesService } from '../services/reportes.service';

/**
 * Exporta o imprime un reporte según el período, la categoría, el tipo y el formato elegidos.
 * El reporte de inventario es una foto actual: ignora las fechas. La impresión solo aplica a PDF.
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

      <div class="export-field">
        <label for="formato-reporte">Formato</label>
        <select
          id="formato-reporte"
          [ngModel]="formato()"
          (ngModelChange)="formato.set($event)"
          [disabled]="ocupado()"
        >
          @for (op of formatos; track op.valor) {
            <option [value]="op.valor">{{ op.etiqueta }}</option>
          }
        </select>
      </div>

      <div class="export-actions">
        <button type="button" class="btn-primary" (click)="exportar('descargar')" [disabled]="ocupado()">
          Descargar {{ formato() }}
        </button>
        <button
          type="button"
          class="btn-secondary"
          (click)="exportar('imprimir')"
          [disabled]="ocupado() || !puedeImprimir()"
          [attr.title]="puedeImprimir() ? null : 'La impresión está disponible solo en PDF'"
        >
          Imprimir
        </button>
      </div>

      @if (!puedeImprimir()) {
        <p class="hint">Excel y CSV se descargan para analizarlos; la impresión está disponible solo en PDF.</p>
      }

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
        grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) minmax(0, 1.4fr);
        align-items: end;
        gap: 1rem;
        width: 100%;
        min-width: 0;
        box-sizing: border-box;
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
        grid-template-columns: repeat(2, minmax(0, 1fr));
        min-width: 0;
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

      .hint {
        grid-column: 1 / -1;
        margin: 0;
        color: #6b7280;
        font-size: 0.78rem;
      }

      @media (max-width: 900px) {
        .export-box {
          grid-template-columns: minmax(0, 1fr);
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
  readonly formatos = OPCIONES_FORMATO;
  readonly tipo = signal<TipoReporte>('ventas');
  readonly formato = signal<FormatoReporte>('PDF');
  readonly fase = signal<'inactiva' | 'generando' | 'descargando'>('inactiva');
  readonly progreso = signal<number | null>(null);
  readonly error = signal('');
  readonly exito = signal('');

  readonly ocupado = computed(() => this.fase() !== 'inactiva');
  readonly puedeImprimir = computed(() => this.formato() === 'PDF');
  readonly mensajeFase = computed(() => {
    if (this.fase() === 'generando') return 'Generando el reporte…';
    const p = this.progreso();
    return p === null ? 'Descargando…' : `Descargando… ${p}%`;
  });

  private suscripcion?: Subscription;
  private ventanaImpresion: Window | null = null;

  exportar(accion: AccionReporte): void {
    if (this.ocupado()) return;
    if (accion === 'imprimir' && !this.puedeImprimir()) {
      this.error.set('Solo puedes imprimir reportes en PDF. Elige el formato PDF o descarga el archivo.');
      return;
    }
    this.error.set('');
    this.exito.set('');
    this.progreso.set(null);
    this.fase.set('generando');

    // La ventana se abre dentro del clic: si se abriera al terminar, el navegador bloquearía el popup.
    this.ventanaImpresion = accion === 'imprimir' ? (this.ventanaGlobal?.open('', '_blank') ?? null) : null;

    this.suscripcion = this.reportes
      .exportar(
        this.tipo(),
        {
          startDate: this.startDate() || undefined,
          endDate: this.endDate() || undefined,
          categoria: this.categoria() ?? undefined,
        },
        this.formato(),
      )
      .subscribe({
        next: (estado) => void this.aplicar(estado, accion),
        error: (err: Error) => {
          this.ventanaImpresion?.close();
          this.terminar();
          this.error.set(err.message);
          const resultado = err instanceof ErrorReporte ? err.resultado : 'ERROR_CLIENTE';
          this.reportes.reportarExportacion(this.tipo(), this.formato(), resultado, err.message);
        },
      });
  }

  cancelar(): void {
    if (!this.ocupado()) return;
    this.suscripcion?.unsubscribe();
    this.ventanaImpresion?.close();
    this.terminar();
    this.reportes.reportarExportacion(
      this.tipo(),
      this.formato(),
      'CANCELADO',
      'El usuario canceló la exportación',
    );
  }

  ngOnDestroy(): void {
    this.suscripcion?.unsubscribe();
    this.ventanaImpresion?.close();
    this.ventanaImpresion = null;
  }

  private async aplicar(estado: EstadoExportacion, accion: AccionReporte): Promise<void> {
    switch (estado.fase) {
      case 'generando':
        this.fase.set('generando');
        break;
      case 'descargando':
        this.fase.set('descargando');
        this.progreso.set(estado.progreso);
        break;
      case 'listo': {
        const ventana = this.ventanaImpresion;
        this.terminar();
        try {
          if (accion === 'imprimir') {
            const resultado = await this.reportes.imprimir(estado.archivo, ventana);
            if (resultado === 'sin-ventana') {
              this.reportes.reportarExportacion(
                this.tipo(),
                this.formato(),
                'ERROR_CLIENTE',
                'No se pudo abrir la impresión',
              );
            } else {
              // El motivo deja rastro de qué significa "OK": archivo validado y entregado, no
              // confirmación de guardado en disco (el navegador no expone ese dato).
              this.reportes.reportarExportacion(
                this.tipo(),
                this.formato(),
                'OK',
                'archivo_validado_impreso',
              );
            }
            this.exito.set(
              resultado === 'impreso'
                ? 'Reporte listo: abrimos el diálogo de impresión del navegador.'
                : resultado === 'pestana'
                  ? 'Reporte listo: imprímelo desde la pestaña que se abrió.'
                  : 'No pudimos abrir la impresión. Descarga el PDF e imprímelo desde tu equipo.',
            );
          } else {
            this.reportes.descargar(estado.archivo);
            this.reportes.reportarExportacion(
              this.tipo(),
              this.formato(),
              'OK',
              'archivo_validado_entregado',
            );
            this.exito.set(`Reporte ${this.formato()} descargado: ${estado.archivo.nombre}`);
          }
        } catch {
          this.error.set(
            'No pudimos guardar el archivo. Revisa los permisos de descarga e inténtalo de nuevo.',
          );
          this.reportes.reportarExportacion(
            this.tipo(),
            this.formato(),
            'ERROR_CLIENTE',
            'El navegador no pudo entregar el archivo',
          );
        }
        break;
      }
    }
  }

  private terminar(): void {
    this.fase.set('inactiva');
    this.progreso.set(null);
    this.ventanaImpresion = null;
  }
}
