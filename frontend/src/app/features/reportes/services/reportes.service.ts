import { DOCUMENT } from '@angular/common';
import {
  HttpErrorResponse,
  HttpClient,
  HttpEvent,
  HttpEventType,
  HttpParams,
} from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, filter, from, map, mergeMap, throwError } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  AccionReporte,
  ArchivoDescargado,
  EstadoExportacion,
  FiltrosReporte,
  TipoReporte,
} from '../models/reporte.models';

const MENSAJE_GENERICO = 'No pudimos exportar el reporte. Inténtalo de nuevo.';

/** Cliente de exportación de reportes PDF (US-16). El alcance (tienda propia o plataforma) lo decide el token. */
@Injectable({ providedIn: 'root' })
export class ReportesService {
  private readonly http = inject(HttpClient);
  private readonly documento = inject(DOCUMENT);
  private readonly base = `${environment.apiUrl}/v1/reportes`;

  exportar(tipo: TipoReporte, filtros: FiltrosReporte): Observable<EstadoExportacion> {
    return this.http
      .get(`${this.base}/${tipo}`, {
        params: this.params(filtros),
        responseType: 'blob',
        observe: 'events',
        reportProgress: true,
      })
      .pipe(
        map((evento) => this.aEstado(evento, tipo)),
        filter((estado): estado is EstadoExportacion => estado !== null),
        catchError((err: unknown) =>
          from(this.mensajeDeError(err)).pipe(mergeMap((m) => throwError(() => new Error(m)))),
        ),
      );
  }

  /** Descarga el archivo, o lo abre en la ventana ya creada para que el usuario lo imprima desde el visor. */
  entregar(archivo: ArchivoDescargado, accion: AccionReporte, ventana: Window | null): void {
    const url = URL.createObjectURL(archivo.blob);
    if (accion === 'imprimir' && ventana && !ventana.closed) {
      ventana.location.href = url;
    } else {
      const enlace = this.documento.createElement('a');
      enlace.href = url;
      enlace.download = archivo.nombre;
      this.documento.body.appendChild(enlace);
      enlace.click();
      enlace.remove();
    }
    setTimeout(() => URL.revokeObjectURL(url), 60_000);
  }

  private aEstado(evento: HttpEvent<Blob>, tipo: TipoReporte): EstadoExportacion | null {
    switch (evento.type) {
      case HttpEventType.Sent:
        return { fase: 'generando' };
      case HttpEventType.ResponseHeader:
        return { fase: 'descargando', progreso: null };
      case HttpEventType.DownloadProgress:
        return {
          fase: 'descargando',
          progreso: evento.total ? Math.min(100, Math.round((evento.loaded * 100) / evento.total)) : null,
        };
      case HttpEventType.Response: {
        const blob = evento.body;
        if (!blob || blob.size === 0) {
          throw new Error('El servidor devolvió un archivo vacío. Inténtalo de nuevo.');
        }
        const nombre = this.nombreDe(evento.headers.get('Content-Disposition'), tipo);
        return { fase: 'listo', archivo: { blob, nombre } };
      }
      default:
        return null;
    }
  }

  private nombreDe(cabecera: string | null, tipo: TipoReporte): string {
    const utf8 = /filename\*=UTF-8''([^;]+)/i.exec(cabecera ?? '');
    if (utf8) return decodeURIComponent(utf8[1]);
    const simple = /filename="?([^";]+)"?/i.exec(cabecera ?? '');
    return simple?.[1] ?? `reporte-${tipo}.pdf`;
  }

  /** Con `responseType: 'blob'` el cuerpo de un error llega como Blob: hay que leerlo para ver el mensaje. */
  private async mensajeDeError(err: unknown): Promise<string> {
    if (!(err instanceof HttpErrorResponse)) {
      return err instanceof Error ? err.message : MENSAJE_GENERICO;
    }
    if (err.status === 0) return 'No hay conexión con el servidor. Revisa tu red e inténtalo de nuevo.';
    if (err.status === 401 || err.status === 403) return 'No tienes permiso para exportar este reporte.';
    if (err.error instanceof Blob && err.status < 500) {
      try {
        const cuerpo = JSON.parse(await err.error.text());
        if (typeof cuerpo?.mensaje === 'string') return cuerpo.mensaje;
      } catch {
        /* el cuerpo no era JSON */
      }
    }
    return err.status >= 500
      ? 'No pudimos generar el reporte. Inténtalo de nuevo en unos minutos.'
      : MENSAJE_GENERICO;
  }

  private params(f: FiltrosReporte): HttpParams {
    let params = new HttpParams().set('formato', 'PDF');
    if (f.startDate) params = params.set('startDate', f.startDate);
    if (f.endDate) params = params.set('endDate', f.endDate);
    if (f.categoria) params = params.set('categoria', f.categoria);
    return params;
  }
}
