import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../../../environments/environment';
import {
  FiltrosModeracion,
  Pagina,
  PrendaAdmin,
  PrendaAdminDetalle,
} from '../models/admin.models';

/**
 * Moderación de prendas publicadas por los vendedores (US-14): cola paginada,
 * detalle, aprobación, desactivación con motivo y restauración.
 * @see US-14
 */
@Injectable({ providedIn: 'root' })
export class ModeracionService {
  private readonly http = inject(HttpClient);
  private readonly base = `${environment.apiUrl}/admin/moderacion/prendas`;

  /**
   * Lista la cola de moderación aplicando solo los filtros definidos.
   * @param filtros Vendedor, estado de prenda, estado de moderación, texto y paginación.
   * @returns Flujo con la página de prendas que coincide con los filtros.
   * @see US-14
   */
  listar(filtros: FiltrosModeracion): Observable<Pagina<PrendaAdmin>> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '') {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get<Pagina<PrendaAdmin>>(this.base, { params });
  }

  /**
   * Consulta el detalle completo de una prenda en moderación.
   * @param id Identificador de la prenda.
   * @returns Flujo con la ficha, composición, tallas e imágenes.
   * @see US-14
   */
  detalle(id: number): Observable<PrendaAdminDetalle> {
    return this.http.get<PrendaAdminDetalle>(`${this.base}/${id}`);
  }

  /**
   * Aprueba la publicación de una prenda pendiente.
   * @param id Identificador de la prenda que se aprueba.
   * @returns Flujo con la prenda actualizada.
   * @see US-14
   */
  aprobar(id: number): Observable<PrendaAdmin> {
    return this.http.post<PrendaAdmin>(`${this.base}/${id}/aprobar`, {});
  }

  /**
   * Desactiva una prenda publicada con el motivo obligatorio.
   * @param id Identificador de la prenda que se desactiva.
   * @param motivo Razón de la desactivación (10 a 500 caracteres).
   * @returns Flujo con la prenda actualizada.
   * @see US-14
   */
  desactivar(id: number, motivo: string): Observable<PrendaAdmin> {
    return this.http.post<PrendaAdmin>(`${this.base}/${id}/desactivar`, { motivo });
  }

  /**
   * Restaura una prenda rechazada para que el vendedor pueda volver a editarla.
   * @param id Identificador de la prenda que se restaura.
   * @returns Flujo con la prenda actualizada.
   * @see US-14
   */
  restaurar(id: number): Observable<PrendaAdmin> {
    return this.http.post<PrendaAdmin>(`${this.base}/${id}/restaurar`, {});
  }
}
